package com.example.myapplication.modules

import android.view.View
import com.example.myapplication.CodingActivity
import java.util.Stack

class Interpreter {
    private val globalData = DataStack()

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

    private val error = false

    constructor(_blocksView: List<View>, _context: CodingActivity, _console: Console) {
        blocksView = _blocksView
        context = _context
        console = _console

        for(blockView in blocksView) {
            val block = blockViewToBlock(blockView, context)
            blocksCode.add(block)
        }
    }

    fun run() {
        while (!error) {
            if(callStack.isNotEmpty()) {
                // Когда вызывается функция, создается кадр стека и кладется в стек вызовов, а в памяти
                // выделяется помять под локальные переменные функции
                // Если стек вызовов не пуст, проверяется закончилась функция или нет, если закончилась,
                // возвращаем значение функции в место, где она вызвана (стек арифметических выражений)
                // и убираем ее из стека, если не закончилась, то выполняем блок, на котором она находится
                //

                if(callStack.peek().funcIsOver) {
                    arithmeticStack.peekArithmetic().variableStack.add(callStack.peek().returnVariable)
                    callStack.pop()

                    while (opsArithmetic("", true)) {
                        if(arithmeticStack.size() > 1) {
                            val index = arithmeticStack.popArithmetic().variableStack.peek()
                            arithmeticStack.peekArithmetic().variableStack.push(index)
                        } else {
//                            if(blocksCode[numbLine - 1].instructionType == InstructionType.SET) {
//                                val nameVar = blocksCode[numbLine - 1].expression.trim().split("\\s+".toRegex())[0]
//                                globalData.setVariable(nameVar, arithmeticStack.popArithmetic().variableStack.peek())
//                            }
                            numbLine--
                            break
                        }
                    }
                }
            } else {
                if(numbLine >= blocksCode.size) {
                    break
                }

                processing(blocksCode[numbLine])

                if(waitForInput) {
                    context.inputRequest()
                    waitForInput = false
                    println(waitForInput.toString())
                    break
                }

                numbLine++
            }
        }
    }

    fun input(expression: String) {
        waitForInput = false
        inputExpression = expression
    }

    // Если зашли в if то после выхода идем до end, если нет, то идем до elif или else
    private fun ifToNext(toEnd: Boolean = true) {
//        numbLine++
//
//        while(!(blocksCode[numbLine].instructionType == InstructionType.END ||
//              !toEnd && (blocksCode[numbLine].instructionType == InstructionType.ELIF ||
//                         blocksCode[numbLine].instructionType == InstructionType.ELSE))) {
//
//            numbLine++
//
//            if(numbLine >= blocksCode.size) {
//                println("ERROR: wrong block code")
//                return
//            }
//        }
//
//        numbLine--

        val stack = Stack<InstructionType>()
        stack.add(blocksCode[numbLine].instructionType);
        while (stack.isNotEmpty()) {
            numbLine++

            if(blocksCode[numbLine].instructionType == InstructionType.IF){
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
        numbLine--
    }

    private fun processing(block: Block) {
        when(block.instructionType) {
            InstructionType.VAR -> {
                val expressions = block.expression.split(",")

                for (exp in expressions) {
                    opsVar(exp)
                }
            }
            InstructionType.SET -> {
                opsSet(block.expression)
            }
            InstructionType.PRINT -> {
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
                           println("ERROR: little input data")
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
                    ifToNext()
                } else {
                    bracketStack.pop()
                    opsIf(block.expression, InstructionType.ELIF)
                }
            }
            InstructionType.ELSE -> {
                println("else " + block.expression)

                if((bracketStack.peek().bracket == InstructionType.IF || bracketStack.peek().bracket == InstructionType.ELIF) && bracketStack.peek().into) {
                    ifToNext()
                } else {
                    bracketStack.pop()
                    bracketStack.add(BracketCondition(InstructionType.ELSE, false))
                    globalData.createNesting()
                }
            }
            InstructionType.END -> {
                println("end " + block.expression)

                if(bracketStack.isNotEmpty() && bracketStack.peek().bracket == InstructionType.IF || bracketStack.peek().bracket == InstructionType.ELIF || bracketStack.peek().bracket == InstructionType.ELSE) {
                    bracketStack.pop()
                    globalData.deleteNesting()
                } else {
                    println("ERROR: unknown operation")
                }
            }
            else -> 0
        }
    }

    private fun arithmeticStringToList(arithmetic: String): List<String> {
        val list: MutableList<String> = mutableListOf()

        val listMathOperation = listOf('-', '+', '/', '*')
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
                    println("ERROR: incorrect expression")
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
                    println("ERROR: incorrect expression")
                }

                if(i < arithmetic.length && arithmetic[i].isLetter()) {
                    println("ERROR: incorrect expression")
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
                        println("ERROR: incorrect expression")
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

    private fun opsArithmetic(expression: String, functionCall: Boolean = false): Boolean {
        val priorityOperation = mapOf(
            "||" to 0, "&&" to 0, ">=" to 0, "<=" to 0, ">" to 0, "<" to 0, "!=" to 0, "==" to 0,
            "+" to 1, "-" to 1,
            "*" to 2, "/" to 2,
            "!" to 3,
            ".toInt" to 4, ".toDouble" to 4, ".toBoolean" to 4
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
                        println("ERROR: incorrect expression")
                        Variable()
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
                    "||" -> var2.or(var1)
                    "&&" -> var2.and(var1)
                    ">" -> var2.compareMore(var1)
                    "<" -> var2.compareLess(var1)
                    ">=" -> var2.compareMore(var1).or(var2.equal(var1))
                    "<=" -> var2.compareLess(var1).or(var2.equal(var1))
                    "==" -> var2.equal(var1)
                    "!=" -> var2.equal(var1).not()
                    else -> {
                        println("ERROR: incorrect expression")
                        Variable()
                    }
                })
            }
        }

        var fC = functionCall

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
                println("ERROR: incorrect expression")
            } else if(VariableType.isConstant(next)){
                println(next + " isConstant")
                arithmeticStack.peekArithmetic().variableStack.push(VariableType.toVariable(next))
            } else if(isFunction(next)) {
                println(next + " isFunction")

                arithmeticStack.peekArithmetic().curIdSplit++
                callStack.add(StackFrame())
                return false // ops is not over
            } else if(isArray(next)) {
                println(next + " isArray")

                if(fC) {
                   // arithmeticStack.peekArithmetic().variableStack.push(arithmeticStack.popArithmetic().variableStack.peek())
                } else {
                    arithmeticStack.addArithmetic(Arithmetic())
                    if(opsArithmetic(getExpressionBetween(next, '['))) {
                        val index = arithmeticStack.popArithmetic().variableStack.peek()
                        arithmeticStack.peekArithmetic().variableStack.push(index)
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
        val expressions = expression.trim().split("\\s+".toRegex())

        if(expressions.size >= 2) {
            val newVar = VariableType.getType(expressions[0])
            if(expressions.size >= 4 && expressions[2] == "=") {
                var arithmetic = ""
                for(i in 3 until  expressions.size) {
                    arithmetic += expressions[i]
                }
                newVar.setValue(VariableType.toVariable(arithmetic))
            } else if(expressions.size >= 3) {
                println("ERROR: to variable incorrect expression")
            }
            globalData.initVariable(expressions[1], newVar)
        } else {
            println("ERROR: to variable incorrect expression")
        }
    }

    private fun opsSet(expression: String) {
        val expressions = expression.trim().split("\\s+".toRegex())

        if(expressions.size > 2 && expressions[1] == "=") {
            var arithmeticExpression = ""
            for(i in 2 until  expressions.size) {
                arithmeticExpression += expressions[i]
            }

            if(arithmeticStack.size() == 1 && arithmeticStack.peekArithmetic().variableStack.size == 1) {
                globalData.setVariable(expressions[0], arithmeticStack.popArithmetic().variableStack.peek())
            } else if(arithmeticStack.size() == 0){
                arithmeticStack.addArithmetic(Arithmetic())
                if(opsArithmetic(arithmeticExpression)) {
                    globalData.setVariable(expressions[0], arithmeticStack.popArithmetic().variableStack.peek())
                }
            } else {
                println("ERROR: arithmetic stack")
            }
        } else {
            println("ERROR: incorrect expression")
        }
    }

    private fun opsPrint(expression: String) {
        val expressions = expression.trim().split("\\s+".toRegex())

        if(expressions.size == 1 && expressions[0].isNotEmpty()) {
            val out = if (expressions[0][0].isLetter()) {
                globalData.getVariable(expressions[0])
            } else {
                VariableType.toVariable(expressions[0])
            }

            console.println(out)
        } else {
            println("ERROR: incorrect expression output")
        }
    }

    private fun opsInput(expression: String, expressionInput: String) {
        val variableName = expression.trim().split("\\s+".toRegex())
        val inputConstant = expressionInput.trim().split("\\s+".toRegex())

        if(variableName.size == 1 && inputConstant.size == 1) {
            globalData.getVariable(variableName[0]).setValue(VariableType.toVariable(inputConstant[0]))
        } else if(variableName.size != 1) {
            println("ERROR: incorrect expression")
        } else {
            println("ERROR: invalid input data")
        }
    }

    private fun opsIf(expression: String, typeIf: InstructionType) {
        if(arithmeticStack.size() == 1 && arithmeticStack.peekArithmetic().variableStack.size == 1) {
            val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
            if(value.getValue()) {
                bracketStack.add(BracketCondition(typeIf, true))
                globalData.createNesting()
            } else {
                ifToNext(false)
                bracketStack.add(BracketCondition(typeIf, false))
            }
        } else if(arithmeticStack.size() == 0){
            arithmeticStack.addArithmetic(Arithmetic())
            if(opsArithmetic(expression.replace(" ", ""))) {
                val value = arithmeticStack.popArithmetic().variableStack.peek().toBoolean()
                if(value.getValue()) {
                    bracketStack.add(BracketCondition(typeIf, true))
                    globalData.createNesting()
                } else {
                    ifToNext(false)
                    bracketStack.add(BracketCondition(typeIf, false))
                }
            }
        } else {
            println("ERROR: arithmetic stack")
        }
    }
}