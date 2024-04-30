package com.example.colorsensor

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.hardware.camera2.params.MeteringRectangle
import android.media.ImageReader
import android.os.*
import android.provider.Settings
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore
import kotlin.math.abs


class CaptureImageActivity : AppCompatActivity() {

    // Camera variables
    private lateinit var cameraManager: CameraManager
    private lateinit var capturedImageBytes: ByteArray
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private lateinit var capReq: CaptureRequest.Builder
    private lateinit var handler: Handler
    private lateinit var textureView: TextureView
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private var isFlashOn: Boolean = false
    private var cameraOpenCloseLock = Semaphore(1)
    private lateinit var deviceId: String
    private lateinit var sharedPreferences: SharedPreferences
    private val universalResolutionWidth = 1080
    private val universalResolutionHeight = 1920

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_image)

        val chatheadImageView = findViewById<ImageView>(R.id.ChatheadImageView)
        chatheadImageView.bringToFront()
        chatheadImageView.invalidate() // Ensure the view is redrawn


        // Get the device ID or token during activity creation
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("PhoneID", "Phone ID: $deviceId")

        openBackgroundThread()
        setupCamera()

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Show lighting condition dialog based on SharedPreferences
        val showDialog = sharedPreferences.getBoolean("showLightingCondition", true)
        if (showDialog) {
            showLightingConditionDialog {}
        }
    }

    override fun onResume() {
        super.onResume()
        openBackgroundThread()
        updateFlashBehavior()
        openCameraPreview()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        closeBackgroundThread()
    }

    // Function to close the camera session and device
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            cameraCaptureSession.close()
            cameraDevice.close()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    // Function to setup camera components if permissions are granted
    private fun setupCamera() {
        if (allPermissionsGranted()) {
            setupCameraManager()
            setupCameraPreview()
            setupCaptureImage()
            setupFlashToggle()
            setupHistoryButton()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    // Function to check if all necessary permissions are granted
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        applicationContext,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
            } else {
                // Handle denied permissions
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to setup camera manager
    private fun setupCameraManager() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCameraPreview() {
        textureView = findViewById(R.id.CameraTextureView)
        textureView.surfaceTextureListener = surfaceTextureListener

        // Add touch listener to the TextureView
        textureView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Calculate focus area from touch event coordinates
                val focusArea = calculateFocusArea(event.x, event.y)
                // Update autofocus request to focus on the specified area
                Log.d("TouchPosition", "Touched at: (${event.x}, ${event.y})")
                updateFocusArea(focusArea)
            }
            true
        }

        findViewById<ImageView>(R.id.CaptureImageView).setOnClickListener {
            captureImage()
        }
    }

    // Function to calculate focus area from touch event coordinates
    private fun calculateFocusArea(x: Float, y: Float): Rect {
        val focusSize = 100 // Adjust this size as needed
        val halfSize = focusSize / 2
        val previewRect = Rect(0, 0, textureView.width, textureView.height)
        val focusX = clamp(x.toInt() - halfSize, previewRect.left, previewRect.right - focusSize)
        val focusY = clamp(y.toInt() - halfSize, previewRect.top, previewRect.bottom - focusSize)
        val focusArea = Rect(focusX, focusY, focusX + focusSize, focusY + focusSize)
        Log.d("TouchPosition", "Focus area: $focusArea")
        return focusArea
    }

    // Function to clamp a value within a range
    private fun clamp(value: Int, min: Int, max: Int): Int {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    // Function to update autofocus request to focus on the specified area
    private fun updateFocusArea(focusArea: Rect) {
        try {
            capReq.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(MeteringRectangle(focusArea, MeteringRectangle.METERING_WEIGHT_MAX)))
            capReq.set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(MeteringRectangle(focusArea, MeteringRectangle.METERING_WEIGHT_MAX)))
            capReq.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
            capReq.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
            cameraCaptureSession.capture(capReq.build(), null, backgroundHandler)
            Log.d("TouchPosition", "Focus updated to: $focusArea")
        } catch (e: CameraAccessException) {
            handleCameraAccessException(e)
        }
    }

    // Function to capture an image
    private fun captureImage() {
        val rotation = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 90
        }

        capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(imageReader.surface)
            // autofocus mode control
            set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
            set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.JPEG_ORIENTATION, rotation)
        }
        cameraCaptureSession.capture(capReq.build(), null, null)
    }

    private fun setupCaptureImage() {
        imageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader?.acquireLatestImage()
            image?.let { img ->
                val width = img.width // Get width of the image
                val height = img.height // Get height of the image

                Log.d("ImageDimensions", "Displayed image dimension: $width x $height")

                val buffer = img.planes[0].buffer
                capturedImageBytes = ByteArray(buffer.remaining())
                buffer.get(capturedImageBytes)
                img.close()

                val imageFile = saveImageToTempFile(capturedImageBytes)

                // Reset flash mode to off after capturing the image
                isFlashOn = false
                updateFlashMode()

                val intent = Intent(this@CaptureImageActivity, ColorPickerActivity::class.java)
                intent.putExtra("capturedImagePath", imageFile.absolutePath)
                intent.putExtra("phoneId", deviceId)  // Pass the deviceId here
                startActivity(intent)
            }
        }, backgroundHandler)
    }

    // Function to update flash behavior based on current state
    private fun updateFlashBehavior() {
        val flashImageView = findViewById<ImageView>(R.id.CameraFlashImageView)
        flashImageView.setImageResource(if (isFlashOn) R.drawable.flash else R.drawable.flash_off)
    }

    // Function to setup flash toggle button
    private fun setupFlashToggle() {
        val flashImageView = findViewById<ImageView>(R.id.CameraFlashImageView)
        flashImageView.setOnClickListener {
            isFlashOn = !isFlashOn
            flashImageView.setImageResource(if (isFlashOn) R.drawable.flash else R.drawable.flash_off)
            updateFlashMode()
        }
    }

    // Function to update flash mode based on current state
    private fun updateFlashMode() {
        try {
            capReq.set(
                CaptureRequest.FLASH_MODE,
                if (isFlashOn) CameraMetadata.FLASH_MODE_TORCH else CameraMetadata.FLASH_MODE_OFF
            )
            cameraCaptureSession.setRepeatingRequest(capReq.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            handleCameraAccessException(e)
        }
    }

    // Function to setup history button
    private fun setupHistoryButton() {
        findViewById<ImageView>(R.id.HistoryImageView).setOnClickListener {
            val intent = Intent(this@CaptureImageActivity, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    // Function open camera preview
    private fun openCameraPreview() {
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    // Modify openCamera function to calculate appropriate preview size with aspect ratio
    private fun openCamera() {
        try {
            val cameraId = getCameraId(cameraFacing)
                ?: // Handle no camera available scenario
                return

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val previewSize = chooseOptimalSize(
                map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray(),
                universalResolutionWidth, universalResolutionHeight
            )

            textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permission
                return
            }
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            handleCameraAccessException(e)
        } catch (e: IllegalStateException) {
            handleIllegalStateException(e)
        }
    }

    // Function to choose optimal size with given aspect ratio
    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val desiredRatio = height.toDouble() / width.toDouble()
        var minDiff = Double.MAX_VALUE
        var optimalSize = choices[0]

        for (size in choices) {
            val ratio = size.width.toDouble() / size.height.toDouble()
            if (abs(ratio - desiredRatio) < minDiff) {
                minDiff = abs(ratio - desiredRatio)
                optimalSize = size
            }
        }

        return optimalSize
    }

    // Function for camera state callback
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            // Handle camera error
        }
    }

    // Function to start camera preview
    private fun startPreview() {
        try {
            val surface = Surface(textureView.surfaceTexture)
            capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            capReq.addTarget(surface)

            // Set auto exposure parameters
            setAutoExposureParameters(capReq)

            val surfaceList = listOf(surface, imageReader.surface)
            cameraDevice.createCaptureSession(surfaceList, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    cameraCaptureSession.setRepeatingRequest(capReq.build(), null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    // handle configuration failure
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            handleCameraAccessException(e)
        }
    }

    // Function to set auto exposure parameters
    private fun setAutoExposureParameters(requestBuilder: CaptureRequest.Builder) {
        requestBuilder.set(
            CaptureRequest.CONTROL_MODE,
            CameraMetadata.CONTROL_MODE_AUTO
        )
        requestBuilder.set(
            CaptureRequest.CONTROL_AE_MODE,
            CameraMetadata.CONTROL_AE_MODE_ON
        )
    }

    // Texture view surface texture listener
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    // Function to save image to raw file
    private fun saveImageToTempFile(data: ByteArray): File {
        val imageFile = File.createTempFile("capturedImage", ".jpg")
        FileOutputStream(imageFile).use { it.write(data) }
        return imageFile
    }

    // Function to get camera ID based on facing
    private fun getCameraId(facing: Int): String? {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection == facing) {
                return cameraId
            }
        }
        return cameraIds.firstOrNull()
    }

    // Function to open background thread for camera operations
    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("backgroundThread").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    // Function to close background thread
    private fun closeBackgroundThread() {
        backgroundThread.quitSafely()
        try {
            backgroundThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    // Function to handle camera access exception
    private fun handleCameraAccessException(e: CameraAccessException) {
        e.printStackTrace()
        // Handle camera access exception
    }

    // Function to handle illegal state exception
    private fun handleIllegalStateException(e: IllegalStateException) {
        e.printStackTrace()
        // Handle illegal state exception
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    // Function to show lighting condition dialog
    private fun showLightingConditionDialog(callback: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.lighting_conditions_dialog, null)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        val understandCheckBox = dialogView.findViewById<CheckBox>(R.id.understandCheckBox)
        val doNotShowAgainCheckBox = dialogView.findViewById<CheckBox>(R.id.doNotShowAgainCheckBox)

        // Set a listener for the checkbox
        doNotShowAgainCheckBox.setOnCheckedChangeListener { _, isChecked ->
            // Update SharedPreferences when checkbox state changes
            sharedPreferences.edit().putBoolean("showLightingCondition", !isChecked).apply()
        }

        understandCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If the checkbox is checked, dismiss the dialog
                alertDialog.dismiss()
                // Call the callback function (if provided)
                callback.invoke()
            }
        }

        alertDialog.show()
    }
}