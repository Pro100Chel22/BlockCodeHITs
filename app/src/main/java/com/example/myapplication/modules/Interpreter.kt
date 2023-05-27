package com.example.myapplication.modules

import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.myapplication.CodingActivity
import com.example.myapplication.R
import java.util.EmptyStackException
import java.util.Stack
import java.util.concurrent.CountDownLatch
import kotlin.Exception

class Interpreter(_blocksView: List<View>, _context: CodingActivity, _console: Console) {
    private val data = Data()
    private val callStack = Stack<CallStackFrame>()
    private val bracketStack = Stack<BracketCondition>()
    private val initFunc = mutableMapOf<String, CallStackFrame>()

    private var waitForInput = false
    private var inputExpression: String = ""

    private var context: CodingActivity = _context
    private val blocksView: List<View> = _blocksView
    private val blocksCode: MutableList<Block> = mutableListOf()
    private var console: Console = _console

    private var isActive = false
    private var wasStop = false

    init {
        callStack.push(CallStackFrame())
        for (blockView in blocksView) {
            val block = blockViewToBlock(blockView, context)
            blocksCode.add(block)

            blockView.findViewById<TextView>(R.id.string_number)
                .setTextColor(ContextCompat.getColor(context, R.color.line_row_numb_default))
        }
    }

    fun deactivate() {
        isActive = false
    }

    fun getVariables(): String {
        return data.getVariables()
    }

    fun getArrays(showArray: Boolean): String {
        return data.getArrays(showArray)
    }

    fun run(toNextBreakPoint: Boolean = false, runOnce: Boolean = false) {
        isActive = true
        Thread {
            try {
                while (isActive) {
                    var callStackHasChanged = false
                    if (callStack.size > 1 && callStack.peek().funcIsOver) {
                        val rV = callStack.peek().returnVariable
                        callStack.pop()

                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.add(rV)

                        while (processArithmetic("", true)) {
                            if (callStack.peek().arithmeticStack.size() > 1) {
                                val index =
                                    callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(
                                    index
                                )
                            } else {
                                break
                            }
                        }
                        callStackHasChanged = true
                    }

                    if (callStack.peek().curNmbLine >= blocksCode.size) {
                        isActive = false
                        Handler(Looper.getMainLooper()).post { context.activateLookVars() }
                        break
                    }

                    if (toNextBreakPoint || runOnce) updateBlockMarkers()

                    if (!runOnce && !wasStop && toNextBreakPoint && blocksCode[callStack.peek().curNmbLine].breakPoint || runOnce && callStackHasChanged) {
                        markBlock(callStack.peek().curNmbLine)
                        break
                    }

                    processing(blocksCode[callStack.peek().curNmbLine])

                    if (waitForInput) {
                        Handler(Looper.getMainLooper()).post { context.inputRequest() }
                        waitForInput = false
                        isActive = false
                        break
                    }

                    callStack.peek().curNmbLine++

                    if (toNextBreakPoint && runOnce && callStack.peek().curNmbLine < blocksCode.size) {
                        markBlock(callStack.peek().curNmbLine)
                        break
                    }

                    wasStop = false
                }
            } catch (e: Exception) {
                if (callStack.peek().curNmbLine >= 0) {
                    blocksView[callStack.peek().curNmbLine].findViewById<TextView>(R.id.string_number)
                        .setTextColor(ContextCompat.getColor(context, R.color.error_default))
                    updateBlockMarkers()
                    Handler(Looper.getMainLooper()).post {
                        context.errorRequest(
                            "Line: " + (callStack.peek().curNmbLine + 1).toString() +
                                    "\nExpression: " + blocksCode[callStack.peek().curNmbLine].expression  +
                                    "\n" + e.message
                        )
                    }
                }
            } catch (e: EmptyStackException) {
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest(
                        "Line: " + (callStack.peek().curNmbLine + 1).toString() +
                                "\nExpression: " + blocksCode[callStack.peek().curNmbLine].expression +
                                "\nStack error: " + e.message
                    )
                }
            } catch (e: IndexOutOfBoundsException) {
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest(
                        "Line: " + (callStack.peek().curNmbLine + 1).toString() +
                                "\nArray error: " + e.message
                    )
                }
            } catch (e: ArrayIndexOutOfBoundsException) {
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest(
                        "Line: " + (callStack.peek().curNmbLine + 1).toString() +
                                "\nArray error: " + e.message
                    )
                }
            }
        }.start()
    }

    fun input(expression: String) {
        waitForInput = false
        inputExpression = expression
    }

    fun updateBlockMarkers() {
        for (i in blocksView.indices) {
            val breakPoint = blocksView[i].findViewById<Button>(R.id.buttonBreakPoint)
            val shapeBreakPoint = breakPoint.background as GradientDrawable
            shapeBreakPoint.setColor(ContextCompat.getColor(context, R.color.break_point_flag))
            if (blocksCode[i].breakPoint) {
                shapeBreakPoint.setColor(
                    ContextCompat.getColor(
                        context,
                        R.color.break_point_flag_marker
                    )
                )
            }
        }
    }

    private fun markBlock(id: Int) {
        val latch = CountDownLatch(1)
        Handler(Looper.getMainLooper()).post {
            context.activateDebugButtons()
            latch.countDown()
        }
        latch.await()

        val breakPoint = blocksView[id].findViewById<Button>(R.id.buttonBreakPoint)
        val shapeBreakPoint = breakPoint.background as GradientDrawable
        shapeBreakPoint.setColor(
            ContextCompat.getColor(
                context,
                R.color.break_point_flag_marker_wait
            )
        )

        wasStop = true
    }

    private fun toNext(toEnd: Boolean = true) {
        if (blocksCode[callStack.peek().curNmbLine].numbLineToNext != -1) {
            callStack.peek().curNmbLine = blocksCode[callStack.peek().curNmbLine].numbLineToNext - 1
        } else {
            val oldNumbLine = callStack.peek().curNmbLine
            val stack = Stack<InstructionType>()
            stack.add(blocksCode[callStack.peek().curNmbLine].instructionType)
            while (stack.isNotEmpty()) {
                callStack.peek().curNmbLine++

                if (blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.FUNC) {
                    throw Exception("Incorrect expression: you cannot initialize functions inside nesting")
                }

                if (blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.IF ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.WHILE ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.FOR
                ) {
                    stack.add(blocksCode[callStack.peek().curNmbLine].instructionType)
                } else if (stack.size == 1 && !toEnd &&
                    (blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.ELIF ||
                            blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.ELSE)
                ) {
                    stack.pop()
                } else if (blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.END) {
                    stack.pop()
                }
            }
            blocksCode[oldNumbLine].numbLineToNext = callStack.peek().curNmbLine
            callStack.peek().curNmbLine--
        }
    }

    private fun splitString(string: String): List<String> {
        val list = mutableListOf<String>()

        var expression = ""
        var isStr = false
        for (ch in string) {
            if (!isStr && ch == ',') {
                list.add(expression)
                expression = ""
            } else if (ch == '\"') {
                isStr = !isStr
                expression += ch
            } else {
                expression += ch
            }
        }
        if (expression.isNotEmpty()) list.add(expression)

        return list
    }

    private fun processing(block: Block) {
        when (block.instructionType) {
            InstructionType.VAR -> {
                if (block.expression.isEmpty()) {
                    throw Exception("Incorrect expression: too few operands")
                }

                val expressions = splitString(block.expression)

                for (exp in expressions) {
                    processVar(exp)
                }
            }

            InstructionType.SET -> {
                processSet(block.expression)
            }

            InstructionType.PRINT -> {
                if (block.expression.isEmpty()) {
                    throw Exception("Incorrect expression: empty expression")
                }

                val expressions = splitString(block.expression)

                for (exp in expressions) {
                    processPrint(exp)
                }
            }

            InstructionType.INPUT -> {
                if (inputExpression.isEmpty()) {
                    waitForInput = true
                } else {
                    val expressions = block.expression.split(",")
                    val expressionsInput = inputExpression.split(",")

                    for (i in expressions.indices) {
                        if (expressionsInput.size > i) {
                            processInput(expressions[i], expressionsInput[i])
                        } else {
                            throw Exception("Little input data")
                        }
                    }

                    inputExpression = ""
                }
            }

            InstructionType.IF -> {
                processIf(block.expression, InstructionType.IF)
            }

            InstructionType.ELIF -> {
                if ((bracketStack.peek().bracket == InstructionType.IF || bracketStack.peek().bracket == InstructionType.ELIF) && bracketStack.peek().into) {
                    toNext(true)
                } else {
                    bracketStack.pop()
                    processIf(block.expression, InstructionType.ELIF)
                }
            }

            InstructionType.ELSE -> {
                if ((bracketStack.peek().bracket == InstructionType.IF ||
                            bracketStack.peek().bracket == InstructionType.ELIF) &&
                    bracketStack.peek().into
                ) {
                    toNext(true)
                } else {
                    bracketStack.pop()
                    bracketStack.add(
                        BracketCondition(
                            InstructionType.ELSE,
                            false,
                            callStack.peek().curNmbLine
                        )
                    )
                    data.createNesting()
                }
            }

            InstructionType.FOR -> {
                processFor(block.expression)
            }

            InstructionType.WHILE -> {
                processWhile(block.expression)
            }

            InstructionType.BREAK -> {
                processCycleOperator(true)
            }

            InstructionType.CONTINUE -> {
                processCycleOperator(false)
            }

            InstructionType.FUNC -> {
                processFunc(block.expression)
            }

            InstructionType.RETURN -> {
                processReturn(block.expression)
            }

            InstructionType.END -> {
                processEnd()
            }

            else -> {
                throw Exception("Unknown instruction")
            }
        }
    }

    private fun arithmeticStringToList(arithmetic: String): List<String> {
        val list: MutableList<String> = mutableListOf()

        val listMathOperation = listOf('-', '+', '/', '*', '%')
        val listLogicOperationPartOne = listOf('=', '!', '<', '>', '|', '&')
        val listLogicOperationFull = listOf("==", "!=", "<=", ">=", "||", "&&")

        val bracketStack = Stack<String>()
        var foundFunOrArray = false
        var expression = ""

        val add = { string: String ->
            if (expression.isNotEmpty()) list.add(expression)
            list.add(string)
            expression = ""
        }

        var i = 0
        while (i < arithmetic.length) {
            if (arithmetic[i] == '\'') {
                if (i < arithmetic.length - 2 && arithmetic.substring(i, i + 3)[2] == '\'') {
                    add(arithmetic.substring(i, i + 3))
                    i += 2
                } else {
                    throw Exception("Incorrect expression: there must be one character between the two operators '")
                }
            } else if (!foundFunOrArray && arithmetic[i] in listMathOperation) {
                add(arithmetic[i].toString())
            } else if (!foundFunOrArray && arithmetic[i] in listLogicOperationPartOne) {
                if (i < arithmetic.length - 1 && arithmetic.substring(
                        i,
                        i + 2
                    ) in listLogicOperationFull
                ) {
                    add(arithmetic.substring(i, i + 2))
                    i++
                } else if (arithmetic[i] in listOf('!', '<', '>')) {
                    add(arithmetic[i].toString())
                } else {
                    throw Exception("Incorrect expression: unknown operator")
                }
            } else if (!foundFunOrArray && arithmetic[i] == '.' && i < arithmetic.length - 1 && arithmetic[i + 1].isLetter()) {
                if (i < arithmetic.length - 5 && arithmetic.substring(i, i + 6) == ".toInt") {
                    add(arithmetic.substring(i, i + 6))
                    i += 6
                } else if (i < arithmetic.length - 8 && arithmetic.substring(
                        i,
                        i + 9
                    ) == ".toDouble"
                ) {
                    add(arithmetic.substring(i, i + 9))
                    i += 9
                } else if (i < arithmetic.length - 9 && arithmetic.substring(
                        i,
                        i + 10
                    ) == ".toBoolean"
                ) {
                    add(arithmetic.substring(i, i + 10))
                    i += 10
                } else if (i < arithmetic.length - 6 && arithmetic.substring(
                        i,
                        i + 7
                    ) == ".toChar"
                ) {
                    add(arithmetic.substring(i, i + 7))
                    i += 7
                } else {
                    throw Exception("Incorrect expression: unknown transformation")
                }

                if (i < arithmetic.length && arithmetic[i].isLetter()) {
                    throw Exception("Incorrect expression")
                } else if (i < arithmetic.length && arithmetic[i] == '(') {
                    list.add(arithmetic[i].toString())
                } else {
                    i--
                }
            } else if (arithmetic[i] == '(' || arithmetic[i] == '[') {
                if (i > 0 && arithmetic[i - 1].isLetterOrDigit() && !foundFunOrArray) {
                    foundFunOrArray = true
                    bracketStack.push(arithmetic[i].toString())
                    expression += arithmetic[i].toString()
                } else if (foundFunOrArray) {
                    bracketStack.push(arithmetic[i].toString())
                    expression += arithmetic[i].toString()
                } else {
                    list.add(arithmetic[i].toString())
                }
            } else if (arithmetic[i] == ')' || arithmetic[i] == ']') {
                if (foundFunOrArray) {
                    if (bracketStack.peek() == "(" && arithmetic[i].toString() == ")" ||
                        bracketStack.peek() == "[" && arithmetic[i].toString() == "]"
                    ) {
                        expression += arithmetic[i].toString()
                        bracketStack.pop()
                    } else {
                        throw Exception("Incorrect expression: missing brackets")
                    }

                    if (bracketStack.isEmpty()) {
                        foundFunOrArray = false
                        list.add(expression)
                        expression = ""
                    }
                } else {
                    add(arithmetic[i].toString())
                }
            } else {
                expression += arithmetic[i]
            }

            i++
        }
        if (expression.isNotEmpty()) list.add(expression)

        for (j in list.indices) {
            if (j >= list.size - 1) {
                if (list[j].isNotEmpty() && list[j] == "!") {
                    throw Exception("Incorrect expression: you can't bet '!' in this place")
                }
                break
            }

            if (list[j] == "!") {
                if (list[j + 1].isNotEmpty() && !list[j + 1][0].isLetter() && !list[j + 1][0].isDigit() && list[j + 1][0] != '(') {
                    throw Exception("Incorrect expression: you can't bet '!' in this place, ${list[j]}${list[j + 1]}")
                }
            } else if (list[j] == "-" || list[j] == "+") {
                if ((j == 0 || (list[j - 1].length == 1 && list[j - 1][0] in listMathOperation ||
                            list[j - 1].length == 2 && list[j - 1][0] in listLogicOperationPartOne ||
                            list[j - 1] == "(")) && VariableType.isConstant(list[j + 1])
                ) {
                    if (list[j] == "-") {
                        list[j + 1] = list[j] + list[j + 1]
                    }
                    list.removeAt(j)
                }
            }
        }

        return list
    }

    private fun processArithmetic(
        expression: String,
        funcCall: Boolean = false,
        doNotCallFunc: Boolean = false,
        doIncrement: Boolean = false
    ): Boolean {
        val priorityOperation = mapOf(
            "||" to 0, "&&" to 0,
            ">=" to 1, "<=" to 1, ">" to 1, "<" to 1, "!=" to 1, "==" to 1,
            "+" to 2, "-" to 2,
            "*" to 3, "/" to 3, "%" to 3,
            "!" to 4,
            ".toInt" to 5, ".toDouble" to 5, ".toBoolean" to 5, ".toChar" to 5
        )

        val split = callStack.peek().arithmeticStack.peekArithmetic().split.ifEmpty {
            callStack.peek().arithmeticStack.peekArithmetic().split =
                arithmeticStringToList(expression)
            callStack.peek().arithmeticStack.peekArithmetic().split
        }

        val operatorAction = fun(operator: String) {
            if (operator in listOf(".toInt", ".toDouble", ".toBoolean", ".toChar", "!")) {
                val var1 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(
                    when (operator) {
                        ".toInt" -> var1.toInt()
                        ".toDouble" -> var1.toDouble()
                        ".toBoolean" -> var1.toBoolean()
                        ".toChar" -> var1.toChar()
                        "!" -> var1.not()
                        else -> {
                            Variable()
                            throw Exception("Incorrect expression: such an operation does not exist: $operator")
                        }
                    }
                )
            } else {
                val var1 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                val var2 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(
                    when (operator) {
                        "+" -> var2 + var1
                        "-" -> var2 - var1
                        "*" -> var2 * var1
                        "/" -> var2 / var1
                        "%" -> var2 % var1
                        "||" -> var2.or(var1)
                        "&&" -> var2.and(var1)
                        ">" -> var2.compareMore(var1)
                        "<" -> var2.compareLess(var1)
                        ">=" -> var2.compareMore(var1).or(var2.equal(var1))
                        "<=" -> var2.compareLess(var1).or(var2.equal(var1))
                        "==" -> var2.equal(var1)
                        "!=" -> var2.equal(var1).not()
                        else -> {
                            Variable()
                            throw Exception("Incorrect expression: such an operation does not exist: $operator")
                        }
                    }
                )
            }
        }

        var fC = funcCall

        var foundCloseBracket = false
        while (callStack.peek().arithmeticStack.peekArithmetic().curIdSplit < split.size) {
            val next = split[callStack.peek().arithmeticStack.peekArithmetic().curIdSplit]

            if (!foundCloseBracket && next in priorityOperation || next == "!") {
                if (callStack.peek().arithmeticStack.peekArithmetic().operationStack.isEmpty()) {
                    callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    val prevOperator =
                        callStack.peek().arithmeticStack.peekArithmetic().operationStack.peek()

                    if (prevOperator == "(" || priorityOperation[prevOperator]!! < priorityOperation[next]!!) {
                        callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                    } else {
                        operatorAction(prevOperator)
                        callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
                        callStack.peek().arithmeticStack.peekArithmetic().curIdSplit--
                    }
                }
            } else if (!foundCloseBracket && next in listOf("(", ")")) {
                if (next == "(") {
                    callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    foundCloseBracket = true
                }
            } else if (foundCloseBracket) {
                val prevOperator =
                    callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
                if (prevOperator == "(") {
                    foundCloseBracket = false
                } else {
                    operatorAction(prevOperator)
                }
                callStack.peek().arithmeticStack.peekArithmetic().curIdSplit--
            } else if (next in listOf("[", "]")) {
                throw Exception("Incorrect expression: false brackets: []")
            } else if (VariableType.isConstant(next) || VariableType.isSymbol(next)) {
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(
                    VariableType.toVariable(
                        next
                    )
                )
            } else if (isFunction(next)) {
                if (doNotCallFunc) {
                    throw Exception("The function cannot be called in this expression, \"$expression\"")
                } else {
                    val nameFunc = getName(next, '(')
                    VariableType.isName(nameFunc)

                    if (initFunc[nameFunc] != null) {
                        if (callStack.peek().arithmeticStack.peekArithmetic().callFrame.size == 0) {
                            val cloneFuncFrame = getCloneCallStackFrame(
                                initFunc[nameFunc]!!,
                                funcCall || doIncrement
                            )
                            callStack.peek().arithmeticStack.peekArithmetic().callFrame.add(
                                cloneFuncFrame
                            )
                        } else if (callStack.peek().arithmeticStack.peekArithmetic().callFrame.size != 1) {
                            throw Exception("Arithmetic stack: wrong call frame")
                        }

                        var inputs = getExpressionBetween(next, '(').split(",")
                        if (inputs.size == 1 && inputs[0].isEmpty()) inputs = listOf()

                        if (inputs.size < callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables.size) {
                            throw Exception("There are few arguments when calling the function: $next")
                        } else if (inputs.size > callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables.size) {
                            throw Exception("Many arguments when calling a function: $next")
                        }

                        for (i in inputs.indices) {
                            if (!callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited) {
                                if (fC) {
                                    val value =
                                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].value.setValue(
                                        value
                                    )
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited =
                                        true
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().curNmbLine =
                                        callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().startNumLine
                                    fC = false
                                } else {
                                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                                    if (processArithmetic(inputs[i].replace(" ", ""), funcCall)) {
                                        val value =
                                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                        callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].value.setValue(
                                            value
                                        )
                                        callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited =
                                            true
                                    } else {
                                        return false
                                    }
                                }
                            }
                        }

                        data.createFuncNesting()
                        for (input in callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables) {
                            data.initVariable(input.name, input.value)
                        }
                        callStack.peek().arithmeticStack.peekArithmetic().curIdSplit++
                        callStack.add(callStack.peek().arithmeticStack.peekArithmetic().callFrame.pop())
                    } else {
                        throw Exception("The function not exist: $nameFunc()")
                    }
                }

                return false // ops is not over
            } else if (isArray(next)) {
                if (fC) {
                    val index =
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                    val name = getName(next, '[')
                    val value = data.getArrayElement(name, index)

                    callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(value)
                } else {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(
                            getExpressionBetween(next, '['),
                            false,
                            false,
                            funcCall
                        )
                    ) {
                        val index =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        val name = getName(next, '[')
                        val value = data.getArrayElement(name, index)

                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(value)
                    } else {
                        return false
                    }
                }
            } else {
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(
                    data.getVariable(
                        next
                    )
                )
            }

            fC = false
            callStack.peek().arithmeticStack.peekArithmetic().curIdSplit++
        }

        while (callStack.peek().arithmeticStack.peekArithmetic().operationStack.isNotEmpty()) {
            val prevOperator =
                callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
            if (prevOperator !in listOf("(", ")")) {
                operatorAction(prevOperator)
            }
        }

        return true// ops is over
    }

    private fun isFunction(string: String): Boolean {
        for (ch in string) {
            if (ch == '(' && string[string.length - 1] == ')') {
                return true
            }
        }

        return false
    }

    private fun isArray(string: String): Boolean {
        for (ch in string) {
            if (ch == '[' && string[string.length - 1] == ']') {
                return true
            }
        }

        return false
    }

    private fun getName(string: String, bracket: Char): String {
        for (i in string.indices) {
            if (string[i] == bracket) {
                println(string.substring(0, i))
                return string.substring(0, i)
            }
        }

        return ""
    }

    private fun getExpressionBetween(string: String, bracket: Char): String {
        for (i in string.indices) {
            if (string[i] == bracket) {
                return string.substring(i + 1, string.length - 1)
            }
        }

        return ""
    }

    private fun processVar(expression: String) {
        val expressions = expression.trim().split("\\s+".toRegex())

        if (expressions.size > 1) {
            val type = expressions[0]
            var partTwo = ""
            for (i in 1 until expressions.size) {
                partTwo += expressions[i]

                if (expressions[i].isNotEmpty() && expressions[i].contains("\"")) {
                    break
                }

                if (i < expressions.size - 1) {
                    val cur = expressions[i][expressions[i].length - 1]
                    val next = expressions[i + 1][0]
                    if ((cur.isLetter() || cur.isDigit() || cur == '_') &&
                        (next.isLetter() || next.isDigit() || next == '_')
                    ) {
                        throw Exception("Incorrect expression: ${expressions[i]} ${expressions[i + 1]}")
                    }
                }
            }

            if (partTwo.contains("=")) {
                val exp = partTwo.split("=")
                val name = exp[0]
                val arithmetic = partTwo.substring(name.length + 1, partTwo.length)

                if (isArray(name)) {
                    val string =
                        expression.substring(expression.indexOf("=") + 1, expression.length).trim()

                    if (type != "char") {
                        throw Exception(
                            "Incorrect expression: you cannot assign a value to this array, ${
                                getName(
                                    name,
                                    '['
                                )
                            }[]"
                        )
                    }

                    if (!VariableType.isString(string)) {
                        throw Exception(
                            "Incorrect expression: only a string can be assigned to an array of type char, ${
                                getName(
                                    name,
                                    '['
                                )
                            }[]"
                        )
                    }

                    val expressionBetween = getExpressionBetween(name, '[')

                    if (expressionBetween.isEmpty()) {
                        data.initArray(
                            getName(name, '['),
                            VariableInt(-1),
                            VariableType.getType(type),
                            string
                        )
                    } else {
                        callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                        if (processArithmetic(
                                expressionBetween,
                                funcCall = false,
                                doNotCallFunc = true
                            ) &&
                            callStack.peek().arithmeticStack.size() == 1 &&
                            callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                        ) {
                            val countElement =
                                callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                            data.initArray(
                                getName(name, '['),
                                countElement,
                                VariableType.getType(type),
                                string
                            )
                        } else {
                            throw Exception("Incorrect expression: $partTwo")
                        }
                    }
                } else {
                    val newVar = VariableType.getType(type)

                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(arithmetic, funcCall = false, doNotCallFunc = true) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        newVar.setValue(callStack.peek().arithmeticStack.popArithmetic().variableStack.peek())
                        data.initVariable(name, newVar)
                    } else {
                        throw Exception("Incorrect expression")
                    }
                }
            } else {
                if (isArray(partTwo)) {
                    val name = getName(partTwo, '[')
                    val expressionBetween = getExpressionBetween(partTwo, '[')

                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(
                            expressionBetween,
                            funcCall = false,
                            doNotCallFunc = true
                        ) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        val countElement =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        data.initArray(name, countElement, VariableType.getType(type))
                    } else {
                        throw Exception("Incorrect expression: $partTwo")
                    }
                } else {
                    data.initVariable(partTwo, VariableType.getType(type))
                }
            }
        } else {
            throw Exception("Incorrect expression: too few operands")
        }
    }

    private fun processSet(expression: String): Boolean {
        if (expression.contains("=")) {
            val expressions = expression.split("=")

            val setOperation = fun(variable: String) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                if (isArray(variable)) {
                    val name = getName(variable, '[')
                    val expressionBetween = getExpressionBetween(variable, '[')

                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(
                            expressionBetween,
                            funcCall = false,
                            doNotCallFunc = true
                        ) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        val index =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        data.setArrayElement(name, index, value)
                    } else {
                        throw Exception("Incorrect expression")
                    }
                } else {
                    data.setVariable(variable, value)
                }
            }

            val name = expressions[0].replace(" ", "")
            var arithmetic =
                expression.substring(expression.indexOf("=") + 1, expression.length).trim()
            if (data.checkArray(name)) {
                data.setCharArray(name, arithmetic)
            } else {
                arithmetic = arithmetic.replace(" ", "")

                if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                    setOperation(name)
                } else if (callStack.peek().arithmeticStack.size() == 0) {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(arithmetic)) {
                        setOperation(name)
                    } else {
                        return false
                    }
                } else {
                    throw Exception("Incorrect expression: arithmetic stack error")
                }
            }
        } else {
            throw Exception("Incorrect expression: the expression does not contain the operator =")
        }

        return true
    }

    private fun processPrint(expression: String) {
        val exp = expression.replace(" ", "")

        if (exp.isNotEmpty()) {
            val latch = CountDownLatch(1)

            if (VariableType.isString(expression.trim())) {
                Handler(Looper.getMainLooper()).post {
                    if (isActive) console.print(expression.trim())
                    latch.countDown()
                }
                latch.await()
            } else {
                if (data.checkArray(exp)) {
                    Handler(Looper.getMainLooper()).post {
                        if (isActive) console.println(data.getArray(exp))
                        latch.countDown()
                    }
                    latch.await()
                } else {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(exp, funcCall = false, doNotCallFunc = true) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        val out =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        Handler(Looper.getMainLooper()).post {
                            if (isActive) console.println(out)
                            latch.countDown()
                        }
                        latch.await()
                    } else {
                        throw Exception("Incorrect expression: stack is full")
                    }
                }
            }
        } else {
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun processInput(expression: String, expressionInput: String) {
        val variableName = expression.trim().split("\\s+".toRegex())

        if (variableName.size == 1) {
            if (data.checkArray(variableName[0])) {
                data.setCharArray(variableName[0], "\"" + expressionInput.trim() + "\"")
            } else {
                val inputConstant = expressionInput.trim().split("\\s+".toRegex())
                if (inputConstant.size != 1) {
                    throw Exception("Incorrect expression: invalid input data, $expressionInput")
                }
                data.getVariable(variableName[0])
                    .setValue(VariableType.toVariable(inputConstant[0]))
            }
        } else {
            throw Exception("Incorrect expression")
        }
    }

    private fun processIf(expression: String, typeIf: InstructionType) {
        if (expression.isNotEmpty()) {
            if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                    .toBoolean()
                if (value.getValue()) {
                    bracketStack.add(BracketCondition(typeIf, true, callStack.peek().curNmbLine))
                    data.createNesting()
                } else {
                    toNext(false)
                    bracketStack.add(BracketCondition(typeIf, false, callStack.peek().curNmbLine))
                }
            } else if (callStack.peek().arithmeticStack.size() == 0) {
                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if (processArithmetic(expression.replace(" ", ""))) {
                    val value =
                        callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                            .toBoolean()
                    if (value.getValue()) {
                        bracketStack.add(
                            BracketCondition(
                                typeIf,
                                true,
                                callStack.peek().curNmbLine
                            )
                        )
                        data.createNesting()
                    } else {
                        toNext(false)
                        bracketStack.add(
                            BracketCondition(
                                typeIf,
                                false,
                                callStack.peek().curNmbLine
                            )
                        )
                    }
                }
            } else {
                throw Exception("Incorrect expression: arithmetic stack error")
            }
        } else {
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun processFor(expression: String) {
        if (expression.isNotEmpty()) {
            val operationBlocks = expression.split(";")
            if (expression.contains(";") && operationBlocks.size == 3) {
                if (!(bracketStack.isNotEmpty() &&
                            bracketStack.peek().bracket == InstructionType.FOR &&
                            bracketStack.peek().line == callStack.peek().curNmbLine)
                ) {
                    bracketStack.push(
                        BracketCondition(
                            InstructionType.FOR,
                            false,
                            callStack.peek().curNmbLine
                        )
                    )
                    data.createNesting()
                    val expressions = operationBlocks[0].split(",")

                    for (exp in expressions) {
                        processVar(exp)
                    }
                }

                if (bracketStack.peek().doArithmeticBlock) {
                    if (operationBlocks[2].isNotEmpty()) {
                        if (!processSet(operationBlocks[2])) {
                            return
                        }
                        bracketStack.peek().doArithmeticBlock = false
                    }
                }

                if (operationBlocks[1].isNotEmpty()) {
                    if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                        val value =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                .toBoolean()
                        if (value.getValue()) {
                            bracketStack.peek().into = true
                            data.createNesting()
                        } else {
                            bracketStack.peek().into = false
                            toNext(true)
                        }
                    } else if (callStack.peek().arithmeticStack.size() == 0) {
                        callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                        if (processArithmetic(operationBlocks[1].replace(" ", ""))) {
                            val value =
                                callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                    .toBoolean()
                            if (value.getValue()) {
                                bracketStack.peek().into = true
                                data.createNesting()
                            } else {
                                bracketStack.peek().into = false
                                toNext(true)
                            }
                        }
                    }
                } else {
                    bracketStack.peek().into = true
                    data.createNesting()
                }
            } else {
                throw Exception("Incorrect expression: the for block must contain two delimiters ';'")
            }
        } else {
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun processWhile(expression: String) {
        if (expression.isNotEmpty()) {
            if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                    .toBoolean()
                if (value.getValue()) {
                    bracketStack.add(
                        BracketCondition(
                            InstructionType.WHILE,
                            true,
                            callStack.peek().curNmbLine
                        )
                    )
                    data.createNesting()
                } else {
                    toNext(true)
                    bracketStack.add(
                        BracketCondition(
                            InstructionType.WHILE,
                            false,
                            callStack.peek().curNmbLine
                        )
                    )
                }
            } else if (callStack.peek().arithmeticStack.size() == 0) {
                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if (processArithmetic(expression.replace(" ", ""))) {
                    val value =
                        callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                            .toBoolean()
                    if (value.getValue()) {
                        bracketStack.add(
                            BracketCondition(
                                InstructionType.WHILE,
                                true,
                                callStack.peek().curNmbLine
                            )
                        )
                        data.createNesting()
                    } else {
                        toNext(true)
                        bracketStack.add(
                            BracketCondition(
                                InstructionType.WHILE,
                                false,
                                callStack.peek().curNmbLine
                            )
                        )
                    }
                }
            } else {
                throw Exception("Incorrect expression: arithmetic stack error")
            }
        } else {
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun processCycleOperator(exitTheLoop: Boolean) {
        if (bracketStack.isEmpty()) {
            throw Exception("The break instruction must be inside for or while")
        }

        while (!(bracketStack.peek().bracket == InstructionType.WHILE ||
                    bracketStack.peek().bracket == InstructionType.FOR)
        ) {
            bracketStack.pop()
            if (bracketStack.isEmpty()) {
                throw Exception("The break instruction must be inside for or while")
            }
        }

        if (exitTheLoop) {
            bracketStack.peek().into = false
            data.deleteNesting()
        }

        while (!(blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.WHILE ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.FOR)
        ) {
            callStack.peek().curNmbLine--
        }

        toNext(true)
    }

    private fun processFunc(expression: String) {
        if (expression.isNotEmpty()) {
            val exp = expression.trim().replace("\\s+".toRegex(), " ")
            val returnVariable = exp.split(" ")[0]
            val partTwo = exp.substring(returnVariable.length + 1)

            if (isFunction(partTwo)) {
                val nameFunc: String = getName(partTwo, '(').trim()
                VariableType.isName(nameFunc)

                if (initFunc[nameFunc] != null) {
                    if (callStack.size > 1) {
                        bracketStack.add(
                            BracketCondition(
                                InstructionType.FUNC,
                                true,
                                callStack.peek().curNmbLine
                            )
                        )
                    } else {
                        throw Exception("Incorrect expression: such a function name already exists $nameFunc()")
                    }
                } else if (initFunc[nameFunc] == null && callStack.size == 1) {
                    if (bracketStack.isNotEmpty()) {
                        throw Exception("Incorrect expression: you cannot initialize functions inside nesting")
                    }

                    val listParameters = mutableListOf<ParametersFuncCondition>()
                    val parameters = getExpressionBetween(partTwo, '(').split(",")
                    if (parameters.isNotEmpty()) {
                        for (i in parameters.indices) {
                            if (parameters[i].isNotEmpty()) {
                                val exp2 = parameters[i].trim().split(" ")

                                if (exp2.size == 2) {
                                    VariableType.isName(exp2[1])
                                    listParameters.add(
                                        ParametersFuncCondition(
                                            exp2[1],
                                            VariableType.getType(exp2[0])
                                        )
                                    )
                                } else {
                                    throw Exception("Incorrect expression: the type and name of the variable must be specified in the parameters of the function")
                                }
                            } else if (!(parameters.size == 1 && parameters[0].isEmpty())) {
                                throw Exception("Incorrect expression: empty expression")
                            }
                        }
                    }

                    initFunc[nameFunc] = CallStackFrame(
                        callStack.peek().curNmbLine,
                        callStack.peek().curNmbLine,
                        true,
                        VariableType.getType(returnVariable, true),
                        listParameters
                    )

                    bracketStack.add(
                        BracketCondition(
                            InstructionType.FUNC,
                            false,
                            callStack.peek().curNmbLine
                        )
                    )
                    toNext(true)
                    bracketStack.pop()
                    callStack.peek().curNmbLine++
                } else {
                    throw Exception("Incorrect expression: you cannot initialize functions inside nesting")
                }
            } else {
                throw Exception("Incorrect expression: $partTwo")
            }
        } else {
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun processReturn(expression: String) {
        if (callStack.size > 1) {
            if (expression.isNotEmpty()) {
                val exp = expression.split(" ")
                var partTwo = ""
                for (i in exp.indices) {
                    partTwo += exp[i]

                    if (i < exp.size - 1 && exp[i].isNotEmpty() && exp[i + 1].isNotEmpty()) {
                        val cur = exp[i][exp[i].length - 1]
                        val next = exp[i + 1][0]
                        if ((cur.isLetter() || cur.isDigit() || cur == '_') &&
                            (next.isLetter() || next.isDigit() || next == '_')
                        ) {
                            throw Exception("Incorrect expression: ${exp[i]} ${exp[i + 1]}")
                        }
                    }
                }

                if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                    val value =
                        callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                    callStack.peek().returnVariable.setValue(value)
                    callStack.peek().funcIsOver = true
                    while (bracketStack.peek().bracket != InstructionType.FUNC) {
                        bracketStack.pop()
                    }
                    while (blocksCode[callStack.peek().curNmbLine].instructionType != InstructionType.FUNC) {
                        callStack.peek().curNmbLine--
                    }
                    toNext(true)
                    bracketStack.pop()
                    data.deleteFuncNesting()
                } else if (callStack.peek().arithmeticStack.size() == 0) {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (processArithmetic(partTwo)) {
                        val value =
                            callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        callStack.peek().returnVariable.setValue(value)
                        callStack.peek().funcIsOver = true
                        while (bracketStack.peek().bracket != InstructionType.FUNC) {
                            bracketStack.pop()
                        }
                        while (blocksCode[callStack.peek().curNmbLine].instructionType != InstructionType.FUNC) {
                            callStack.peek().curNmbLine--
                        }
                        toNext(true)
                        bracketStack.pop()
                        data.deleteFuncNesting()
                    }
                } else {
                    throw Exception("Incorrect expression: arithmetic stack error")
                }
            } else {
                throw Exception("Incorrect expression: empty expression")
            }
        } else {
            throw Exception("The return block can only be inside functions")
        }
    }

    private fun processEnd() {
        if (bracketStack.isNotEmpty() &&
            (bracketStack.peek().bracket == InstructionType.IF ||
                    bracketStack.peek().bracket == InstructionType.ELIF ||
                    bracketStack.peek().bracket == InstructionType.ELSE ||
                    bracketStack.peek().bracket == InstructionType.ELSE)
        ) {
            if (bracketStack.peek().into) data.deleteNesting()
            bracketStack.pop()
        } else if (bracketStack.isNotEmpty() &&
            bracketStack.peek().bracket == InstructionType.WHILE
        ) {
            if (bracketStack.peek().into) {
                callStack.peek().curNmbLine = bracketStack.peek().line - 1
                data.deleteNesting()
            }
            bracketStack.pop()
        } else if (bracketStack.isNotEmpty() &&
            bracketStack.peek().bracket == InstructionType.FOR
        ) {
            if (bracketStack.peek().into) {
                callStack.peek().curNmbLine = bracketStack.peek().line - 1
                bracketStack.peek().doArithmeticBlock = true
            } else {
                bracketStack.pop()
            }
            data.deleteNesting()
        } else if (bracketStack.isNotEmpty() &&
            bracketStack.peek().bracket == InstructionType.FUNC
        ) {
            bracketStack.pop()
            callStack.peek().funcIsOver = true

            throw Exception("The function should return the value")
        } else {
            throw Exception("Unknown operation")
        }
    }
}