package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById<Button>(R.id.button).setOnClickListener(View.OnClickListener {
            val intent = Intent();
            intent.setClass(this, CodingActivity::class.java);
            startActivity(intent);
            overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_bottom, androidx.appcompat.R.anim.abc_slide_out_top)
        })

        findViewById<Button>(R.id.button2).setOnClickListener(View.OnClickListener {
            val intent = Intent();
            intent.setClass(this, SavedActivity::class.java);
            startActivity(intent);
            overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_bottom, androidx.appcompat.R.anim.abc_slide_out_top)
        })
    }
}