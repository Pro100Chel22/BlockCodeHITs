package com.example.myapplication.modules.recycler_view_logic

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class ItemsDecoration(context : Context, private val margin : Int) : RecyclerView.ItemDecoration(){

    private val linePaint : Paint = Paint();

    init{
        val color = ContextCompat.getColor(context, R.color.color_stroke_block)
        linePaint.color = color;
        linePaint.strokeWidth = margin / 20F
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        /*if(parent.getChildAdapterPosition(view) == 2){
            outRect.left += 5;
        }*/
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount;
        for(i in 0 until childCount){
            val child = parent.getChildAt(i);
            c.drawLine(child.right.toFloat() + 30f, child.top.toFloat(), child.right.toFloat() + 30f, child.bottom.toFloat(), linePaint);
        }
    }
}