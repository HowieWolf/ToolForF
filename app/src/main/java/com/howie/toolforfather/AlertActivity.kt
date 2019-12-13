package com.howie.toolforfather

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_alert.*

class AlertActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert)

        btnOk.setOnClickListener {
            RingtonePlayer.stop()
            finish()
        }
    }

    override fun onBackPressed() {}
}