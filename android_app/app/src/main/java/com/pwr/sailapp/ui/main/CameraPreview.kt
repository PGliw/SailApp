package com.pwr.sailapp.ui.main

import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException
import java.lang.Exception

class CameraPreview(
    context: Context,
    private val camera: Camera
) : SurfaceView(context), SurfaceHolder.Callback {

    companion object {
        const val TAG = "CameraPreview"
    }

    private val surfaceHolder: SurfaceHolder = holder.apply {
        addCallback(this@CameraPreview)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
        // The Surface has been created, now tell the camera where to draw the preview
        camera.apply {
            try {
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.d(TAG, "Error setting camera preview: ${e.message}")
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        // release camera in activity
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        // e.g. preview changed or rotated
        if (surfaceHolder?.surface == null) return
        try {
            camera.stopPreview()
        } catch (e: Exception) {
        }

        // set preview changes here
        camera.apply {
            try {
                setPreviewDisplay(surfaceHolder)
                startPreview()
            } catch (e: Exception) {
                Log.d(TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }


}