package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("DEPRECATION")
class SavedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        readFromFile()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private fun readFromFile(){
        val fileName = "button.json"
        val fileInputStream = openFileInput(fileName)
        val inputStreamReader = InputStreamReader(fileInputStream)
        val bufferReader = BufferedReader(inputStreamReader)

        val stringBuilder = StringBuilder()
        var line : String?
        while(bufferReader.readLine().also{line = it} != null){
            stringBuilder.append(line)
        }
        fileInputStream.close()
        val jsonArray = JSONArray(stringBuilder.toString())
        for(i in 0 until jsonArray.length()){
            val buttonObject = jsonArray.getJSONObject(i)
            val buttonText = buttonObject.getString("button_text")

            val button = Button(this)
            button.text = buttonText
            findViewById<LinearLayout>(R.id.parent).addView(button)
        }
        Toast.makeText(this, findViewById<LinearLayout>(R.id.parent).childCount.toString(), Toast.LENGTH_SHORT).show()
    }
    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }
}