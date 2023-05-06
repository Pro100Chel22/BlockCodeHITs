package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivityCodingBinding
import com.example.myapplication.databinding.LayoutCompilerBinding
import com.example.myapplication.databinding.LayoutNewBlocksBinding
import com.example.myapplication.modules.BlockView
import com.example.myapplication.modules.InstructionType
import com.google.android.material.bottomsheet.BottomSheetDialog

@Suppress("DEPRECATION")
class CodingActivity : AppCompatActivity() {
    private var scaleDp: Float = 1f

    private lateinit var listBlocks: Map<InstructionType, BlockView>
    private lateinit var listBlocksNotHaveText: List<InstructionType>
    private lateinit var listBlocksEnds: List<InstructionType>

    //private lateinit var bottomSheetViewNewBlock: View
    private lateinit var bottomSheetDialogNewBlock: BottomSheetDialog
    private lateinit var bottomSheetDialogCompiler: BottomSheetDialog

    private lateinit var binding : ActivityCodingBinding
    private lateinit var bottomSheetViewNewBlockBinding : LayoutNewBlocksBinding
    private lateinit var bottomSheetViewCompilerBinding : LayoutCompilerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coding)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        scaleDp = resources.displayMetrics.density

        listBlocks = mapOf(
            InstructionType.VAR to BlockView(InstructionType.VAR,"Var", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_var),
            InstructionType.SET to BlockView(InstructionType.SET,"Set", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_set),
            InstructionType.PRINT to BlockView(InstructionType.PRINT,"Print", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_print),
            InstructionType.INPUT to BlockView(InstructionType.INPUT, "Input", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_input),
            InstructionType.IF to BlockView(InstructionType.IF, "If", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_if),
            InstructionType.ENDCHOICEIF to BlockView(InstructionType.ENDCHOICEIF, "EndChoiceIf", R.layout.block_insctuction_end_choice_if, R.color.color_stroke_block, R.color.color_block_if),
            InstructionType.ELIF to BlockView(InstructionType.ELIF,"Elif", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_if),
            InstructionType.ELSE to BlockView(InstructionType.ELSE, "Else", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_if),
            InstructionType.ENDIF to BlockView(InstructionType.ENDIF, "EndIf", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_if),
            InstructionType.FOR to BlockView(InstructionType.FOR, "For", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_for),
            InstructionType.ENDFOR to BlockView(InstructionType.ENDFOR, "EndFor", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_for),
            InstructionType.WHILE to BlockView(InstructionType.WHILE, "While", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_while),
            InstructionType.ENDWHILE to BlockView(InstructionType.ENDWHILE, "EndWhile", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_while),
            InstructionType.BREAK to BlockView(InstructionType.BREAK, "Break", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_break),
            InstructionType.CONTINUE to BlockView(InstructionType.CONTINUE, "Continue", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_continue),
            InstructionType.FUNC to BlockView(InstructionType.FUNC, "Func", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_func),
            InstructionType.ENDFUNC to BlockView(InstructionType.ENDFUNC, "EndFunc", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_func),
            InstructionType.RETURN to BlockView(InstructionType.RETURN, "Return", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_return),
            InstructionType.END to BlockView(InstructionType.END, "End", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_end),
        )

        binding = DataBindingUtil.setContentView(this, R.layout.activity_coding);

        listBlocksNotHaveText = listOf(
            InstructionType.ENDCHOICEIF,
            InstructionType.ELSE,
            InstructionType.ENDIF,
            InstructionType.ENDFOR,
            InstructionType.ENDWHILE,
            InstructionType.BREAK,
            InstructionType.CONTINUE,
            InstructionType.ENDFUNC,
            InstructionType.RETURN,
            InstructionType.END,
        )

        listBlocksEnds = listOf(
            InstructionType.ENDCHOICEIF,
            InstructionType.ENDIF,
            InstructionType.ENDFOR,
            InstructionType.ENDWHILE,
            InstructionType.ENDFUNC,
            InstructionType.END
        )

        initBottomSheetViewNewBlock()
        initBottomSheetCompiler()

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
        binding.buttonCompiler.setOnClickListener{
            bottomSheetDialogCompiler.show()
        }
    }

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
            containerBottomSheetView.addView(buildTextView(category, 0, 5))
            containerBottomSheetView.addView(buildContainerBlocks(id))
        }

        for ((key, blockView) in listBlocks) {
            if(key !in listBlocksEnds && key != InstructionType.ELSE && key != InstructionType.ELIF){
                val block = buildBlock(blockView, key)

                val addBlocks = { v: View ->
                    // <------------Код для добавления блоков в поле---------
                    Toast.makeText(this, blockView.instruction, Toast.LENGTH_SHORT).show()
                }

                block.setOnClickListener(addBlocks)

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

    private fun initBottomSheetCompiler () {
        val layoutParamsBottomSheet = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (500 * scaleDp + 0.5).toInt())

        bottomSheetViewCompilerBinding = DataBindingUtil.inflate(layoutInflater, R.layout.layout_compiler, null, false)
        bottomSheetViewCompilerBinding.modalBottomSheetContainer.layoutParams = layoutParamsBottomSheet
        bottomSheetDialogCompiler = BottomSheetDialog(this, R.style.BottomSheetDialogThem)
        bottomSheetDialogCompiler.setContentView(bottomSheetViewCompilerBinding.root)
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

            LinearLayout.LayoutParams((350 * scaleDp + 0.5).toInt(), (90 * scaleDp + 0.5).toInt())
        } else {
            LinearLayout.LayoutParams((200 * scaleDp + 0.5).toInt(), (55 * scaleDp + 0.5).toInt())
        }
        layoutParams.bottomMargin = (10 * scaleDp + 0.5).toInt()

        block.layoutParams = layoutParams
        if(instructionType !in listBlocksEnds) {
            block.findViewById<TextView>(R.id.instructionType).text = blockView.instruction
        }

        return block
    }

    private fun buildTextView(text: String, marginTop: Int = 0, marginBottom: Int = 0): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.setTypeface(null, Typeface.BOLD)
        textView.setTextColor(ContextCompat.getColor(this, R.color.color_stroke_block))
        textView.gravity = Gravity.CENTER
        textView.textSize = (scaleDp * 10)

        val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.bottomMargin = (marginBottom * scaleDp + 0.5).toInt()
        layoutParams.topMargin = (marginTop * scaleDp + 0.5).toInt()
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