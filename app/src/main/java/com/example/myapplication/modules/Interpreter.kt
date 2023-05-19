package com.example.myapplication.modules

import android.os.Handler
import android.os.Looper
import android.view.View
import com.example.myapplication.CodingActivity
import java.util.EmptyStackException
import java.util.Stack
import java.util.concurrent.CountDownLatch
import kotlin.Exception

class Interpreter {
    private var globalData = DataStack()

    private val callStack = Stack<StackFrame>()
    private val data = Data()
    private val arithmeticStack = ArithmeticStack()
    private val bracketStack = Stack<BracketCondition>()

    private var numbLine: Int = 0
    private var waitForInput = false
    private var inputExpression: String = ""

    private var context: CodingActivity
    private val blocksView: List<View>
    private val blocksCode: MutableList<Block> = mutableListOf()
    private var console: Console

    private var isActive = false

    constructor(_blocksView: List<View>, _context: CodingActivity, _console: Console) {
        blocksView = _blocksView
        context = _context
        console = _console

        for(blockView in blocksView) {
            val block = blockViewToBlock(blockView, context)
            blocksCode.add(block)
        }
    }

    fun getIsActive(): Boolean {
        return isActive
    }

    fun deactivate() {
        isActive = false
    }

    fun run() {
        isActive = true
        Thread {
            try {
                while (isActive) {
                    if (callStack.isNotEmpty()) {
                        // Когда вызывается функция, создается кадр стека и кладется в стек вызовов, а в памяти
                        // выделяется помять под локальные переменные функции
                        // Если стек вызовов не пуст, проверяется закончилась функция или нет, если закончилась,
                        // возвращаем значение функции в место, где она вызвана (стек арифметических выражений)
                        // и убираем ее из стека, если не закончилась, то выполняем блок, на котором она находится
                        //

                        if (callStack.peek().funcIsOver) {
                            arithmeticStack.peekArithmetic().variableStack.add(callStack.peek().returnVariable)
                            callStack.pop()

                            while (opsArithmetic("", true)) {
                                if (arithmeticStack.size() > 1) {
                                    val index = arithmeticStack.popArithmetic().variableStack.peek()
                                    arithmeticStack.peekArithmetic().variableStack.push(index)
                                } else {
                                    numbLine--
                                    break
                                }
                            }
                        }
                    } else {
                        if (numbLine >= blocksCode.size) {
                            isActive = false
                            break
                        }

                        processing(blocksCode[numbLine])

                        if (waitForInput) {
                            Handler(Looper.getMainLooper()).post {
                                context.inputRequest()
                            }
                            waitForInput = false
                            isActive = false
                            break
                        }

                        numbLine++
                    }
                }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest("Line: " + numbLine.toString() +
                            "\nExpression: " + blocksCode[numbLine].expression +
                            "\n" + e.message)
                }
            } catch (e: EmptyStackException) {
                Handler(Looper.getMainLooper()).post {
                    context.errorRequest("Line: " + numbLine.toString() +
                            "\nExpression: " + blocksCode[numbLine].expression +
                            "\nStack error: " + e.message)
                }
            }
        }.start()
    }

    fun input(expression: String) {
        waitForInput = false
        inputExpression = expression
    }

    private fun toNext(toEnd: Boolean = true) {
        if(blocksCode[numbLine].numbLineToNext != -1) {
            numbLine = blocksCode[numbLine].numbLineToNext - 1
        } else {
            val oldNumbLine = numbLine
            val stack = Stack<InstructionType>()
            stack.add(blocksCode[numbLine].instructionType)
            while (stack.isNotEmpty()) {
                numbLine++

                if(blocksCode[numbLine].instructionType == InstructionType.IF ||
                    blocksCode[numbLine].instructionType == InstructionType.WHILE ||
                    blocksCode[numbLine].instructionType == InstructionType.FOR){
                    stack.add(blocksCode[numbLine].instructionType)
                } else if(stack.size == 1 && !toEnd &&
                    (blocksCode[numbLine].instructionType == InstructionType.ELIF ||
                            blocksCode[numbLine].instructionType == InstructionType.ELSE)) {
                    stack.pop()
                }
                else if(blocksCode[numbLine].instructionType == InstructionType.END) {
                    stack.pop()
                }
            }
            blocksCode[oldNumbLine].numbLineToNext = numbLine
            numbLine--
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

                if((bracketStack.peek().bracket == InstructionType.IF || bracketStack.peek().bracket == InstructionType.ELIF) && bracketStack.peek().into) {
                    toNext(true)
                } else {
                    bracketStack.pop()
                    bracketStack.add(BracketCondition(InstructionType.ELSE, false, numbLine))
                    globalData.createNesting()
                }
            }
            InstructionType.END -> {
                println("end " + block.expression)

                if(bracketStack.isNotEmpty() &&
                           (bracketStack.peek().bracket == InstructionType.IF ||
                            bracketStack.peek().bracket == InstructionType.ELIF ||
                            bracketStack.peek().bracket == InstructionType.ELSE ||
                            bracketStack.peek().bracket == InstructionType.ELSE)) {

                    if(bracketStack.peek().into) globalData.deleteNesting()
                    bracketStack.pop()
                } else if(bracketStack.isNotEmpty() &&
                            bracketStack.peek().bracket == InstructionType.WHILE) {

                    if(bracketStack.peek().into) {
                        numbLine = bracketStack.peek().line - 1
                        globalData.deleteNesting()
                    }
                    bracketStack.pop()
                }
                else {
                    println("ERR: unknown operation")

                    throw Exception("Unknown operation")
                }
            }
            InstructionType.WHILE -> {
                println("while " + block.expression)

                opsWhile(block.expression)
            }
            InstructionType.BREAK -> {
                println("break " + block.expression)

                if(bracketStack.isEmpty()) {
                    throw Exception("The break instruction must be inside for or while")
                }

                while (bracketStack.peek().bracket != InstructionType.WHILE) {
                    bracketStack.pop()
                    if(bracketStack.isEmpty()) {
                        throw Exception("The break instruction must be inside for or while")
                    }
                }

                bracketStack.peek().into = false

                while (blocksCode[numbLine].instructionType != InstructionType.WHILE) {
                    numbLine--
                }

                toNext(true)
            }
            InstructionType.CONTINUE -> {
                println("continue " + block.expression)

                if(bracketStack.isEmpty()) {
                    throw Exception("The continue instruction must be inside for or while")
                }

                while (bracketStack.peek().bracket != InstructionType.WHILE) {
                    bracketStack.pop()
                    if(bracketStack.isEmpty()) {
                        throw Exception("The continue instruction must be inside for or while")
                    }
                }

                while (blocksCode[numbLine].instructionType != InstructionType.WHILE) {
                    numbLine--
                }

                toNext(true)
            }
            else -> 0
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

        val split = arithmeticStack.peekArithmetic().split.ifEmpty {
            arithmeticStack.peekArithmetic().split = arithmeticStringToList(expression)
            arithmeticStack.peekArithmetic().split
        }

        val operatorAction = fun (operator: String){
            if(operator in listOf(".toInt", ".toDouble", ".toBoolean", "!")) {
                val var1 = arithmeticStack.peekArithmetic().variableStack.pop()
                arithmeticStack.peekArithmetic().variableStack.push(when(operator) {
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
                val var1 = arithmeticStack.peekArithmetic().variableStack.pop()
                val var2 = arithmeticStack.peekArithmetic().variableStack.pop()
                arithmeticStack.peekArithmetic().variableStack.push(when(operator) {
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
        while (arithmeticStack.peekArithmetic().curIdSplit < split.size){
            val next = split[arithmeticStack.peekArithmetic().curIdSplit]

            if (!foundCloseBracket && next in priorityOperation || next == "!"){
                println("$next isOperator")

                if(arithmeticStack.peekArithmetic().operationStack.isEmpty()) {
                    arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    val prevOperator = arithmeticStack.peekArithmetic().operationStack.peek()

                    if(prevOperator == "(" || priorityOperation[prevOperator]!! < priorityOperation[next]!!) {
                        arithmeticStack.peekArithmetic().operationStack.add(next)
                    } else {
                        operatorAction(prevOperator)
                        arithmeticStack.peekArithmetic().operationStack.pop()
                        arithmeticStack.peekArithmetic().curIdSplit--
                    }
                }
            } else if(!foundCloseBracket && next in listOf("(", ")")){
                println("$next isBracket")

                if(next == "(") {
                    arithmeticStack.peekArithmetic().operationStack.add(next)
                } else {
                    foundCloseBracket = true
                }
            } else if(foundCloseBracket) {
                val prevOperator = arithmeticStack.peekArithmetic().operationStack.pop()
                if(prevOperator == "(") {
                    foundCloseBracket = false
                } else {
                    operatorAction(prevOperator)
                }
                arithmeticStack.peekArithmetic().curIdSplit--
            } else if(next in listOf("[", "]")) {
                println("ERR: incorrect expression")
                throw Exception("Incorrect expression: false brackets: []")
            } else if(VariableType.isConstant(next)){
                println(next + " isConstant")
                arithmeticStack.peekArithmetic().variableStack.push(VariableType.toVariable(next))
            } else if(isFunction(next)) {
                println(next + " isFunction")

                if(doNotCallFunc) {
                    println("ERR: the function cannot be called in this expression")
                    throw Exception("The function cannot be called in this expression")
                } else {
                    arithmeticStack.peekArithmetic().curIdSplit++
                    callStack.add(StackFrame())
                }

                return false // ops is not over
            } else if(isArray(next)) {
                println(next + " isArray")

                if(fC) {
                    val index = arithmeticStack.peekArithmetic().variableStack.pop()
                    val name = getName(next, '[')
                    val value = globalData.getArrayElement(name, index)

                    arithmeticStack.peekArithmetic().variableStack.push(value)
                } else {
                    arithmeticStack.addArithmetic(Arithmetic())
                    if(opsArithmetic(getExpressionBetween(next, '['))) {
                        val index = arithmeticStack.popArithmetic().variableStack.peek()
                        val name = getName(next, '[')
                        val value = globalData.getArrayElement(name, index)

                        arithmeticStack.peekArithmetic().variableStack.push(value)
                    } else {
                        return false
                    }
                }
            } else {
                println(next + " isVariable")
                arithmeticStack.peekArithmetic().variableStack.push(globalData.getVariable(next))
            }

            fC = false
            arithmeticStack.peekArithmetic().curIdSplit++
        }

        while (arithmeticStack.peekArithmetic().operationStack.isNotEmpty()) {
            val prevOperator = arithmeticStack.peekArithmetic().operationStack.pop()
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
            }

            if(isArray(partTwo)) {
                val name = getName(partTwo, '[')
                val expressionBetween = getExpressionBetween(partTwo, '[')

                arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(expressionBetween, false, true) &&
                    arithmeticStack.size() == 1 &&
                    arithmeticStack.peekArithmetic().variableStack.size == 1
                ) {
                    val countElement = arithmeticStack.popArithmetic().variableStack.peek()
                    globalData.intArray(name, countElement, VariableType.getType(type))
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression: $partTwo")
                }
            } else if(partTwo.contains("=")) {
                val exp = partTwo.split("=")
                val name = exp[0]

                if(exp.size == 2) {
                    val newVar = VariableType.getType(type)

                    arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(exp[1], false, true) &&
                        arithmeticStack.size() == 1 &&
                        arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        newVar.setValue(arithmeticStack.popArithmetic().variableStack.peek())
                        globalData.initVariable(name, newVar)
                    } else {
                        println("ERR: incorrect expression")
                        throw Exception("Incorrect expression")
                    }
                } else {
                    println("ERR: to variable incorrect expression")
                    throw Exception("Incorrect expression: there must be one assignment operator =")
                }
            } else {
                globalData.initVariable(partTwo, VariableType.getType(type))
            }
        } else {
            println("ERR: to variable incorrect expression")
            throw Exception("Incorrect expression: too few operands")
        }
    }

    private fun opsSet(expression: String) {
        if(expression.contains("=")) {
            val expressions = expression.split("=")

            val setOperation = fun (variable: String) {
                val value = arithmeticStack.popArithmetic().variableStack.peek()
                if(isArray(variable)) {
                    val name = getName(variable, '[')
                    val expressionBetween = getExpressionBetween(variable, '[')

                    arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(expressionBetween, false, true) &&
                        arithmeticStack.size() == 1 &&
                        arithmeticStack.peekArithmetic().variableStack.size == 1
                    ) {
                        val index = arithmeticStack.popArithmetic().variableStack.peek()
                        globalData.setArrayElement(name, index, value)
                    } else {
                        println("ERR: incorrect expression")
                        throw Exception("Incorrect expression")
                    }
                } else {
                    globalData.setVariable(variable, value)
                }
            }

            if(expressions.size == 2) {
                val variable = expressions[0].replace(" ", "")
                val arithmeticExpression = expressions[1].replace(" ", "")

                if (arithmeticStack.size() == 1 && arithmeticStack.peekArithmetic().variableStack.size == 1) {
                    setOperation(variable)
                } else if (arithmeticStack.size() == 0) {
                    arithmeticStack.addArithmetic(Arithmetic())
                    if (opsArithmetic(arithmeticExpression)) {
                        setOperation(variable)
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
    }

    private fun opsPrint(expression: String) {
        val exp = expression.replace(" ", "")

        if(exp.isNotEmpty()) {
            if(globalData.checkArray(exp)) {
                console.println(globalData.getArray(exp))
            } else {
                arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(exp, false, true) &&
                    arithmeticStack.size() == 1 &&
                    arithmeticStack.peekArithmetic().variableStack.size == 1
                ) {
                    val out = arithmeticStack.popArithmetic().variableStack.peek()

                    val latch = CountDownLatch(1)
                    val handler = Handler(Looper.getMainLooper())

                    handler.post {
                        if(isActive) {
                            console.println(out)
                        }

                        latch.countDown()
                    }

                    latch.await()
                } else {
                    println("ERR: incorrect expression")
                    throw Exception("Incorrect expression")
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
            globalData.getVariable(variableName[0]).setValue(VariableType.toVariable(inputConstant[0]))
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
            if (arithmeticStack.size() == 1 && arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                if (value.getValue()) {
                    bracketStack.add(BracketCondition(typeIf, true, numbLine))
                    globalData.createNesting()
                } else {
                    toNext(false)
                    bracketStack.add(BracketCondition(typeIf, false, numbLine))
                }
            } else if (arithmeticStack.size() == 0) {
                arithmeticStack.addArithmetic(Arithmetic())
                if (opsArithmetic(expression.replace(" ", ""))) {
                    val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                    if (value.getValue()) {
                        bracketStack.add(BracketCondition(typeIf, true, numbLine))
                        globalData.createNesting()
                    } else {
                        toNext(false)
                        bracketStack.add(BracketCondition(typeIf, false, numbLine))
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

    private fun opsWhile(expression: String) {
        if(expression.isNotEmpty()) {
            if(arithmeticStack.size() == 1 && arithmeticStack.peekArithmetic().variableStack.size == 1) {
                val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                if(value.getValue()) {
                    bracketStack.add(BracketCondition(InstructionType.WHILE, true, numbLine))
                    globalData.createNesting()
                } else {
                    toNext(true)
                    bracketStack.add(BracketCondition(InstructionType.WHILE, false, numbLine))
                }
            } else if(arithmeticStack.size() == 0){
                arithmeticStack.addArithmetic(Arithmetic())
                if(opsArithmetic(expression.replace(" ", ""))) {
                    val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                    if(value.getValue()) {
                        bracketStack.add(BracketCondition(InstructionType.WHILE, true, numbLine))
                        globalData.createNesting()
                    } else {
                        toNext(true)
                        bracketStack.add(BracketCondition(InstructionType.WHILE, false, numbLine))
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

}