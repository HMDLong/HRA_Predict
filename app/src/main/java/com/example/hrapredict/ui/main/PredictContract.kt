package com.example.hrapredict.ui.main

import android.content.Context
import com.example.hrapredict.utils.dataframe.D2Frame
import java.lang.Exception

interface PredictContract {
    interface View {
        fun onPredictResult(result: String)
        fun onError(exception: Exception)
        fun onSensorChanged()
    }

    interface Presenter {
        fun setView(view_: View)
        fun initSession(applicationContext: Context)
        fun startSession()
        fun stopSession()
//        fun predict(data: D2Frame)
    }
}