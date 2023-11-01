package com.example.hrapredict.ui.main

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.hrapredict.recorder.SensorRecordSession
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PredictService : Service() {
    lateinit var ortSession: OrtSession
    lateinit var ortEnvironment: OrtEnvironment
    lateinit var dataSession: SensorRecordSession
    private val predictExecutor = Executors.newScheduledThreadPool(2)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Executors.newScheduledThreadPool(1).schedule(
            { dataSession.startRecord() },
            400,
            TimeUnit.MILLISECONDS
        )
        predictExecutor.scheduleAtFixedRate(
            {
                val data = dataSession.getEpochData()
                predictExecutor.submit {
                    //makePrediction(data)
                }
            },
            600,
            200,
            TimeUnit.MILLISECONDS
        )
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        dataSession.stopRecord()
        super.onDestroy()
    }

    private fun makePrediction(data: FloatArray){
        val inputName = ortSession.inputNames?.iterator()?.next()
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(data), longArrayOf(1, 6))
        val results = ortSession.run(mapOf(inputName to inputTensor))
        val output = results[0].value as LongArray
        Log.d("output", output[0].toString())
    }
}