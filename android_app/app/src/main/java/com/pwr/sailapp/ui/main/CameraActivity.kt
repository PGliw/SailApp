package com.pwr.sailapp.ui.main

import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pwr.sailapp.R
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class CameraActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Camera"
        const val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 100
    }

    private var camera: Camera? = null
    private var preview: CameraPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        val cameraPermission =
            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_EXTERNAL_STORAGE_REQUEST_CODE
            )
        }

        camera = getCameraInstance()
        preview = camera?.let {
            CameraPreview(this, it)
        }

        preview?.also {
            camera_preview.addView(it)
        }

        button_photo.setOnClickListener {
            val cameraPerm =
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (cameraPerm != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE
                )
            } else camera?.takePicture(shutterCallback, null, pictureCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        camera?.release()
        camera = null
    }

    /** A safe way to get an instance of the Camera object. */
    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    private val pictureCallback = Camera.PictureCallback { data, _ ->
        val pictureFile: File = getOutputImageFile() ?: kotlin.run {
            Log.d(TAG, "Error creating media file, check storage permissions")
            return@PictureCallback
        }
        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: ${e.message}")
        }
    }

    private val shutterCallback = Camera.ShutterCallback {
        photo_done.visibility = View.VISIBLE
        Handler().postDelayed(
            {
                photo_done.visibility = View.INVISIBLE
            }, 500
        )
    }

    /**
     * Creates a file for saving image
     */
    private fun getOutputImageFile(): File? {
        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "SailApp"
        )
        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d(TAG, "failed to create directory")
                    return null
                }
            }
        }
        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
    }

    /**
     * Create a file Uri for saving an image
     */
    private fun getOutputImageFileUri(): Uri {
        return Uri.fromFile(getOutputImageFile())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                renderStoragePermissionWarning()
            }
        }
    }

    private fun renderStoragePermissionWarning() {
        Toast.makeText(this, resources.getString(R.string.no_storage_permission), Toast.LENGTH_LONG)
            .show()
    }
}
