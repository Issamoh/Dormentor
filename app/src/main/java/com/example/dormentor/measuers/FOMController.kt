package com.example.dormentor.measuers

import android.util.Log
import com.example.dormentor.alerts.AlertsController
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

object FOMController {
    var storage = arrayListOf<Array<Any>>()
    private const val seuilFOMToleree = 0.4


    fun updateFOM() {

        Log.d("FOM_CONTROLLER", "size"+storage.size.toString())
        if (storage.isNotEmpty()){
            val current = LocalDateTime.now()
            var stop = false
            var counterOpen = 0
            var counterTotal = 0
            var fom: Float
            var i = storage.size-1
            while(i>=0 && !stop) {
                //Check first if record is not obsolete (datant de moins de 12 secondes)
                val diff = ChronoUnit.MILLIS.between(storage[i][2] as LocalDateTime,current)
                if (diff >= 12*1000) {
                    stop = true
                } else {
                    //check if probability of the classification is high i.e >80%
                    if (storage[i][1] as Float > 0.8){
                        if (storage[i][0] == "yawn") {
                            counterOpen++
                        }
                        counterTotal++
                    }
                }
                i--
            }
            if (counterTotal >= 30) { //On exige de ne calculer la mesure FOM que si 30 images au moins ont été analysées
                Log.d("FOM_CONTROLLER", "$counterOpen / $counterTotal")
                fom = counterOpen.toFloat() / counterTotal.toFloat()
                if (fom > seuilFOMToleree) {
                    AlertsController.lanerAlerteFOM()
                }
                Log.d("FOM_CONTROLLER", "fom"+fom.toString())
            }
            if (stop) { //means that the loop breaked beacause old record is reached at index i we delete these old records
                Log.d("FOM_CONTROLLER", "clearing $i items")
                if (i == -1) {
                    storage.clear()
                }
                storage.removeAll(storage.subList(0,i+1))
            }
        }
    }
}