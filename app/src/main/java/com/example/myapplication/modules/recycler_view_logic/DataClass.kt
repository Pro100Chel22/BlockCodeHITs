package com.example.myapplication.modules.recycler_view_logic

import com.example.myapplication.R


class DataSource{
    fun loadOperators() : List<Operators>{
        return mutableListOf<Operators>(
            Operators(R.string.addition),
            Operators(R.string.subtraction),
            Operators(R.string.multiplication),
            Operators(R.string.division),
            Operators(R.string.exponent),
            Operators(R.string.modulo),
            Operators(R.string.equality),
            Operators(R.string.inequality),
            Operators(R.string.greater_than),
            Operators(R.string.less_than),
            Operators(R.string.less_than_or_equal_to),
            Operators(R.string.greater_than_or_equal_to),
            Operators(R.string.logical_and),
            Operators(R.string.logical_or),
            Operators(R.string.logical_not),
        )
    }
}
