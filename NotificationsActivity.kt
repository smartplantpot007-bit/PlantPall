package com.example.plantpall

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class NotificationsActivity : AppCompatActivity() {

    private lateinit var rvNotifications: RecyclerView
    private lateinit var btnPredict: Button
    private lateinit var tvPredictionResult: TextView

    private val apiUrl = "https://plant-predict-4u3k.onrender.com/predict"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)

        val notifications = listOf(
            NotificationModel("Rosey", "Rosey needs watering! Soil moisture is low 🌿", "Oct 15, 3:10 PM")
        )

        rvNotifications.adapter = NotificationAdapter(notifications)

        btnPredict = findViewById(R.id.btnPredict)
        tvPredictionResult = findViewById(R.id.tvPredictionResult)

        btnPredict.setOnClickListener {
            val inputData = JSONObject().apply {
                put("Soil_Moisture", 40)
                put("Ambient_Temperature", 27)
                put("Soil_Temperature", 25)
                put("Humidity", 70)
            }
            sendPredictionRequest(inputData)
        }
    }

    private fun sendPredictionRequest(data: JSONObject) {
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, data,
            { response ->
                val prediction = response.optString("prediction", "No response")
                tvPredictionResult.text = "🪴 Prediction: $prediction"
            },
            { error ->
                tvPredictionResult.text = "❌ Error: ${error.message}"
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }
}
