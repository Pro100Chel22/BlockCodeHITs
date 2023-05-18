package com.example.myapplication.modules

import android.annotation.SuppressLint
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
        val into: Boolean = false,
        val line: Int
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
    private val arrays: MutableMap<String, Array<Variable>> = mutableMapOf()
    private val deleteStack: Stack<MutableList<String>> = Stack()

    init {
        deleteStack.push(mutableListOf())
    }

    fun initVariable(name: String, variable: Variable) {
        if(variables[name] == null && arrays[name] == null) {
            deleteStack.peek().add(name)
            variables[name] = variable
        } else {
            println("ERROR: name already exist")
        }
    }

    fun intArray(name: String, countElement: Variable, initValue: Variable) {
        if(variables[name] == null && arrays[name] == null) {
            deleteStack.peek().add(name)
            if(countElement is VariableInt) {
                if(countElement.getValue() in 1..1000) {
                    arrays[name] = Array(countElement.getValue()) {
                        if(initValue is VariableInt) {
                            VariableInt(initValue.getValue())
                        } else if(initValue is VariableDouble) {
                            VariableDouble(initValue.getValue())
                        } else if(initValue is VariableBoolean) {
                            VariableBoolean(initValue.getValue())
                        } else {
                            println("ERROR: incorrect data type!!!!!!!!")
                            Variable()
                        }
                    }
                } else {
                    println("ERROR: the array can contain from 1 to 1000 elements")
                }
            } else {
                println("ERROR: incorrect data type")
            }
        } else {
            println("ERROR: name already exist")
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

    fun getArrayElement(name: String, index: Variable): Variable {
        val array = arrays[name]

        return if(array != null) {
            if(index is VariableInt) {
                if(index.getValue() in (array.indices)) {
                    array[index.getValue()]
                } else {
                    println("there is no such element")
                    Variable()
                }
            } else {
                println("ERROR: incorrect data type")
                Variable()
            }
        } else {
            println("ERROR: array not exist")
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

    fun setArrayElement (name: String, index: Variable, value: Variable) {
        val array = arrays[name]

        if(array != null) {
            if(index is VariableInt) {
                if(index.getValue() in (array.indices)) {
                    arrays[name]!![index.getValue()].setValue(value)
                } else {
                    println("there is no such element")
                }
            } else {
                println("ERROR: incorrect data type")
            }
        } else {
            println("ERROR: array not exist")
        }
    }

    fun createNesting() {
        deleteStack.push(mutableListOf())
    }

    fun deleteNesting() {
        for(name in deleteStack.peek()) {
            variables.remove(name)
            arrays.remove(name)
        }
        deleteStack.pop()
    }
}

class Console(_consoleContainer: LinearLayout, _context: CodingActivity, _resourceLayout: Int) {
    private var consoleContainer: LinearLayout = _consoleContainer
    private var context: CodingActivity = _context
    private var resourceLayout: Int = _resourceLayout
    private var countLine: Int = 0

    init {
        addLine()
    }

    fun clear() {
        consoleContainer.removeAllViewsInLayout()
        addLine()
    }

    fun print(variable: Variable) {

    }

    @SuppressLint("SetTextI18n")
    fun println(variable: Variable) {
        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        if(consoleContainer.childCount > 100 || countLine >  100) {
            clear()
            countLine = 0
        } else {
            //addLine()
        }

        when (variable) {
            is VariableInt -> {
                currentLine.text = currentLine.text.toString()  + variable.getValue().toString() + "\n"
                countLine++
            }
            is VariableDouble -> {
                currentLine.text = currentLine.text.toString()  + variable.getValue().toString() + "\n"
                countLine++
            }
            is VariableBoolean -> {
                currentLine.text = currentLine.text.toString()  + variable.getValue().toString() + "\n"
                countLine++
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
