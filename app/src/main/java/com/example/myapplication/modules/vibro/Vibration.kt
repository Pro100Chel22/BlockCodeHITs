package com.example.myapplication.modules.vibro

import android.content.Context
import android.os.Vibrator

object Vibration {
    fun vibrate(context : Context, duration : Long){
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as  Vibrator
        vibrator.vibrate(duration)
    }
}