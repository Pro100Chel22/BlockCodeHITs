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

class Interpreter {
    private val data = Data()
    private val callStack = Stack<CallStackFrame>()
    private val bracketStack = Stack<BracketCondition>()
    private val initFunc = mutableMapOf<String, CallStackFrame>()

    private var waitForInput = false
    private var inputExpression: String = ""

    private var context: CodingActivity
    private val blocksView: List<View>
    private val blocksCode: MutableList<Block> = mutableListOf()
    private var console: Console

    private var isActive = false
    private var wasStop = false

    constructor(_blocksView: List<View>, _context: CodingActivity, _console: Console) {
        blocksView = _blocksView
        context = _context
        console = _console
        callStack.push(CallStackFrame())

        for(blockView in blocksView) {
            val block = blockViewToBlock(blockView, context)
            blocksCode.add(block)

            blockView.findViewById<TextView>(R.id.string_number).setTextColor(ContextCompat.getColor(context, R.color.line_row_numb_default))
        }
    }

    fun getIsActive(): Boolean {
        return isActive
    }

    fun deactivate() {
        isActive = false
    }

    fun run(toNextBreakPoint: Boolean = false, runOnce: Boolean = false) {
        // Добавить проверку скобок,
        // Запретить подряд использовать == >= <= !=
        // В set и var добавить поддержку ==
        // Добавить проверку, что перед ! идет операнд
        // Проверить изменяет ли значение переменной в локальной памяти, если присваиваем новое значение переменной объявленной в локальной памяти функции
        // Добавить инициализацию массива стартовым значением

        isActive = true
        Thread {
            try {
                while (isActive) {
                    var callStackHasChanged = false
                    if (callStack.size > 1 && callStack.peek().funcIsOver) {
                        val rV = callStack.peek().returnVariable
                        callStack.pop()

                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.add(rV)

                        while (opsArithmetic("", true)) {
                            if (callStack.peek().arithmeticStack.size() > 1) {
                                val index = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(index)
                            } else {
                                break
                            }
                        }
                        callStackHasChanged = true
                    }

                    if (callStack.peek().curNmbLine >= blocksCode.size) {
                        isActive = false
                        break
                    }

                    if(toNextBreakPoint || runOnce) updateBlockMarkers()

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

                    if(toNextBreakPoint && runOnce && callStack.peek().curNmbLine < blocksCode.size) {
                        markBlock(callStack.peek().curNmbLine)
                        break
                    }

                    wasStop = false
                }
            } catch (e: Exception) {
                blocksView[callStack.peek().curNmbLine].findViewById<TextView>(R.id.string_number)
                    .setTextColor(ContextCompat.getColor(context, R.color.error_default))
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest("Line: " + (callStack.peek().curNmbLine + 1).toString() +
                            "\nExpression: " + blocksCode[callStack.peek().curNmbLine].expression +
                            "\n" + e.message)
                }
            } catch (e: EmptyStackException) {
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest("Line: " + (callStack.peek().curNmbLine + 1).toString() +
                            "\nExpression: " + blocksCode[callStack.peek().curNmbLine].expression +
                            "\nStack error: " + e.message)
                }
            } catch (e: IndexOutOfBoundsException) {
                updateBlockMarkers()
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest("Line: " + (callStack.peek().curNmbLine + 1).toString() +
                            "\nExpression: " + blocksCode[callStack.peek().curNmbLine].expression +
                            "\nArray error: " + e.message)
                }
            }
         }.start()
    }

    fun input(expression: String) {
        waitForInput = false
        inputExpression = expression
    }

    fun updateBlockMarkers() {
        for(i in blocksView.indices) {
            val breakPoint =  blocksView[i].findViewById<Button>(R.id.buttonBreakPoint)
            val shapeBreakPoint = breakPoint.background as GradientDrawable
            shapeBreakPoint.setColor(ContextCompat.getColor(context, R.color.break_point_flag))
            if(blocksCode[i].breakPoint) {
                shapeBreakPoint.setColor(ContextCompat.getColor(context, R.color.break_point_flag_marker))
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

        val breakPoint =  blocksView[id].findViewById<Button>(R.id.buttonBreakPoint)
        val shapeBreakPoint = breakPoint.background as GradientDrawable
        shapeBreakPoint.setColor(ContextCompat.getColor(context, R.color.break_point_flag_marker_wait))

        wasStop = true
    }

    private fun toNext(toEnd: Boolean = true) {
        if(blocksCode[callStack.peek().curNmbLine].numbLineToNext != -1) {
            callStack.peek().curNmbLine = blocksCode[callStack.peek().curNmbLine].numbLineToNext - 1
        } else {
            val oldNumbLine = callStack.peek().curNmbLine
            val stack = Stack<InstructionType>()
            stack.add(blocksCode[callStack.peek().curNmbLine].instructionType)
            while (stack.isNotEmpty()) {
                callStack.peek().curNmbLine++

                if(blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.FUNC) {
                    throw Exception("Incorrect expression: you cannot initialize functions inside nesting")
                }

                if(blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.IF ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.WHILE ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.FOR
                ){
                    stack.add(blocksCode[callStack.peek().curNmbLine].instructionType)
                } else if(stack.size == 1 && !toEnd &&
                    (blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.ELIF ||
                    blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.ELSE)
                ) {
                    stack.pop()
                }
                else if(blocksCode[callStack.peek().curNmbLine].instructionType == InstructionType.END) {
                    stack.pop()
                }
            }
            blocksCode[oldNumbLine].numbLineToNext = callStack.peek().curNmbLine
            callStack.peek().curNmbLine--
        }
    }

    private fun processing(block: Block) {
        when(block.instructionType) {
            InstructionType.VAR -> {
                println("var " + block.expression)
                val expressions = block.expression.split(",")

                for (exp in expressions) {
                    opsVar(exp)
                }
            }
            InstructionType.SET -> {
                println("set " + block.expression)

                opsSet(block.expression)
            }
            InstructionType.PRINT -> {
                println("print " + block.expression)

                val expressions = block.expression.split(",")

                for (exp in expressions) {
                    opsPrint(exp)
                }
            }
            InstructionType.INPUT -> {
                println("input " + block.expression)

                if(inputExpression.isEmpty()) {
                    waitForInput = true
                } else {
                    val expressions = block.expression.split(",")
                    val expressionsInput = inputExpression.split(" ")

                    for (i in expressions.indices) {
                       if (expressionsInput.size > i) {
                           opsInput(expressions[i], expressionsInput[i])
                       } else {
                           println("ERR: little input data")
                           throw Exception("Little input data")
                       }
                    }

                    inputExpression = ""
                }
            }
            InstructionType.IF -> {
                println("if " + block.expression)

                opsIf(block.expression, InstructionType.IF)
            }
            InstructionType.ELIF -> {
                println("elif " + block.expression)

                if((bracketStack.peek().bracket == InstructionType.IF || bracketStack.peek().bracket == InstructionType.ELIF) && bracketStack.peek().into) {
                    toNext(true)
                } else {
                    bracketStack.pop()
                    opsIf(block.expression, InstructionType.ELIF)
                }
            }
            InstructionType.ELSE -> {
                println("else " + block.expression)

                if((bracketStack.peek().bracket == InstructionType.IF ||
                    bracketStack.peek().bracket == InstructionType.ELIF) &&
                    bracketStack.peek().into
                ) {
                    toNext(true)
                } else {
                    bracketStack.pop()
                    bracketStack.add(BracketCondition(InstructionType.ELSE, false, callStack.peek().curNmbLine))
                    data.createNesting()
                }
            }
            InstructionType.FOR-> {
                println("for " + block.expression)

                opsFor(block.expression)
            }
            InstructionType.WHILE -> {
                println("while " + block.expression)

                opsWhile(block.expression)
            }
            InstructionType.BREAK -> {
                println("break " + block.expression)

                opsCycleOperator(true)
            }
            InstructionType.CONTINUE -> {
                println("continue " + block.expression)

                opsCycleOperator(false)
            }
            InstructionType.FUNC -> {
                println("func " + block.expression)

                opsFunc(block.expression)
            }
            InstructionType.RETURN -> {
                println("return " + block.expression)

               opsReturn(block.expression)
            }
            InstructionType.END -> {
                println("end " + block.expression)

                opsEnd()
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
            if(expression.isNotEmpty()) list.add(expression)
            list.add(string)
            expression = ""
        }

        var i = 0
        while (i < arithmetic.length) {
            if(!foundFunOrArray && arithmetic[i] in listMathOperation) {
                add(arithmetic[i].toString())
            } else if(!foundFunOrArray && arithmetic[i] in listLogicOperationPartOne) {
                if(i < arithmetic.length - 1 && arithmetic.substring(i, i + 2) in listLogicOperationFull) {
                    add(arithmetic.substring(i, i + 2))
                    i++
                } else if(arithmetic[i] in listOf('!', '<', '>')) {
                    add(arithmetic[i].toString())
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression: unknown operator")
                }
            } else if(!foundFunOrArray && arithmetic[i] == '.' && i < arithmetic.length - 1 && arithmetic[i + 1].isLetter()) {
                if(i < arithmetic.length - 5 && arithmetic.substring(i, i + 6) == ".toInt") {
                    add(arithmetic.substring(i, i + 6))
                    i += 6
                } else if(i < arithmetic.length - 8 && arithmetic.substring(i, i + 9) == ".toDouble") {
                    add(arithmetic.substring(i, i + 9))
                    i += 9
                } else if(i < arithmetic.length - 9 && arithmetic.substring(i, i + 10) == ".toBoolean") {
                    add(arithmetic.substring(i, i + 10))
                    i += 10
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression: unknown transformation")
                }

                if(i < arithmetic.length && arithmetic[i].isLetter()) {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression")
                } else if(i < arithmetic.length && arithmetic[i] == '(') {
                    list.add(arithmetic[i].toString())
                } else {
                    i--
                }
            }
            else if(arithmetic[i] == '(' || arithmetic[i] == '[') {
                if(i > 0 && arithmetic[i - 1].isLetterOrDigit() && !foundFunOrArray) {
                    foundFunOrArray = true
                    bracketStack.push(arithmetic[i].toString())
                    expression += arithmetic[i].toString()
                } else if(foundFunOrArray) {
                    bracketStack.push(arithmetic[i].toString())
                    expression += arithmetic[i].toString()
                } else {
                    list.add(arithmetic[i].toString())
                }
            } else if(arithmetic[i] == ')' || arithmetic[i] == ']') {
                if(foundFunOrArray) {
                    if(bracketStack.peek() == "(" && arithmetic[i].toString() == ")" ||
                        bracketStack.peek() == "[" && arithmetic[i].toString() == "]") {
                        expression += arithmetic[i].toString()
                        bracketStack.pop()
                    } else {
                        println("ERR: incorrect expression")
                        throw Exception("Incorrect expression: missing brackets")
                    }

                    if(bracketStack.isEmpty()) {
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
        if(expression.isNotEmpty()) list.add(expression)

        for(j in 0 until list.size - 1) {
            if(j >= list.size - 1) {
                break
            }

            if(list[j] == "-" || list[j] == "+") {
                if ((j == 0 || (list[j - 1].length == 1 && list[j - 1][0] in listMathOperation ||
                    list[j - 1].length == 2 &&  list[j - 1][0] in listLogicOperationPartOne ||
                    list[j - 1] == "(")) && VariableType.isConstant(list[j + 1])
                ) {
                    if(list[j] == "-") {
                        list[j + 1] = list[j] + list[j + 1]
                    }
                    list.removeAt(j)
                }
            }
        }

        return list
    }

    private fun opsArithmetic(expression: String, funcCall: Boolean = false, doNotCallFunc: Boolean = false): Boolean {
        val priorityOperation = mapOf(
            "||" to 0, "&&" to 0,
            ">=" to 1, "<=" to 1, ">" to 1, "<" to 1, "!=" to 1, "==" to 1,
            "+" to 2, "-" to 2,
            "*" to 3, "/" to 3, "%" to 3,
            "!" to 4,
            ".toInt" to 5, ".toDouble" to 5, ".toBoolean" to 5
        )

        val split = callStack.peek().arithmeticStack.peekArithmetic().split.ifEmpty {
            callStack.peek().arithmeticStack.peekArithmetic().split = arithmeticStringToList(expression)
            callStack.peek().arithmeticStack.peekArithmetic().split
        }

        val operatorAction = fun (operator: String){
            if(operator in listOf(".toInt", ".toDouble", ".toBoolean", "!")) {
                val var1 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(when(operator) {
                    ".toInt" -> var1.toInt()
                    ".toDouble" -> var1.toDouble()
                    ".toBoolean" -> var1.toBoolean()
                    "!" -> var1.not()
                    else -> {
                        println("ERR: incorrect expression")
                        Variable()
                        throw Exception("Incorrect expression: such an operation does not exist: $operator")
                    }
                })
            } else {
                val var1 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                val var2 = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(when(operator) {
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
                        println("ERR: incorrect expression")
                        Variable()
                        throw Exception("Incorrect expression: such an operation does not exist: $operator")
                    }
                })
            }
        }

        var fC = funcCall

        var foundCloseBracket = false
        while (callStack.peek().arithmeticStack.peekArithmetic().curIdSplit < split.size){
            val next = split[callStack.peek().arithmeticStack.peekArithmetic().curIdSplit]

            if (!foundCloseBracket && next in priorityOperation || next == "!"){
                println("$next isOperator")

                if(callStack.peek().arithmeticStack.peekArithmetic().operationStack.isEmpty()) {
                    callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    val prevOperator = callStack.peek().arithmeticStack.peekArithmetic().operationStack.peek()

                    if(prevOperator == "(" || priorityOperation[prevOperator]!! < priorityOperation[next]!!) {
                        callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                    } else {
                        operatorAction(prevOperator)
                        callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
                        callStack.peek().arithmeticStack.peekArithmetic().curIdSplit--
                    }
                }
            } else if(!foundCloseBracket && next in listOf("(", ")")){
                println("$next isBracket")

                if(next == "(") {
                    callStack.peek().arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    foundCloseBracket = true
                }
            } else if(foundCloseBracket) {
                val prevOperator = callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
                if(prevOperator == "(") {
                    foundCloseBracket = false
                } else {
                    operatorAction(prevOperator)
                }
                callStack.peek().arithmeticStack.peekArithmetic().curIdSplit--
            } else if(next in listOf("[", "]")) {
                println("ERR: incorrect expression")
                throw Exception("Incorrect expression: false brackets: []")
            } else if(VariableType.isConstant(next)){
                println(next + " isConstant")
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(VariableType.toVariable(next))
            } else if(isFunction(next)) {
                println(next + " isFunction")

                if(doNotCallFunc) {
                    println("ERR: the function cannot be called in this expression")
                    throw Exception("The function cannot be called in this expression, \"$expression\"")
                } else {
                    val nameFunc = getName(next, '(')
                    VariableType.isName(nameFunc)

                    if(initFunc[nameFunc] != null) {
                        if(callStack.peek().arithmeticStack.peekArithmetic().callFrame.size == 0) {
                            val cloneFuncFrame = getCloneCallStackFrame(initFunc[nameFunc]!!, funcCall)
                            callStack.peek().arithmeticStack.peekArithmetic().callFrame.add(cloneFuncFrame)
                        } else if(callStack.peek().arithmeticStack.peekArithmetic().callFrame.size != 1) {
                            throw Exception("Arithmetic stack: wrong call frame")
                        }

                        var inputs = getExpressionBetween(next, '(').split(",")
                        if(inputs.size == 1 && inputs[0].isEmpty()) inputs = listOf()

                        if(inputs.size < callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables.size) {
                            throw Exception("There are few arguments when calling the function: $next")
                        } else if(inputs.size > callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables.size){
                            throw Exception("Many arguments when calling a function: $next")
                        }

                        for(i in inputs.indices) {
                            if(!callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited) {
                                if(fC) {
                                    val value = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].value.setValue(value)
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited = true
                                    callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().curNmbLine = callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().startNumLine
                                    fC = false
                                } else {
                                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                                    if(opsArithmetic(inputs[i].replace(" ", ""), funcCall)) {
                                        val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                                        callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].value.setValue(value)
                                        callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables[i].inited = true
                                    } else {
                                        return false
                                    }
                                }
                            }
                        }

                        data.createFuncNesting()
                        for(input in callStack.peek().arithmeticStack.peekArithmetic().callFrame.peek().inputVariables) {
                            data.initVariable(input.name, input.value)
                        }
                        callStack.peek().arithmeticStack.peekArithmetic().curIdSplit++
                        callStack.add(callStack.peek().arithmeticStack.peekArithmetic().callFrame.pop())
                    } else {
                        throw Exception("The function not exist: $nameFunc()")
                    }
                }

                return false // ops is not over
            } else if(isArray(next)) {
                println(next + " isArray")

                if(fC) {
                    val index = callStack.peek().arithmeticStack.peekArithmetic().variableStack.pop()
                    val name = getName(next, '[')
                    val value = data.getArrayElement(name, index)

                    callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(value)
                } else {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if(opsArithmetic(getExpressionBetween(next, '['))) {
                        val index = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        val name = getName(next, '[')
                        val value = data.getArrayElement(name, index)

                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(value)
                    } else {
                        return false
                    }
                }
            } else {
                println(next + " isVariable")
                callStack.peek().arithmeticStack.peekArithmetic().variableStack.push(data.getVariable(next))
            }

            fC = false
            callStack.peek().arithmeticStack.peekArithmetic().curIdSplit++
        }

        while (callStack.peek().arithmeticStack.peekArithmetic().operationStack.isNotEmpty()) {
            val prevOperator = callStack.peek().arithmeticStack.peekArithmetic().operationStack.pop()
            if(prevOperator !in listOf("(", ")")) {
                operatorAction(prevOperator)
            }
        }

        return true// ops is over
    }

    private fun isFunction(string: String): Boolean {
        for(ch in string) {
            if(ch == '(' && string[string.length - 1] == ')') {
                return true
            }
        }

        return false
    }

    private fun isArray(string: String): Boolean {
        for(ch in string) {
            if(ch == '[' && string[string.length - 1] == ']') {
                return true
            }
        }

        return false
    }

    private fun getName(string: String, bracket: Char): String {
        for(i in string.indices) {
            if(string[i] == bracket) {
                println(string.substring(0, i))
                return string.substring(0, i)
            }
        }

        return ""
    }

    private fun getExpressionBetween(string: String, bracket: Char): String {
        for(i in string.indices) {
            if(string[i] == bracket) {
                println(string.substring(i+1, string.length - 1))
                return string.substring(i+1, string.length - 1)
            }
        }

        return ""
    }

    private fun opsVar(expression: String) {
        println(expression)

        val expressions = expression.trim().split("\\s+".toRegex())

        if(expressions.size > 1) {
            val type = expressions[0]
            var partTwo = ""
            for(i in 1 until expressions.size) {
                partTwo += expressions[i]

                if(i < expressions.size - 1) {
                    val cur = expressions[i][expressions[i].length - 1]
                    val next = expressions[i+1][0]
                    if ((cur.isLetter() || cur.isDigit() || cur == '_') &&
                        (next.isLetter() || next.isDigit() || next == '_')
                    ) {
                        throw Exception("Incorrect expression: ${expressions[i]} ${expressions[i + 1]}")
                    }
                }
            }

            if(!partTwo.contains("=") && isArray(partTwo)) {
                val name = getName(partTwo, '[')
                val expressionBetween = getExpressionBetween(partTwo, '[')

                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(expressionBetween, false, true) &&
                    callStack.peek().arithmeticStack.size() == 1 &&
                    callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                ) {
                    val countElement = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                    data.initArray(name, countElement, VariableType.getType(type))
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression: $partTwo")
                }
            } else if(partTwo.contains("=")) {
                val exp = partTwo.split("=")
                val name = exp[0]

                if(exp.size == 2) {
                    val newVar = VariableType.getType(type)

                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(exp[1], false, true) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        newVar.setValue(callStack.peek().arithmeticStack.popArithmetic().variableStack.peek())
                        data.initVariable(name, newVar)
                    } else {
                        println("ERR: incorrect expression")
                        throw Exception("Incorrect expression")
                    }
                } else {
                    println("ERR: to variable incorrect expression")
                    throw Exception("Incorrect expression: there must be one assignment operator =")
                }
            } else {
                data.initVariable(partTwo, VariableType.getType(type))
            }
        } else {
            println("ERR: to variable incorrect expression")
            throw Exception("Incorrect expression: too few operands")
        }
    }

    private fun opsSet(expression: String): Boolean {
        if(expression.contains("=")) {
            val expressions = expression.split("=")

            val setOperation = fun (variable: String) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                if(isArray(variable)) {
                    val name = getName(variable, '[')
                    val expressionBetween = getExpressionBetween(variable, '[')

                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(expressionBetween, false, true) &&
                        callStack.peek().arithmeticStack.size() == 1 &&
                        callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        val index = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                        data.setArrayElement(name, index, value)
                    } else {
                        println("ERR: incorrect expression")
                        throw Exception("Incorrect expression")
                    }
                } else {
                    data.setVariable(variable, value)
                }
            }

            if(expressions.size == 2) {
                val variable = expressions[0].replace(" ", "")
                val arithmeticExpression = expressions[1].replace(" ", "")

                if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                    setOperation(variable)
                } else if (callStack.peek().arithmeticStack.size() == 0) {
                    callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(arithmeticExpression)) {
                        setOperation(variable)
                    } else {
                        return false
                    }
                } else {
                    println("ERR: arithmetic stack")
                    throw Exception("Incorrect expression: arithmetic stack error")
                }
            } else {
                println("ERR: incorrect expression")
                throw Exception("Incorrect expression: there must be one assignment operator =")
            }
        } else {
            println("ERR: incorrect expression")
            throw Exception("Incorrect expression: the expression does not contain the operator =")
        }

        return true
    }

    private fun opsPrint(expression: String) {
        val exp = expression.replace(" ", "")

        if(exp.isNotEmpty()) {
            val latch = CountDownLatch(1)
            val handler = Handler(Looper.getMainLooper())

            if(data.checkArray(exp)) {
                handler.post {
                    if(isActive) console.println(data.getArray(exp))
                    latch.countDown()
                }
                latch.await()
            } else {
                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(exp, false, true) &&
                    callStack.peek().arithmeticStack.size() == 1 &&
                    callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1
                ) {
                    val out = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
                    handler.post {
                        if(isActive) console.println(out)
                        latch.countDown()
                    }
                    latch.await()
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression: stack is full")
                }
            }
        } else {
            println("ERR: incorrect expression output")
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun opsInput(expression: String, expressionInput: String) {
        val variableName = expression.trim().split("\\s+".toRegex())
        val inputConstant = expressionInput.trim().split("\\s+".toRegex())

        if(variableName.size == 1 && inputConstant.size == 1) {
            data.getVariable(variableName[0]).setValue(VariableType.toVariable(inputConstant[0]))
        } else if(variableName.size != 1) {
            println("ERR: incorrect expression")
            throw Exception("Incorrect expression")
        } else {
            println("ERR: invalid input data")
            throw Exception("Incorrect expression: invalid input data")
        }
    }

    private fun opsIf(expression: String, typeIf: InstructionType) {
        if(expression.isNotEmpty()) {
            if (callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                if (value.getValue()) {
                    bracketStack.add(BracketCondition(typeIf, true, callStack.peek().curNmbLine))
                    data.createNesting()
                } else {
                    toNext(false)
                    bracketStack.add(BracketCondition(typeIf, false, callStack.peek().curNmbLine))
                }
            } else if (callStack.peek().arithmeticStack.size() == 0) {
                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(expression.replace(" ", ""))) {
                    val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                    if (value.getValue()) {
                        bracketStack.add(BracketCondition(typeIf, true, callStack.peek().curNmbLine))
                        data.createNesting()
                    } else {
                        toNext(false)
                        bracketStack.add(BracketCondition(typeIf, false, callStack.peek().curNmbLine))
                    }
                }
            } else {
                println("ERR: arithmetic stack")
                throw Exception("Incorrect expression: arithmetic stack error")
            }
        } else {
            println("ERR: incorrect expression")
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun opsFor(expression: String) {
        if(expression.isNotEmpty()) {
            val operationBlocks = expression.split(";")
            if(expression.contains(";") && operationBlocks.size == 3) {
                if (!(bracketStack.isNotEmpty() &&
                    bracketStack.peek().bracket == InstructionType.FOR &&
                    bracketStack.peek().line == callStack.peek().curNmbLine)
                ) {
                    bracketStack.push(BracketCondition(InstructionType.FOR, false, callStack.peek().curNmbLine))
                    data.createNesting()
                    val expressions = operationBlocks[0].split(",")

                    for (exp in expressions) {
                        opsVar(exp)
                    }
                }

                if(bracketStack.peek().doArithmeticBlock) {
                    if(operationBlocks[2].isNotEmpty()) {
                        if(!opsSet(operationBlocks[2])) {
                            return
                        }
                        bracketStack.peek().doArithmeticBlock = false
                    }
                }

                if(operationBlocks[1].isNotEmpty()) {
                    if(callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                        val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                        if (value.getValue()) {
                            bracketStack.peek().into = true
                            data.createNesting()
                        } else {
                            bracketStack.peek().into = false
                            toNext(true)
                        }
                    } else if(callStack.peek().arithmeticStack.size() == 0) {
                        callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                        if (opsArithmetic(operationBlocks[1].replace(" ", ""))) {
                            val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
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
                println("ERR: incorrect expression")
                throw Exception("Incorrect expression: the for block must contain two delimiters ';'")
            }
        } else {
            println("ERR: incorrect expression")
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun opsWhile(expression: String) {
        if(expression.isNotEmpty()) {
            if(callStack.peek().arithmeticStack.size() == 1 && callStack.peek().arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                if(value.getValue()) {
                    bracketStack.add(BracketCondition(InstructionType.WHILE, true, callStack.peek().curNmbLine))
                    data.createNesting()
                } else {
                    toNext(true)
                    bracketStack.add(BracketCondition(InstructionType.WHILE, false, callStack.peek().curNmbLine))
                }
            } else if(callStack.peek().arithmeticStack.size() == 0){
                callStack.peek().arithmeticStack.addArithmetic(Arithmetic())
                if(opsArithmetic(expression.replace(" ", ""))) {
                    val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                    if(value.getValue()) {
                        bracketStack.add(BracketCondition(InstructionType.WHILE, true, callStack.peek().curNmbLine))
                        data.createNesting()
                    } else {
                        toNext(true)
                        bracketStack.add(BracketCondition(InstructionType.WHILE, false, callStack.peek().curNmbLine))
                    }
                }
            } else {
                println("ERR: arithmetic stack")
                throw Exception("Incorrect expression: arithmetic stack error")
            }
        } else {
            println("ERR: incorrect expression")
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun opsCycleOperator(exitTheLoop: Boolean) {
        if(bracketStack.isEmpty()) {
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

        if(exitTheLoop) {
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

    private fun opsFunc(expression: String) {
        if(expression.isNotEmpty()) {
            val exp = expression.trim().replace("\\s+".toRegex(), " ")
            val returnVariable = exp.split(" ")[0]
            val partTwo = exp.substring(returnVariable.length + 1)

            if(isFunction(partTwo)) {
                val nameFunc: String = getName(partTwo, '(').trim()
                VariableType.isName(nameFunc)

                if(initFunc[nameFunc] != null) {
                    if(callStack.size > 1) {
                        bracketStack.add(BracketCondition(InstructionType.FUNC, true, callStack.peek().curNmbLine))
                    } else {
                        throw Exception("Incorrect expression: such a function name already exists $nameFunc()")
                    }
                } else if(initFunc[nameFunc] == null && callStack.size == 1){
                    if(bracketStack.isNotEmpty()) {
                        throw Exception("Incorrect expression: you cannot initialize functions inside nesting")
                    }

                    val listParameters = mutableListOf<ParametersFuncCondition>()
                    val parameters = getExpressionBetween(partTwo, '(').split(",")
                    if(parameters.isNotEmpty()) {
                        for(i in parameters.indices) {
                            if(parameters[i].isNotEmpty()) {
                                val exp2 = parameters[i].trim().split(" ")

                                if(exp2.size == 2) {
                                    VariableType.isName(exp2[1])
                                    listParameters.add(ParametersFuncCondition(exp2[1], VariableType.getType(exp2[0])))
                                } else {
                                    throw Exception("Incorrect expression: the type and name of the variable must be specified in the parameters of the function")
                                }
                            } else if(!(parameters.size == 1 && parameters[0].isEmpty())) {
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

                    bracketStack.add(BracketCondition(InstructionType.FUNC, false, callStack.peek().curNmbLine))
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
            println("ERR: incorrect expression func")
            throw Exception("Incorrect expression: empty expression")
        }
    }

    private fun opsReturn(expression: String) {
        if(callStack.size > 1) {
            if(expression.isNotEmpty()) {
                val exp = expression.split(" ")
                var partTwo = ""
                for(i in 0 until exp.size) {
                    partTwo += exp[i]

                    if(i < exp.size - 1 && exp[i].isNotEmpty() && exp[i + 1].isNotEmpty()) {
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
                    val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
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
                    if (opsArithmetic(partTwo)) {
                        val value = callStack.peek().arithmeticStack.popArithmetic().variableStack.peek()
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
                    println("ERR: arithmetic stack")
                    throw Exception("Incorrect expression: arithmetic stack error")
                }
            } else {
                throw Exception("Incorrect expression: empty expression")
            }
        } else {
            throw Exception("The return block can only be inside functions")
        }
    }

    private fun opsEnd() {
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
            println("ERR: unknown operation")

            throw Exception("Unknown operation")
        }
    }
}