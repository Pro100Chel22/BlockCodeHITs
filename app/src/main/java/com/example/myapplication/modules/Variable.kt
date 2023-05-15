package com.example.myapplication.modules

enum class VariableType {
    VOID, INT, DOUBLE, BOOLEAN;

    companion object {
        fun getType(name: String): Variable {
            return when(name) {
                "int" -> VariableInt(0)
                "double" -> VariableDouble(0.0)
                "boolean" -> VariableBoolean(false)
                else -> {
                    println("ERROR: you cannot create a variable of this type")
                    Variable()
                }
            }
        }

        fun toVariable(constant: String): Variable {
            if(constant == "false"){
                return VariableBoolean(false)
            } else if(constant == "true") {
                return VariableBoolean(true)
            }

            var isRealVariable = false
            for(i in constant.indices) {
                if(constant[i] == '.' && isRealVariable) {
                    println("ERROR: to variable incorrect expression")
                    return Variable()
                } else if(constant[i] == '.') {
                    isRealVariable = true
                } else if(constant[i].isLetter()){
                    println("ERROR: to variable incorrect expression")
                    return Variable()
                }
            }

            return if(isRealVariable) {
                VariableDouble(constant)
            } else {
                VariableInt(constant)
            }
        }
    }
}

// Добавить логические операции > < <= >= != ==

open class Variable(_type: VariableType = VariableType.VOID) {
    private var type: VariableType = _type

    fun getType(): VariableType { return type }

    open operator fun plus(operand: Variable): Variable { println("ERROR: plus void"); return operand }

    open operator fun minus(operand: Variable): Variable { println("ERROR: minus void"); return operand }

    open operator fun div(operand: Variable): Variable { println("ERROR: div void"); return operand }

    open operator fun times(operand: Variable): Variable { println("ERROR: multi void"); return operand }

    open operator fun rem(operand: Variable): Variable { println("ERROR: mod void"); return operand }

    open fun toInt(): VariableInt { println("ERROR: void to int"); return VariableInt(0) }

    open fun toDouble(): VariableDouble { println("ERROR: void to double"); return VariableDouble(0.0) }

    open fun toBoolean(): VariableBoolean { println("ERROR: void to boolean"); return VariableBoolean(false) }

    open fun setValue(operand: Variable) { println("ERROR: set value void")}
}

class VariableInt: Variable {
    private var value: Int = 0

    constructor(_value: String = "0"): super(VariableType.INT) {
        value = _value.toInt()
    }

    constructor(_value: Int = 0): super(VariableType.INT) {
        value = _value
    }

    fun getValue(): Int {
        return value
    }

    override fun plus(operand: Variable): Variable {
        return VariableInt(value + operand.toInt().getValue())
    }

    override fun minus(operand: Variable): Variable {
        return VariableInt(value - operand.toInt().getValue())
    }

    override fun div(operand: Variable): Variable {
        return VariableInt(value / operand.toInt().getValue())
    }

    override fun times(operand: Variable): Variable {
        return VariableInt(value * operand.toInt().getValue())
    }

    override fun rem(operand: Variable): Variable {
        return VariableInt(value % operand.toInt().getValue())
    }

    override fun toInt(): VariableInt {
        return VariableInt(value.toInt())
    }

    override fun toDouble(): VariableDouble {
        return VariableDouble(value.toDouble())
    }

    override fun toBoolean(): VariableBoolean {
        return if(value == 0) {
            VariableBoolean(false)
        } else {
            VariableBoolean(true)
        }
    }

    override fun setValue(operand: Variable) {
        value = operand.toInt().getValue()
    }
}

class VariableDouble: Variable {
    private var value: Double = 0.0

    constructor(_value: String = "0.0"): super(VariableType.DOUBLE) {
        value = _value.toDouble()
    }

    constructor(_value: Double = 0.0): super(VariableType.DOUBLE) {
        value = _value
    }

    fun getValue(): Double {
        return value
    }

    override fun plus(operand: Variable): Variable {
        return VariableDouble(value + operand.toDouble().getValue())
    }

    override fun minus(operand: Variable): Variable {
        return VariableDouble(value - operand.toDouble().getValue())
    }

    override fun div(operand: Variable): Variable {
        return VariableDouble(value / operand.toDouble().getValue())
    }

    override fun times(operand: Variable): Variable {
        return VariableDouble(value * operand.toDouble().getValue())
    }

    override fun rem(operand: Variable): Variable {
        return VariableDouble(value % operand.toDouble().getValue())
    }

    override fun toInt(): VariableInt {
        return VariableInt(value.toInt())
    }

    override fun toDouble(): VariableDouble {
        return VariableDouble(value.toDouble())
    }

    override fun toBoolean(): VariableBoolean {
        return if(value == 0.0) {
            VariableBoolean(false)
        } else {
            VariableBoolean(true)
        }
    }

    override fun setValue(operand: Variable) {
        value = operand.toDouble().getValue()
    }
}

class VariableBoolean: Variable {
    private var value: Boolean = false

    constructor(_value: Variable): super(VariableType.BOOLEAN) {
        value = _value.toBoolean().getValue()
    }

    constructor(_value: Boolean = false): super(VariableType.BOOLEAN) {
        value = _value
    }

    fun getValue(): Boolean {
        return value
    }

    override fun plus(operand: Variable): Variable {
        println("operation '+' is not available for Boolean")
        return VariableBoolean(value)
    }

    override fun minus(operand: Variable): Variable {
        println("operation '-' is not available for Boolean")
        return VariableBoolean(value)
    }

    override fun div(operand: Variable): Variable {
        println("operation '/' is not available for Boolean")
        return VariableBoolean(value)
    }

    override fun times(operand: Variable): Variable {
        println("operation '*' is not available for Boolean")
        return VariableBoolean(value)
    }

    override fun rem(operand: Variable): Variable {
        println("operation '%' is not available for Boolean")
        return VariableBoolean(value)
    }

    override fun toInt(): VariableInt {
        return if (value) VariableInt(1) else VariableInt(0)
    }

    override fun toDouble(): VariableDouble {
        return if (value) VariableDouble(1.0) else VariableDouble(0.0)
    }

    override fun toBoolean(): VariableBoolean {
        return VariableBoolean(value)
    }

    override fun setValue(operand: Variable) {
        value = operand.toBoolean().getValue()
    }
}