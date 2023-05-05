package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.bottomsheet.BottomSheetDialog

@Suppress("DEPRECATION")
class CodingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coding)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        findViewById<Button>(R.id.buttonCodingSwapMode).setOnClickListener {
            val sharedPrefs = applicationContext.getSharedPreferences("myPrefs", Context.MODE_PRIVATE)
            val editor = sharedPrefs.edit()

            if (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_NO) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putInt("UI_MODE_NIGHT", AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putInt("UI_MODE_NIGHT", AppCompatDelegate.MODE_NIGHT_NO)
            }

            editor.apply()
        }

        findViewById<Button>(R.id.buttonAddNewBlock).setOnClickListener {
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,500.0f, resources.displayMetrics).toInt()

            val bottomSheetView = View.inflate(this, R.layout.layout_new_blocks, null)
            bottomSheetView.findViewById<LinearLayout>(R.id.modalBottomSheetContainer).layoutParams = layoutParams

            val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogThem)
            bottomSheetDialog.setContentView(bottomSheetView)

            bottomSheetDialog.show()
        }

        findViewById<Button>(R.id.buttonCompiler).setOnClickListener{
            val layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            layoutParams.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,500.0f, resources.displayMetrics).toInt()

            val bottomSheetView = View.inflate(this, R.layout.layout_compiler, null)
            bottomSheetView.findViewById<LinearLayout>(R.id.modalBottomSheetContainer).layoutParams = layoutParams

            val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogThem)
            bottomSheetDialog.setContentView(bottomSheetView)

            bottomSheetDialog.show()
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(androidx.appcompat.R.anim.abc_slide_in_top, androidx.appcompat.R.anim.abc_slide_out_bottom)
    }
}