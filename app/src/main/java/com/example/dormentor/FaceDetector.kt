package com.example.dormentor

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.dormentor.measuers.PerclosController
import com.example.dormentor.ml.*
import com.example.dormentor.ui.BitmapViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import util.BitmapUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
class FaceDetector(private val lifecycleOwner: LifecycleOwner) : ImageAnalysis.Analyzer {

    val bitmapViewModel : BitmapViewModel = ViewModelProvider(lifecycleOwner as ViewModelStoreOwner).get(BitmapViewModel::class.java)
//    private val alexNetV2 = AlexNetV2.newInstance(lifecycleOwner as Context)
//    private val mobileNetEyesRLDD55 = MobileNetEyesRLDD55.newInstance(lifecycleOwner as Context)
//    private val mobileNetMouthCrop55 = MobileNetMouthCrop55.newInstance(lifecycleOwner as Context)
//    private val mobileNetMouthCrop55V3 = MobileNetMouthCrop55V3.newInstance(lifecycleOwner as Context)
    private val mobileNetV3MouthCrop55V4 = MobileNetV3MouthCrop55V4.newInstance(lifecycleOwner as Context)
    private val mobileNetV3EyesV4 = MobileNetV3EyesV4.newInstance(lifecycleOwner as Context)



    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        //Creating Bitmap image based on ImageProxy to be used after for display
        val bitmap : Bitmap? = BitmapUtils.getBitmap(image)

        //Creating InputImage object used as input for the faceDetector

        val img = image.image
        val inputImage = img?.let { InputImage.fromMediaImage(it,image.imageInfo.rotationDegrees) }
        if (inputImage != null) {
            val begin = System.currentTimeMillis()
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                        for (face in faces) {
                        bitmap?.let {
                            var isCreated = bitmapViewModel.getIsBitmapCreated().value
                            if(!isCreated!! || true) {
                                val eyeBitmap = extractEyeRegion(face, it,5)
                                eyeBitmap?.let { ms ->
                                    val copyeyeBitmap = ms.copy(ms.config,true)
                                    val wholePictureResizd1 = BitmapUtils.getResizedBitmap(copyeyeBitmap,256,256)
                                    wholePictureResizd1?.let { it1 -> bitmapViewModel.changeEyeBitmap(it1) }
                                }
                                val eyeBitmapBig =
                                    BitmapUtils.getResizedBitmap(eyeBitmap, 55, 55)
                                eyeBitmapBig?.let {
//                                        it1 -> bitmapViewModel.changeEyeBitmap(it1)
                                }
                                val tfEyeImageA = TensorImage.fromBitmap(eyeBitmapBig)
                                val tfEyeImage = TensorImage.createFrom(tfEyeImageA, DataType.FLOAT32)
                                var outputs =
                                    mobileNetV3EyesV4.process(tfEyeImage).probabilityAsCategoryList
                                             .apply {
                                                     sortByDescending { it.score } // Sort with highest confidence first
                                                 }

                                outputs.forEach {
                                    Log.d(TAG, "Eye is: "+it.label + " " + it.score)
                                }
                                Log.d(TAG, "Eye status : " + outputs[0].label + " * " + outputs[0].score)

                                val current = LocalDateTime.now()
                                PerclosController.storage.add(arrayOf(outputs[0].label,outputs[0].score,current))
                                bitmapViewModel.changeEyeStatusLabelScore(outputs[0].label, outputs[0].score)


                                val mouthBitmap = extractMouthRegion(face, it, 20)
                                mouthBitmap?.let { bs ->
                                    val copymouthBitmap = bs.copy(bs.config,true)
                                    val wholePictureResizd = BitmapUtils.getResizedBitmap(copymouthBitmap,256,256)
                                    wholePictureResizd?.let { it2 -> bitmapViewModel.changeMouthBitmap(it2) }
                                }
                                mouthBitmap?.let { bimp ->
                                    val mouthBitmapBig =
                                        BitmapUtils.getResizedBitmap(bimp, 55, 55)
                                    mouthBitmapBig?.let {
//                                            it2 -> bitmapViewModel.changeMouthBitmap(it2)
                                    }
                                    val tfMouthImageA = TensorImage.fromBitmap(mouthBitmapBig)
                                    val tfMouthImage = TensorImage.createFrom(tfMouthImageA, DataType.FLOAT32)
                                    var outputs2 = mobileNetV3MouthCrop55V4.process(tfMouthImage).probabilityAsCategoryList
                                        .apply {
                                            sortByDescending { it.score } // Sort with highest confidence first
                                        }
                                    val end = System.currentTimeMillis()
                                    bitmapViewModel.changeElapsed(end-begin)
                                    Log.d(TAG, "Mouth status : " + outputs2[0].label + " * " + outputs2[0].score)
                                    bitmapViewModel.changeMouthStatusLabelScore(outputs2[0].label, outputs2[0].score)
                                }
                                bitmapViewModel.changeIsBitmapCreated()
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


    private fun extractEyeRegion(face: Face, srcBitmap: Bitmap, margin: Int) : Bitmap? {
        val rightEyeContours = face.getContour(FaceContour.LEFT_EYE)?.points
        val rightEyeBrowContours = face.getContour(FaceContour.LEFT_EYEBROW_TOP)?.points


        if(rightEyeContours != null && rightEyeBrowContours != null) {
            //Finding the starting point (nearestX,highestY) :

            //1. finding the highest point in the eye brow:
            val highestY = rightEyeBrowContours.minByOrNull { it.y }!!.y

            //2. finding the nearest point to face's edge
            val nearestX = abs(min(rightEyeBrowContours[0].x, rightEyeContours[0].x))

            //Finding the width of the eye :
            // 1. finding the furthest point of the eye
            val furthestXeyeBrow = rightEyeBrowContours.maxByOrNull { it.x }!!.x

            val furthestXeye = rightEyeContours.maxByOrNull { it.x }!!.x

            val furthestX = abs(max(furthestXeyeBrow, furthestXeye))

            // 2. Calculating the width
            var width = abs((furthestX - nearestX).toInt())

            //Finding the height of the eye :
            //1. finding the lowest point of the eye :
            val lowestY = rightEyeContours.maxByOrNull { it.y }!!.y

            //2. Calculating the height
            val height = abs((lowestY - highestY).toInt())

           // case without margin
//            return Bitmap.createBitmap(
//                srcBitmap,
//                nearestX.toInt(),
//                highestY.toInt(),
//                width,
//                height
//            )
            var result : Bitmap? = null
            try {
            //case with margin
            result =  Bitmap.createBitmap(
                srcBitmap,
                nearestX.toInt(),
                highestY.toInt(),
                min((width+margin).toInt(),srcBitmap.width),
                min((height+margin).toInt(),srcBitmap.height)

            )
            }
            catch (e: java.lang.IllegalArgumentException) {
                    e.message?.let { Log.e(TAG, it) }
            } finally {
                 return result
            }

        } else {
            return null
        }
    }

    private fun extractMouthRegion(face: Face, srcBitmap: Bitmap, margin: Int) : Bitmap? {
        val upLipTop = face.getContour(FaceContour.UPPER_LIP_TOP)?.points
        val lowLipBottom = face.getContour(FaceContour.LOWER_LIP_BOTTOM)?.points


        if(upLipTop != null && lowLipBottom != null) {
            //Finding the starting point (nearestX,highestY) :
                // 1. Finding the nearest point of the mouth to face's edge
            val nearestXUp = upLipTop.minByOrNull { it.x }!!.x
            val nearestXLow = lowLipBottom.minByOrNull { it.x }!!.x
            val nearestX = min(nearestXUp,nearestXLow)

                //2. Finding the highest point of the mouth
            val highestY = upLipTop.minByOrNull { it.y }!!.y

            //Finding the width of the mouth :
                // 1. Finding the furthest point of the mouth :
            val furthestXUp = upLipTop.maxByOrNull { it.x }!!.x
            val furthestXLow = lowLipBottom.maxByOrNull { it.x }!!.x
            val furthestX = max(furthestXLow,furthestXUp)
                // 2. Calculating the width :
            val width = abs(furthestX - nearestX).toInt()

            //Finding the height of the mouth :
                //1. Finding the lowest point of the mouth :
            val lowestY = lowLipBottom.maxByOrNull { it.y }!!.y
                //2. Calculating the height :
            val height = abs(lowestY - highestY).toInt()
            Log.d(TAG,"****"+srcBitmap.height)
            Log.d(TAG,"****"+highestY)
            Log.d(TAG,"****"+height)
            Log.d(TAG,"****"+(highestY.toInt()+srcBitmap.height))
            var result : Bitmap? = null
            try {
//                case without margin
//             result =  Bitmap.createBitmap(
//                srcBitmap,
//                nearestX.toInt(),
//                highestY.toInt(),
//                width,
//                height
//            )
                result = Bitmap.createBitmap(
                    srcBitmap,
                    nearestX.toInt(),
                    highestY.toInt(),
                    min((width+margin).toInt(),srcBitmap.width),
                    min((height+margin).toInt(),srcBitmap.height)

                )
            } catch (e: java.lang.IllegalArgumentException) {
                e.message?.let { Log.e(TAG, it) }
            } finally {
                return result
            }

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
    }
}
