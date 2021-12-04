package com.ams.hook.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import android.widget.Toast
import com.ams.hook.R
import com.ams.hook.SPUtil
import com.rlz.annotation.JudgeLogin
import com.rlz.annotation.Login

@Login
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Toast.makeText(this, "LoginActivity", Toast.LENGTH_SHORT).show()
        findViewById<TextView>(R.id.tv).setOnClickListener {
            SPUtil.save("昵称")
            finish()
        }
    }

    companion object {
        @JvmStatic
        @JudgeLogin
        fun isLogin(): Boolean {
            return !TextUtils.isEmpty(SPUtil.get())
        }
    }
}