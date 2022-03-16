package com.example.dormentor

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class FaceDetector : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        Log.d(TAG, image.cropRect.flattenToString())

        image.close()
    }

    companion object {
        private const val TAG = "FaceDetectorAnalyzer"
    }
}
