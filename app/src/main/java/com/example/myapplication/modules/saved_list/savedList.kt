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


fun fibonacci() : Result{
    val instructionList = mutableListOf(
        InstructionType.FUNC,
        InstructionType.IF,
        InstructionType.RETURN,
        InstructionType.ENDCHOICEIF,
        InstructionType.IF,
        InstructionType.SET,
        InstructionType.ENDCHOICEIF,
        InstructionType.RETURN,
        InstructionType.ENDFUNC,
        InstructionType.VAR,
        InstructionType.INPUT,
        InstructionType.VAR,
        InstructionType.SET,
        InstructionType.PRINT,
    )
    val expressions = mutableListOf(
        "int f(int n)",
        "n <= 2",
        "1",
        "arr[n-1]==0",
        "arr [n-1]=f(n-1)+f(n-2)",
        "arr[n-1]",
        "int a, int n",
        "n",
        "int arr[n]",
        "a = f(n)",
        "a",
    )
    val margins = mutableListOf(
        0,
        84,
        168,
        84,
        84,
        168,
        84,
        84,
        0,
        0,
        0,
        0,
        0,
        0,
    )
    val widths = mutableListOf(
        253,
        337,
        421,
        337,
        337,
        421,
        337,
        337,
        253,
        253,
        253,
        253,
        253,
        253,
    )
    return Result(instructionList, expressions, margins, widths)
}

fun pow() : Result{
    val instructionList = mutableListOf(
        InstructionType.VAR,
        InstructionType.FUNC,
        InstructionType.IF,
        InstructionType.RETURN,
        InstructionType.ELSE,
        InstructionType.RETURN,
        InstructionType.ENDIF,
        InstructionType.ENDFUNC,
        InstructionType.SET,
        InstructionType.PRINT,

    )
    val expressions = mutableListOf(
        "int i, int c",
        "int f(int x, int y)",
        "x <= 0",
        "1",
        "f(x-1, y) * y",
        "i = f(2+5, 3)",
        "i",
    )
    val margins = mutableListOf(
        0,
        0,
        84,
        168,
        84,
        168,
        84,
        0,
        0,
        0,
    )
    val widths = mutableListOf(
        253,
        253,
        337,
        421,
        337,
        421,
        337,
        253,
        253,
        253,
    )
    return Result(instructionList, expressions, margins, widths)
}


fun fastEuclidAlgorithm() : Result{
    val instructionList = mutableListOf(
        InstructionType.VAR,
        InstructionType.INPUT,
        InstructionType.INPUT,
        InstructionType.SET,
        InstructionType.WHILE,
        InstructionType.SET,
        InstructionType.SET,
        InstructionType.SET,
        InstructionType.ENDWHILE,
        InstructionType.PRINT,
    )
    val expressions = mutableListOf(
        "int a, int b, int temp",
        "a",
        "b",
        "temp = b",
        "a % b != 0",
        "b = a % b",
        "a = temp",
        "temp = b",
        "b"
    )
    val margins = mutableListOf(
        0,
        0,
        0,
        0,
        0,
        84,
        84,
        84,
        0,
        0,
    )
    val widths = mutableListOf(
        253,
        253,
        253,
        253,
        253,
        337,
        337,
        337,
        253,
        253,
    )
    return Result(instructionList, expressions, margins, widths)
}


fun Stirlitz() : Result{
    val instructionList = mutableListOf(
        InstructionType.PRINT,
        InstructionType.PRINT,
        InstructionType.PRINT,
    )
    val expressions = mutableListOf(
        "\"Stirlitz gave the cat petrol to drink and let it go\"",
        "\"The cat walked half a meter and fell down\"",
        "\"Wow that's a fuel consumption, Stirlitz said\"",
    )
    val margins = mutableListOf(
        0,
        0,
        0,
    )
    val widths = mutableListOf(
        253,
        253,
        253,
    )
    return Result(instructionList, expressions, margins, widths)
}