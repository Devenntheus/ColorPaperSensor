package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
import kotlinx.coroutines.launch
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.Dispatchers
import retrofit2.http.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


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

        //get the image path from the intent
        imageFilePath = intent.getStringExtra("capturedImagePath") ?: ""

        //load the image into CapturedImageView
        displayCapturedImage()

        //set onTouchListener to move CrosshairImageView
        setCrosshairTouchListener()

        confirmCheckImageButton.setOnClickListener {
            //get the hex color under the crosshair
            val hexColor = getHexColorUnderCrosshair()

            //show progress dialog and then display color dialog
            showProgressDialog {
                showColorDialog(hexColor, imageFilePath)
            }
        }
    }

    private fun displayCapturedImage() {
        if (imageFilePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            capturedImageView.setImageBitmap(bitmap)
            capturedImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCrosshairTouchListener() {
        val frameLayout = findViewById<FrameLayout>(R.id.ImageFrameLayout)

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    //update the position of crosshair
                    val newX = event.rawX - crossHairImageView.width / 2
                    val newY = event.rawY - crossHairImageView.height - 150f

                    //ensure the crosshair stays within the frame layout
                    if (newX >= 0 && newX <= frameLayout.width - crossHairImageView.width &&
                        newY >= 0 && newY <= frameLayout.height - crossHairImageView.height
                    ) {
                        crossHairImageView.x = newX
                        crossHairImageView.y = newY

                        //update ConfirmCheckImageButton visibility and enable state
                        confirmCheckImageButton.visibility = View.VISIBLE
                        confirmCheckImageButton.isEnabled = true
                    }
                }
            }
            true
        }
    }

    private fun getHexColorUnderCrosshair(): String {
        val bitmap = getBitmapFromImageView(capturedImageView)
        val x = crossHairImageView.x.toInt() + crossHairImageView.width / 2
        val y = crossHairImageView.y.toInt() + crossHairImageView.height / 2
        return getHexColorFromBitmap(bitmap, x, y)
    }

    private fun getBitmapFromImageView(imageView: ImageView): Bitmap {
        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache(true)
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun getHexColorFromBitmap(bitmap: Bitmap, x: Int, y: Int): String {
        val pixel = bitmap.getPixel(x, y)
        return String.format("#%06X", 0xFFFFFF and pixel)
    }

    private val colorApiService: ColorApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.thecolorapi.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ColorApiService::class.java)
    }

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

    //function to show progress dialog
    private fun showProgressDialog(callback: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()

        //you can customize the message here
        textViewMessage.text = getString(R.string.process)

        //dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 1000) //adjust the delay time as needed
    }

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


    private fun showColorDialog(color: String, imageFilePath: String) {
        val dialogView = layoutInflater.inflate(R.layout.meat_description_dialog, null)
        val closeButtonImageView = dialogView.findViewById<ImageView>(R.id.CloseImageButton)
        val colorImageView = dialogView.findViewById<ImageView>(R.id.ConfirmImageView)
        val meatTypeTextView = dialogView.findViewById<TextView>(R.id.MeatTypeTextView)
        val colorNameTextView = dialogView.findViewById<TextView>(R.id.ColorNameTextView)
        val hexCodeTextView = dialogView.findViewById<TextView>(R.id.HexCodeTextView)

        // Create a GradientDrawable and set its color based on the hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(color))
        gradientDrawable.shape = GradientDrawable.OVAL

        // Apply the drawable to the ImageView
        colorImageView.setImageDrawable(gradientDrawable)

        // Set meat type
        val meatType = GlobalData.meatType
        meatTypeTextView.text = meatType.toString()

        // Convert meatTypeTextView.text to String
        val meatTypeString: String = meatTypeTextView.text.toString()

        closeButtonImageView.isEnabled = false

        // Declare a variable to store the colorName
        var colorName: String? = null

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
                meatStatus = "Fresh",  // Replace "YourMeatStatus" with the actual meat status
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