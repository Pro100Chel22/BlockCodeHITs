package com.example.myapplication.modules

enum class VariableType {
    VOID, INT, DOUBLE, BOOLEAN, CHAR;

    companion object {
        fun getType(name: String, func: Boolean = false): Variable {
            return when(name) {
                "int" -> VariableInt(0)
                "double" -> VariableDouble(0.0)
                "boolean" -> VariableBoolean(false)
                "char" -> VariableChar((0).toChar())
                else -> {
                    println("ERR: you cannot create a variable of this type")
                    Variable()
                    if(!func) {
                        throw Exception("You cannot create a variable of this type: $name")
                    } else {
                        throw Exception("Functions can't return this type: $name")
                    }
                }
            }
        }

        fun toVariable(constant: String): Variable {
            if(constant.length == 3 && constant[0] == '\'' && constant[2] == '\'') {
                return VariableChar(constant[1])
            }

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
        
        fun isSymbol(string: String): Boolean {
            if(string.length == 3 && string[0] == '\'' && string[2] == '\'') {
                return true
            }
            return false
        }

        fun isString(string: String): Boolean {
            if(string.length > 1 && string[0] == '\"' && string[string.length - 1] == '\"') {
                return true
            }
            return false
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

    open fun clone(): Variable {
        throw Exception("Incorrect expression: clone void")
    }

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

    open fun toChar(): VariableChar {
        throw Exception("Incorrect expression: you cannot perform a 'toChar' operation with the void type")
    }

    open fun setValue(operand: Variable) {
        println("ERR: set value void")
        throw Exception("Incorrect expression: you cannot perform a '=' operation with the void type")
    }
}

class VariableInt: Variable {
    private var value: Long = 0

    constructor(_value: String = "0"): super(VariableType.INT) {
        value = _value.toLong()
    }

    constructor(_value: Long = 0): super(VariableType.INT) {
        value = _value
    }

    fun getValue(): Long {
        return value
    }

    override fun clone(): Variable {
        return VariableInt(value)
    }

    override fun plus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt  -> VariableInt(value + operand.getValue())
            is VariableDouble -> VariableDouble(value.toDouble() + operand.getValue())
            is VariableBoolean -> VariableInt(value + operand.toInt().getValue())
            is VariableChar -> VariableInt(value + operand.toInt().getValue())
            else ->  throw Exception("Incorrect expression: operator '+' not exist for this type")
        }
    }

    override fun minus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value - operand.getValue())
            is VariableDouble -> VariableDouble(value.toDouble() - operand.getValue())
            is VariableBoolean -> VariableInt(value - operand.toInt().getValue())
            is VariableChar -> VariableInt(value - operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '-' not exist for this type")
        }
    }

    override fun div(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value / operand.getValue())
            is VariableDouble -> VariableDouble(value.toDouble() / operand.getValue())
            is VariableBoolean -> VariableInt(value / operand.toInt().getValue())
            is VariableChar -> VariableInt(value / operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '/' not exist for this type")
        }
    }

    override fun times(operand: Variable): Variable {
        return when (operand) {
            is VariableInt  -> VariableInt(value * operand.getValue())
            is VariableDouble -> VariableDouble(value.toDouble() * operand.getValue())
            is VariableBoolean -> VariableInt(value * operand.toInt().getValue())
            is VariableChar -> VariableInt(value * operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '*' not exist for this type")
        }
    }

    override fun rem(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value % operand.getValue())
            is VariableDouble -> VariableDouble(value.toDouble() % operand.getValue())
            is VariableBoolean -> VariableInt(value % operand.toInt().getValue())
            is VariableChar -> VariableInt(value % operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '%' not exist for this type")
        }
    }

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value > operand.getValue())
            is VariableDouble -> VariableBoolean(value.toDouble() > operand.getValue())
            is VariableBoolean -> VariableBoolean(value > operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value > operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '>' not exist for this type")
        }
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value < operand.getValue())
            is VariableDouble -> VariableBoolean(value.toDouble() < operand.getValue())
            is VariableBoolean -> VariableBoolean(value < operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value < operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '<' not exist for this type")
        }
    }

    override fun equal(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value == operand.getValue())
            is VariableDouble -> VariableBoolean(value.toDouble() == operand.getValue())
            is VariableBoolean -> VariableBoolean(value == operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value == operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '==' not exist for this type")
        }
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!this.toBoolean().getValue())
    }

    override fun toInt(): VariableInt {
        return VariableInt(value)
    }

    override fun toDouble(): VariableDouble {
        return VariableDouble(value.toDouble())
    }

    override fun toBoolean(): VariableBoolean {
        return if(value == 0L) {
            VariableBoolean(false)
        } else {
            VariableBoolean(true)
        }
    }

    override fun toChar(): VariableChar {
        return VariableChar(value.toChar())
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

    override fun clone(): Variable {
        return VariableDouble(value)
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
        return VariableInt(value.toLong())
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

    override fun toChar(): VariableChar {
        return VariableChar(value.toChar())
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

    override fun clone(): Variable {
        return VariableBoolean(value)
    }

    override fun plus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(this.toInt().getValue() + operand.getValue())
            is VariableDouble -> VariableDouble(this.toDouble().getValue() + operand.getValue())
            is VariableBoolean -> VariableInt(this.toInt().getValue() + operand.toInt().getValue())
            is VariableChar -> VariableInt(this.toInt().getValue() + operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '+' not exist for this type")
        }
    }

    override fun minus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(this.toInt().getValue() - operand.getValue())
            is VariableDouble -> VariableDouble(this.toDouble().getValue() - operand.getValue())
            is VariableBoolean -> VariableInt(this.toInt().getValue() - operand.toInt().getValue())
            is VariableChar -> VariableInt(this.toInt().getValue() - operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '-' not exist for this type")
        }
    }

    override fun div(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(this.toInt().getValue() / operand.getValue())
            is VariableDouble -> VariableDouble(this.toDouble().getValue() / operand.getValue())
            is VariableBoolean -> VariableInt(this.toInt().getValue() / operand.toInt().getValue())
            is VariableChar -> VariableInt(this.toInt().getValue() / operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '/' not exist for this type")
        }
    }

    override fun times(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(this.toInt().getValue() * operand.getValue())
            is VariableDouble -> VariableDouble(this.toDouble().getValue() * operand.getValue())
            is VariableBoolean -> VariableInt(this.toInt().getValue() * operand.toInt().getValue())
            is VariableChar -> VariableInt(this.toInt().getValue() * operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '*' not exist for this type")
        }
    }

    override fun rem(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(this.toInt().getValue() % operand.getValue())
            is VariableDouble -> VariableDouble(this.toDouble().getValue() % operand.getValue())
            is VariableBoolean -> VariableInt(this.toInt().getValue() % operand.toInt().getValue())
            is VariableChar -> VariableInt(this.toInt().getValue() % operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '%' not exist for this type")
        }
    }

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(value || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(value && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(this.toInt().getValue() > operand.getValue())
            is VariableDouble -> VariableBoolean(this.toDouble().getValue() > operand.getValue())
            is VariableBoolean -> VariableBoolean(this.toInt().getValue() > operand.toInt().getValue())
            is VariableChar -> VariableBoolean(this.toInt().getValue() > operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '>' not exist for this type")
        }
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(this.toInt().getValue() < operand.getValue())
            is VariableDouble -> VariableBoolean(this.toDouble().getValue() < operand.getValue())
            is VariableBoolean -> VariableBoolean(this.toInt().getValue() < operand.toInt().getValue())
            is VariableChar -> VariableBoolean(this.toInt().getValue() < operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '<' not exist for this type")
        }
    }

    override fun equal(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(this.toInt().getValue() == operand.getValue())
            is VariableDouble -> VariableBoolean(this.toDouble().getValue() == operand.getValue())
            is VariableBoolean -> VariableBoolean(this.toInt().getValue() == operand.toInt().getValue())
            is VariableChar -> VariableBoolean(this.toInt().getValue() == operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '==' not exist for this type")
        }
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!value)
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

    override fun toChar(): VariableChar {
        return if (value) VariableChar((1).toChar()) else VariableChar((0).toChar())
    }

    override fun setValue(operand: Variable) {
        value = operand.toBoolean().getValue()
    }
}

class VariableChar: Variable {
    private var value: Char = (0).toChar()

    constructor(_value: Variable): super(VariableType.CHAR) {
        value = _value.toChar().getValue()
    }

    constructor(_value: Char = (0).toChar()): super(VariableType.BOOLEAN) {
        value = _value
    }

    fun getValue(): Char {
        return value
    }

    override fun plus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value.code.toLong() + operand.getValue())
            is VariableDouble -> VariableDouble(value.code.toDouble() + operand.getValue())
            is VariableBoolean -> VariableInt(value.code.toLong() + operand.toInt().getValue())
            is VariableChar -> VariableInt(value.code.toLong() + operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '+' not exist for this type")
        }
    }

    override fun minus(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value.code.toLong() - operand.getValue())
            is VariableDouble -> VariableDouble(value.code.toDouble() - operand.getValue())
            is VariableBoolean -> VariableInt(value.code.toLong() - operand.toInt().getValue())
            is VariableChar -> VariableInt(value.code.toLong() - operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '-' not exist for this type")
        }
    }

    override fun div(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value.code.toLong() / operand.getValue())
            is VariableDouble -> VariableDouble(value.code.toDouble() / operand.getValue())
            is VariableBoolean -> VariableInt(value.code.toLong()/ operand.toInt().getValue())
            is VariableChar -> VariableInt(value.code.toLong() / operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '/' not exist for this type")
        }
    }

    override fun times(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value.code.toLong()  * operand.getValue())
            is VariableDouble -> VariableDouble(value.code.toDouble() * operand.getValue())
            is VariableBoolean -> VariableInt(value.code.toLong() * operand.toInt().getValue())
            is VariableChar -> VariableInt(value.code.toLong()  * operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '*' not exist for this type")
        }
    }

    override fun rem(operand: Variable): Variable {
        return when (operand) {
            is VariableInt -> VariableInt(value.code.toLong() % operand.getValue())
            is VariableDouble -> VariableDouble(value.code.toDouble() % operand.getValue())
            is VariableBoolean -> VariableInt(value.code.toLong()  % operand.toInt().getValue())
            is VariableChar -> VariableInt(value.code.toLong()  % operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '%' not exist for this type")
        }
    }

    override fun or(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() || operand.toBoolean().getValue())
    }

    override fun and(operand: Variable): VariableBoolean {
        return VariableBoolean(this.toBoolean().getValue() && operand.toBoolean().getValue())
    }

    override fun compareMore(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value.code.toLong()  > operand.getValue())
            is VariableDouble -> VariableBoolean(value.code.toDouble() > operand.getValue())
            is VariableBoolean -> VariableBoolean(value.code.toLong() > operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value.code.toLong()  > operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '>' not exist for this type")
        }
    }

    override fun compareLess(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value.code.toLong()  < operand.getValue())
            is VariableDouble -> VariableBoolean(value.code.toDouble() < operand.getValue())
            is VariableBoolean -> VariableBoolean(value.code.toLong()  < operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value.code.toLong()  < operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '<' not exist for this type")
        }
    }

    override fun equal(operand: Variable): VariableBoolean {
        return when (operand) {
            is VariableInt -> VariableBoolean(value.code.toLong() == operand.getValue())
            is VariableDouble -> VariableBoolean(value.code.toDouble() == operand.getValue())
            is VariableBoolean -> VariableBoolean(value.code.toLong() == operand.toInt().getValue())
            is VariableChar -> VariableBoolean(value.code.toLong() == operand.toInt().getValue())
            else -> throw Exception("Incorrect expression: operator '==' not exist for this type")
        }
    }

    override fun not(): VariableBoolean {
        return VariableBoolean(!this.toBoolean().getValue())
    }
    override fun toInt(): VariableInt {
        return VariableInt(value.code.toLong())
    }

    override fun toDouble(): VariableDouble {
        return VariableDouble(value.code.toDouble())
    }

    override fun toBoolean(): VariableBoolean {
        return if(value == (0).toChar()) {
            VariableBoolean(false)
        } else {
            VariableBoolean(true)
        }
    }

    override fun toChar(): VariableChar {
        return VariableChar(value)
    }

    override fun setValue(operand: Variable) {
        value = operand.toChar().getValue()
    }
}