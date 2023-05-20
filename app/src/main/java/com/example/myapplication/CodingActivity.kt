package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.DragEvent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnDragListener
import android.view.ViewGroup
import android.view.ViewParent
import android.view.WindowManager
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.size
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityCodingBinding
import com.example.myapplication.databinding.LayoutConsoleBinding
import com.example.myapplication.databinding.LayoutNewBlocksBinding
import com.example.myapplication.modules.Block
import com.example.myapplication.modules.BlockView
import com.example.myapplication.modules.Console
import com.example.myapplication.modules.InstructionType
import com.example.myapplication.modules.Interpreter
import com.example.myapplication.modules.getListBlocks
import com.example.myapplication.modules.getListBlocksContainers
import com.example.myapplication.modules.getListBlocksEnds
import com.example.myapplication.modules.getListBlocksNotHaveText
import com.example.myapplication.modules.recycler_view_logic.DataSource
import com.example.myapplication.modules.recycler_view_logic.ItemsDecoration
import com.example.myapplication.modules.recycler_view_logic.OperatorAdapter
import com.example.myapplication.modules.recycler_view_logic.Operators
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Text
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class CodingActivity : AppCompatActivity() {
    private var scaleDp: Float = 1f
    private val scrollSpeed : Float = 20.0f
    private val leftPadding : Int = 30
    private val marginForRecyclerViewItems : Int = 30
    private val marginForEveryNonContainerBlock : Int = 15
    private val marginForEveryContainerBlock : Int = 10
    private val additionalWidth : Int = 0



    private var listBlocks = getListBlocks()
    private var listBlocksNotHaveText = getListBlocksNotHaveText()
    private var listBlocksEnds = getListBlocksEnds()
    private var listContainersBlocks = getListBlocksContainers()


    private var editTextsFocuses = mutableMapOf<EditText, Boolean>()
    private var instructionList = mutableListOf<InstructionType>()
    private var blockList = mutableListOf<View>()
    private var tempViewList = mutableListOf<View>()
    private var previousMargins = mutableListOf<Int>()
    private var previousWidths = mutableListOf<Int>()

    private lateinit var bottomSheetDialogNewBlock: BottomSheetDialog
    private lateinit var bottomSheetDialogConsole: BottomSheetDialog
    private lateinit var operatorsRecycler : RecyclerView;
    private lateinit var operatorsAdapter : OperatorAdapter;

    private lateinit var binding : ActivityCodingBinding
    private lateinit var bottomSheetViewNewBlockBinding : LayoutNewBlocksBinding
    private lateinit var bottomSheetViewConsoleBinding : LayoutConsoleBinding

    private var numberOfBlockFieldChildren :Int = 0
    private val regularBlockWidth : Int = 350;  private val regularBlockHeight : Int = 90
    private val specificBlockWidth : Int = 200; private val specificBlockHeight: Int = 55

    private lateinit var console: Console
    private lateinit var interpreter: Interpreter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coding)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        scaleDp = resources.displayMetrics.density

        binding = DataBindingUtil.setContentView(this, R.layout.activity_coding)


        val dataList = DataSource().loadOperators()
        operatorsRecycler = binding.operatorRecycler
        operatorsAdapter = OperatorAdapter(this@CodingActivity, marginForRecyclerViewItems, editTextsFocuses, dataList)

        operatorsRecycler.layoutManager = LinearLayoutManager(this@CodingActivity, LinearLayoutManager.HORIZONTAL, false)
        operatorsRecycler.adapter = operatorsAdapter


        val itemDecoration = ItemsDecoration(this, (40 * scaleDp).toInt());
        operatorsRecycler.addItemDecoration(itemDecoration)

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
        binding.upperBoundContainer.setOnDragListener { view, dragEvent ->
            scrollBlocks(view, dragEvent, scrollSpeed);
        }
        binding.lowerBoundContainer.setOnDragListener { view, dragEvent ->
            scrollBlocks(view, dragEvent, -scrollSpeed);
        }
        binding.deleteBlock.setOnLongClickListener {
            binding.blockField.removeAllViews();
            blockList.clear()
            instructionList.clear()
            true
        }
        binding.buttonSaveCode.setOnLongClickListener {
            saveButton()
            Toast.makeText(this, "button saves", Toast.LENGTH_SHORT).show()
            true
        }
        console = Console(bottomSheetViewConsoleBinding.consoleOutput, this, R.layout.layout_console_line)

        binding.buttonCompiler.setOnClickListener {
            if(::interpreter.isInitialized) interpreter.deactivate()

            val blocksView = blockList

            console.clear()

            interpreter = Interpreter(blocksView, this, console)

            interpreter.run()
        }

        binding.buttonDebug.setOnClickListener{
            if(::interpreter.isInitialized) interpreter.deactivate()
        }
    }

    private fun createButton() : Button{
        val button = Button(this);
        button.text = "BEBRA"
        return button;
    }
    private fun saveButton(){
        val button = createButton()
        val jsonArray = JSONArray()
        val jsonObject = JSONObject()
        jsonObject.put("button_text", button.text.toString())
        jsonArray.put(jsonObject)

        val fileName = "button.json"
        val file = File(filesDir, fileName)
        val fos = FileOutputStream(file)
        fos.write(jsonArray.toString().toByteArray())
        fos.close()
    }

    fun errorRequest(string: String) {
        val builder = AlertDialog.Builder(this)
        val dialogLayout = layoutInflater.inflate(R.layout.layout_dialog_alert_error, null)
        builder.setView(dialogLayout)

        val dialog = builder.create()

        dialogLayout.findViewById<Button>(R.id.buttonOk).setOnClickListener { dialog.dismiss() }
        dialogLayout.findViewById<TextView>(R.id.errorInformation).text = string

        dialog.show()

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent))
        dialog.window?.setLayout((300 * scaleDp + 0.5).toInt(), (400 * scaleDp + 0.5).toInt())
    }

    fun inputRequest() {
        val builder = AlertDialog.Builder(this)
        val dialogLayout = layoutInflater.inflate(R.layout.layout_dialog_alert_input, null)
        builder.setView(dialogLayout)

        val dialog = builder.create()

        val enter = {
            interpreter.input(dialogLayout.findViewById<EditText>(R.id.inputExpression).text.toString())
            interpreter.run()
            dialog.dismiss()
        }

        dialogLayout.findViewById<Button>(R.id.buttonEnter).setOnClickListener { enter() }
        dialog.setOnCancelListener { enter() }

        dialog.show()

        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(this, android.R.color.transparent))
        dialog.window?.setLayout((300 * scaleDp + 0.5).toInt(), (200 * scaleDp + 0.5).toInt())
    }

    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////

    private val deleteBlock = View.OnDragListener { view, dragEvent ->
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
                numberOfBlockFieldChildren--
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.invalidate()
                view.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_trash_close, null)
                true
            }
            else -> false
        }
    }
    private fun scrollBlocks(view : View, event : DragEvent, speed: Float) : Boolean {
        return when(event.action){
            DragEvent.ACTION_DRAG_STARTED -> {
                view.invalidate();
                true
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                binding.parentOfBlocks.panBy(0.0f, speed, false);
                view.invalidate();
                true;
            }
            else -> false
        }
    }

    private fun shiftBlocks(view : View, dragEvent : DragEvent, instruction : InstructionType) : Boolean{
        return when (dragEvent.action) {
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
                if(view.alpha < 1.0f){
                    makeViewsVisible()
                    tempViewList.clear()
                    return OnDragListener@false
                }

                val v = dragEvent.localState as View
                val vY = dragEvent.y
                val targetViewHalf = view.height / 2

                val vInd: Int = blockList.indexOf(v)
                val targetViewOwnerInd: Int = blockList.indexOf(view)

                val minInd = min(vInd, targetViewOwnerInd);
                val maxInd = max(vInd, targetViewOwnerInd)

                var flagForMainBlocks : Boolean = false
                var flagForEndBlocks : Boolean = false

                updateListOfMargins()
                updateListOfWidths()

                val mainParams = v.layoutParams as LinearLayout.LayoutParams
                var closeParams : LinearLayout.LayoutParams? = null
                val marg = mainParams.leftMargin
                mainParams.setMargins(view.marginLeft, v.marginTop, v.marginRight, v.marginBottom)
                if(tempViewList.size > 1){
                    closeParams = tempViewList[tempViewList.size - 2].layoutParams as LinearLayout.LayoutParams
                    closeParams.setMargins(view.marginLeft, v.marginTop, v.marginRight, v.marginBottom)
                    for(i in 0 until tempViewList.size - 2){
                        val innerParams = tempViewList[i].layoutParams as LinearLayout.LayoutParams
                        innerParams.setMargins(v.marginLeft + tempViewList[i].marginLeft - marg,
                            tempViewList[i].marginTop, tempViewList[i].marginRight, tempViewList[i].marginBottom)
                    }
                }

                if(instruction in listContainersBlocks || instruction == InstructionType.ELIF || instruction == InstructionType.ELSE){
                    flagForMainBlocks = true
                }
                else if(instruction in listBlocksEnds){
                    flagForEndBlocks = true
                }

                if (vY > targetViewHalf) {
                    if(flagForMainBlocks){
                        mainParams.setMargins(view.marginLeft + (leftPadding * scaleDp).toInt(), v.marginTop, v.marginRight, v.marginBottom)

                        closeParams?.setMargins(view.marginLeft + (leftPadding * scaleDp).toInt(), v.marginTop, v.marginRight, v.marginBottom)
                        for(i in 0 until tempViewList.size - 2){
                            val innerParams = tempViewList[i].layoutParams as LinearLayout.LayoutParams
                            innerParams.setMargins(tempViewList[i].marginLeft + (leftPadding * scaleDp).toInt(),
                                tempViewList[i].marginTop, tempViewList[i].marginRight, tempViewList[i].marginBottom)
                        }
                    }
                    if (vInd < targetViewOwnerInd) {
                        for(i in minInd until minInd + tempViewList.size){
                            blockList.add(maxInd + 1, binding.blockField.getChildAt(minInd))
                            instructionList.add(maxInd + 1, instructionList[minInd])
                            blockList.removeAt(minInd)
                            instructionList.removeAt(minInd)
                            binding.blockField.removeViewAt(minInd)
                        }
                        addViewsToBlockField()
                    }
                    else {
                        for (i in maxInd downTo minInd + 2) {
                            blockList.add(maxInd + tempViewList.size, binding.blockField.getChildAt(minInd + 1))
                            instructionList.add(maxInd + tempViewList.size, instructionList[minInd + 1])
                            blockList.removeAt(minInd + 1)
                            instructionList.removeAt(minInd + 1)
                            binding.blockField.removeViewAt(minInd + 1)
                        }
                        addViewsToBlockField()
                    }
                }
                else {
                    if(flagForEndBlocks || instruction == InstructionType.ELIF || instruction == InstructionType.ELSE){
                        mainParams.setMargins(view.marginLeft + (leftPadding * scaleDp).toInt(), view.marginTop ,view.marginRight, view.marginBottom)

                        closeParams?.setMargins(view.marginLeft + (leftPadding * scaleDp).toInt(), view.marginTop ,view.marginRight, view.marginBottom)
                        for(i in 0 until tempViewList.size - 2){
                            val innerParams = tempViewList[i].layoutParams as LinearLayout.LayoutParams
                            innerParams.setMargins(tempViewList[i].marginLeft + (leftPadding * scaleDp).toInt(),
                                tempViewList[i].marginTop, tempViewList[i].marginRight, tempViewList[i].marginBottom)
                        }
                    }
                    if (vInd < targetViewOwnerInd) {
                        for(i in minInd until minInd + tempViewList.size){
                            blockList.add(maxInd, binding.blockField.getChildAt(minInd))
                            instructionList.add(maxInd, instructionList[minInd])
                            blockList.removeAt(minInd)
                            instructionList.removeAt(minInd)
                            binding.blockField.removeViewAt(minInd)
                        }
                        addViewsToBlockField()
                    }
                    else {
                        for (i in maxInd downTo minInd + 1) {
                            blockList.add(maxInd + tempViewList.size, binding.blockField.getChildAt(minInd))
                            instructionList.add(maxInd + tempViewList.size, instructionList[minInd])
                            blockList.removeAt(minInd)
                            instructionList.removeAt(minInd)
                            binding.blockField.removeViewAt(minInd)
                        }
                        addViewsToBlockField()
                    }
                }
                updateMargins()
                setAllConnectorsZero()
                callUpdate()
                alignStringNumbers()
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.invalidate()
                makeViewsVisible()
                tempViewList.clear()
                return true
            }
            else -> {
                false
            }
        }
    }

    private fun updateListOfMargins(){
        for(i in 0 until blockList.size){
            previousMargins[i] = blockList[i].marginLeft
        }
    }
    private fun updateListOfWidths(){
        for(i in 0 until blockList.size){
            previousWidths[i] = blockList[i].findViewById<ConstraintLayout>(R.id.string_number_placeholder).width
        }
    }

    private fun alignStringNumbers(){
        for(i in 0 until blockList.size){
            val nextNumber = blockList[i].findViewById<ConstraintLayout>(R.id.string_number_placeholder)
            nextNumber.layoutParams.width += (blockList[i].marginLeft - previousMargins[i] - (nextNumber.width - previousWidths[i]))
            blockList[i].requestLayout()
            nextNumber.requestLayout()
        }
    }

    private fun addViewsToBlockField(){
        binding.blockField.removeAllViews()
        for(i in blockList){
            binding.blockField.addView(i)
        }
    }

    private fun updateMargins(){
        for(i in 0 until blockList.size){
            val params = blockList[i].layoutParams as LayoutParams
            params.setMargins(blockList[i].marginLeft, blockList[i].marginTop,
                blockList[i].marginRight, (marginForEveryNonContainerBlock * scaleDp).toInt())
            blockList[i].findViewById<TextView>(R.id.string_number).text = (i + 1).toString()
        }
    }

    private fun setAllConnectorsZero(){
        for(i in 0 until blockList.size){
            blockList[i].findViewById<ConstraintLayout>(R.id.connector).layoutParams.height = 0
            blockList[i].findViewById<ConstraintLayout>(R.id.connector).requestLayout()
        }
    }

    private fun callUpdate(){
        for(i in 0 until blockList.size){
            updateConnectors(i)
        }
    }

    private fun updateConnectors(index : Int){
        var sizeOfConnector : Int = 0
        for(i in index + 1 until blockList.size){
            sizeOfConnector += blockList[i].height + (marginForEveryNonContainerBlock * scaleDp).toInt()
            if(blockList[i].marginLeft < blockList[index].marginLeft)
                break
            if(blockList[i].marginLeft == blockList[index].marginLeft){
                blockList[index].findViewById<ConstraintLayout>(R.id.connector).layoutParams.height = sizeOfConnector
                blockList[index].findViewById<ConstraintLayout>(R.id.connector).requestLayout()
                //blockList[i].findViewById<ConstraintLayout>(R.id.connector).requestLayout()
                break
            }
        }
    }

    private fun addElseOrElif(elifOrElse: String, motion : MotionEvent, indexInList : Int, _ifIndex : Int) : Boolean{
        val handler = Handler()
        return when(motion.action){
            MotionEvent.ACTION_DOWN -> {
                var requiredBlock : View? = null
                var endBlock : View? = null

                var params : LinearLayout.LayoutParams

                if(elifOrElse == "Elif"){
                    requiredBlock = listBlocks[InstructionType.ELIF]?.let { buildBlock(it, InstructionType.ELIF, additionalWidth) } as View

                    requiredBlock.setOnDragListener { view, dragEvent ->
                        shiftBlocks(view, dragEvent, InstructionType.ELIF)
                    }

                    blockList.add(indexInList, requiredBlock)
                    instructionList.add(indexInList, InstructionType.ELIF)
                    previousMargins.add(0)
                    previousWidths.add(0)
                    requiredBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).layoutParams.width = blockList[_ifIndex].findViewById<ConstraintLayout>(R.id.string_number_placeholder).width
                    requiredBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).requestLayout()

                    params = requiredBlock.layoutParams as LayoutParams
                    params.setMargins(blockList[_ifIndex].marginLeft, blockList[_ifIndex].marginTop,
                        blockList[_ifIndex].marginRight, blockList[_ifIndex].marginBottom)
                }
                else{
                    requiredBlock = listBlocks[InstructionType.ELSE]?.let { buildBlock(it, InstructionType.ELSE, 0) } as View

                    requiredBlock.setOnDragListener { view, dragEvent ->
                        shiftBlocks(view, dragEvent, InstructionType.ELSE)
                    }
                    endBlock = listBlocks[InstructionType.ENDIF]?.let { buildBlock(it, InstructionType.ENDIF, 0) } as View
                    endBlock.setOnDragListener { view, dragEvent ->
                        shiftBlocks(view, dragEvent, InstructionType.ENDIF)
                    }

                    blockList.add(indexInList + 1, requiredBlock);
                    blockList.add(indexInList + 2, endBlock)
                    requiredBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).layoutParams.width = blockList[_ifIndex].findViewById<ConstraintLayout>(R.id.string_number_placeholder).width
                    requiredBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).requestLayout()

                    endBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).layoutParams.width = blockList[_ifIndex].findViewById<ConstraintLayout>(R.id.string_number_placeholder).width
                    endBlock.findViewById<ConstraintLayout>(R.id.string_number_placeholder).requestLayout()
                    blockList.removeAt(indexInList)
                    previousMargins.add(0); previousMargins.add(0)
                    previousWidths.add(0); previousWidths.add(0)

                    for(i in 0 until 2){
                        params = blockList[indexInList + i].layoutParams as LinearLayout.LayoutParams
                        params.setMargins(blockList[_ifIndex].marginLeft, blockList[_ifIndex].marginTop,
                            blockList[_ifIndex].marginRight, blockList[_ifIndex].marginBottom)
                    }

                    instructionList.add(indexInList + 1, InstructionType.ELSE);
                    instructionList.add(indexInList + 2, InstructionType.ENDIF)
                    instructionList.removeAt(indexInList)
                }
                addViewsToBlockField()
                updateMargins()
                handler.postDelayed({
                    callUpdate()
                }, 10)
                true
            }
            else -> {
                handler.postDelayed({
                    callUpdate()
                }, 10)
                false
            }
        }
    }

    private fun makeContainerDraggable(instruction : InstructionType, view : View) : Boolean{
        val clipText = ""
        val item = ClipData.Item(clipText)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val data = ClipData(clipText, mimeTypes, item)

        val dragShadowBuilder = View.DragShadowBuilder(view)
        view.startDragAndDrop(data, dragShadowBuilder, view, 0)
        view.alpha = 0.3f

        makeViewsInvisible(blockList.indexOf(view), instruction)
        return true
    }

    private fun makeViewsInvisible(indexInList : Int, instruction : InstructionType){
        if(instruction in listContainersBlocks){
            for(i in indexInList + 1 until blockList.size){
                tempViewList.add(blockList[i])
                blockList[i].alpha = 0.3f
                if(instructionList[i] in listBlocksEnds && blockList[i].marginLeft == blockList[indexInList].marginLeft){
                    break
                }
            }
        }
        tempViewList.add(blockList[indexInList])
    }
    private fun makeViewsVisible(){
        for(i in 0 until tempViewList.size){
            tempViewList[i].alpha = 1.0f
        }
    }
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    /////////////////////////////////
    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }

    @SuppressLint("ClickableViewAccessibility")
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
                val block = buildBlock(blockView, key, 0)

                val addBlocks = { v: View ->
                    if(key in listBlocksNotHaveText && numberOfBlockFieldChildren == 0){
                        Toast.makeText(this, "You cant place it as first element", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val newBlock = buildBlock(blockView, key, additionalWidth); newBlock.id = numberOfBlockFieldChildren
                        newBlock.setOnLongClickListener {
                            makeContainerDraggable(key, it)
                        }
                        newBlock.setOnDragListener { view, dragEvent ->
                            shiftBlocks(view, dragEvent, key)
                        }
                        if(key !in listBlocksNotHaveText){
                            val newBlockEditText = newBlock.findViewById<EditText>(R.id.inputExpression); editTextsFocuses[newBlockEditText] = false
                            newBlockEditText.setOnFocusChangeListener { _, b ->
                                editTextsFocuses[newBlockEditText] = b
                            }
                        }

                        val newBlockParams  = newBlock.layoutParams as LayoutParams
                        newBlockParams.setMargins(0, 0, 0, (marginForEveryNonContainerBlock * scaleDp).toInt())

                        binding.blockField.addView(newBlock)
                        blockList.add(newBlock)
                        instructionList.add(key)
                        previousMargins.add(0)
                        previousWidths.add(0)

                        var endBlock : View? = null
                        if(key in listContainersBlocks){
                            when(key){
                                InstructionType.IF -> {
                                    endBlock = listBlocks[InstructionType.ENDCHOICEIF]?.let { buildBlock(it, InstructionType.ENDCHOICEIF, 0) } as ConstraintLayout
                                    instructionList.add(InstructionType.ENDCHOICEIF)

                                    val buttonElif = endBlock.getViewById(R.id.buttonElif);
                                    val buttonElse = endBlock.getViewById(R.id.buttonElse);

                                    buttonElif.setOnTouchListener { _, motionEvent ->
                                        addElseOrElif("Elif", motionEvent, blockList.indexOf(endBlock),
                                            blockList.indexOf(newBlock))
                                    }
                                    buttonElse.setOnTouchListener { _, motionEvent ->
                                        addElseOrElif("Else", motionEvent, blockList.indexOf(endBlock),
                                            blockList.indexOf(newBlock))
                                    }
                                }
                                InstructionType.WHILE -> {
                                    endBlock = listBlocks[InstructionType.ENDWHILE]?.let { buildBlock(it, InstructionType.ENDWHILE, 0) } as ConstraintLayout
                                    instructionList.add(InstructionType.ENDWHILE)
                                }
                                InstructionType.FUNC -> {
                                    endBlock = listBlocks[InstructionType.ENDFUNC]?.let { buildBlock(it, InstructionType.ENDFUNC, 0) } as ConstraintLayout
                                    instructionList.add(InstructionType.ENDFUNC)
                                }
                                InstructionType.FOR -> {
                                    endBlock = listBlocks[InstructionType.ENDFOR]?.let { buildBlock(it, InstructionType.ENDFOR, 0) } as ConstraintLayout
                                    instructionList.add(InstructionType.ENDFOR)
                                }
                                else -> {}
                            }
                        }

                        /*for(i in 0 until instructionList.size){
                            Toast.makeText(this, instructionList[i].name, Toast.LENGTH_SHORT).show()
                        }*/

                        if(endBlock != null){
                            endBlock.id = numberOfBlockFieldChildren + 1; numberOfBlockFieldChildren++
                            endBlock.setOnDragListener { view, dragEvent ->
                                shiftBlocks(view, dragEvent, InstructionType.ENDFOR)
                            }
                            binding.blockField.addView(endBlock)
                            blockList.add(endBlock)
                            previousMargins.add(0)
                            previousWidths.add(0)
                        }

                        numberOfBlockFieldChildren++
                        updateMargins()
                        setAllConnectorsZero()
                        callUpdate()
                        /*if(numberOfBlockFieldChildren > 0){
                            alignStringNumbers()
                        }*/

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

    private fun buildBlock(blockView: BlockView, instructionType: InstructionType, additionalWidth : Int): View {
        val block = layoutInflater.inflate(blockView.layout, null)

        val shapeBlock = block.background as GradientDrawable
        shapeBlock.setColor(ContextCompat.getColor(this, blockView.colorFill))
        shapeBlock.setStroke((3 * scaleDp + 0.5).toInt(), ContextCompat.getColor(this, blockView.colorStroke))
        val layoutParams = if(instructionType !in listBlocksNotHaveText) {
            val shapeText = block.findViewById<EditText>(R.id.inputExpression).background as GradientDrawable
            shapeText.setColor(ContextCompat.getColor(this, R.color.color_window_text_block))
            shapeText.setStroke((2 * scaleDp + 0.5).toInt(), ContextCompat.getColor(this, blockView.colorStroke))
            LinearLayout.LayoutParams(((regularBlockWidth + additionalWidth) * scaleDp + 0.5).toInt(), (regularBlockHeight * scaleDp + 0.5).toInt())
        } else {
            LinearLayout.LayoutParams((specificBlockWidth * scaleDp + 0.5).toInt(), (specificBlockHeight * scaleDp + 0.5).toInt())
        }
        layoutParams.bottomMargin = (10 * scaleDp + 0.5).toInt()

        block.layoutParams = layoutParams
        if(instructionType !in listBlocksEnds) {
            block.findViewById<TextView>(R.id.instructionType).text = blockView.instruction
        }

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(ContextCompat.getColor(this, R.color.break_point_flag))
        gradientDrawable.cornerRadius = 999f
        gradientDrawable.setStroke((scaleDp * 5).toInt(), ContextCompat.getColor(this, R.color.color_stroke_block))

        val breakPoint = block.findViewById<Button>(R.id.buttonBreakPoint)
        breakPoint.background = gradientDrawable
        breakPoint.setOnClickListener{
            val shapeBreakPoint = it.background as GradientDrawable

            if (shapeBreakPoint.color?.defaultColor == ContextCompat.getColor(this, R.color.break_point_flag_marker)) {
                shapeBreakPoint.setColor(ContextCompat.getColor(this, R.color.break_point_flag))
            } else {
                shapeBreakPoint.setColor(ContextCompat.getColor(this, R.color.break_point_flag_marker))
            }

            //Toast.makeText(this, block.findViewById<EditText>(R.id.inputExpression).text.toString(), Toast.LENGTH_SHORT).show()
        }

        block.id = View.generateViewId()

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