package com.mut_jaeryo.circletimer

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val timer : CircleTimer = findViewById<CircleTimer>(R.id.main_timer).apply {
            setMaximumTime(3600)
            setInitPosition(2000)
        }

        findViewById<Button>(R.id.button_start).setOnClickListener {
            timer.start()
        }

        findViewById<Button>(R.id.button_stop).setOnClickListener {
            timer.stop()
        }

        //Timer 가 종료 되었을 때 호출 된다.
        timer.setBaseTimerEndedListener {
            Toast.makeText(this@MainActivity, "timer End", Toast.LENGTH_LONG).show()
        }
    }
}