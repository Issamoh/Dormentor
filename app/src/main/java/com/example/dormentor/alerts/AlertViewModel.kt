package com.example.dormentor.alerts

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class AlertViewModel:ViewModel() {
    private var isAlert : MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private var fomALert: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private var perclosALert: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)

    fun launchAlert() {
        isAlert.value = true
    }
    fun getisAlert(): LiveData<Boolean> {
        return isAlert
    }

    fun setFomAlert() {
        fomALert.value = true
    }
    fun getFomAlert(): LiveData<Boolean> {
        return fomALert
    }

    fun setPerclosAlert() {
        perclosALert.value = true
    }
    fun getPerclosAlert(): LiveData<Boolean> {
        return perclosALert
    }
}