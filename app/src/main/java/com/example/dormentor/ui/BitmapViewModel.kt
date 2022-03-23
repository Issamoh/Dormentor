package com.example.dormentor.ui

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BitmapViewModel:ViewModel() {
    private var eyeBitmapLiveData : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()
    private var mouthBitmapLiveData : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()

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
}