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
                    println("ERR: you cannot create a variable of this type")
                    Variable()
                    throw Exception("You cannot create a variable of this type: $name")
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
                    println("ERR: to variable incorrect expression")
                    throw Exception("Incorrect expression: $constant")
                } else if(constant[i] == '.') {
                    isRealVariable = true
                } else if(constant[i].isLetter()){
                    println("ERR: to variable incorrect expression")
                    throw Exception("Incorrect expression: $constant")
                }
            }

            return if(isRealVariable) {
                VariableDouble(constant)
            } else {
                VariableInt(constant)
            }
        }

        fun isConstant(string: String): Boolean {
            if(string == "false"){
                return true
            } else if(string == "true") {
                return true
            }

            for (i in string.indices) {
                if(!(string[i].isDigit() || string[i] == '.' || string[i] == '-' && i == 0)) {
                    return false
                }
            }


            return true
        }

        fun isName(name: String) {
            for(ch in name) {
                if(!(ch.isLetter() || ch.isDigit() || ch == '_')) {
                    throw Exception("Incorrect expression: the name can contain only letters numbers and underscores,  $name")
                }
            }

            if(!(name[0].isLetter() || name[0] == '_')) {
                throw Exception("Incorrect expression: the name can only start with a letter or an underscore,  $name")
            }

            if(name in listOf("int", " double", " boolean", "toInt", "toDouble", "toBoolean")) {
                throw Exception("Incorrect expression: the name already exists,  $name")
            }
        }
    }
}

open class Variable(_type: VariableType = VariableType.VOID) {
    private var type: VariableType = _type

    fun getType(): VariableType { return type }

    open operator fun plus(operand: Variable): Variable {
        println("ERR: plus void")
        throw Exception("Incorrect expression: you cannot perform a '+' operation with the void type")
    }

    open operator fun minus(operand: Variable): Variable {
        println("ERR: minus void")
        throw Exception("Incorrect expression: you cannot perform a '-' operation with the void type")
    }

    open operator fun div(operand: Variable): Variable {
        println("ERR: div void")
        throw Exception("Incorrect expression: you cannot perform a '/' operation with the void type")
    }

    open operator fun times(operand: Variable): Variable {
        println("ERR: multi void")
        throw Exception("Incorrect expression: you cannot perform a '*' operation with the void type")
    }

    open operator fun rem(operand: Variable): Variable {
        println("ERR: mod void")
        throw Exception("Incorrect expression: you cannot perform a '%' operation with the void type")
    }

    open fun or(operand: Variable): VariableBoolean {
        println("ERR: or void")
        throw Exception("Incorrect expression: you cannot perform a '||' operation with the void type")
    }

    open fun and(operand: Variable): VariableBoolean {
        println("ERR: and void")
        throw Exception("Incorrect expression: you cannot perform a '&&' operation with the void type")
    }

    open fun compareMore(operand: Variable): VariableBoolean {
        println("ERR: compare more void")
        throw Exception("Incorrect expression: you cannot perform a '>' or '>=' operation with the void type")
    }

    open fun compareLess(operand: Variable): VariableBoolean {
        println("ERR: compare less void")
        throw Exception("Incorrect expression: you cannot perform a '<' or '<=' operation with the void type")
    }

    open fun equal(operand: Variable): VariableBoolean {
        println("ERR: equals void")
        throw Exception("Incorrect expression: you cannot perform a '==' operation with the void type")
    }

    open fun not(): VariableBoolean {
        println("ERR: not void")
        throw Exception("Incorrect expression: you cannot perform a '!' operation with the void type")
    }

    open fun toInt(): VariableInt {
        println("ERR: void to int")
        throw Exception("Incorrect expression: you cannot perform a 'toInt' operation with the void type")
    }

    open fun toDouble(): VariableDouble {
        println("ERR: void to double")
        throw Exception("Incorrect expression: you cannot perform a 'toDouble' operation with the void type")
    }

    open fun toBoolean(): VariableBoolean {
        println("ERR: void to boolean")
        throw Exception("Incorrect expression: you cannot perform a 'toBoolean' operation with the void type")
    }

    open fun setValue(operand: Variable) {
        println("ERR: set value void")
        throw Exception("Incorrect expression: you cannot perform a '=' operation with the void type")
    }
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

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toInt().getValue() > operand.toInt().getValue())
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toInt().getValue() < operand.toInt().getValue())
    }

    override fun equal(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toInt().getValue() == operand.toInt().getValue())
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!this.toBoolean().getValue())
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

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toDouble().getValue() > operand.toDouble().getValue())
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toDouble().getValue() < operand.toDouble().getValue())
    }

    override fun equal(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toDouble().getValue() == operand.toDouble().getValue())
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!this.toBoolean().getValue())
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

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toInt().getValue() > operand.toInt().getValue())
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toInt().getValue() < operand.toInt().getValue())
    }

    override fun equal(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() == operand.toBoolean().getValue())
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!this.toBoolean().getValue())
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