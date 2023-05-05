package com.example.myapplication.modules

enum class InstructionType {
    VAR, SET, PRINT, INPUT, IF, ENDCHOICEIF, ELIF, ELSE, ENDIF, FOR, ENDFOR, WHILE, ENDWHILE, BREAK, CONTINUE, FUNC, ENDFUNC, RETURN, END
}

data class Block (val instructionType: InstructionType, val expression: String, val breakPoint: Boolean, val numbLine: UInt)

data class BlockView (val instructionType: InstructionType, val instruction: String, val layout: Int, val colorStroke: Int, val colorFill: Int)