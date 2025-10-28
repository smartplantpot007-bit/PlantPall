package com.example.plantpall

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        val notifications = listOf(
            NotificationModel("Rosey", "Rosey needs watering! Soil moisture is low 🌿", "Oct 15, 3:10 PM"),
            NotificationModel("Tulip", "Tulip’s soil pH is too high. Adjust nutrients 🌸", "Oct 15, 2:45 PM"),
            NotificationModel("Fern", "Cold stress alert! Fern’s temperature dropped ❄", "Oct 15, 1:30 PM"),
            NotificationModel("Lily", "Lily’s moisture level is optimal 🌼", "Oct 15, 12:00 PM")
        )

        rvNotifications.adapter = NotificationAdapter(notifications)

        val tvCheckPrediction = findViewById<TextView>(R.id.tvCheckPrediction)
        tvCheckPrediction.setOnClickListener {
            // Move to Prediction Activity
            startActivity(Intent(this, PredictActivity::class.java))
            Toast.makeText(this, "🔮 Opening plant prediction screen...", Toast.LENGTH_SHORT).show()
        }
    }
}
