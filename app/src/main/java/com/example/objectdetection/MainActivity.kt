package com.example.objectdetection

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.text.TextRecognizer
import java.util.regex.Pattern


class MainActivity : AppCompatActivity() {
    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 1001
        const val EMIRATE_ID_PATTERN = "784-[0-9]{4}-[0-9]{7}-[0-9]{1}"
    }

    private lateinit var cameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var camera_preview: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        camera_preview = findViewById(R.id.camera_preview);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Log.e("CameraActivity", "Text recognizer not operational")
            Toast.makeText(this, "Text recognizer not operational", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cameraSource = CameraSource.Builder(this, textRecognizer)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1280, 1024)
            .setRequestedFps(15.0f)
            .setAutoFocusEnabled(true)
            .build()

        camera_preview.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }
                    cameraSource.start(holder)
                } catch (e: Exception) {
                    Log.e("CameraActivity", "Error starting camera source: ${e.message}")
                    Toast.makeText(
                        this@MainActivity,
                        "Error starting camera source: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })

        textRecognizer.setProcessor(object :
            com.google.android.gms.vision.Detector.Processor<com.google.android.gms.vision.text.TextBlock> {
            override fun release() {}

            override fun receiveDetections(detections: com.google.android.gms.vision.Detector.Detections<com.google.android.gms.vision.text.TextBlock>?) {
                val textBlocks = detections?.detectedItems
                if (textBlocks != null && textBlocks.size() > 0) {
                    val emirateIds = mutableListOf<String>()
                    for (i in 0 until textBlocks.size()) {
                        val text = textBlocks.valueAt(i)
                        if (text.value.matches("784-[0-9]{4}-[0-9]{7}-[0-9]{1}".toRegex())) {
                            emirateIds.add(text.value)
                        }
                        if (emirateIds.isNotEmpty()) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Emirate IDs found: $emirateIds",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission required to use this feature.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the resources used by the text recognizer and camera source
        textRecognizer.release()
        cameraSource.release()
    }

}