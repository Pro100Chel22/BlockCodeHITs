package com.example.myapplication.modules

import android.view.View
import com.example.myapplication.CodingActivity
import java.lang.ArithmeticException

class Interpreter {
    private val globalData = DataStack()
    private val callStack = CallStack()

    private var numbLine: Int = 0
    private var waitForInput = false
    private var inputExpression: String = ""

    private var context: CodingActivity
    private val blocksView: List<View>
    private val blocksCode: MutableList<Block> = mutableListOf()
    private var console: Console

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
        for(i in numbLine until blocksCode.size) {
            numbLine = i

            processing(blocksCode[i])

            if(waitForInput) {
                context.inputRequest()
                waitForInput = false
                println(waitForInput.toString())
                break
            }
        }
    }

    fun input(expression: String) {
        waitForInput = false
        inputExpression = expression
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
            else -> 0
        }

    }

    private fun arithmeticStringToList(arithmetic: String): List<String> {
        val list: MutableList<String> = mutableListOf()

        val listSigns = listOf('-', '+', '/', '*', '(', ')', '[', ']')
        // "==", "!=", "<=", ">=", "||", "&&"

        var i = 0
        var expression = ""
        while (i < arithmetic.length) {
            if(arithmetic[i] in listSigns) {
                list.add(expression)
                expression = ""
            } else if(true) {

            } else {
                expression += arithmetic[i]
            }

            i++
        }

        return list
    }

    private fun opsArithmetic(expression: String): Variable {
        arithmeticStringToList(expression)
        return VariableType.toVariable(expression)
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
            var arithmetic = ""
            for(i in 2 until  expressions.size) {
                arithmetic += expressions[i]
            }
            globalData.setVariable(expressions[0], opsArithmetic(arithmetic))
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
}