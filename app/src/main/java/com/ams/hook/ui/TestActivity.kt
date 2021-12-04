package com.ams.hook.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.ams.hook.R
import com.ams.hook.SPUtil
import com.rlz.annotation.NeedLogin

@NeedLogin
class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        Toast.makeText(this,"TestActivity",Toast.LENGTH_SHORT).show()
        SPUtil.get().let {
            findViewById<TextView>(R.id.tv).text = it
        }
    }
}