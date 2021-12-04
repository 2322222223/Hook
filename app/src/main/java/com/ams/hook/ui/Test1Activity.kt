package com.ams.hook.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.ams.hook.R
import com.ams.hook.SPUtil
import com.rlz.annotation.NeedLogin

class Test1Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        Toast.makeText(this,"TestActivity1", Toast.LENGTH_SHORT).show()
        findViewById<TextView>(R.id.tv).text = SPUtil.get()
    }
}