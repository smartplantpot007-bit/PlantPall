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
import java.text.SimpleDateFormat
import java.util.*

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

        // Load stored notifications
        val notifications = NotificationStorage.getNotifications(this).toMutableList()
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

                // 🧠 Custom popup message based on prediction
                val message = when {
                    prediction.contains("moderate", ignoreCase = true) ->
                        "Plant is okay 🙂"
                    prediction.contains("high", ignoreCase = true) ->
                        "Plant is not okay 😢"
                    prediction.contains("healthy", ignoreCase = true) ->
                        "Plant is more than okay 😄"
                    else ->
                        "Plant condition unknown 🤔"
                }

                // Show popup message
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // Show prediction result in text view
                tvPredictionResult.text = "🪴 Prediction: $prediction"

                // Add timestamped notification entry
                val timestamp = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date())
                val newNotification = NotificationModel(
                    "Prediction Result 🌿",
                    message,
                    timestamp
                )

                // Save and update list
                NotificationStorage.saveNotification(this, newNotification)
                updateRecyclerView()
            },
            { error ->
                tvPredictionResult.text = "❌ Error: ${error.message ?: "Server error"}"
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    private fun updateRecyclerView() {
        val updatedList = NotificationStorage.getNotifications(this)
        rvNotifications.adapter = NotificationAdapter(updatedList)
    }
}
