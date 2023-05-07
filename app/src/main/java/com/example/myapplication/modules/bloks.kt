package com.example.myapplication.modules

import com.example.myapplication.R

enum class InstructionType {
    VAR, SET, PRINT, INPUT, IF, ENDCHOICEIF, ELIF, ELSE, ENDIF, FOR, ENDFOR, WHILE, ENDWHILE, BREAK, CONTINUE, FUNC, ENDFUNC, RETURN, END
}

data class Block (val instructionType: InstructionType, val expression: String, val breakPoint: Boolean, val numbLine: UInt)

data class BlockView (val instructionType: InstructionType, val instruction: String, val layout: Int, val colorStroke: Int, val colorFill: Int)

fun getListBlocksNotHaveText(): List<InstructionType> {
    return listOf(
        InstructionType.ENDCHOICEIF,
        InstructionType.ELSE,
        InstructionType.ENDIF,
        InstructionType.ENDFOR,
        InstructionType.ENDWHILE,
        InstructionType.BREAK,
        InstructionType.CONTINUE,
        InstructionType.ENDFUNC,
        InstructionType.END
    )
}

fun getListBlocksEnds(): List<InstructionType> {
    return listOf(
        InstructionType.ENDCHOICEIF,
        InstructionType.ENDIF,
        InstructionType.ENDFOR,
        InstructionType.ENDWHILE,
        InstructionType.ENDFUNC,
        InstructionType.END
    )
}

fun getListBlocks() : Map<InstructionType, BlockView> {
    return mapOf(
        InstructionType.VAR to BlockView(InstructionType.VAR,"Var", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_var),
        InstructionType.SET to BlockView(InstructionType.SET,"Set", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_set),
        InstructionType.PRINT to BlockView(InstructionType.PRINT,"Print", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_print),
        InstructionType.INPUT to BlockView(InstructionType.INPUT, "Input", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_input),
        InstructionType.IF to BlockView(InstructionType.IF, "If", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_if),
        InstructionType.ENDCHOICEIF to BlockView(InstructionType.ENDCHOICEIF, "EndChoiceIf", R.layout.block_insctuction_end_choice_if, R.color.color_stroke_block, R.color.color_block_if),
        InstructionType.ELIF to BlockView(InstructionType.ELIF,"Elif", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_if),
        InstructionType.ELSE to BlockView(InstructionType.ELSE, "Else", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_if),
        InstructionType.ENDIF to BlockView(InstructionType.ENDIF, "EndIf", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_if),
        InstructionType.FOR to BlockView(InstructionType.FOR, "For", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_for),
        InstructionType.ENDFOR to BlockView(InstructionType.ENDFOR, "EndFor", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_for),
        InstructionType.WHILE to BlockView(InstructionType.WHILE, "While", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_while),
        InstructionType.ENDWHILE to BlockView(InstructionType.ENDWHILE, "EndWhile", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_while),
        InstructionType.BREAK to BlockView(InstructionType.BREAK, "Break", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_break),
        InstructionType.CONTINUE to BlockView(InstructionType.CONTINUE, "Continue", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_continue),
        InstructionType.FUNC to BlockView(InstructionType.FUNC, "Func", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_func),
        InstructionType.ENDFUNC to BlockView(InstructionType.ENDFUNC, "EndFunc", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_func),
        InstructionType.RETURN to BlockView(InstructionType.RETURN, "Return", R.layout.block_instruction_text, R.color.color_stroke_block, R.color.color_block_return),
        InstructionType.END to BlockView(InstructionType.END, "End", R.layout.block_instruction_not_have_text, R.color.color_stroke_block, R.color.color_block_end),
    )
}
