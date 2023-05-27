package com.example.myapplication.modules

import android.annotation.SuppressLint
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication.CodingActivity
import java.util.Stack

data class CallStackFrame(
    val startNumLine: Int = 0,
    var curNmbLine: Int = 0,
    var funcIsOver: Boolean = true,
    val returnVariable: Variable = VariableDouble(10.0),
    val inputVariables: List<ParametersFuncCondition> = listOf(),
    val arithmeticStack: ArithmeticStack = ArithmeticStack()
)

fun getCloneCallStackFrame(origin: CallStackFrame, funcCall: Boolean): CallStackFrame {
    val cloneInput = mutableListOf<ParametersFuncCondition>()

    for (input in origin.inputVariables) {
        cloneInput.add(
            ParametersFuncCondition(
                input.name,
                if (input.value is VariableInt) {
                    VariableInt(0)
                } else if (input.value is VariableDouble) {
                    VariableDouble(0.0)
                } else if (input.value is VariableBoolean) {
                    VariableBoolean()
                } else {
                    Variable()
                    throw Exception("Wrong type")
                },
                input.inited
            )
        )
    }

    return CallStackFrame(
        origin.startNumLine,
        if (funcCall) origin.curNmbLine else origin.curNmbLine - 1,
        false,
        origin.returnVariable.clone(),
        cloneInput
    )
}

data class Arithmetic(
    var split: List<String> = listOf(),
    val variableStack: Stack<Variable> = Stack(),
    val operationStack: Stack<String> = Stack(),
    var curIdSplit: Int = 0,
    var functionCall: Boolean = false,
    val callFrame: Stack<CallStackFrame> = Stack()
)

data class BracketCondition(
    val bracket: InstructionType,
    var into: Boolean = false,
    val line: Int,
    var doArithmeticBlock: Boolean = false
)

data class ParametersFuncCondition(
    val name: String,
    val value: Variable,
    var inited: Boolean = false
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

    fun getVariables(): String {
        var str = "Global scope: \n"

        val printVar = fun(name: String, variable: Variable) {
            str += when (variable) {
                is VariableInt -> "\tint $name = ${variable.getValue()}\n"
                is VariableDouble -> "\tdouble $name = ${variable.getValue()}\n"
                is VariableBoolean -> "\tboolean $name = ${variable.getValue()}\n"
                is VariableChar -> "\tchar $name = '${variable.getValue()}'\n"
                else -> "\tvoid $name\n"
            }
        }

        for ((name, variable) in globalData.getVariables().entries) {
            printVar(name, variable)
        }

        if (funcLocalData.isNotEmpty()) {
            str += "\nCurrent func local scope: \n"
            for ((name, variable) in funcLocalData.peek().getVariables().entries) {
                printVar(name, variable)
            }
        }

        return str
    }

    fun getArrays(shawArrays: Boolean): String {
        var str = "Global scope: \n"

        val printArr = fun(name: String, array: Array<Variable>) {
            if (array.isNotEmpty()) {
                var line = ""
                line += when (array[0]) {
                    is VariableInt -> "\tint $name[${array.size}] = ["
                    is VariableDouble -> "\tdouble $name[${array.size}] = ["
                    is VariableBoolean -> "\tboolean $name[${array.size}] = ["
                    is VariableChar -> "\tchar $name[${array.size}] = ["
                    else -> "\tvoid $name"
                }

                for (i in array.indices) {
                    if (!shawArrays && i >= 8) {
                        line += "..., "
                        break
                    }

                    val variable = array[i]

                    line += when (variable) {
                        is VariableInt -> "${variable.getValue()}, "
                        is VariableDouble -> "${variable.getValue()}, "
                        is VariableBoolean -> "${variable.getValue()}, "
                        is VariableChar -> "'${variable.getValue()}', "
                        else -> ", "
                    }
                }

                str += line.substring(0, line.length - 2) + "]\n"
            } else {
                str += "\tchar $name[${array.size}] = []"
            }
        }

        for ((name, array) in globalData.getArrays()) {
            printArr(name, array)
        }

        if (funcLocalData.isNotEmpty()) {
            str += "\nCurrent func local scope: \n"
            for ((name, array) in funcLocalData.peek().getArrays()) {
                printArr(name, array)
            }
        }

        return str
    }

    fun checkArray(name: String): Boolean {
        if (globalData.checkArray(name) || funcLocalData.isNotEmpty() && funcLocalData.peek()
                .checkArray(name)
        ) {
            return true
        }
        return false
    }

    fun getArray(name: String): Array<Variable> {
        return if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().getArray(name)
        } else if (globalData.checkArray(name)) {
            globalData.getArray(name)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    }

    fun setCharArray(name: String, string: String) {
        if (funcLocalData.isEmpty()) {
            if (!globalData.setCharArray(name, string)) {
                throw Exception(globalData.getErrorText())
            }
        } else {
            if (!funcLocalData.peek().setCharArray(name, string) && !globalData.setCharArray(
                    name,
                    string
                )
            ) {
                throw Exception(globalData.getErrorText())
            }
        }
    }

    fun initVariable(name: String, variable: Variable) {
        if (funcLocalData.isEmpty()) {
            globalData.initVariable(name, variable)
        } else {
            funcLocalData.peek().initVariable(name, variable)
        }
    }

    fun initArray(name: String, countElement: Variable, initValue: Variable, string: String = "") {
        if (funcLocalData.isEmpty()) {
            globalData.initArray(name, countElement, initValue, string)
        } else {
            funcLocalData.peek().initArray(name, countElement, initValue, string)
        }
    }

    fun getVariable(name: String): Variable {
        val global = globalData.getVariable(name)

        return if (funcLocalData.isEmpty()) {
            if (global !is VariableInt && global !is VariableDouble && global !is VariableBoolean && global !is VariableChar) {
                throw Exception(globalData.getErrorText())
            }
            global
        } else {
            val local = funcLocalData.peek().getVariable(name)
            if (local !is VariableInt && local !is VariableDouble && local !is VariableBoolean && local !is VariableChar) {
                if (global !is VariableInt && global !is VariableDouble && global !is VariableBoolean && global !is VariableChar) {
                    throw Exception(globalData.getErrorText())
                }
                global
            } else {
                local
            }
        }
    }

    fun getArrayElement(name: String, index: Variable): Variable {
        return if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().getArrayElement(name, index)
        } else if (globalData.checkArray(name)) {
            globalData.getArrayElement(name, index)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    }

    fun setVariable(name: String, variable: Variable) {
        if (funcLocalData.isEmpty()) {
            if (!globalData.setVariable(name, variable)) {
                throw Exception(globalData.getErrorText())
            }
        } else {
            if (!funcLocalData.peek().setVariable(name, variable) && !globalData.setVariable(
                    name,
                    variable
                )
            ) {
                throw Exception(globalData.getErrorText())
            }
        }
    }

    fun setArrayElement(name: String, index: Variable, value: Variable) {
        if (funcLocalData.isNotEmpty() && funcLocalData.peek().checkArray(name)) {
            funcLocalData.peek().setArrayElement(name, index, value)
        } else if (globalData.checkArray(name)) {
            globalData.setArrayElement(name, index, value)
        } else {
            throw Exception("The array not exist: $name[]")
        }
    }

    fun createNesting() {
        if (funcLocalData.isEmpty()) {
            globalData.createNesting()
        } else {
            funcLocalData.peek().createNesting()
        }
    }

    fun deleteNesting() {
        if (funcLocalData.isEmpty()) {
            globalData.deleteNesting()
        } else {
            funcLocalData.peek().deleteNesting()
        }
    }

    fun createFuncNesting() {
        funcLocalData.push(DataStack())
    }

    fun deleteFuncNesting() {
        funcLocalData.pop()
    }
}

class DataStack {
    private val variables: MutableMap<String, Variable> = mutableMapOf()
    private val arrays: MutableMap<String, Array<Variable>> = mutableMapOf()
    private val deleteStack: Stack<MutableList<String>> = Stack()
    private var errorText: String = ""

    init {
        deleteStack.push(mutableListOf())
    }

    fun getVariables(): Map<String, Variable> {
        return variables
    }

    fun getArrays(): Map<String, Array<Variable>> {
        return arrays
    }

    fun getErrorText(): String {
        return errorText
    }

    fun checkArray(name: String): Boolean {
        if (name in arrays) {
            return true
        }
        return false
    }

    fun getArray(name: String): Array<Variable> {
        return arrays[name]!!
    }

    fun initVariable(name: String, variable: Variable) {
        if (variables[name] == null && arrays[name] == null) {
            VariableType.isName(name)

            deleteStack.peek().add(name)
            variables[name] = variable
        } else {
            throw Exception("The name already exist: $name")
        }
    }

    fun initArray(name: String, countElement: Variable, initValue: Variable, string: String) {
        if (variables[name] == null && arrays[name] == null) {
            VariableType.isName(name)

            deleteStack.peek().add(name)
            if (countElement is VariableInt) {
                if (countElement.getValue() in 1..1000) {
                    if (initValue is VariableChar && string.length > 1) {
                        if (countElement.getValue().toInt() < string.length - 2) {
                            throw Exception("Incorrect data type: the entered string is larger than the size of the array, $string")
                        }
                        arrays[name] = Array(countElement.getValue().toInt()) {
                            VariableChar(initValue.getValue())
                        }
                        for (i in arrays[name]!!.indices) {
                            if (string.length - 3 < i) {
                                break
                            }
                            arrays[name]!![i] = VariableChar(string[i + 1])
                        }
                    } else {
                        arrays[name] = Array(countElement.getValue().toInt()) {
                            when (initValue) {
                                is VariableInt -> VariableInt(initValue.getValue())
                                is VariableDouble -> VariableDouble(initValue.getValue())
                                is VariableBoolean -> VariableBoolean(initValue.getValue())
                                is VariableChar -> VariableChar(initValue.getValue())
                                else -> throw Exception("Incorrect data type: this type does not exist, $name")
                            }
                        }
                    }
                } else if (initValue is VariableChar && countElement.getValue()
                        .toInt() == -1 && string.length > 1
                ) {
                    arrays[name] = Array(string.length - 2) {
                        VariableChar(initValue.getValue())
                    }
                    if (arrays[name]!!.isNotEmpty()) {
                        for (i in arrays[name]!!.indices) {
                            arrays[name]!![i] = VariableChar(string[i + 1])
                        }
                    }
                } else {
                    val i = countElement.getValue()
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                throw Exception("Incorrect data type: the number of elements must be integer, $name")
            }
        } else {
            throw Exception("The name already exist: $name[]")
        }
    }

    fun getVariable(name: String): Variable {
        VariableType.isName(name)

        val variable = variables[name]
        return if (variable != null) {
            variable
        } else {
            errorText = "The variable not exist: $name"
            Variable()
        }
    }

    fun getArrayElement(name: String, index: Variable): Variable {
        VariableType.isName(name)

        val array = arrays[name]
        return if (array != null) {
            if (index is VariableInt) {
                val i = index.getValue().toInt()
                if (i in (array.indices)) {
                    array[i]
                } else {
                    Variable()
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                Variable()
                throw Exception("Incorrect data type: the array index must be an integer, $name")
            }
        } else {
            Variable()
            throw Exception("The array not exist: $name[]")
        }
    }

    fun setCharArray(name: String, string: String): Boolean {
        if (arrays[name] != null) {
            if (arrays[name]!!.size > 0) {
                if (arrays[name]!![0] !is VariableChar) {
                    errorText = "You cannot assign a string to this array: $name[]"
                    return false
                }
            } else if (string != "") {
                errorText = "The size of the array is small: $name[]"
                return false
            }
        } else {
            errorText = "The array not exist: $name[]"
            return false
        }

        if (arrays[name]!!.size < string.length - 2) {
            errorText =
                "Incorrect data type: the entered string is larger than the size of the array, $string"
            return false
        }

        if (string.length > 1 && VariableType.isString(string)) {
            for (i in arrays[name]!!.indices) {
                arrays[name]!![i] = VariableChar((0).toChar())
            }

            for (i in arrays[name]!!.indices) {
                if (string.length - 3 < i) {
                    break
                }
                arrays[name]!![i] = VariableChar(string[i + 1])
            }
        } else if (string != "") {
            errorText =
                "Incorrect expression: only a string can be assigned to an array of type char, $name[]"
            return false
        }

        return true
    }

    fun setVariable(name: String, variable: Variable): Boolean {
        VariableType.isName(name)

        if (variables[name] != null) {
            variables[name]?.setValue(variable)
        } else {
            errorText = "The variable not exist: $name"
            return false
        }

        return true
    }

    fun setArrayElement(name: String, index: Variable, value: Variable) {
        VariableType.isName(name)

        val array = arrays[name]
        if (array != null) {
            if (index is VariableInt) {
                val i = index.getValue().toInt()
                if (i in (array.indices)) {
                    arrays[name]!![i].setValue(value)
                } else {
                    throw Exception("Incorrect data type: there is no such element, $name[$i]")
                }
            } else {
                throw Exception("Incorrect data type: the array index must be an integer, $name")
            }
        } else {
            throw Exception("The array not exist: $name[]")
        }
    }

    fun createNesting() {
        deleteStack.push(mutableListOf())
    }

    fun deleteNesting() {
        for (name in deleteStack.peek()) {
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

    @SuppressLint("SetTextI18n")
    fun print(string: String) {
        if (consoleContainer.childCount > 100 || countLine >= 100) {
            clear()
        }

        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        if (string.length > 1) {
            currentLine.text =
                currentLine.text.toString() + string.substring(1, string.length - 1) + "\n"
        }
        countLine++
    }

    @SuppressLint("SetTextI18n")
    fun println(variable: Variable) {
        if (consoleContainer.childCount > 100 || countLine >= 100) {
            clear()
        }

        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        when (variable) {
            is VariableInt -> {
                currentLine.text =
                    currentLine.text.toString() + variable.getValue().toString() + "\n"
                countLine++
            }

            is VariableDouble -> {
                currentLine.text =
                    currentLine.text.toString() + variable.getValue().toString() + "\n"
                countLine++
            }

            is VariableBoolean -> {
                currentLine.text =
                    currentLine.text.toString() + variable.getValue().toString() + "\n"
                countLine++
            }

            is VariableChar -> {
                currentLine.text =
                    currentLine.text.toString() + variable.getValue().toString() + "\n"
                countLine++
            }

            else -> {
                throw Exception("Incorrect expression output")
            }
        }
        currentLine.requestLayout()
    }

    @SuppressLint("SetTextI18n")
    fun println(array: Array<Variable>) {
        if (consoleContainer.childCount > 100 || countLine >= 100) {
            clear()
        }

        val currentLine = consoleContainer.getChildAt(consoleContainer.childCount - 1) as TextView

        if (array.isNotEmpty()) {
            if (array[0] is VariableChar) {
                for (ch in array) {
                    if (ch.toChar().getValue() == (0).toChar()) {
                        break
                    }
                    currentLine.text = currentLine.text.toString() + ch.toChar().getValue()
                }
                currentLine.text = currentLine.text.toString() + "\n"
            } else {
                currentLine.text = currentLine.text.toString() + "["
                for (element in array) {
                    when (element) {
                        is VariableInt -> {
                            currentLine.text =
                                currentLine.text.toString() + element.getValue() + ", "
                        }

                        is VariableDouble -> {
                            currentLine.text =
                                currentLine.text.toString() + element.getValue() + ", "
                        }

                        is VariableBoolean -> {
                            currentLine.text =
                                currentLine.text.toString() + element.getValue() + ", "
                        }

                        else -> {
                            println("ERR: incorrect expression output")
                            throw Exception("Incorrect expression output")
                        }
                    }
                }
                currentLine.text = currentLine.text.toString()
                    .substring(0, currentLine.text.toString().length - 2) + "]\n"
            }
        } else {
            currentLine.text = currentLine.text.toString() + "\n"
        }
        countLine++

    }

    private fun addLine() {
        val line = TextView.inflate(context, resourceLayout, null)
        consoleContainer.addView(line)
    }
}
