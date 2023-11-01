package com.example.hrapredict.interpreters

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import com.example.hrapredict.R
import com.example.hrapredict.recorder.SensorRecordSession
import com.example.hrapredict.utils.dataframe.D2Frame
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OrtInterpreter : Interpreter {
    private lateinit var ortEnvironment: OrtEnvironment
    lateinit var dataSession: SensorRecordSession

    lateinit var sfpSession: OrtSession
    lateinit var jspSession: OrtSession
    lateinit var pspSession: OrtSession
//    lateinit var ortSession: OrtSession
    private lateinit var pocketSession: OrtSession
//    private var inputName: String? = null
    private var pocketInputName: String? = null

    private val mainDataLoop = Executors.newScheduledThreadPool(1)
    private var sensorListener: SensorEventListener? = null
    private var predictListener: Interpreter.OnPredictListener? = null

    override fun init(applicationContext: Context) {
        ortEnvironment = OrtEnvironment.getEnvironment()
//        ortSession = loadModel(MODEL_ID, applicationContext)
//        inputName = ortSession.inputNames.iterator().next()
        pspSession = loadModel(PSP_MODEL, applicationContext)
        jspSession = loadModel(JSP_MODEL, applicationContext)
        sfpSession = loadModel(SFP_MODEL, applicationContext)
        pocketSession = loadModel(POCKET_MODEL_ID, applicationContext)
        pocketInputName = pocketSession.inputNames.iterator().next()
        // init data session
        val dummyListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {}
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensorManager = applicationContext.getSystemService(AppCompatActivity.SENSOR_SERVICE) as SensorManager
        dataSession = SensorRecordSession(sensorManager).apply {
            registerSensor(Sensor.TYPE_GRAVITY, object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    sensorListener?.onSensorChanged(event)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    sensorListener?.onAccuracyChanged(sensor, accuracy)
                }
            })
            registerSensor(Sensor.TYPE_LINEAR_ACCELERATION, dummyListener)
            registerSensor(Sensor.TYPE_ROTATION_VECTOR, dummyListener)
        }
    }

    private fun loadModel(modelId: Int, applicationContext: Context): OrtSession {
        val modelBytes = applicationContext.applicationContext.resources.openRawResource(modelId).readBytes()
        val session = ortEnvironment.createSession(modelBytes, OrtSession.SessionOptions().apply {
            addConfigEntry("session.load_model_format", "ORT")
            addConfigEntry("session.use_ort_model_bytes_directly", "1")
        })
        return session
    }

    override fun start() {
        Executors.newScheduledThreadPool(1).schedule(
            { dataSession.startRecord() },
            400L,
            TimeUnit.MILLISECONDS
        )
        mainDataLoop.scheduleAtFixedRate(
            {
                predict(dataSession.getEpochData())
            },
            600L,
            SAMPLING_EPOCH_TIME,
            TimeUnit.MILLISECONDS
        )
    }

    private fun predict(data: D2Frame) {
        try {
            val inputTensor = preProcess(data)
//            val results = ortSession.run(mapOf(inputName to inputTensor))
            val pocket = pocketSession.run(mapOf(pocketInputName to inputTensor))
            val intPocket = postProcess(pocket)
            val position = when(intPocket) {
                0 -> predict2(data, sfpSession)
                1 -> predict2(data, jspSession)
                2 -> predict2(data, pspSession)
                else -> -1
            }
            predictListener?.onResult(mapResultToString(intPocket, position))
        } catch (e: Exception) {
            predictListener?.onError(e)
            e.printStackTrace()
        }
    }

    private fun predict2(data: D2Frame, model: OrtSession) : Int {
        val inputTensor = preProcess2(data)
        val inputName = model.inputNames.iterator().next()
        val results = model.run(mapOf(inputName to inputTensor))
        return postProcess(results)
    }

    /**
     * Converts raw data type (D2Frame) from sensors into input type of model
     * @param data a D2Frame to convert
     * @return data in model-input type format, in this case OnnxTensor
     */
    private fun preProcess(data: D2Frame) : OnnxTensor {
//        val dataChunks = data.chunk(CHUNK_SIZE)
//        val flattenChunks = dataChunks.map { d2Frame -> d2Frame.extractMeanAndStd() }.reduce { floatArr1, floatArr2 -> floatArr1 + floatArr2 }
//        val floatBufferInput = FloatBuffer.wrap(flattenChunks)
        val floatBufferInput = FloatBuffer.wrap(data.flattenByRow())
//        return OnnxTensor.createTensor(ortEnvironment, floatBufferInput, longArrayOf(dataChunks.size.toLong(), 18))
        return OnnxTensor.createTensor(ortEnvironment, floatBufferInput, longArrayOf(data.shape.first.toLong(), data.shape.second.toLong()))
    }

    private fun preProcess2(data: D2Frame) : OnnxTensor {
        val floatBufferInput = FloatBuffer.wrap(data.flattenByRow())
        return OnnxTensor.createTensor(ortEnvironment, floatBufferInput, longArrayOf(data.shape.first.toLong(), data.shape.second.toLong()))
    }

    /**
     * Convert the output of the model into string representation to return to UI
     * @param results OrtSession.Result object
     * @return a string representing the result
     */
    private fun postProcess(results: OrtSession.Result) : Int {
        val output = results[0].value as Array<LongArray>
        val (result, _) = output.map { array ->
            array.indexOfFirst { value -> value == array.maxOrNull() }
        }.groupingBy { it }.eachCount().maxByOrNull { it.value }!!
        return result
    }

    private fun mapResultToString(pocket: Int, position: Int) : String {
        val stringPosition = when (position) {
            0 -> "Sit"
            1 -> "Stand"
            2 -> "StandUp"
            3 -> "SitDown"
            else -> "Unknown"
        }
        val stringPocket = when (pocket) {
            0 -> "shirt front"
            1 -> "jacket side"
            2 -> "pant side"
            else -> "Unknown"
        }
        return "$stringPocket/$stringPosition"
    }

    override fun stop() {
        dataSession.stopRecord()
    }

    override fun setOnSensorListener(listener: SensorEventListener){
        sensorListener = listener
    }

    override fun setOnPredictListener(listener : Interpreter.OnPredictListener) {
        predictListener = listener
    }

    companion object {
        const val SAMPLING_EPOCH_TIME = 250L //ms
        const val CHUNK_SIZE = 20
        const val MODEL_ID = R.raw.rf_3feat2class_1_0
        const val JSP_MODEL = R.raw.rf_jsp_2_0
        const val PSP_MODEL = R.raw.rf_psp_2_0
        const val SFP_MODEL = R.raw.rf_sfp_2_0
        const val POCKET_MODEL_ID = R.raw.rf_2pocket_2_0_nodropmove
    }
}