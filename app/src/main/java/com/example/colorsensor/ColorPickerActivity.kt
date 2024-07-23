package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import android.media.ExifInterface
import android.graphics.Matrix

data class ColorInfo(
    val hex: Hex,
    val name: Name
)

data class Hex(
    val value: String
)

data class Name(
    val value: String
)

interface ColorApiService {
    @GET("/id")
    suspend fun getColorInfo(@Query("hex") hex: String): ColorInfo
}

private var alertDialog: AlertDialog? = null  // Declare alertDialog as nullable

class ColorPickerActivity : AppCompatActivity() {

    // Declaring image views and file path for the captured image
    private lateinit var crossHairImageView: ImageView
    private lateinit var capturedImageView: ImageView
    private lateinit var imageFilePath: String
    private lateinit var phoneId: String
    private lateinit var sizeSeekBar: SeekBar

    private lateinit var checkButtonLinearLayout: LinearLayout
    private lateinit var bottomFunctionsFrameLayout: FrameLayout
    private lateinit var confirmImageView: ImageView
    private lateinit var confirmCheckImageButton: ImageButton
    private lateinit var topFunctionsRelativeLayout:RelativeLayout
    private lateinit var imageFrameLayout:FrameLayout
    private lateinit var cameraImageView:ImageView
    private lateinit var detailsTextView:TextView
    private lateinit var homeImageView:ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        crossHairImageView = findViewById(R.id.CrosshairImageView)
        capturedImageView = findViewById(R.id.CapturedImageView)

        checkButtonLinearLayout = findViewById(R.id.CheckButtonLinearLayout);
        bottomFunctionsFrameLayout = findViewById(R.id.BottomFunctionsFrameLayout);
        confirmImageView = findViewById(R.id.ConfirmImageView);
        confirmCheckImageButton = findViewById(R.id.ConfirmCheckImageButton);
        topFunctionsRelativeLayout = findViewById(R.id.TopFunctionsRelativeLayout);
        imageFrameLayout = findViewById(R.id.ImageFrameLayout)
        cameraImageView = findViewById(R.id.cameraImageView)
        detailsTextView = findViewById(R.id.detailsTextView)
        homeImageView = findViewById(R.id.homeImageView)

        // Get the image path from the intent
        imageFilePath = intent.getStringExtra("capturedImagePath") ?: ""
        phoneId = intent.getStringExtra("phoneId") ?: ""

        // Load the image into CapturedImageView
        displayCapturedImage()

        // Set onTouchListener to move CrosshairImageView
        setCrosshairTouchListener()

        val cameraImageView = findViewById<ImageView>(R.id.cameraImageView)
        val homeImageView = findViewById<ImageView>(R.id.homeImageView)

        // Set click listener for cameraImageView
        cameraImageView.setOnClickListener {
            // Launch the Camera Activity with intent
            val intent = Intent(this, CaptureImageActivity::class.java)
            intent.putExtra("meatType", GlobalData.meatType.toString()) // Pass meat type to CameraActivity if needed
            startActivity(intent)
        }

        // Set click listener for homeImageView
        homeImageView.setOnClickListener {
            // Redirect to MainMenuActivity
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish() // Optional: finish the current activity
        }

        sizeSeekBar = findViewById(R.id.sizeSeekBar)

        // Set up seek bar listener
        sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Adjust the size of the crosshair based on seek bar progress
                val newSize = 50 + progress  // Range: 50 to 200 dp
                val layoutParams = crossHairImageView.layoutParams
                layoutParams.width = newSize
                layoutParams.height = newSize
                crossHairImageView.layoutParams = layoutParams
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        confirmCheckImageButton.setOnClickListener {
            // Get the hex color under the cursor
            val hexColor = getHexColorUnderCrosshair()

            // Show progress dialog and then display color dialog
            showProgressDialog {}

            // Disable movement of crossHairImageView by intercepting touch events
            imageFrameLayout.setOnTouchListener { _, _ ->
                // Return true to indicate that the touch event was consumed
                true
            }
            launchMeatDescriptionActivity(hexColor, imageFilePath, phoneId)
        }
    }

    // Function to load captured image into CapturedImageView
    private fun displayCapturedImage() {
        if (imageFilePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imageFilePath)

            // Read EXIF data
            val exif = ExifInterface(imageFilePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            // Rotate the bitmap if necessary
            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(bitmap, true, false)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(bitmap, false, true)
                else -> bitmap
            }

            capturedImageView.setImageBitmap(rotatedBitmap)

            // Log the dimension of the displayed captured image
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            Log.d("ImageDimension", "Displayed image dimension: $width x $height")
        }
    }

    // Function to rotate bitmap
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Function to flip bitmap
    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) preScale(-1f, 1f)
            if (vertical) preScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Function to calculate the sample size for bitmap decoding
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    @SuppressLint("ClickableViewAccessibility")
    // Function to set onTouchListener for moving CrosshairImageView
    private fun setCrosshairTouchListener() {
        val frameLayout = findViewById<FrameLayout>(R.id.ImageFrameLayout)

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    // Update the position of cursor
                    val newX = event.rawX - crossHairImageView.width / 2
                    val newY = event.rawY - crossHairImageView.height - 150f

                    // Ensure the cursor stays within the frame layout
                    if (newX >= 0 && newX <= frameLayout.width - crossHairImageView.width &&
                        newY >= 0 && newY <= frameLayout.height - crossHairImageView.height
                    ) {
                        crossHairImageView.x = newX
                        crossHairImageView.y = newY

                        // Update ConfirmCheckImageButton visibility and enable state
                        confirmCheckImageButton.visibility = View.VISIBLE
                        confirmCheckImageButton.isEnabled = true
                    }
                }
            }
            true
        }
    }

    // Function to get hex color under the cursor
    private fun getHexColorUnderCrosshair(): String {
        val bitmap = getBitmapFromImageView(capturedImageView)
        val crosshairX = crossHairImageView.x.toInt()
        val crosshairY = crossHairImageView.y.toInt()
        val crosshairWidth = crossHairImageView.width
        val crosshairHeight = crossHairImageView.height

        // Define the rectangle around the cursor
        val rectLeft = crosshairX
        val rectTop = crosshairY
        val rectRight = crosshairX + crosshairWidth
        val rectBottom = crosshairY + crosshairHeight

        // Ensure the rectangle is within the bounds of the bitmap
        val rect = Rect(rectLeft.coerceAtLeast(0), rectTop.coerceAtLeast(0), rectRight.coerceAtMost(bitmap.width), rectBottom.coerceAtMost(bitmap.height))

        // Get the hex color from the bitmap for the defined rectangle
        return getHexColorFromBitmap(bitmap, rect)
    }

    // Function to convert ImageView to Bitmap
    private fun getBitmapFromImageView(imageView: ImageView): Bitmap {
        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache(true)
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = false
        return bitmap
    }

    // Function to get hex color from Bitmap
    private fun getHexColorFromBitmap(bitmap: Bitmap, rect: Rect): String {
        val croppedBitmap = Bitmap.createBitmap(bitmap, rect.left, rect.top, rect.width(), rect.height())
        // Calculate the average color of the cropped area
        val averageColor = getAverageColor(croppedBitmap)
        return String.format("#%06X", averageColor and 0xFFFFFF) // Ensure only 6 least significant hex digits are considered
    }

    // Function to get average color from Bitmap
    private fun getAverageColor(bitmap: Bitmap): Int {
        var red = 0
        var green = 0
        var blue = 0
        var count = 0

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val pixel = bitmap.getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
                count++
            }
        }

        red /= count
        green /= count
        blue /= count

        return Color.rgb(red, green, blue)
    }

    // Function to get HSV values from hex color
    private fun getHSVFromHexColor(hexColor: String): FloatArray {
        // Parse the hexadecimal color string to an integer
        val colorInt = Color.parseColor(hexColor)

        // Convert the color integer to HSV
        val hsv = FloatArray(3)
        Color.RGBToHSV(Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt), hsv)

        // Adjust saturation and value to two decimal places
        hsv[1] = (hsv[1] * 100).toFloat()
        hsv[2] = (hsv[2] * 100).toFloat()

        return hsv
    }

    // Function to convert hex color to RGB
    private fun hexToRgb(hexColor: String): Triple<Int, Int, Int>? {
        return try {
            val colorInt = Color.parseColor(hexColor)
            val red = Color.red(colorInt)
            val green = Color.green(colorInt)
            val blue = Color.blue(colorInt)
            Triple(red, green, blue)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    // Function to use API for data scraping the color name
    private val colorApiService: ColorApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.thecolorapi.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ColorApiService::class.java)
    }

    // Function to get the color name from the API
    private suspend fun getColorName(hexColor: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response = colorApiService.getColorInfo(hexColor)
                response.name.value
            } catch (e: Exception) {
                e.printStackTrace()
                "Unknown"
            }
        }
    }

    // Function to show progress dialog
    private fun showProgressDialog(callback: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        alertDialog = alertDialogBuilder.create()

        alertDialog?.show()

        //you can customize the message here
        textViewMessage.text = getString(R.string.process)

        //dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog?.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 10000) //adjust the delay time as needed
    }

    // Converts a Bitmap image to a Base64-encoded string with resizing and compression.
    object ImageUtils {

        fun convertImageToString(bitmap: Bitmap): String {
            // Resize the image before converting to Base64
            val resizedBitmap = resizeBitmap(bitmap, 800, 800)

            val byteArrayOutputStream = ByteArrayOutputStream()
            // Change compression format to JPEG and adjust quality (e.g., 80)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)

            // Use Base64.NO_WRAP to remove any newline characters
            val base64String = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.NO_WRAP)

            // Ensure that the length is a multiple of 4 by adding '=' padding
            return addPaddingToBase64(base64String)
        }

        private fun addPaddingToBase64(base64String: String): String {
            val padding = "="
            val paddingLength = (4 - base64String.length % 4) % 4
            return base64String + padding.repeat(paddingLength)
        }

        private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }

    private fun launchMeatDescriptionActivity(color: String, imagePath: String, phoneId: String) {

        // Set meat type
        val meatType = GlobalData.meatType.toString()  // Convert meatType to String immediately

        // Launch the coroutine to get meat status and color name
        lifecycleScope.launch {
            // Convert hex color to RGB
            val rgbValues = withContext(Dispatchers.Default) { hexToRgb(color) }

            /*// Get meat status based on meat type and RGB values
            val (meatStatus, labValues, _) = PlanHPoultryMeatStatus.getMeatStatus(meatType, rgbValues)
*/
            //Get meat status based on meat type and RGB values
            val (meatStatus, labValues, _) = when (meatType) {
                "Poultry" -> PoultryMeatStatus.getMeatStatus(meatType, rgbValues)
                "Beef" -> BeefMeatStatus.getMeatStatus(meatType, rgbValues)
                "Pork" -> PorkMeatStatus.getMeatStatus(meatType, rgbValues)
                else -> {
                    // Navigate back to main menu activity if meat type is not recognized
                    startActivity(Intent(this@ColorPickerActivity, MainMenuActivity::class.java))
                    return@launch
                }
            }


            // Get color name asynchronously
            val colorName = withContext(Dispatchers.IO) { getColorName(color) }

            val bitmap = BitmapFactory.decodeFile(imagePath)
            val capturedImageString = ImageUtils.convertImageToString(bitmap)

            val labValue = "(${"%.2f".format(labValues[0])}, ${"%.4f".format(labValues[1])}, ${"%.4f".format(labValues[2])})"


            // Example of displaying data in TextViews (replace with your UI elements)
            val meatStatusTextView = findViewById<TextView>(R.id.statusTextView)
            val meatTypeTextView = findViewById<TextView>(R.id.MeatTypeTextView)
            val colorNameTextView = findViewById<TextView>(R.id.ColorNameTextView)
            val hexCodeTextView = findViewById<TextView>(R.id.HexCodeTextView)
            val labValuesTextView = findViewById<TextView>(R.id.LabValuesTextView)
            val capturedImageView = findViewById<ImageView>(R.id.ShowColorImage)
            val referenceImageView = findViewById<ImageView>(R.id.ShowReferenceColorImage)

            // Decode and display the image
            meatStatusTextView.text = "$meatStatus"
            meatTypeTextView.text = "$meatType"
            colorNameTextView.text = "$colorName"
            hexCodeTextView.text = "$color"
            labValuesTextView.text = "(${"%.2f".format(labValues[0])}, ${"%.4f".format(labValues[1])}, ${"%.4f".format(labValues[2])})"

            // Set text color of meatStatusTextView based on the hex code
            setTextColorBasedOnHexCode(meatStatusTextView, color)
            setCapturedColor(capturedImageView, color)
            setReferenceColor(referenceImageView, meatStatus, meatType)

            // Disable resizing of imageFrameLayout after layout changes
            imageFrameLayout.post {
                val layoutParams = imageFrameLayout.layoutParams
                layoutParams.width = imageFrameLayout.width  // Set width to current width
                layoutParams.height = imageFrameLayout.height  // Set height to current height
                imageFrameLayout.layoutParams = layoutParams
            }

            // Hide CheckButtonFrameLayout and show BottomFunctionsLinearLayout
            crossHairImageView.setVisibility(View.GONE)
            checkButtonLinearLayout.setVisibility(View.GONE)
            bottomFunctionsFrameLayout.setVisibility(View.VISIBLE)
            cameraImageView.setVisibility(View.VISIBLE)
            detailsTextView.setVisibility(View.VISIBLE)
            homeImageView.setVisibility(View.VISIBLE)

            // Bring BottomFunctionsFrameLayout to the front
            bottomFunctionsFrameLayout.bringToFront()

            // Request layout to reflect changes
            imageFrameLayout.requestLayout();
            checkButtonLinearLayout.requestLayout();

            hideProgressDialog();

            // Now that the activity is started, save meat information in the background
            saveMeatInformationAsync(meatStatus, meatType, colorName ?: "", color, phoneId, imagePath, labValue)
            hideProgressDialog()
        }
    }

    // Function to set text color based on hex code
    private fun setTextColorBasedOnHexCode(textView: TextView, hexCode: String?) {
        if (hexCode != null) {
            // Set the text color of the TextView based on the hex code
            textView.setTextColor(Color.parseColor(hexCode))
        }
    }

    private fun setCapturedColor(capturedImageView: ImageView, hexCode: String?) {
        if (hexCode != null) {
            // Set background color of the capturedImageView
            capturedImageView.setBackgroundColor(Color.parseColor(hexCode))
        }
    }

    private fun setReferenceColor(referenceImageView: ImageView, meatStatus: String?, meatType: String?) {
        // Set background color of the ImageView based on meat status
        if (meatType == "Poultry"){
            if (meatStatus == "Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(185, 170, 177))
            } else if (meatStatus == "Moderately Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(165, 165, 173))
            } else if (meatStatus == "Borderline Spoilage") {
                referenceImageView.setBackgroundColor(Color.rgb(163, 163, 171))
            }
        }
        else if (meatType == "Pork"){
            if (meatStatus == "Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(135, 80, 94))
            } else if (meatStatus == "Moderately Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(136, 93, 105))
            } else if (meatStatus == "Borderline Spoilage") {
                referenceImageView.setBackgroundColor(Color.rgb(121, 87, 100))
            }
        }

        else if (meatType == "Beef"){
            if (meatStatus == "Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(193, 175, 177))
            } else if (meatStatus == "Moderately Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(183, 171, 177))
            } else if (meatStatus == "Borderline Spoilage") {
                referenceImageView.setBackgroundColor(Color.rgb(175, 172, 179))
            }
        }
    }

    //Function to hide progress dialog
    private fun hideProgressDialog() {

        alertDialog?.dismiss()
    }

    // Function to save meat information asynchronously
    private suspend fun saveMeatInformationAsync(
        meatStatus: String,
        meatType: String,
        colorName: String,
        hexCode: String,
        phoneId: String,
        imagePath: String,
        labValue: String
    ) {
        withContext(Dispatchers.IO) {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val capturedImageString = ImageUtils.convertImageToString(bitmap)

            val id = MeatInformationManager.generateId()
            val currentDate = SimpleDateFormat("MM-dd-yyyy").format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss").format(Date())

            val meatInformation = MeatInformation(
                id = id,
                meatStatus = meatStatus,
                meatType = meatType,
                color = colorName,
                hexCode = hexCode,
                date = currentDate,
                time = currentTime,
                meatImage = capturedImageString,
                phoneId = phoneId,
                labValue = labValue
            )

            saveMeatInformation(meatInformation)
        }
    }


    private fun String.MeatInformation(
        id: String,
        meatStatus: String,
        meatType: String,
        hexCode: String,
        date: String,
        time: String,
        meatImage: String,
        phoneId: String,
        labValue: String
    ) {

    }

    // Function to display recapture dialog for unknown meat status
    private fun showRecaptureDialog() {
        val dialogView = layoutInflater.inflate(R.layout.recapture_dialog, null)
        val closeButtonImageView = dialogView.findViewById<ImageView>(R.id.closeImageButton)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        // Set close button click listener
        closeButtonImageView.setOnClickListener {
            // Dismiss the dialog
            alertDialog.dismiss()

            // Start MainMenuActivity or use an intent to navigate back
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
        }

        alertDialog.show()
    }

    // Function to save meat information to the cloud
    private fun saveMeatInformation(meatInformation: MeatInformation) {
        // Initialize Firestore
        val db = FirebaseFirestore.getInstance()

        // Get a reference to the "History" collection
        val historyCollection = db.collection("History")

        // Use the id field from meatInformation as the document ID
        val documentId = meatInformation.id

        // Create a batch write
        val batch = db.batch()

        // Create a reference to the document with the generated ID
        val historyDocument = historyCollection.document(documentId)

        // Set the meat information in the Firestore document
        batch.set(historyDocument, meatInformation)

        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Meat information saved SUCCESSFULLY.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "ERROR! Failed saving the meat information.", e)
            }
    }


    // Create a data class to hold the meat information
    data class MeatInformation(
        val id: String,
        val meatStatus: String,
        val meatType: String,
        val color: String,
        val hexCode: String,
        val date: String,
        val time: String,
        val meatImage: String,
        val phoneId: String,
        val labValue: String
    )

    class MeatInformationManager {
        companion object {
            suspend fun generateId(): String {
                val lastId = getLastIdFromDatabase()
                var newId = lastId + 1
                while (isIdExists("HIST$newId")) { // Convert newId to String
                    newId++
                }
                return "HIST$newId"
            }

            private suspend fun getLastIdFromDatabase(): Int {
                val firestore = FirebaseFirestore.getInstance()
                val historyCollectionRef: CollectionReference = firestore.collection("History")

                return try {
                    val querySnapshot: QuerySnapshot = historyCollectionRef.get().await()

                    if (!querySnapshot.isEmpty) {
                        val sortedDocuments = querySnapshot.documents.sortedByDescending { it["timestamp"] as? Long }

                        val lastDocumentId = sortedDocuments[0].id
                        val lastNumber = lastDocumentId.substring(4).toInt()
                        lastNumber
                    } else {
                        0
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    0
                }
            }

            private suspend fun isIdExists(id: String): Boolean {
                return try {
                    val firestore = FirebaseFirestore.getInstance()
                    val querySnapshot = withContext(Dispatchers.IO) {
                        firestore.collection("History")
                            .whereEqualTo("id", id)
                            .get()
                            .await()
                    }

                    querySnapshot.documents.size > 0
                } catch (e: Exception) {
                    // Handle exceptions (e.g., Firebase network issues)
                    false
                }
            }
        }
    }
}