package com.skyboundapps.scanvision

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var objectImage: ImageView
    private lateinit var captureImgbtn: Button
    private lateinit var uploadImgbtn: Button // New button for uploading images
    private lateinit var labeltext: TextView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var imageLabeler: ImageLabeler

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        objectImage = findViewById(R.id.objectImage)
        captureImgbtn = findViewById(R.id.captureImgbtn)
        uploadImgbtn = findViewById(R.id.uploadImgbtn) // Initialize the upload button
        labeltext = findViewById(R.id.labeltext)

        checkCameraPermission()

        // Initialize the imageLabeler
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val imageBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    extras?.getParcelable("data") as? Bitmap
                }
                if (imageBitmap != null) {
                    objectImage.setImageBitmap(imageBitmap)
                    labelImage(imageBitmap)
                } else {
                    labeltext.text = "Unable to capture text from image"
                }
            }
        }

        // Gallery launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                objectImage.setImageURI(uri)
                labelImageFromUri(uri)
            }
        }

        // Capture Image button click event
        captureImgbtn.setOnClickListener {
            val clickPicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (clickPicture.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(clickPicture)
            }
        }

        // Upload Image button click event
        uploadImgbtn.setOnClickListener {
            galleryLauncher.launch("image/*") // Opens the gallery to select an image
        }
    }

    // Function to label image from Bitmap (captured by camera)
    private fun labelImage(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage)
            .addOnSuccessListener { labels -> dispalylabel(labels) }
            .addOnFailureListener { e -> labeltext.text = "Error: ${e.message}" }
    }

    // Function to label image from Uri (selected from gallery)
    private fun labelImageFromUri(uri: Uri) {
        try {
            val inputImage = InputImage.fromFilePath(this, uri)
            imageLabeler.process(inputImage)
                .addOnSuccessListener { labels -> dispalylabel(labels) }
                .addOnFailureListener { e -> labeltext.text = "Error: ${e.message}" }
        } catch (e: Exception) {
            labeltext.text = "Error: ${e.message}"
        }
    }

    // Function to display the label with the highest confidence
    private fun dispalylabel(labels: List<ImageLabel>) {
        if (labels.isNotEmpty()) {
            val mostConfidentLabel = labels[0]
            labeltext.text = mostConfidentLabel.text
        } else {
            labeltext.text = "No object detected"
        }
    }

    // Check camera permissions
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }
}
