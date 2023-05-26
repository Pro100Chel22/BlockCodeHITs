package com.example.myapplication.modules.recycler_view_logic

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import java.lang.StringBuilder

class OperatorAdapter(private val context : Context, private val marginInDp : Int,
                      private val mapOfEditTexts : Map<EditText, Boolean>,
                      private val operatorsList : List<Operators>) : RecyclerView.Adapter<OperatorAdapter.OperatorViewHolder>(){

    inner class OperatorViewHolder(view : View) : RecyclerView.ViewHolder(view), View.OnClickListener{
        val textView : TextView = view.findViewById(R.id.operator)

        override fun onClick(view : View){
            val keys : Set<EditText> = mapOfEditTexts.keys
            for(key in keys){
                if(mapOfEditTexts[key] == true){
                    val cursorPosition : Int = key.selectionStart
                    val editable : Editable = key.text
                    val stringBuilder = StringBuilder(editable)
                    stringBuilder.insert(cursorPosition, textView.text)
                    key.setText(stringBuilder.toString())
                    key.setSelection(cursorPosition + textView.text.length)
                    break
                }
            }
        }
        init{
            itemView.setOnClickListener(this)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : OperatorViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context).inflate(R.layout.operator_item, parent, false)
        return OperatorViewHolder(adapterLayout)
    }


    override fun onBindViewHolder(holder: OperatorViewHolder, position: Int) {
        val item = operatorsList[position]
        holder.textView.text = context.resources.getString(item.operator)

        val marginLeft = if (position == 0) marginInDp * 2 else marginInDp

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(marginLeft, layoutParams.topMargin, marginInDp, layoutParams.bottomMargin)
        if(position < 14){
            if(holder.textView.text.length == 1 || holder.textView.text == "||"){
                layoutParams.width = marginInDp * 4
            }
            else{
                layoutParams.width = (marginInDp * 5)
            }
        }
        else if(position == operatorsList.size - 1){
            layoutParams.width = marginInDp * 3 * holder.textView.text.length / 2
        }
        else{
            layoutParams.width = marginInDp * 4 * holder.textView.text.length / 2
        }
        holder.itemView.layoutParams = layoutParams
    }

    override fun getItemCount() = operatorsList.size
}
