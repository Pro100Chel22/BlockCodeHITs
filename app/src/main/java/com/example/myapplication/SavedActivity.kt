package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.DragEvent
import android.view.View
import android.view.View.OnDragListener
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout.LayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivitySavedBinding
import com.example.myapplication.modules.vibro.Vibration
import org.json.JSONArray

@Suppress("DEPRECATION")
class SavedActivity : AppCompatActivity() {
    private val vibrator = Vibration


    private lateinit var binding : ActivitySavedBinding
    private var topMargin = 20
    private val viewTransparent : Float = 0.3f
    private val viewOpaque : Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_saved)

        binding.bubbleSort.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("bubble_sort", binding.bubbleSort.text.toString())
            startActivity(intent)
            finish()
        }

        binding.twozerotwothree.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("2023", binding.twozerotwothree.text.toString())
            startActivity(intent)
            finish()
        }

        binding.harp.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("harp", binding.harp.text.toString())
            startActivity(intent)
            finish()
        }

        binding.fibonacci.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("fibonacci", binding.fibonacci.text.toString())
            startActivity(intent)
            finish()
        }

        binding.pow.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("pow", binding.pow.text.toString())
            startActivity(intent)
            finish()
        }

        binding.fastEuclid.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("fastEuclid", binding.fastEuclid.text.toString())
            startActivity(intent)
            finish()
        }

        binding.stirlitz.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("stirlitz", binding.stirlitz.text.toString())
            startActivity(intent)
            finish()
        }

        startFunction()
        setParams()

        binding.deleteBlock.setOnDragListener(deleteSavedCode)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    private fun startFunction(){
        val list = getDataFromFile()
        fillFieldWithButtons(list)
    }
    private fun getDataFromFile() : List<String>{
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

        return list
    }
    private fun fillFieldWithButtons(list : List<String>){
        for(i in list.indices){
            val button = createButton(list[i])
            binding.secondParent.addView(button)
        }
    }
    @SuppressLint("UseCompatLoadingForDrawables")
    private fun createButton(text : String) : Button{
        val button = Button(this)
        button.text = text

        val contextWrapper = ContextThemeWrapper(this, R.style.savedButtonStyle)
        val styledButton = Button(contextWrapper, null, 0)

        button.background = styledButton.background
        button.setTextAppearance(R.style.savedButtonStyle)

        button.setOnClickListener {
            val intent = Intent(this, CodingActivity::class.java)
            intent.putExtra("fileName", text)
            startActivity(intent)
            finish()
        }
        button.setOnLongClickListener {
            makeButtonDraggable(it)
        }
        return button
    }


    private fun makeButtonDraggable(button : View) : Boolean{
        val clipText = ""
        val item = ClipData.Item(clipText)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val data = ClipData(clipText, mimeTypes, item)

        val dragShadowBuilder = View.DragShadowBuilder(button)
        button.startDragAndDrop(data, dragShadowBuilder, button, 0)
        button.alpha = viewTransparent

        vibrator.vibrate(this, 100)
        return true
    }


    private val deleteSavedCode = OnDragListener { view, dragEvent ->
        when(dragEvent.action){
            DragEvent.ACTION_DRAG_STARTED -> {
                view.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_trash_open, null)
                dragEvent.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                view.invalidate()
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> true
            DragEvent.ACTION_DRAG_EXITED -> {
                view.invalidate()
                true
            }
            DragEvent.ACTION_DROP -> {
                view.invalidate()

                val v = dragEvent.localState as Button

                val text = v.text.toString()
                deleteSavedCode(text)
                binding.secondParent.removeView(v)

                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.invalidate()
                val v = dragEvent.localState as Button
                v.alpha = viewOpaque
                view.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_trash_close, null)
                true
            }
            else -> false
        }
    }


    private fun deleteSavedCode(fileName : String){
        val fileToDelete = this.getFileStreamPath("$fileName.json")
        val savedProgramsNames = this.getFileStreamPath(R.string.saved_buttons.toString() + ".json")
        val list = getDataFromFile().toMutableList()

        fileToDelete.delete()
        list.remove(fileName)

        val jsonArray = JSONArray(list)
        savedProgramsNames.writeText(jsonArray.toString())
    }


    private fun setParams(){
        for(i in 0 until binding.secondParent.childCount){
            val button = binding.secondParent.getChildAt(i) as Button

            val params = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            params.topMargin = topMargin
            button.layoutParams = params
        }
    }


    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }
}