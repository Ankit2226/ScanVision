package com.skyboundapps.scanvision

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var objectImage: ImageView
    private lateinit var captureImgbtn: Button
    private lateinit var labeltext: TextView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var imageLabeler: ImageLabeler

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        objectImage = findViewById(R.id.objectImage)
        captureImgbtn = findViewById(R.id.captureImgbtn)
        labeltext = findViewById(R.id.labeltext)

        checkCameraPermission()

        // Initialize the imageLabeler correctly
        imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

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

        captureImgbtn.setOnClickListener {
            val clickPicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (clickPicture.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(clickPicture)
            }
        }
    }

    private fun labelImage(bitmap: Bitmap) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        imageLabeler.process(inputImage).addOnSuccessListener { labels ->
            dispalylabel(labels)
        }.addOnFailureListener { e ->
            labeltext.text = "Error: ${e.message}"
        }
    }

    private fun dispalylabel(labels: List<ImageLabel>) {
        if (labels.isNotEmpty()) {
            val mostConfidentLabel = labels[0]
            labeltext.text = mostConfidentLabel.text
        } else {
            labeltext.text = "No object detected"
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        }
    }
}