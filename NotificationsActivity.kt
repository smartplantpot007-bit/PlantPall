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

    private var soilMoisture = 0f
    private var ambientTemp = 0f
    private var soilTemp = 0f
    private var humidity = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        btnPredict = findViewById(R.id.btnPredict)
        tvPredictionResult = findViewById(R.id.tvPredictionResult)

        // Retrieve values from Intent
        soilMoisture = intent.getFloatExtra("Soil_Moisture", 0f)
        ambientTemp = intent.getFloatExtra("Ambient_Temperature", 0f)
        soilTemp = intent.getFloatExtra("Soil_Temperature", 0f)
        humidity = intent.getFloatExtra("Humidity", 0f)


        // RecyclerView setup
        rvNotifications.layoutManager = LinearLayoutManager(this)
        val notifications = listOf(
            NotificationModel(
                "Live Plant 🌱",
                "Live data received from Firebase!",
                "Now: ${System.currentTimeMillis()}"
            )
        )
        rvNotifications.adapter = NotificationAdapter(notifications)

        // On button click → Send data for prediction
        btnPredict.setOnClickListener {
            val inputData = JSONObject().apply {
                put("Soil_Moisture", soilMoisture)
                put("Ambient_Temperature", ambientTemp)
                put("Soil_Temperature", soilTemp)
                put("Humidity", humidity)
            }
            sendPredictionRequest(inputData)
        }
    }

    private fun sendPredictionRequest(data: JSONObject) {
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, data,
            { response ->
                val prediction = response.optString("prediction", "No response from server")
                tvPredictionResult.text = "🪴 Prediction: $prediction"
            },
            { error ->
                tvPredictionResult.text = "❌ Error: ${error.message ?: "Server error"}"
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }
}
