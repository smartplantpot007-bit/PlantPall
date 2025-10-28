package com.example.plantpall

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class PredictActivity : AppCompatActivity() {

    private val apiUrl = "https://plant-predict-4u3k.onrender.com/predict"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_predict)

        // 🌿 Action Bar styling
        supportActionBar?.apply {
            title = "🌿 Plant Prediction"
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            setBackgroundDrawable(getDrawable(R.drawable.bg_actionbar_gradient))
        }

        val btnPredict = findViewById<Button>(R.id.btnPredict)
        val tvResult = findViewById<TextView>(R.id.tvPredictionResult)

        btnPredict.setOnClickListener {
            // 🧠 Sample input data (you can replace with actual sensor values)
            val inputData = JSONObject()
            inputData.put("Soil_Moisture", 40)
            inputData.put("Ambient_Temperature", 27)
            inputData.put("Soil_Temperature", 25)
            inputData.put("Humidity", 70)

            // 🚀 Send request
            sendPredictionRequest(inputData, tvResult)
        }
    }

    private fun sendPredictionRequest(data: JSONObject, tvResult: TextView) {
        val queue = Volley.newRequestQueue(this)

        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, data,
            { response ->
                val prediction = response.optString("prediction", "No response")
                tvResult.text = "🪴 Prediction: $prediction"
            },
            { error ->
                tvResult.text = "❌ Error: ${error.message}"
                Toast.makeText(this, "Failed to connect to server", Toast.LENGTH_SHORT).show()
            }
        )

        queue.add(request)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
