package com.example.myapplication.modules

import android.annotation.SuppressLint
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication.CodingActivity
import java.util.Stack

data class CallStackFrame (
    val startNumLine: Int = 0,
    var curNmbLine: Int = 0,
    var funcIsOver: Boolean = true,
    val returnVariable: Variable = VariableDouble(10.0),
    val inputVariables: Map<String, Variable> = mapOf(),
    val arithmeticStack: ArithmeticStack = ArithmeticStack()
)

data class Arithmetic(
    var split: List<String> = listOf(),
    val variableStack: Stack<Variable> = Stack(),
    val operationStack: Stack<String> = Stack(),
    var curIdSplit: Int = 0,
    var functionCall: Boolean = false
)

data class BracketCondition(
    val bracket: InstructionType,
    var into: Boolean = false,
    val line: Int,
    var doArithmeticBlock: Boolean = false
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

//    init {
//        funcLocalData.push(DataStack())
//    }

    fun checkArray(name: String): Boolean {
        //throw Exception("checkArray")

        if(globalData.checkArray(name) || funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            return true
        }
        return false
    } // YES

    fun getArray(name: String): Array<Variable> {
        //throw Exception("getArray")

        return if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().getArray(name)
        } else if(globalData.checkArray(name)) {
            globalData.getArray(name)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    } // YES

    fun initVariable(name: String, variable: Variable) {
        //throw Exception("initVariable")

        if(funcLocalData.isEmpty()) {
            globalData.initVariable(name, variable)
        } else {
            funcLocalData.peek().initVariable(name, variable)
        }
    } // YES

    fun initArray(name: String, countElement: Variable, initValue: Variable) {
        //throw Exception("intArray")

        if(funcLocalData.isEmpty()) {
            globalData.initArray(name, countElement, initValue)
        } else {
            funcLocalData.peek().initArray(name, countElement, initValue)
        }
    } // YES

    fun getVariable(name: String): Variable {
        //throw Exception("getVariable")
        val global = globalData.getVariable(name)

        return if(funcLocalData.isEmpty()) {
            if(global !is VariableInt && global !is VariableDouble && global !is VariableBoolean) {
                throw Exception(globalData.getErrorText())
            }
            global
        } else {
            val local = funcLocalData.peek().getVariable(name)
            if(local !is VariableInt && local !is VariableDouble && local !is VariableBoolean) {
                if(global !is VariableInt && global !is VariableDouble && global !is VariableBoolean) {
                    throw Exception(globalData.getErrorText())
                }
                global
            } else {
                local
            }
        }
    } // YES

    fun getArrayElement(name: String, index: Variable): Variable {
        // throw Exception("getArrayElement")

        return if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().getArrayElement(name, index)
        } else if(globalData.checkArray(name)) {
            globalData.getArrayElement(name, index)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    } // YES

    fun setVariable(name: String, variable: Variable) {
        // throw Exception("setVariable")

        if(funcLocalData.isEmpty()) {
            if(!globalData.setVariable(name, variable)) {
                throw Exception(globalData.getErrorText())
            }
        } else {
            if(!funcLocalData.peek().setVariable(name, variable) && !globalData.setVariable(name, variable)) {
                throw Exception(globalData.getErrorText())
            }
        }
    } // YES

    fun setArrayElement (name: String, index: Variable, value: Variable) {
        // throw Exception("setArrayElement")

        if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().setArrayElement(name, index, value)
        } else if(globalData.checkArray(name)) {
            globalData.setArrayElement(name, index, value)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    } // YES

    fun createNesting() {
        // throw Exception("createNesting")

        if(funcLocalData.isEmpty()) {
            globalData.createNesting()
        } else {
            funcLocalData.peek().createNesting()
        }

    } // YES

    fun deleteNesting() {
        // throw Exception("deleteNesting")

        if(funcLocalData.isEmpty()) {
            globalData.deleteNesting()
        } else {
            funcLocalData.peek().deleteNesting()
        }
    } // YES
}

class DataStack {
    private val variables: MutableMap<String, Variable> = mutableMapOf()
    private val arrays: MutableMap<String, Array<Variable>> = mutableMapOf()
    private val deleteStack: Stack<MutableList<String>> = Stack()
    private var errorText: String = ""

    init {
        deleteStack.push(mutableListOf())
    }

    fun getErrorText(): String {
        return errorText
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

    fun initArray(name: String, countElement: Variable, initValue: Variable) {
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
            errorText = "The variable not exist: $name"
            Variable()
           // throw Exception("The variable not exist: $name")
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
                    // errorText = "Incorrect data type: there is no such element, $name[$i]"
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                println("ERR: incorrect data type")
                Variable()
                // errorText = "Incorrect data type: the array index must be an integer, $name"
                throw Exception("Incorrect data type: the array index must be an integer, $name")
            }
        } else {
            println("ERR: array not exist")
            Variable()
            // errorText = "The array not exist: $name[]"
            throw Exception("The array not exist: $name[]")
        }
    }

    fun setVariable(name: String, variable: Variable): Boolean {
        VariableType.isName(name)

        if(variables[name] != null) {
            variables[name]?.setValue(variable)
        } else {
            println("ERR: variable not exist")
            errorText = "The variable not exist: $name"
            //throw Exception("The variable not exist: $name")
            return false
        }

        return true
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
        if(consoleContainer.childCount > 100 || countLine >= 100) {
            clear()
        }

        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

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
        currentLine.requestLayout()
    }

    @SuppressLint("SetTextI18n")
    fun println(array: Array<Variable>) {
        if(consoleContainer.childCount > 100 || countLine >=  100) {
            clear()
        }

        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

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
