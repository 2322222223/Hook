package com.ams.hook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.ams.hook.ui.Test1Activity
import com.ams.hook.ui.TestActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.tv).setOnClickListener {
            startActivity(Intent(this, TestActivity::class.java))
        }
        findViewById<TextView>(R.id.tv1).setOnClickListener {
            startActivity(Intent(this, Test1Activity::class.java))
        }
        findViewById<TextView>(R.id.tv2).setOnClickListener {
            SPUtil.clear()
        }
    }
}