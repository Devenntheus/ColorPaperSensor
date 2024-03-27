package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
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


class ColorPickerActivity : AppCompatActivity() {

    // Declaring image views and file path for the captured image
    private lateinit var crossHairImageView: ImageView
    private lateinit var capturedImageView: ImageView
    private lateinit var confirmCheckImageButton: ImageView
    private lateinit var confirmImageView: ImageView
    private lateinit var imageFilePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        crossHairImageView = findViewById(R.id.CrosshairImageView)
        capturedImageView = findViewById(R.id.CapturedImageView)
        confirmCheckImageButton = findViewById(R.id.ConfirmCheckImageButton)
        confirmImageView = findViewById(R.id.ConfirmImageView)

        // Get the image path from the intent
        imageFilePath = intent.getStringExtra("capturedImagePath") ?: ""

        // Load the image into CapturedImageView
        displayCapturedImage()

        // Set onTouchListener to move CrosshairImageView
        setCrosshairTouchListener()

        confirmCheckImageButton.setOnClickListener {
            // Get the hex color under the cursor
            val hexColor = getHexColorUnderCrosshair()

            // Show progress dialog and then display color dialog
            showProgressDialog {
                showColorDialog(hexColor, imageFilePath)
            }
        }
    }

    // Function to load captured image into CapturedImageView
    private fun displayCapturedImage() {
        if (imageFilePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            capturedImageView.setImageBitmap(bitmap)
            capturedImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
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

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()

        textViewMessage.text = getString(R.string.process)

        // Dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 1000) //adjust the delay time as needed
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

    // Function to display meat information in a dialog box
    private fun showColorDialog(color: String, imageFilePath: String) {
        val dialogView = layoutInflater.inflate(R.layout.meat_description_dialog, null)
        val closeButtonImageView = dialogView.findViewById<ImageView>(R.id.CloseImageButton)
        val meatStatusTextView = dialogView.findViewById<TextView>(R.id.statusTextView)
        val meatTypeTextView = dialogView.findViewById<TextView>(R.id.MeatTypeTextView)
        val colorNameTextView = dialogView.findViewById<TextView>(R.id.ColorNameTextView)
        val hexCodeTextView = dialogView.findViewById<TextView>(R.id.HexCodeTextView)
        val rgbTextView = dialogView.findViewById<TextView>(R.id.RGBTextView)
        val hsvCodeTextView = dialogView.findViewById<TextView>(R.id.HsvCodeTextView)
        val xyzValuesTextView = dialogView.findViewById<TextView>(R.id.XYZValuesTextView)
        val labValuesTextView = dialogView.findViewById<TextView>(R.id.LabValuesTextView)
        val colorShapeView = dialogView.findViewById<ImageView>(R.id.ShowColorImage)

        // Create a GradientDrawable and set its color based on the hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(color))
        gradientDrawable.shape = GradientDrawable.RECTANGLE

        // Set corner radius (adjust the value as needed)
        gradientDrawable.cornerRadius = 20f

        // Apply the drawable to the ImageView
        colorShapeView.setImageDrawable(gradientDrawable)

        // Set meat type
        val meatType = GlobalData.meatType
        meatTypeTextView.text = meatType.toString()

        // Convert hex color to RGB
        val rgbValues = hexToRgb(color)

        // Set the RGB values to the TextView
        if (rgbValues != null) {
            val (red, green, blue) = rgbValues
            val rgbValuesText = "($red, $green, $blue)"
            rgbTextView.text = rgbValuesText
        } else {
            rgbTextView.text = "N/A"
        }

        // Get the hex color under the cursor
        val hexColor = getHexColorUnderCrosshair();

        // Log message before saving meat information
        Log.d(TAG, "Hex Code Value: $hexColor")

        // Get meat status based on meat type and RGB values
        val (meatStatus, labValues, xyzValues) = PlanHPoultryMeatStatus.getMeatStatus(meatType.toString(), rgbValues);

        // Set meat status
        meatStatusTextView.text = meatStatus

        // Convert meatTypeTextView.text to String
        val meatTypeString: String = meatTypeTextView.text.toString()

        closeButtonImageView.isEnabled = false

        // Declare a variable to store the colorName
        var colorName: String? = null

        // Set the hsv code to textview
        val hsvValues = getHSVFromHexColor(color)
        val formattedHSV = "(${String.format("%.2f", hsvValues[0])}, ${String.format("%.4f", hsvValues[1])}, ${String.format("%.4f", hsvValues[2])})"
        hsvCodeTextView.text = formattedHSV

        // Set the xyz values to the textview with two decimal places
        val xyzValuesText = "(${"%.2f".format(xyzValues[0])}, ${"%.4f".format(xyzValues[1])}, ${"%.4f".format(xyzValues[2])})"
        xyzValuesTextView.text = xyzValuesText

        // Set the lab values to the textview with two decimal places
        val labValuesText = "(${"%.2f".format(labValues[0])}, ${"%.4f".format(labValues[1])}, ${"%.4f".format(labValues[2])})"
        labValuesTextView.text = labValuesText

        // Launch the coroutine to get the colorName
        val job = lifecycleScope.launch {
            colorName = getColorName(color)

            // Set color name to TextView
            colorNameTextView.text = colorName

            // Enable the close button now that the colorName is retrieved
            closeButtonImageView.isEnabled = true

            // Convert the captured image to Base64 using the utility class
            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            val capturedImageString = ImageUtils.convertImageToString(bitmap)

            // Generate the ID using the MeatInformationManager
            val id = MeatInformationManager.generateId()

            // Get the current date and time in separate formats
            val currentDate = SimpleDateFormat("MM-dd-yyyy").format(Date())
            val currentTime = SimpleDateFormat("HH:mm:ss").format(Date())

            // Create an instance of MeatInformation with the generated ID
            val meatInformation = MeatInformation(
                id = id,
                meatStatus = meatStatus,  // Replace "YourMeatStatus" with the actual meat status
                meatType = meatTypeString,
                color = colorName ?: "", // Use the colorName if not null, or an empty string if null
                hexCode = color,
                date = currentDate,
                time = currentTime,
                meatImage = capturedImageString
            )

            // Log message before saving meat information
            Log.d(TAG, "Saving meat information: $meatInformation")

            // Save the meat information
            saveMeatInformation(meatInformation)

            // Log message after saving meat information
            Log.d(TAG, "Meat information saved successfully.")

        }

        // Set the hex code
        hexCodeTextView.text = color

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        // Set close button click listener
        closeButtonImageView.setOnClickListener {
            // Cancel the coroutine when the dialog is dismissed
            job.cancel()
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
        val meatImage: String
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