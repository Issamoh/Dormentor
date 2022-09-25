package com.example.dormentor.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dormentor.FaceDetector

class BitmapViewModel:ViewModel() {
    private var eyeBitmapLiveData : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()
    private var mouthBitmapLiveData : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()
    private var eyeStatusLabel : MutableLiveData<String> = MutableLiveData<String>()
    private var eyeStatusScore : MutableLiveData<Float> = MutableLiveData<Float>()
    private var mouthStatusLabel : MutableLiveData<String> = MutableLiveData<String>()
    private var mouthStatusScore : MutableLiveData<Float> = MutableLiveData<Float>()
    private var isbitmapCreated : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private var perclos : MutableLiveData<Array<Int>> = MutableLiveData<Array<Int>>(arrayOf(0,0))
    private var fom : MutableLiveData<Array<Int>> = MutableLiveData<Array<Int>>(arrayOf(0,0))
    private var elapsedTime : MutableLiveData<Long> = MutableLiveData<Long>()

    public fun getEyeBitmap(): LiveData<Bitmap> {
        return eyeBitmapLiveData
    }
    public fun getMouthBitmap(): LiveData<Bitmap> {
        return mouthBitmapLiveData
    }
    public fun changeEyeBitmap(bitmap: Bitmap) {
        eyeBitmapLiveData.value = bitmap
    }

    fun changeMouthBitmap(bitmap: Bitmap) {
        mouthBitmapLiveData.value = bitmap
    }
    fun changeEyeStatusLabelScore(newLabel: String,newScore: Float){
        eyeStatusLabel.value = newLabel
        eyeStatusScore.value = newScore

        var newPerclos = perclos.value

        if (newScore > 0.8) {
            if (newLabel == "Closed") {
                newPerclos!![0]++
            }

            newPerclos!![1]++
        }
        perclos.value = newPerclos
    }
    fun getEyeStatusLabel():LiveData<String>{
        return eyeStatusLabel
    }
    fun getEyeStatusScore():LiveData<Float>{
        return eyeStatusScore
    }

    fun changeMouthStatusLabelScore(newLabel: String,newScore: Float){
        mouthStatusLabel.value = newLabel
        mouthStatusScore.value = newScore

        var newFom = fom.value
        if (newScore > 0.8) {
            if (newLabel == "yawn") {
                newFom!![0]++
            }

            newFom!![1]++
        }
        fom.value = newFom
    }
    fun getmouthStatusLabel():LiveData<String>{
        return mouthStatusLabel
    }
    fun getmouthStatusScore():LiveData<Float>{
        return mouthStatusScore
    }

    fun getPerclos():LiveData<Array<Int>> {
        return perclos
    }

    fun getFom():LiveData<Array<Int>> {
        return fom
    }

    fun getIsBitmapCreated(): LiveData<Boolean> {
        return isbitmapCreated
    }

    fun changeIsBitmapCreated() {
        isbitmapCreated.value = isbitmapCreated.value != true
    }

    fun getElapsed():LiveData<Long> {
        return elapsedTime
    }
    fun changeElapsed(newValue: Long) {
        elapsedTime.value = newValue
    }

}