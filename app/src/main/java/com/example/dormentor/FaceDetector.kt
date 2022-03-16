package com.example.dormentor

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceDetector : ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {

        //Creating InputImage object used as input for the faceDetector
        val img = image.image
        val inputImage = img?.let { InputImage.fromMediaImage(it,image.imageInfo.rotationDegrees) }

        if (inputImage != null) {
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        val bounds = face.boundingBox
                        val rotY = face.headEulerAngleY.toString()
                        val rotZ = face.headEulerAngleZ.toString()
                        Log.d(TAG,"Tête tournée par $rotY degrees")
                        Log.d(TAG,"Tête inclinée par $rotZ degrees")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG,"Prcoessing image failed !",e)
                }
                .addOnCompleteListener { //IF NOT ADDED -> the image will be closed before the processing finishes
                    image.close()
                }
        }
    }

    companion object {
        private const val TAG = "FaceDetectorAnalyzer"

        //Choosing the options recommended for real-time apps
        private val options : FaceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
    }
}
