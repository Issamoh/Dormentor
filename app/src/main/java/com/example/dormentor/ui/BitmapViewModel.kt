package com.example.dormentor.ui

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BitmapViewModel:ViewModel() {
    private var bitmapLiveData : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()

    public fun getBitmap(): LiveData<Bitmap> {
        return bitmapLiveData
    }
    public fun changeBitmap(bitmap: Bitmap) {
        bitmapLiveData.value = bitmap
    }
}