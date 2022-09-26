package com.example.dormentor.alerts

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AlertViewModel:ViewModel() {
    private var isAlert : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    fun launchAlert() {
        isAlert.value = true
    }
    fun getisAlert(): LiveData<Boolean> {
        return isAlert
    }
}