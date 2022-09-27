package com.example.dormentor.alerts

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.dormentor.R
import com.example.dormentor.measuers.PerclosController

object AlertsController {
    lateinit var alertViewModel: AlertViewModel
    private var alertAudioEnCours = false
    private val dureeAlert:Long = 5*1000


    val audioAlertsHandler = Handler(Looper.getMainLooper())
    private val audioAlertsRunnable = object : Runnable {
        override fun run(){
            alertAudioEnCours = false
        }
    }

    fun lanerAlertePerclos() {
        //        visual alert
        alertViewModel.setPerclosAlert()

        if (!alertAudioEnCours) {
            alertAudioEnCours = true
            alertViewModel.launchAlert()
            audioAlertsHandler.postDelayed({
                audioAlertsRunnable.run()
            },dureeAlert)

        }
    }

    fun lanerAlerteFOM() {
//        visual alert
        alertViewModel.setFomAlert()

//        audio alert
        if (!alertAudioEnCours) {
            alertAudioEnCours = true
            alertViewModel.launchAlert()
            audioAlertsHandler.postDelayed({
                audioAlertsRunnable.run()
            },dureeAlert)

        }
    }


}