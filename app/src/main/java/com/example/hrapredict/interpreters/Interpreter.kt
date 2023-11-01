package com.example.hrapredict.interpreters

import android.content.Context
import android.hardware.SensorEventListener
import java.lang.Exception

interface Interpreter {
    fun init(applicationContext: Context)
    fun start()
    fun stop()
    fun setOnSensorListener(listener: SensorEventListener) {}
    fun setOnPredictListener(listener : OnPredictListener) {}

    interface OnPredictListener {
        fun onResult(result: String)
        fun onError(exception: Exception)
    }
}