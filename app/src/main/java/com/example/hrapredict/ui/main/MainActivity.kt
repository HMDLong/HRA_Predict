package com.example.hrapredict.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hrapredict.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), PredictContract.View {
    private val presenter : PredictPresenter by lazy { PredictPresenter() }
    var count0 = 0
    var count1 = 0
    private val binding : ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.btnStop.setOnClickListener {}
        resetCountText()
        presenter.setView(this)
    }

    private fun resetCountText() {
        count0 = 0
        count1 = 0
        updateCountText()
    }

    private fun updateCountText() {
        binding.txtStatus.text = "count0=$count0\ncount1=$count1"
    }

    override fun onStart() {
        super.onStart()
        presenter.initSession(this.applicationContext)
        presenter.startSession()
    }

    override fun onStop() {
        super.onStop()
        presenter.stopSession()
    }

    override fun onPredictResult(result: String) {
        runOnUiThread {
            binding.pred.text = result
        }
    }

    override fun onError(exception: java.lang.Exception) {
        //TODO("Not yet implemented")
    }

    override fun onSensorChanged() {
//        count1 += 1
//        updateCountText()
    }
}