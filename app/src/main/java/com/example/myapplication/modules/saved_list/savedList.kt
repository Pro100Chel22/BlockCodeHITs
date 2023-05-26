package com.example.myapplication.modules.saved_list

import com.example.myapplication.modules.InstructionType
import kotlin.math.exp

data class Result(val instructionList : List<InstructionType>, val expressions : List<String>, val margins : List<Int>, val widths : List<Int>)


fun bubbleSort() : Result {
    val instructionList = mutableListOf(
        InstructionType.VAR,
        InstructionType.INPUT,
        InstructionType.VAR,
        InstructionType.FOR,
        InstructionType.SET,
        InstructionType.SET,
        InstructionType.ENDFOR,
        InstructionType.PRINT,
        InstructionType.FOR,
        InstructionType.FOR,
        InstructionType.IF,
        InstructionType.VAR,
        InstructionType.SET,
        InstructionType.SET,
        InstructionType.ENDCHOICEIF,
        InstructionType.ENDFOR,
        InstructionType.ENDFOR,
        InstructionType.PRINT,
    )
    val expressions = mutableListOf(
        "int n, int a = 46294, int c = 24527",
        "n",
        "int arr[n]",
        "int i ;i<n;i=i+1",
        "arr[i] = a % 101",
        "a = a + c",
        "arr",
        "int i ;i<n;i=i+1",
        "int j=i+1;j<n;j=j+1",
        "arr[i]>arr[j]",
        "int temp = arr[i]",
        "arr[i] = arr[j]",
        "arr[j] = temp",
        "arr",
    )
    val margins = mutableListOf(
        0,
        0,
        0,
        0,
        84,
        84,
        0,
        0,
        0,
        84,
        168,
        252,
        252,
        252,
        168,
        84,
        0,
        0,
    )
    val widths = mutableListOf(
        253,
        253,
        253,
        253,
        337,
        337,
        253,
        253,
        253,
        337,
        421,
        505,
        505,
        505,
        421,
        337,
        253,
        253,
    )
    return Result(instructionList, expressions, margins, widths)
}


fun oneThousandBlocks() : Result{
    val instructionList = mutableListOf<InstructionType>()
    val expressions = mutableListOf<String>()
    val margins = mutableListOf<Int>()
    val widths = mutableListOf<Int>()
    for(i in 0 until 2023){
        instructionList.add(InstructionType.PRINT)
        expressions.add("${i + 1}")
        margins.add(0)
        widths.add(253)
    }
    return Result(instructionList, expressions, margins, widths)
}

fun harp() : Result{
    val instructionList = mutableListOf<InstructionType>()
    val expressions = mutableListOf<String>()
    val margins = mutableListOf<Int>()
    val widths = mutableListOf<Int>()

    instructionList.add(InstructionType.VAR); expressions.add("int a = 251"); margins.add(0); widths.add(253)

    var marginValue : Int = 0; var placeHolderWidth : Int = 253
    for(i in 0 until 250){
        instructionList.add(InstructionType.IF)
        expressions.add("${i + 1} < a")
        margins.add(marginValue)
        widths.add(placeHolderWidth)

        marginValue += 84; placeHolderWidth += 84
    }

    instructionList.add(InstructionType.PRINT); expressions.add("a"); margins.add(marginValue); widths.add(placeHolderWidth)
    marginValue -= 84; placeHolderWidth -= 84;

    for(i in 0 until 250){
        instructionList.add(InstructionType.ENDCHOICEIF)
        margins.add(marginValue)
        widths.add(placeHolderWidth)

        marginValue -= 84; placeHolderWidth -= 84
    }

    return Result(instructionList, expressions, margins, widths)
}