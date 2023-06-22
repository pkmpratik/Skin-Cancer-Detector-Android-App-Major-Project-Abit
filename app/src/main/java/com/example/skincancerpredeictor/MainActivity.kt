package com.example.skincancerpredeictor

import android.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.io.InputStream


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2

    private val mInputSize = 224
    private val mModelPath = "model.tflite"
    private val mLabelPath = "labels.txt"
    private val mSamplePath = "skin-icon.jpg"
    private val mLogoPath = "abit logo.png"

    private lateinit var mPhotoImageView: ImageView
    private lateinit var mCameraButton: Button
    private lateinit var mGalleryButton: Button
    private lateinit var mDetectButton: Button
    private lateinit var mResetButton: Button
    private lateinit var mResultTextView: TextView
    private lateinit var mMessageTextView: TextView
    private lateinit var mLogoImageView: ImageView

    private var isImageSelected = false
    private var isDetectButtonClicked = false


    @SuppressLint("SetTextI18n")
    private fun resetState() {
        try {
            val inputStream = assets.open(mSamplePath)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            mBitmap = BitmapFactory.decodeStream(inputStream, null, options)!!
            mBitmap = scaleImage(mBitmap)
            mPhotoImageView.setImageBitmap(mBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        mResultTextView.text = "Default Image set now..."
        isImageSelected = false
    }



    @SuppressLint("MissingInflatedId", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
//            window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimaryDark) // Replace "your_color" with the desired color resource
//            window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.statusBarColor = resources.getColor(android.R.color.transparent,theme)

        // Set the status bar font color based on the system theme
        val flags = window.decorView.systemUiVisibility
        if (isDarkTheme()) {
            window.decorView.systemUiVisibility = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        } else {
            window.decorView.systemUiVisibility = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        setContentView(R.layout.activity_main)
        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)



        mPhotoImageView = findViewById(R.id.photoImageView)
        mCameraButton = findViewById(R.id.cameraButton)
        mGalleryButton = findViewById(R.id.galleryButton)
        mDetectButton = findViewById(R.id.detectButton)
        mResetButton = findViewById(R.id.resetButton)
        mResetButton.isActivated = false
        mResultTextView = findViewById(R.id.resultTextView)
        mMessageTextView = findViewById(R.id.messageTextView)
        mLogoImageView = findViewById(R.id.logoImageView)


        try {
            val inputStream: InputStream = assets.open(mLogoPath) // Replace "image.jpg" with the actual image file name in your assets folder
            val logoBitmap = BitmapFactory.decodeStream(inputStream)
            mLogoImageView.setImageBitmap(logoBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
            // Handle the error here, such as displaying a default image or showing an error message
            mLogoImageView.setImageResource(R.drawable.abit_logo) // Replace "default_logo" with the actual default image resource
        }





        mCameraButton.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(callCameraIntent, mCameraRequestCode)
            isImageSelected = true
            isDetectButtonClicked = false
        }
        mGalleryButton.setOnClickListener {
            val callGalleryIntent = Intent(Intent.ACTION_PICK)
            callGalleryIntent.type = "image/*"
            startActivityForResult(callGalleryIntent, mGalleryRequestCode)
            isImageSelected = true
            isDetectButtonClicked = false
        }
        mDetectButton.setOnClickListener {
            if (!isDetectButtonClicked && isImageSelected) { // Add condition to check if detect button is not already clicked
                val drawable = mPhotoImageView.drawable
                if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
                    mResultTextView.text = "Please select or capture an image"
                } else {
                    val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
//                    if you want to print the result with confidence
//                    mResultTextView.text = results?.title + "\nConfidence: " + results?.confidence
                    if(results?.title == "Benign"){
                        mResultTextView.text = "Benign safe!! No need to worry"
                        mMessageTextView.text = "(The actual medical result may vary)"
                    }else{
                        mResultTextView.text = "Malignant!! Need to consult the doctor"
                        mMessageTextView.text = "(The actual medical result may vary)"
                    }
                    mResetButton.isEnabled = true // Enable reset button
                    isDetectButtonClicked = true // Set detect button as clicked
                }
            }else {
                mResultTextView.text = "Please select or capture an image"
            }
        }
        mResetButton.setOnClickListener{
            resetState()
            isDetectButtonClicked = false
        }

        loadSampleImage()
    }

    private fun loadSampleImage() {
        try {
            val inputStream = assets.open(mSamplePath)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }
            mBitmap = BitmapFactory.decodeStream(inputStream, null, options)!!
            mBitmap = scaleImage(mBitmap)
            mPhotoImageView.setImageBitmap(mBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun scaleImage(bitmap: Bitmap?): Bitmap {
        val originalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / originalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCameraRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            mBitmap = data.extras?.get("data") as Bitmap
            mBitmap = scaleImage(mBitmap)
            //Here to add The code for knowing image size
//            val toast = Toast.makeText(this, "Image cropped to: w=${mBitmap.width} h=${mBitmap.height}", Toast.LENGTH_LONG)
//            toast.setGravity(Gravity.BOTTOM, 0, 20)
//            toast.show()
            mPhotoImageView.setImageBitmap(mBitmap)
            mResultTextView.text = "Your photo image is set now."
        } else if (requestCode == mGalleryRequestCode && resultCode == Activity.RESULT_OK && data != null) {
            val uri = data.data
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                mBitmap = scaleImage(mBitmap)
                mPhotoImageView.setImageBitmap(mBitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            mResultTextView.text = "Image not Selected !!"
            isDetectButtonClicked = true
        }
    }
//     Status bar color
    private fun isDarkTheme(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

}

