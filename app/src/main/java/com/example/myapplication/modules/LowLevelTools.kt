package com.example.myapplication.modules

import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication.CodingActivity
import java.util.Stack

class StackFrame {
    val returnVariable = VariableInt(10)
    var numbLine = 0
    val funcIsOver = true
}

data class Arithmetic(
    var split: List<String> = listOf(),
    val variableStack: Stack<Variable> = Stack(),
    val operationStack: Stack<String> = Stack(),
    var curIdSplit: Int = 0,
    var functionCall: Boolean = false
)

data class BracketCondition (
        val bracket: InstructionType,
        val into: Boolean = false
)

class ArithmeticStack {
    private val arithmeticStack = Stack<Arithmetic>()

    fun addArithmetic(arithmetic: Arithmetic) {
        arithmeticStack.add(arithmetic)
    }

    fun popArithmetic(): Arithmetic {
        return arithmeticStack.pop()
    }

    fun peekArithmetic(): Arithmetic {
        return arithmeticStack.peek()
    }

    fun size(): Int {
        return arithmeticStack.size
    }
}
class Data {
    private val globalData = DataStack()
    private val funcLocalData = Stack<DataStack>()

    fun addFuncLocalData() {

    }

    fun popFuncLocalData() {

    }

    fun initVariable(name: String, variable: Variable) {

    }

    fun getVariable(name: String): Variable {
       return Variable()
    }

    fun setVariable(name: String, variable: Variable) {

    }

    fun createNesting() {

    }

    fun deleteNesting() {

    }
}

class DataStack {
    private val variables: MutableMap<String, Variable> = mutableMapOf()
    private val deleteStack: Stack<MutableList<String>> = Stack()

    init {
        deleteStack.push(mutableListOf())
    }

    fun initVariable(name: String, variable: Variable) {
        if(variables[name] == null) {
            deleteStack.peek().add(name)
            variables[name] = variable
        } else {
            println("ERROR: variable already exist")
        }
    }

    fun getVariable(name: String): Variable {
        val variable = variables[name]

        return if(variable != null) {
            variable
        } else {
            println("ERROR: variable not exist")
            Variable()
        }
    }

    fun setVariable(name: String, variable: Variable) {
        if(variables[name] != null) {
            variables[name]?.setValue(variable)
        } else {
            println("ERROR: variable not exist")
        }
    }

    fun createNesting() {
        deleteStack.push(mutableListOf())
    }

    fun deleteNesting() {
        for(name in deleteStack.peek()) {
            variables.remove(name)
        }
        deleteStack.pop()
    }
}

class Console(_consoleContainer: LinearLayout, _context: CodingActivity, _resourceLayout: Int) {
    private var consoleContainer: LinearLayout = _consoleContainer
    private var context: CodingActivity = _context
    private var resourceLayout: Int = _resourceLayout

    init {
        addLine()
    }

    fun clear() {
        consoleContainer.removeAllViewsInLayout()
        addLine()
    }

    fun print(variable: Variable) {

    }

    fun println(variable: Variable) {
        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        addLine()

        when (variable) {
            is VariableInt -> {
                currentLine.text = variable.getValue().toString()
            }
            is VariableDouble -> {
                currentLine.text = variable.getValue().toString()
            }
            is VariableBoolean -> {
                currentLine.text = variable.getValue().toString()
            }
            else -> {
                println("ERROR: incorrect expression output")
            }
        }
    }

    private fun addLine() {
        val line = TextView.inflate(context, resourceLayout, null)
        consoleContainer.addView(line)
    }
}
