package com.example.hrapredict.recorder

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.hrapredict.utils.dataframe.D2Frame

class SensorRecordSession(private val sensorManager: SensorManager) {
    private val _sensors = mutableListOf<DataRecorder>()
    var recording = false

    fun registerSensor(sensorType : Int, listener: SensorEventListener){
        _sensors.add(DataRecorder(sensorType, listener))
    }

    fun reset(){
        stopRecord()
        for (sensor in _sensors) {
            sensor.destroy()
        }
        _sensors.clear()
    }

    fun getEpochData() : D2Frame {
        stopRecord()
//        val res = _sensors[0].frame.concat(_sensors[1].frame)
//        val res = _sensors[0].frame.dataframe
        val res = _sensors.map { it.frame }.reduce { acc, d2Frame ->
            acc.concat(d2Frame)
        }
        startRecord()
        return res
    }

    fun startRecord(){
        if(_sensors.isEmpty()) return
        clear()
        recording = true
    }

    private fun clear() {
        for(sensor in _sensors){
            sensor.clearData()
        }
    }

    fun stopRecord(){
        recording = false
    }

    inner class DataRecorder(sensorType : Int, listener: SensorEventListener) {
        private val sensor by lazy { sensorManager.getDefaultSensor(sensorType) }
        var frame = D2Frame(3)
        private val sensorListener by lazy {
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if(!recording)
                        return
                    event?.values?.let {
                        frame.putValue(it[0], it[1], it[2])
                    }
                    listener.onSensorChanged(event)
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    listener.onAccuracyChanged(sensor, accuracy)
                }
            }
        }

        init {
            sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        fun clearData(){
            frame.clearData()
        }

        fun destroy(){
            sensorManager.unregisterListener(sensorListener)
        }
    }
}