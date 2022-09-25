package com.example.dormentor.measuers

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object PerclosController {
    var storage = arrayListOf<Array<Any>>()



    fun updatePerclos() {

        Log.d("PERCLOS_CONTROLLER", "size"+storage.size.toString())
        if (storage.size >= 150){
            val current = LocalDateTime.now()
//            storage.subList()
            var stop = false
            var i = storage.size-1
            var counterClosed = 0
            var counterTotal = 0
            var perclos: Float
            while(i>=0 && storage.size-1-i <= 90 && !stop) {

                //Check first if record is not obsolete (datant de moins de 40 secondes)
                val diff = ChronoUnit.MILLIS.between(storage[i][2] as LocalDateTime,current)
                if (diff >= 40*1000) {
                    stop = true
                } else {
                    //check if probability of the classification is high >85%
                    if (storage[i][1] as Float > 0.8){
                        if (storage[i][0] == "Closed") {
                            counterClosed++
                        }
                        counterTotal++
                    }
                }
                i--
            }
            if (stop == true) { //means that the loop breaked beacause old record is reached at index i we delete these old records
                Log.d("PERCLOS_CONTROLLER", "clearing $i items")
                storage.removeAll(storage.subList(0,i))
            }
            if (counterTotal != 0) {
                Log.d("PERCLOS_CONTROLLER", "$counterClosed / $counterTotal")
                perclos = counterClosed.toFloat() / counterTotal.toFloat()
                Log.d("PERCLOS_CONTROLLER", "perclos"+perclos.toString())
            }
        }
    }
}