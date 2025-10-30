package com.example.plantpall

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val getStartedButton: Button = findViewById(R.id.getStartedButton)
        val plantImage: CircleImageView = findViewById(R.id.plantImage)

        getStartedButton.setOnClickListener {
            // 🌿 Start zoom animation
            val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
            plantImage.startAnimation(zoomIn)

            // ⏳ Wait for animation to finish, then open next screen
            plantImage.postDelayed({
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

            }, 500) // same as animation duration (500ms)
        }
    }
}
