package com.example.plantpall

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        val secondPlantImage: CircleImageView = findViewById(R.id.secondPlantImage)

        // Hide second image initially
        secondPlantImage.visibility = View.GONE

        getStartedButton.setOnClickListener {
            // Hide first image and show second one
            plantImage.visibility = View.GONE
            secondPlantImage.visibility = View.VISIBLE

            // Load zoom-in animation
            val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
            secondPlantImage.startAnimation(zoomIn)

            // Wait for animation to finish then open SignUpActivity
            secondPlantImage.postDelayed({
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }, 600) // match animation duration
        }
    }
}
