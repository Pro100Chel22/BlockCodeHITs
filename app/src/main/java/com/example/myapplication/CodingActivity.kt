package com.example.myapplication

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivityCodingBinding
import com.example.myapplication.databinding.LayoutConsoleBinding
import com.example.myapplication.databinding.LayoutNewBlocksBinding
import com.example.myapplication.modules.BlockView
import com.example.myapplication.modules.InstructionType
import com.example.myapplication.modules.getListBlocks
import com.example.myapplication.modules.getListBlocksEnds
import com.example.myapplication.modules.getListBlocksNotHaveText
import com.google.android.material.bottomsheet.BottomSheetDialog

@Suppress("DEPRECATION")
class CodingActivity : AppCompatActivity() {
    private var scaleDp: Float = 1f

    private var listBlocks = getListBlocks()
    private var listBlocksNotHaveText = getListBlocksNotHaveText()
    private var listBlocksEnds = getListBlocksEnds()

    private lateinit var bottomSheetDialogNewBlock: BottomSheetDialog
    private lateinit var bottomSheetDialogConsole: BottomSheetDialog

    private lateinit var binding : ActivityCodingBinding
    private lateinit var bottomSheetViewNewBlockBinding : LayoutNewBlocksBinding
    private lateinit var bottomSheetViewConsoleBinding : LayoutConsoleBinding

    private var numberOfBlockFieldChildren :Int = 0
    private val regularBlockWidth : Int = 350;  private val regularBlockHeight : Int = 90
    private val specificBlockWidth : Int = 200; private val specificBlockHeight: Int = 55

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coding)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        scaleDp = resources.displayMetrics.density

        binding = DataBindingUtil.setContentView(this, R.layout.activity_coding)

        initBottomSheetViewNewBlock()
        initBottomSheetConsole()

        binding.buttonCodingSwapMode.setOnClickListener {
            val sharedPrefs = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putInt("UI_MODE_NIGHT", AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putInt("UI_MODE_NIGHT", AppCompatDelegate.MODE_NIGHT_NO)
            }

            editor.apply()
        }
        binding.buttonAddNewBlock.setOnClickListener {
            bottomSheetDialogNewBlock.show()
        }
        binding.buttonConsole.setOnClickListener{
            bottomSheetDialogConsole.show()
        }
        binding.deleteBlock.setOnDragListener(deleteBlock)
    }


    ////////////////////////////
    val deleteBlock = View.OnDragListener { view, dragEvent ->
        when(dragEvent.action){
            DragEvent.ACTION_DRAG_STARTED -> {
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
                val v = dragEvent.localState as ConstraintLayout
                val owner = v.parent as ViewGroup
                val ownerParent = owner.parent as ViewGroup
                ownerParent.removeView(owner)
                numberOfBlockFieldChildren--
                true
            }
            else -> false
        }
    }

    val containerDragListener = View.OnDragListener{view, event ->
        when(event.action){
            DragEvent.ACTION_DRAG_STARTED -> {
                event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
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
                val v = event.localState as ViewGroup
                val owner = v.parent as ViewGroup
                val destination = view as ConstraintLayout
                val destinationChild = destination.getChildAt(0)
                if(destination.height < owner.height){
                    val ownerParams = owner.layoutParams; val destinationParams = destination.layoutParams

                    owner.removeView(v); destination.removeView(destinationChild)
                    owner.addView(destinationChild); destination.addView(v)

                    owner.layoutParams = destinationParams; destination.layoutParams = ownerParams

                    v.visibility = View.VISIBLE
                    return@OnDragListener false
                }
                owner.removeView(v)
                if(destination.parent !== owner.parent || destination.height != owner.height){
                    owner.addView(v)
                    v.visibility = View.VISIBLE
                }
                else{
                    if(destination.childCount > 0){
                        val destinationChild = destination.getChildAt(0)
                        destination.removeView(destinationChild)
                        owner.addView(destinationChild)
                    }
                    destination.addView(v)
                    v.visibility = View.VISIBLE
                }
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val v = event.localState as ViewGroup; v.visibility = View.VISIBLE
                view.invalidate()
                true
            }
            else -> false
        }
    }

    /////////////////////////////////
    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }


    private fun initBottomSheetViewNewBlock () {
        val layoutParamsBottomSheet = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (500 * scaleDp + 0.5).toInt())

        bottomSheetViewNewBlockBinding = DataBindingUtil.inflate(layoutInflater, R.layout.layout_new_blocks, null, false)
        bottomSheetViewNewBlockBinding.modalBottomSheetContainer.layoutParams = layoutParamsBottomSheet
        bottomSheetDialogNewBlock = BottomSheetDialog(this, R.style.BottomSheetDialogThem)
        bottomSheetDialogNewBlock.setContentView(bottomSheetViewNewBlockBinding.root)

        val containerBottomSheetView = bottomSheetViewNewBlockBinding.blocks

        val listCategory = mapOf(
            "Variables" to View.generateViewId(),
            "Inputs and outputs" to View.generateViewId(),
            "Conditions" to View.generateViewId(),
            "Cycles" to View.generateViewId(),
            "Functions" to View.generateViewId()
        )

        for ((category, id) in listCategory) {
            containerBottomSheetView.addView(buildTextView(category))
            containerBottomSheetView.addView(buildContainerBlocks(id))
        }

        for ((key, blockView) in listBlocks) {
            if(key !in listBlocksEnds && key != InstructionType.ELSE && key != InstructionType.ELIF){
                val block = buildBlock(blockView, key)

                val addBlocks = { v: View ->
                    // <------------Код для добавления блоков в поле--------->
                    if(key in listBlocksNotHaveText && numberOfBlockFieldChildren == 0){
                        Toast.makeText(this, "You cant place it as first element", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        var isSpecific = true
                        var heightParams : Int = specificBlockHeight; var widthParams : Int = specificBlockWidth
                        if(key !in listBlocksNotHaveText){
                            heightParams = regularBlockHeight; widthParams = regularBlockWidth
                            isSpecific = false
                        }
                        val container = ConstraintLayout(this)
                        val contParams = ConstraintLayout.LayoutParams((widthParams * scaleDp + 0.5).toInt(), (heightParams * scaleDp + 20.5).toInt())
                        container.layoutParams = contParams
                        container.setOnDragListener(containerDragListener)
                        //container.setBackgroundColor(getResources().getColor(R.color.white));
                        container.id = numberOfBlockFieldChildren * 1000


                        val newBlock = buildBlock(blockView, key)
                        newBlock.setOnLongClickListener{
                            val clipText = ""
                            val item = ClipData.Item(clipText)
                            val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            val data = ClipData(clipText, mimeTypes, item)

                            val dragShadowBuilder = View.DragShadowBuilder(it)
                            it.startDragAndDrop(data, dragShadowBuilder, it, 0)
                            it.visibility = View.INVISIBLE
                            true
                        }
                        container.addView(newBlock)

                        binding.blockField.addView(container)

                        numberOfBlockFieldChildren++

                        Toast.makeText(this, blockView.instruction, Toast.LENGTH_SHORT).show()
                    }
                }

                block.setOnClickListener(addBlocks)
                block.findViewById<Button>(R.id.buttonBreakPoint).setOnClickListener(addBlocks)

                if(key !in listBlocksNotHaveText) {
                    val editText: EditText = block.findViewById(R.id.inputExpression)
                    editText.isFocusable = false
                    editText.keyListener = null
                    editText.setOnClickListener(addBlocks)
                }

                val categoryId = when (key) {
                    InstructionType.VAR,
                    InstructionType.SET -> {
                        listCategory.getValue("Variables")
                    }
                    InstructionType.PRINT,
                    InstructionType.INPUT -> {
                        listCategory.getValue("Inputs and outputs")
                    }
                    InstructionType.IF,
                    InstructionType.ENDCHOICEIF,
                    InstructionType.ENDIF,
                    InstructionType.ELIF,
                    InstructionType.ELSE -> {
                        listCategory.getValue("Conditions")
                    }
                    InstructionType.FOR,
                    InstructionType.WHILE,
                    InstructionType.BREAK,
                    InstructionType.CONTINUE,
                    InstructionType.ENDFOR,
                    InstructionType.ENDWHILE -> {
                        listCategory.getValue("Cycles")
                    }
                    InstructionType.FUNC,
                    InstructionType.RETURN,
                    InstructionType.ENDFUNC,
                    InstructionType.END -> {
                        listCategory.getValue("Functions")
                    }
                    else -> 0
                }

                if(categoryId != 0) {
                    containerBottomSheetView.findViewById<LinearLayout>(categoryId).addView(block)
                }
            }
        }
    }

    private fun initBottomSheetConsole () {
        val layoutParamsBottomSheet = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (500 * scaleDp + 0.5).toInt())

        bottomSheetViewConsoleBinding = DataBindingUtil.inflate(layoutInflater, R.layout.layout_console, null, false)
        bottomSheetViewConsoleBinding.modalBottomSheetContainer.layoutParams = layoutParamsBottomSheet
        bottomSheetDialogConsole = BottomSheetDialog(this, R.style.BottomSheetDialogThem)
        bottomSheetDialogConsole.setContentView(bottomSheetViewConsoleBinding.root)
    }

    private fun buildBlock(blockView: BlockView, instructionType: InstructionType): View {
        val block = layoutInflater.inflate(blockView.layout, null)

        val shapeBlock = block.background as GradientDrawable
        shapeBlock.setColor(ContextCompat.getColor(this, blockView.colorFill))
        shapeBlock.setStroke((3 * scaleDp + 0.5).toInt(), ContextCompat.getColor(this, blockView.colorStroke))

        val layoutParams = if(instructionType !in listBlocksNotHaveText) {
            val shapeText = block.findViewById<EditText>(R.id.inputExpression).background as GradientDrawable
            shapeText.setColor(ContextCompat.getColor(this, R.color.color_window_text_block))
            shapeText.setStroke((2 * scaleDp + 0.5).toInt(), ContextCompat.getColor(this, blockView.colorStroke))
            LinearLayout.LayoutParams((regularBlockWidth * scaleDp + 0.5).toInt(), (regularBlockHeight * scaleDp + 0.5).toInt())
        } else {
            LinearLayout.LayoutParams((specificBlockWidth * scaleDp + 0.5).toInt(), (specificBlockHeight * scaleDp + 0.5).toInt())
        }
        layoutParams.bottomMargin = (10 * scaleDp + 0.5).toInt()

        block.layoutParams = layoutParams
        if(instructionType !in listBlocksEnds) {
            block.findViewById<TextView>(R.id.instructionType).text = blockView.instruction
        }

        val breakPoint = block.findViewById<Button>(R.id.buttonBreakPoint)
        val shapeBreakPoint = breakPoint.background as GradientDrawable
        shapeBreakPoint.setColor(ContextCompat.getColor(this, R.color.break_point_flag))
        breakPoint.setOnClickListener{
            if (shapeBreakPoint.color?.defaultColor == ContextCompat.getColor(this, R.color.break_point_flag_marker)) {
                shapeBreakPoint.setColor(ContextCompat.getColor(this, R.color.break_point_flag))
            } else {
                shapeBreakPoint.setColor(ContextCompat.getColor(this, R.color.break_point_flag_marker))
            }
        }

        return block
    }

    private fun buildTextView(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setTypeface(null, Typeface.BOLD)
        textView.setTextColor(ContextCompat.getColor(this, R.color.color_stroke_block))
        textView.gravity = Gravity.CENTER
        textView.textSize = (scaleDp * 10)

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.bottomMargin = 0
        layoutParams.topMargin = (5 * scaleDp + 0.5).toInt()
        textView.layoutParams = layoutParams

        return textView
    }


    private fun buildContainerBlocks(id: Int) : View {
        val linearLayout = LinearLayout.inflate(this, R.layout.block_container, null)

        val layoutParams = LinearLayout.LayoutParams((370 * scaleDp + 0.5).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.bottomMargin = (20 * scaleDp + 0.5).toInt()

        linearLayout.layoutParams = layoutParams
        linearLayout.id = id

        return linearLayout
    }
}