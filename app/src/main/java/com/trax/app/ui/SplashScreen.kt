package com.trax.app.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.trax.app.R
import com.trax.app.activities.LoginActivity
import com.trax.app.activities.MainActivity
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        Handler().postDelayed(
            Runnable {
                if(PrefManager.getBoolean(AppConstant.IS_LOGIN)){
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else{
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }, 4000
        )
    }
}