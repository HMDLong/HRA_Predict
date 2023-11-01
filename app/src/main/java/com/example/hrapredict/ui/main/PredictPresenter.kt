package com.example.hrapredict.ui.main

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.example.hrapredict.interpreters.Interpreter
import com.example.hrapredict.interpreters.OrtInterpreter
import java.lang.Exception

class PredictPresenter : PredictContract.Presenter {
    private var view: PredictContract.View? = null
    private var interpreter: Interpreter = OrtInterpreter()

    override fun setView(view_ : PredictContract.View){
        view = view?: view_
    }

    override fun initSession(applicationContext: Context) {
        interpreter.init(applicationContext)
    }

    override fun startSession(){
        interpreter.apply {
            setOnPredictListener(object : Interpreter.OnPredictListener {
                override fun onResult(result: String) {
                    view?.onPredictResult(result)
                }
                override fun onError(exception: Exception) {
                    view?.onError(exception)
                }
            })
            setOnSensorListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    view?.onSensorChanged()
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            })
        }
        interpreter.start()
    }

    override fun stopSession(){
        interpreter.stop()
    }
}