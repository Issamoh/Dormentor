package com.example.dormentor.measuers

import android.util.Log
import com.example.dormentor.alerts.AlertsController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object PerclosController {
    var storage = arrayListOf<Array<Any>>()
    private const val seuilPerclosToleree = 0.6


    fun updatePerclos() {

        Log.d("PERCLOS_CONTROLLER", "size"+storage.size.toString())
        if (storage.isNotEmpty()){
            val current = LocalDateTime.now()
            var stop = false
            var counterClosed = 0
            var counterTotal = 0
            var perclos: Float
            var i = storage.size-1
            while(i>=0 && !stop) {
                //Check first if record is not obsolete (datant de moins de 12 secondes)
                val diff = ChronoUnit.MILLIS.between(storage[i][2] as LocalDateTime,current)
                if (diff >= 12*1000) {
                    stop = true
                } else {
                    //check if probability of the classification is high i.e >80%
                    if (storage[i][1] as Float > 0.8){
                        if (storage[i][0] == "Closed") {
                            counterClosed++
                        }
                        counterTotal++
                    }
                }
                i--
            }
            if (counterTotal >= 30) { //On exige de ne calculer la mesure perclos que si 30 images au moins ont été analysées
                Log.d("PERCLOS_CONTROLLER", "$counterClosed / $counterTotal")
                perclos = counterClosed.toFloat() / counterTotal.toFloat()
                if (perclos > seuilPerclosToleree) {
                    AlertsController.lanerAlertePerclos()
                }
                Log.d("PERCLOS_CONTROLLER", "perclos"+perclos.toString())
            }
            if (stop) { //means that the loop breaked beacause old record is reached at index i we delete these old records
                Log.d("PERCLOS_CONTROLLER", "clearing $i items")
                if (i == -1) {
                    storage.clear()
                }
                storage.removeAll(storage.subList(0,i+1))
            }
        }
    }
}