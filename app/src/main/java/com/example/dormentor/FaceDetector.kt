package com.example.dormentor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.graphics.minus
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.dormentor.ui.BitmapViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import util.BitmapUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FaceDetector(private val lifecycleOwner: ViewModelStoreOwner) : ImageAnalysis.Analyzer {

    val bitmapViewModel : BitmapViewModel = ViewModelProvider(lifecycleOwner).get(BitmapViewModel::class.java)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        //Creating Bitmap image based on ImageProxy to be used after for display
        val bitmap : Bitmap? = BitmapUtils.getBitmap(image)

        //Creating InputImage object used as input for the faceDetector
        val img = image.image
        val inputImage = img?.let { InputImage.fromMediaImage(it,image.imageInfo.rotationDegrees) }

        if (inputImage != null) {
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    for (face in faces) {

                       bitmap?.let {
                            if(!bitmapAlreadyCreated) {
                                val eyeBitmap = extractEyeRegion(face,it)
                                eyeBitmap?.let { it1 -> bitmapViewModel.changeBitmap(it1) }
                                bitmapAlreadyCreated = true
                                Log.d(TAG,"eye bitmap created")
                            }
                        }
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


    private fun extractEyeRegion(face: Face, srcBitmap: Bitmap) : Bitmap? {
        val rightEyeContours = face.getContour(FaceContour.LEFT_EYE)?.points
        val rightEyeBrowContours = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points


        if(rightEyeContours != null && rightEyeBrowContours != null) {
            //Finding the starting point (nearestX,highestY) :

            //1. finding the highest point in the eye brow:
            val indexHighestY = rightEyeBrowContours.withIndex().minByOrNull { it.value.y }?.index
            val highestY = abs(rightEyeBrowContours[indexHighestY!!].y)

            //2. finding the nearest point to face's edge
            val nearestX = abs(min(rightEyeBrowContours[0].x, rightEyeContours[0].x))

            //Finding the width of the eye :
            // 1. finding the furthest point of the eye
            val indexfurthestXeyeBrow = rightEyeBrowContours.withIndex().maxByOrNull { it.value.x }?.index
            val furthestXeyeBrow = abs(rightEyeBrowContours[indexfurthestXeyeBrow!!].x)

            val indexfurthestXeye = rightEyeContours.withIndex().maxByOrNull { it.value.x }?.index
            val furthestXeye= abs(rightEyeContours[indexfurthestXeye!!].x)

            val furthestX = abs(max(furthestXeyeBrow, furthestXeye))

            // 2. Calculating the width
            var width = abs((furthestX - nearestX).toInt())

            //Finding the height of the eye :
            //1. finding the lowest point of the eye :
            val indexLowestY = rightEyeContours.withIndex().maxByOrNull { it.value.y }?.index
            val lowestY = abs(rightEyeContours[indexLowestY!!].y)
            //2. Calculating the height
            val height = abs((lowestY - highestY).toInt())

            return Bitmap.createBitmap(
                srcBitmap,
                nearestX.toInt(),
                highestY.toInt(),
                width,
                height
            )

        } else {
            return null
        }
    }

    companion object {
        private const val TAG = "FaceDetectorAnalyzer"

        //Choosing the options recommended for real-time apps
        private val options : FaceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        private var bitmapAlreadyCreated : Boolean = false
    }
}
