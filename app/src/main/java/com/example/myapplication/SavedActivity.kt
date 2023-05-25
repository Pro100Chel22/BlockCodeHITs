package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.LayoutParams
import android.widget.ScrollView
import android.widget.Toast
import com.example.myapplication.databinding.ActivitySavedBinding
import com.example.myapplication.modules.InstructionType
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("DEPRECATION")
class SavedActivity : AppCompatActivity() {

    private lateinit var binding : ActivitySavedBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        getDataFromFile()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }


    private fun getDataFromFile(){
        val file = this.getFileStreamPath(R.string.saved_buttons.toString() + ".json")
        val list = mutableListOf<String>()
        if(file.exists() && file.length() > 0){
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)

            for(i in 0 until jsonArray.length()){
                val item = jsonArray.get(i).toString()
                list.add(item)
            }
        }

        fillFieldWithButtons(list)
    }
    private fun fillFieldWithButtons(list : List<String>){
        for(i in list.indices){
            val button = createButton(list[i])
            findViewById<LinearLayout>(R.id.parent).addView(button)
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun createButton(text : String) : Button{
        val button = Button(this)
        button.text = text

        val contextWrapper = ContextThemeWrapper(this, R.style.savedButtonStyle)
        val styledButton = Button(contextWrapper, null, 0)

        button.background = styledButton.background
        button.setTextColor(resources.getColor(R.color.black))

        val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.topMargin = 20
        button.layoutParams = params

        button.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("fileName", text)
            startActivity(intent)
            finish()
        }
        return button
    }


    private fun makeButtonDraggable(button : Button) : Boolean{
        val clipText = ""
        val item = ClipData.Item(clipText)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val data = ClipData(clipText, mimeTypes, item)

        val dragShadowBuilder = View.DragShadowBuilder(button)
        button.startDragAndDrop(data, dragShadowBuilder, button, 0)
        button.alpha = 0.3f
        return true
    }

    /*private fun setListenersToButtons(){
        for(i in 0 until )
    }*/


    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }
}