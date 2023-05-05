package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager

@Suppress("DEPRECATION")
class SavedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }
}