package com.example.colorsensor
import android.Manifest
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.ExifInterface
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import java.io.ByteArrayOutputStream
import java.io.File


class CaptureImageActivity : AppCompatActivity() {

    private lateinit var handlerThread: HandlerThread
    private lateinit var cameraManager: CameraManager
    private lateinit var capturedImageBytes: ByteArray
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    lateinit var capReq: CaptureRequest.Builder
    lateinit var handler: Handler
    lateinit var textureView: TextureView
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice
    lateinit var imageReader: ImageReader
    private var isFlashOn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture_image)
        getPermissions()
        cameraFlash()
        cameraFlip()
        cameraPreview()
        captureImage()
        openHistoryActivity()
    }

    private fun getPermissions() {
        val permissionList = mutableListOf<String>()
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.CAMERA
        )
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permissionList.add(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissionList.size > 0) {
            requestPermissions(permissionList.toTypedArray(), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        grantResults.forEach {
            if (it != PackageManager.PERMISSION_GRANTED) {
                getPermissions()
            }
        }
    }

    private fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
        val targetRatio = height.toDouble() / width
        var optimalSize = choices[0]
        var maxPixels = 0

        for (size in choices) {
            val ratio = size.width.toDouble() / size.height
            val pixels = size.width * size.height

            if (pixels > maxPixels && Math.abs(ratio - targetRatio) < 0.3) {
                optimalSize = size
                maxPixels = pixels
            }
        }

        return optimalSize
    }

    private fun closeCamera() {
        if (::cameraCaptureSession.isInitialized) {
            cameraCaptureSession.close()
        }
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val cameraId = getCameraId(cameraFacing)

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSize = chooseOptimalSize(
            map?.getOutputSizes(SurfaceTexture::class.java) ?: emptyArray(),
            textureView.width,
            textureView.height
        )

        textureView.surfaceTexture?.setDefaultBufferSize(previewSize.width, previewSize.height)

        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(pO: CameraDevice) {
                    cameraDevice = pO
                    capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    var surface = Surface(textureView.surfaceTexture)
                    capReq.addTarget(surface)

                    val surfaceList = listOf(surface, imageReader.surface)
                    cameraDevice.createCaptureSession(surfaceList, object :
                        CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            cameraCaptureSession = session
                            cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            // handle configuration failure
                        }
                    }, handler)
                }

                override fun onDisconnected(camera: CameraDevice) {
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera error: $error")
                }
            },
            handler
        )
    }

    private fun getCameraId(facing: Int): String {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection == facing) {
                return cameraId
            }
        }
        //default to the first camera if no match is found
        return cameraIds[0]
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            closeCamera()
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            return false
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }

    private fun cameraPreview() {
        textureView = findViewById(R.id.CameraTextureView)
        textureView.surfaceTextureListener = surfaceTextureListener

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        findViewById<ImageView>(R.id.CaptureImageView).setOnClickListener {
            capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)

                //autofocus mode control
                set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            }
            cameraCaptureSession.capture(capReq.build(), null, null)
        }
    }

    private fun cameraFlash() {
        val flashImageView = findViewById<ImageView>(R.id.CameraFlashImageView)

        // Set initial drawable based on the initial state of isFlashOn
        flashImageView.setImageResource(if (isFlashOn) R.drawable.flash else R.drawable.flash_off)

        flashImageView.setOnClickListener {
            isFlashOn = !isFlashOn
            flashImageView.setImageResource(if (isFlashOn) R.drawable.flash else R.drawable.flash_off)
            try {
                val cameraId = cameraManager.cameraIdList[0]
                //check if the flashlight is supposed to be on or off before toggling
                if (isFlashOn) {
                    //set flash mode to ON
                    capReq.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH)
                } else {
                    //set flash mode to OFF
                    capReq.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF)
                }
                cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun cameraFlip() {
        val flipCameraImageView = findViewById<ImageView>(R.id.FlipCameraImageView)

        flipCameraImageView.setOnClickListener {
            cameraFacing = if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
                CameraCharacteristics.LENS_FACING_FRONT
            } else {
                CameraCharacteristics.LENS_FACING_BACK
            }
            openCamera()
        }
    }

    private fun captureImage() {
        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader?.acquireLatestImage()
            val buffer = image!!.planes[0].buffer
            capturedImageBytes = ByteArray(buffer.remaining())
            buffer.get(capturedImageBytes)
            image.close()

            //check the orientation of the captured image
            val orientation = getOrientation(capturedImageBytes)

            //rotate the image if needed and adjust for front camera mirroring
            val rotatedImageBytes = rotateImageIfNeeded(capturedImageBytes, orientation)

            //save the rotated image to a temporary file
            val imageFile = saveImageToTempFile(rotatedImageBytes)

            //pass the file path to the next activity
            val intent = Intent(this@CaptureImageActivity, ColorPickerActivity::class.java)
            intent.putExtra("capturedImagePath", imageFile.absolutePath)
            startActivity(intent)
        }, handler)
    }

    private fun getOrientation(imageBytes: ByteArray): Int {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

        return when {
            options.outWidth > options.outHeight -> ExifInterface.ORIENTATION_NORMAL
            else -> ExifInterface.ORIENTATION_ROTATE_90
        }
    }

    private fun rotateImageIfNeeded(imageBytes: ByteArray, orientation: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val matrix = Matrix()

        //check if the front camera is active and adjust for mirroring
        if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
            matrix.setScale(-1f, 1f) // Mirror horizontally
        }

        if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
            matrix.postRotate(90f)
        }

        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )

        val outputStream = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return outputStream.toByteArray()
    }

    private fun saveImageToTempFile(data: ByteArray): File {
        val tempDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("captured_image", ".jpg", tempDir)
        imageFile.writeBytes(data)
        return imageFile
    }

    private fun openHistoryActivity() {
        findViewById<ImageView>(R.id.HistoryImageView).apply {
            setOnClickListener {
                val intent = Intent(this@CaptureImageActivity, HistoryActivity::class.java)
                startActivity(intent)
            }
        }
    }
}