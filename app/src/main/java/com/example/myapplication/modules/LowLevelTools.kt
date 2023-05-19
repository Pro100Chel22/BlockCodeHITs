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
    //private val globalData = DataStack()
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
   // private var errorController: ErrorController = _errorController

    init {
        deleteStack.push(mutableListOf())
    }

    fun checkArray(name: String): Boolean {
        if(name in arrays) {
            return true
        }
        return false
    }

    fun getArray(name: String): Array<Variable> {
        return arrays[name]!!
    }

    fun initVariable(name: String, variable: Variable) {
        if(variables[name] == null && arrays[name] == null) {
            VariableType.isName(name)

            deleteStack.peek().add(name)
            variables[name] = variable
        } else {
            println("ERR: name already exist")
            throw Exception("The name already exist: $name")
        }
    }

    fun intArray(name: String, countElement: Variable, initValue: Variable) {
        if(variables[name] == null && arrays[name] == null) {
            VariableType.isName(name)

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
                            println("ERR: incorrect data type!!!!!!!!")
                            Variable()
                            throw Exception("Incorrect data type: this type does not exist, $name")
                        }
                    }
                } else {
                    val i = countElement.getValue()
                    println("ERR: the array can contain from 1 to 1000 elements")
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                throw Exception("Incorrect data type: the number of elements must be integer, $name")
            }
        } else {
            println("ERR: name already exist")
            throw Exception("The name already exist: $name[]")
        }
    }

    fun getVariable(name: String): Variable {
        VariableType.isName(name)

        val variable = variables[name]
        return if(variable != null) {
            variable
        } else {
            println("ERR: variable not exist")
            Variable()
            throw Exception("The variable not exist: $name")
        }
    }

    fun getArrayElement(name: String, index: Variable): Variable {
        VariableType.isName(name)

        val array = arrays[name]
        return if(array != null) {
            if(index is VariableInt) {
                val i = index.getValue()
                if(i in (array.indices)) {
                    array[i]
                } else {
                    println("ERR: there is no such element")
                    Variable()
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                println("ERR: incorrect data type")
                Variable()
                throw Exception("Incorrect data type: the array index must be an integer, $name")
            }
        } else {
            println("ERR: array not exist")
            Variable()
            throw Exception("The array not exist: $name[]")
        }
    }

    fun setVariable(name: String, variable: Variable) {
        VariableType.isName(name)

        if(variables[name] != null) {
            variables[name]?.setValue(variable)
        } else {
            println("ERR: variable not exist")
            throw Exception("The variable not exist: $name")
        }
    }

    fun setArrayElement (name: String, index: Variable, value: Variable) {
        VariableType.isName(name)

        val array = arrays[name]
        if(array != null) {
            if(index is VariableInt) {
                val i = index.getValue()
                if(i in (array.indices)) {
                    arrays[name]!![i].setValue(value)
                } else {
                    println("ERR: there is no such element")
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                println("ERR: incorrect data type")
                throw Exception("Incorrect data type: the array index must be an integer, $name")
            }
        } else {
            println("ERR: array not exist")
            throw Exception("The array not exist: $name[]")
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
        countLine = 0
    }

    fun print(variable: Variable) {

    }

    @SuppressLint("SetTextI18n")
    fun println(variable: Variable) {
        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        if(consoleContainer.childCount > 100 || countLine >  100) {
            clear()
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
                println("ERR: incorrect expression output")
                throw Exception("Incorrect expression output")
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun println(array: Array<Variable>) {
        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        if(consoleContainer.childCount > 100 || countLine >  100) {
            clear()
        }

        currentLine.text = currentLine.text.toString() + "["
        for(element in array) {
            when (element) {
                is VariableInt -> {
                    currentLine.text = currentLine.text.toString() + element.getValue() + ", "
                }
                is VariableDouble -> {
                    currentLine.text = currentLine.text.toString() + element.getValue() + ", "
                }
                is VariableBoolean -> {
                    currentLine.text = currentLine.text.toString() + element.getValue() + ", "
                }
                else -> {
                    println("ERR: incorrect expression output")
                    throw Exception("Incorrect expression output")
                }
            }
        }
        countLine++
        currentLine.text = currentLine.text.toString().substring(0, currentLine.text.toString().length - 2) + "]\n"
    }

    private fun addLine() {
        val line = TextView.inflate(context, resourceLayout, null)
        consoleContainer.addView(line)
    }
}
