package com.example.myapplication.modules.recycler_view_logic

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class OperatorAdapter(private val context : Context, private val marginInDp : Int,
                      private val mapOfEditTexts : Map<EditText, Boolean>,
                      private val operatorsList : List<Operators>) : RecyclerView.Adapter<OperatorAdapter.OperatorViewHolder>(){

    inner class OperatorViewHolder(private val view : View) : RecyclerView.ViewHolder(view), View.OnClickListener{
        val textView : TextView = view.findViewById(R.id.operator)

        override fun onClick(view : View){
            val keys : Set<EditText> = mapOfEditTexts.keys
            for(key in keys){
                if(mapOfEditTexts[key] == true){
                    key.append(textView.text)
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

        val marginLeft = if (position == 0) 0 else marginInDp

        val marginRight = if (position == operatorsList.size - 1) 0 else marginInDp

        val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
        layoutParams.setMargins(marginLeft, layoutParams.topMargin, marginRight, layoutParams.bottomMargin)

        holder.itemView.layoutParams = layoutParams
    }

    override fun getItemCount() = operatorsList.size
}
