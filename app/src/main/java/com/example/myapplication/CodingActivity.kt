package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ActivityCodingBinding
import com.example.myapplication.databinding.LayoutConsoleBinding
import com.example.myapplication.databinding.LayoutNewBlocksBinding
import com.example.myapplication.modules.BlockView
import com.example.myapplication.modules.Console
import com.example.myapplication.modules.InstructionType
import com.example.myapplication.modules.Interpreter
import com.example.myapplication.modules.getListBlocks
import com.example.myapplication.modules.getListBlocksEnds
import com.example.myapplication.modules.getListBlocksNotHaveText
import com.example.myapplication.modules.recycler_view_logic.DataSource
import com.example.myapplication.modules.recycler_view_logic.ItemsDecoration
import com.example.myapplication.modules.recycler_view_logic.OperatorAdapter
import com.example.myapplication.modules.recycler_view_logic.Operators
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.max
import kotlin.math.min

@Suppress("DEPRECATION")
class CodingActivity : AppCompatActivity() {
    private var scaleDp: Float = 1f
    private val scrollSpeed : Float = 20.0f
    private val leftPadding : Int = 40
    private val marginForRecyclerViewItems : Int = (30 * scaleDp).toInt();

    private var listBlocks = getListBlocks()
    private var listBlocksNotHaveText = getListBlocksNotHaveText()
    private var listBlocksEnds = getListBlocksEnds()
    private var checkedBlocks = mutableMapOf<ViewGroup, Boolean>()
    private var editTextsFocuses = mutableMapOf<EditText, Boolean>()
    private var connectorsMap = mutableMapOf<ViewGroup, ViewGroup>()
    private var fieldList = mutableListOf<ViewGroup>()

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

        fieldList.add(binding.blockField)

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
        binding.deleteBlock.setOnClickListener { startSelectiveErasing() }
        binding.deleteBlock.setOnLongClickListener { binding.blockField.removeAllViews();
            connectorsMap.clear(); numberOfBlockFieldChildren = 0; true
        }

        console = Console(bottomSheetViewConsoleBinding.consoleOutput, this, R.layout.layout_console_line)

        binding.buttonCompiler.setOnClickListener {
            console.clear()

            val blocksView = recurBlockParser(binding.blockField)

            interpreter = Interpreter(blocksView, this, console)

            interpreter.run()
        }
    }

    fun inputRequest() {
        val builder = AlertDialog.Builder(this)
        val dialogLayout = layoutInflater.inflate(R.layout.layout_dialog_alert, null)
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
    private fun startSelectiveErasing(){
        val keys : Set<View> = checkedBlocks.keys
        for(key in keys){
            if(checkedBlocks[key] == true){
                val keyContainer = key.parent.parent as? ViewGroup;
                val keyHolder = key.parent as? ViewGroup;
                connectorsMap.remove(keyHolder)
                keyContainer?.removeView(keyHolder)
                numberOfBlockFieldChildren--
            }
        }
    }


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
                val v = dragEvent.localState as ViewGroup
                val owner = v.parent as ViewGroup
                val ownerParent = owner.parent as ViewGroup

                ownerParent.removeView(owner)
                connectorsMap.remove(owner)
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

    private val swapDragListener = View.OnDragListener{view, event ->
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
                val v = event.localState as ViewGroup; val owner = v.parent as ViewGroup; val ownerParent = owner.parent as ViewGroup;
                val destination = view as ViewGroup; val destinationParent = destination.parent as ViewGroup; val destinationChild = destination.getChildAt(0) as ViewGroup;

                for((key, value) in connectorsMap){
                    value.layoutParams.height = 0
                    value.requestLayout()
                }

                if(owner.parent != destination.parent){
                    ownerParent.removeView(owner); destinationParent.addView(owner);
                    v.visibility = View.VISIBLE
                    return@OnDragListener true
                }
                if(destination == owner){
                    return@OnDragListener false
                }
                if(v.getChildAt(0).height > destinationChild.getChildAt(0).height || owner.parent != destination.parent){
                    val ownerParams = owner.layoutParams; val destinationParams = destination.layoutParams

                    destination.layoutParams = ownerParams; owner.layoutParams = destinationParams
                }
                val connectorOwner = connectorsMap[owner] as ViewGroup; val connectorDestination = connectorsMap[destination] as ViewGroup
                owner.removeView(v); owner.removeView(connectorOwner);
                destination.removeView(destinationChild); destination.removeView(connectorDestination);
                connectorsMap.remove(owner); connectorsMap.remove(destination)

                owner.addView(destinationChild); owner.addView(connectorDestination)
                destination.addView(v); destination.addView(connectorOwner);
                connectorsMap[owner] = connectorDestination; connectorsMap[destination] = connectorOwner

                v.visibility = View.VISIBLE
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val v = event.localState as ViewGroup; v.visibility = View.VISIBLE
                updateConnectors()
                view.invalidate()
                true
            }
            else -> false
        }
    }



    private val shiftBlocks = View.OnDragListener { view, dragEvent ->
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
                var v = dragEvent.localState as ViewGroup; var owner = v.parent as ViewGroup;  val commonFather = owner.parent as ViewGroup

                val targetView = view as ViewGroup; val targetViewOwner = targetView.parent.parent as ViewGroup

                val connectorOwner = connectorsMap[owner] as ViewGroup;

                for((key, value) in connectorsMap){
                    value.layoutParams.height = 0
                    value.requestLayout()
                }

                if(owner.parent != targetViewOwner.parent){
                    return@OnDragListener false;
                }

                var vInd : Int = commonFather.indexOfChild(owner);
                val targetViewOwnerInd : Int = commonFather.indexOfChild(targetViewOwner);

                if(targetViewOwnerInd == vInd - 1){
                    return@OnDragListener false
                }

                owner.removeView(v); owner.removeView(connectorOwner); connectorsMap.remove(owner)
                val originalOwnerParams = owner.layoutParams

                var minInd = min(vInd, targetViewOwnerInd); var maxInd = max(vInd, targetViewOwnerInd)

                var finalView : ViewGroup;

                if(vInd < targetViewOwnerInd){
                    for(i in minInd until maxInd){
                        val viewToShiftParent = commonFather.getChildAt(i + 1) as ViewGroup; val viewToShift = viewToShiftParent.getChildAt(0); val shiftConnector = connectorsMap[viewToShiftParent] as ViewGroup

                        viewToShiftParent.removeView(viewToShift); viewToShiftParent.removeView(shiftConnector)

                        owner.addView(viewToShift); owner.addView(shiftConnector);
                        connectorsMap.remove(viewToShiftParent); connectorsMap[owner] = shiftConnector

                        val destinationParams = viewToShiftParent.layoutParams;
                        owner.layoutParams = destinationParams;

                        owner = viewToShiftParent
                    }

                    finalView = commonFather.getChildAt(maxInd) as ViewGroup

                }
                else{
                    for(i in maxInd downTo minInd + 2){
                        val viewToShiftParent = commonFather.getChildAt(i - 1) as ViewGroup; val viewToShift = viewToShiftParent.getChildAt(0) as ViewGroup; val shiftConnector = connectorsMap[viewToShiftParent] as ViewGroup

                        viewToShiftParent.removeView(viewToShift); viewToShiftParent.removeView(shiftConnector)

                        owner.addView(viewToShift); owner.addView(shiftConnector)
                        connectorsMap.remove(viewToShiftParent); connectorsMap[owner] = shiftConnector

                        val destinationParams = viewToShiftParent.layoutParams
                        owner.layoutParams = destinationParams

                        owner = viewToShiftParent
                    }

                    finalView = commonFather.getChildAt(minInd + 1) as ViewGroup

                }
                finalView.addView(v); finalView.addView(connectorOwner)
                connectorsMap[finalView] = connectorOwner
                finalView.layoutParams = originalOwnerParams
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val v = dragEvent.localState as ViewGroup; v.visibility = View.VISIBLE
                view.invalidate()
                true
            }
            else -> false
        }
    }
    private fun moveBlocksToNewOriginListener(origin : ViewGroup, view : View, dragEvent : DragEvent) : Boolean{
        return when(dragEvent.action){
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
                val v = dragEvent.localState as ViewGroup;
                val owner = v.parent as ViewGroup;
                val ownerParent = owner.parent as ViewGroup;

                if(ownerParent == origin){
                    return OnDragListener@false
                }

                for((key, value) in connectorsMap){
                    value.layoutParams.height = 0
                    value.requestLayout()
                }

                ownerParent.removeView(owner)

                origin.addView(owner)
                true
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                view.invalidate(); val v = dragEvent.localState as ViewGroup; v.visibility = View.VISIBLE
                true
            }
            else -> false
        }
    }
    private fun addElseOrElifByClick(container : ViewGroup, elifOrElse : String,  motion : MotionEvent) : Boolean{
        return when(motion.action){
            MotionEvent.ACTION_DOWN -> {
                val haveToBeReplaced = mutableListOf(container.getChildAt(container.childCount - 2),
                    container.getChildAt(container.childCount - 1))

                container.removeViewAt(container.childCount - 2); container.removeViewAt(container.childCount - 1)

                var requiredBlock : View? = null
                if(elifOrElse == "Elif"){
                    requiredBlock = listBlocks[InstructionType.ELIF]?.let { buildBlock(it, InstructionType.ELIF) } as ConstraintLayout
                }
                else{
                    requiredBlock = listBlocks[InstructionType.ELSE]?.let { buildBlock(it, InstructionType.ELSE) } as ConstraintLayout
                    haveToBeReplaced[0] = listBlocks[InstructionType.ENDIF]?.let { buildBlock(it, InstructionType.ENDIF) } as ConstraintLayout
                }

                val elifChildOrigin = createOriginContainer("LinearLayout",1); val innerLay = createInnerLay(regularBlockWidth, regularBlockHeight)
                fieldList.add(elifChildOrigin)

                innerLay.setOnDragListener { view, dragEvent ->
                    moveBlocksToNewOriginListener(elifChildOrigin, view, dragEvent)
                }
                container.addView(requiredBlock); container.addView(innerLay)
                container.addView(elifChildOrigin)
                container.addView(haveToBeReplaced[0]); container.addView(haveToBeReplaced[1])
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_BUTTON_PRESS -> {
                updateConnectors()
                true
            }
            else -> {
                updateConnectors()
                false
            }
        }
    }

    private fun createOriginContainer(nameOfLayout : String, layerLevel : Int) : ViewGroup {
        var originContainer : ViewGroup? = null
        when(nameOfLayout){
            "LinearLayout" -> {
                originContainer = LinearLayout(this)
                originContainer.orientation = LinearLayout.VERTICAL
            }
            "RelativeLayout" -> {
                originContainer = RelativeLayout(this);
            }
        }
        val contParamsLinear = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        originContainer!!.setPadding((leftPadding * scaleDp * layerLevel).toInt(), 0, 0, 0)
        originContainer.layoutParams = contParamsLinear
        originContainer.id = View.generateViewId()
        return originContainer
    }
    private fun createInnerLay(widthParams : Int, heightParams : Int) : ConstraintLayout{
        val innerLay = ConstraintLayout(this);
        val innerLayParams = ConstraintLayout.LayoutParams((widthParams * scaleDp + 0.5).toInt(), (heightParams / 3 * scaleDp).toInt())
        innerLay.layoutParams = innerLayParams;
        innerLay.id = View.generateViewId();
        return innerLay
    }

    private val makeContainerDraggable = View.OnLongClickListener {
        val clipText = ""
        val item = ClipData.Item(clipText)
        val mimeTypes = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN)
        val data = ClipData(clipText, mimeTypes, item)

        val firstChild = (it as ViewGroup).getChildAt(0);

        val dragShadowBuilder = View.DragShadowBuilder(firstChild)
        it.startDragAndDrop(data, dragShadowBuilder, it, 0)
        it.visibility = View.INVISIBLE
        true
    }

    private fun recurBlockParser(field: ViewGroup): MutableList<View> {
        val viewList: MutableList<View> = mutableListOf()

        for(i in 0 until field.childCount){
            val parent = field.getChildAt(i)

            val blockName = getViewBlock(parent).findViewById<TextView>(R.id.instructionType).text.toString()
            if(blockName == listBlocks[InstructionType.IF]?.instruction) {
                val blockContainer = getViewBlockContainer(parent)

                for(j in 0 until blockContainer.childCount step 3) {
                    val nestBlockView = blockContainer.getChildAt(j) as View
                    val nestBlockName = nestBlockView.findViewById<TextView>(R.id.instructionType).text.toString()

                    if(nestBlockName == listBlocks[InstructionType.IF]?.instruction ||
                        nestBlockName == listBlocks[InstructionType.ELIF]?.instruction ||
                        nestBlockName == listBlocks[InstructionType.ELSE]?.instruction) {

                        viewList.add(nestBlockView)
                        viewList.addAll(recurBlockParser(blockContainer.getChildAt(j + 2) as ViewGroup))
                    } else {
                        viewList.add(nestBlockView)
                    }
                }

            } else if(blockName == listBlocks[InstructionType.WHILE]?.instruction ||
                blockName == listBlocks[InstructionType.FOR]?.instruction ||
                blockName == listBlocks[InstructionType.FUNC]?.instruction) {

                val blockContainer = getViewBlockContainer(parent)
                viewList.add(blockContainer.getChildAt(0) as View)
                viewList.addAll(recurBlockParser(blockContainer.getChildAt(2) as ViewGroup))
                viewList.add(blockContainer.getChildAt(3) as View)
            } else {
                viewList.add(getViewBlock(parent))
            }
        }

        return viewList
    }

    private fun getViewBlock(parent: View): View {
        return ((parent as ViewGroup).getChildAt(0) as ViewGroup).getChildAt(0) as View
    }

    private fun getViewBlockContainer(parent: View): ViewGroup {
        return (parent as ViewGroup).getChildAt(0) as ViewGroup
    }
    private fun createConnector() : ConstraintLayout{
        val connector : ConstraintLayout = ConstraintLayout(this);
        val connectorParams = LinearLayout.LayoutParams((4 * scaleDp).toInt(), ConstraintLayout.LayoutParams.MATCH_PARENT)
        connector.layoutParams = connectorParams;
        connector.setBackgroundColor(resources.getColor(R.color.white))
        return connector
    }

    private fun setPropertiesForConnector(connector : ViewGroup, container : ViewGroup, block : View, breakPoint : Button){
        val pointCenterX = (breakPoint.right - breakPoint.left) / 2
        val pointCenterY = container.top

        connector.x = pointCenterX.toFloat() + breakPoint.left; connector.y = pointCenterY.toFloat();
        connector.translationZ = block.translationZ - 1
        connector.layoutParams.height = container.height - (regularBlockHeight * scaleDp).toInt()
        connector.requestLayout()
    }

    private fun updateConnectors(){
        for((key, value) in connectorsMap){
            key.requestLayout(); key.invalidate()
            value.layoutParams.height = key.height
            value.requestLayout()
            value.invalidate()
        }
        for(i in 0 until fieldList.size){
            val field = fieldList[i];
            if(field.childCount > 0){
                val child = field.getChildAt(field.childCount - 1) as ViewGroup
                val connector = child.getChildAt(1) as ViewGroup; connector.layoutParams.height = child.height - (regularBlockHeight * scaleDp).toInt()
                child.requestLayout()
                child.invalidate()
            }
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
                val block = buildBlock(blockView, key)

                val addBlocks = { v: View ->
                    if(key in listBlocksNotHaveText && numberOfBlockFieldChildren == 0){
                        Toast.makeText(this, "You cant place it as first element", Toast.LENGTH_SHORT).show()
                    }
                    else{
                        val originContainer = createOriginContainer("RelativeLayout", 0)

                        val container = createOriginContainer("LinearLayout",0); checkedBlocks[container] = false

                        val newBlock = buildBlock(blockView, key)

                        val innerLay = createInnerLay(regularBlockWidth, regularBlockHeight); innerLay.setOnDragListener(shiftBlocks)


                        if(key !in listBlocksNotHaveText){
                            val newBlockEditText = newBlock.findViewById<EditText>(R.id.inputExpression); editTextsFocuses[newBlockEditText] = false
                            newBlockEditText.setOnFocusChangeListener { _, b ->
                                editTextsFocuses[newBlockEditText] = b
                            }
                        }


                        container.addView(newBlock); container.addView(innerLay)


                        if(key == InstructionType.IF || key == InstructionType.FOR || key == InstructionType.WHILE || key == InstructionType.FUNC){
                            val origin = createOriginContainer("LinearLayout", 1)

                            container.addView(origin); fieldList.add(origin)

                            val newInnerLay = createInnerLay(specificBlockWidth, regularBlockHeight); newInnerLay.setOnDragListener(shiftBlocks);

                            innerLay.setOnDragListener { view, dragEvent ->
                                moveBlocksToNewOriginListener(origin, view, dragEvent)
                            }

                            when(key){
                                InstructionType.IF -> {
                                    val endChoiceIfBlock = listBlocks[InstructionType.ENDCHOICEIF]?.let { buildBlock(it, InstructionType.ENDCHOICEIF) } as ConstraintLayout
                                    container.addView(endChoiceIfBlock);
                                    val buttonElif = endChoiceIfBlock.getViewById(R.id.buttonElif);
                                    val buttonElse = endChoiceIfBlock.getViewById(R.id.buttonElse);

                                    buttonElif.setOnTouchListener { _, motionEvent ->
                                        addElseOrElifByClick(container, "Elif", motionEvent)
                                    }
                                    buttonElse.setOnTouchListener { _, motionEvent ->
                                        addElseOrElifByClick(container, "Else", motionEvent)
                                    }
                                }
                                InstructionType.WHILE -> {
                                    val endWhileBlock = listBlocks[InstructionType.ENDWHILE]?.let { buildBlock(it, InstructionType.ENDWHILE) } as ConstraintLayout
                                    container.addView(endWhileBlock)
                                }
                                InstructionType.FUNC -> {
                                    val endFuncBlock = listBlocks[InstructionType.ENDFUNC]?.let { buildBlock(it, InstructionType.ENDFUNC) } as ConstraintLayout
                                    container.addView(endFuncBlock)
                                }
                                InstructionType.FOR -> {
                                    val endFuncBlock = listBlocks[InstructionType.ENDFOR]?.let { buildBlock(it, InstructionType.ENDFOR) } as ConstraintLayout
                                    container.addView(endFuncBlock)
                                }
                                else -> {}
                            }

                            container.addView(newInnerLay);
                        }
                        originContainer.setOnDragListener(swapDragListener)
                        container.setOnLongClickListener(makeContainerDraggable)
                        container.setOnClickListener {
                            if(checkedBlocks[container] == false){
                                checkedBlocks[container] = true
                                container.alpha = 0.5f
                            }
                            else if(checkedBlocks[container] == true){
                                checkedBlocks[container] = false
                                container.alpha = 1f
                            }
                        }

                        val connector = createConnector();

                        val breakPoint = newBlock.findViewById<Button>(R.id.buttonBreakPoint);
                        breakPoint.post{
                            setPropertiesForConnector(connector, container, block, breakPoint)
                        }

                        originContainer.addView(container); originContainer.addView(connector);
                        originContainer.requestLayout()

                        binding.blockField.addView(originContainer)
                        binding.blockField.requestLayout()

                        connectorsMap[originContainer] = connector

                        updateConnectors()

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