package com.example.plantpall

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
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
    private lateinit var rvPopups: RecyclerView
    private lateinit var btnPredict: Button
    private lateinit var btnClearPopups: Button
    private lateinit var tvPredictionResult: TextView

    private lateinit var popupAdapter: PopupAdapter
    private lateinit var historyAdapter: NotificationAdapter
    private val popupList = mutableListOf<NotificationModel>()
    private var historyList = mutableListOf<NotificationModel>()

    private val apiUrl = "https://plant-predict-4u3k.onrender.com/predict"

    private var soilMoisture = 0f
    private var ambientTemp = 0f
    private var soilTemp = 0f
    private var humidity = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        rvPopups = findViewById(R.id.rvPopups)
        btnPredict = findViewById(R.id.btnPredict)
        tvPredictionResult = findViewById(R.id.tvPredictionResult)
        btnClearPopups = findViewById(R.id.btnClearPopups)

        // Get sensor data
        soilMoisture = intent.getFloatExtra("Soil_Moisture", 0f)
        ambientTemp = intent.getFloatExtra("Ambient_Temperature", 0f)
        soilTemp = intent.getFloatExtra("Soil_Temperature", 0f)
        humidity = intent.getFloatExtra("Humidity", 0f)

        // History setup
        historyList = NotificationStorage.getNotifications(this).toMutableList()
        rvNotifications.layoutManager = LinearLayoutManager(this)
        historyAdapter = NotificationAdapter(historyList)
        rvNotifications.adapter = historyAdapter

        // Popup setup
        rvPopups.layoutManager = LinearLayoutManager(this)
        popupAdapter = PopupAdapter(popupList)
        rvPopups.adapter = popupAdapter

        // Swipe popups to remove
        val swipePopupHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                popupAdapter.removeAt(viewHolder.adapterPosition)
            }
        }
        ItemTouchHelper(swipePopupHandler).attachToRecyclerView(rvPopups)

        // Swipe history to remove
        val swipeHistoryHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                NotificationStorage.deleteNotificationAt(this@NotificationsActivity, pos)
                updateRecyclerView()
            }
        }
        ItemTouchHelper(swipeHistoryHandler).attachToRecyclerView(rvNotifications)

        // Predict button click
        btnPredict.setOnClickListener {
            btnPredict.isEnabled = false

            // Store original colors
            val originalTint = btnPredict.backgroundTintList
            val originalTextColor = btnPredict.currentTextColor

            // Set pressed style (green)
            btnPredict.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#388E3C"))
            btnPredict.setTextColor(Color.WHITE)
            btnPredict.text = "Predicting..."

            sendPredictionRequest {
                Handler(Looper.getMainLooper()).postDelayed({
                    btnPredict.backgroundTintList = originalTint
                    btnPredict.setTextColor(originalTextColor)
                    btnPredict.text = "Predict Now"
                    btnPredict.isEnabled = true
                }, 600)
            }
        }

        // Clear all popups + history
        btnClearPopups.setOnClickListener {
            popupAdapter.clearAll()
            NotificationStorage.clearAll(this)
            updateRecyclerView()
        }

        btnClearPopups.visibility = View.VISIBLE
    }

    private fun sendPredictionRequest(onComplete: () -> Unit) {
        val data = JSONObject().apply {
            put("Soil_Moisture", soilMoisture)
            put("Ambient_Temperature", ambientTemp)
            put("Soil_Temperature", soilTemp)
            put("Humidity", humidity)
        }

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.POST, apiUrl, data,
            { response ->
                val prediction = response.optString("prediction", "No response from server")
                val message = when {
                    prediction.contains("moderate", ignoreCase = true) -> "Plant is okay 🙂"
                    prediction.contains("high", ignoreCase = true) -> "Plant is not okay 😢"
                    prediction.contains("healthy", ignoreCase = true) -> "Plant is more than okay 😄"
                    else -> "Plant condition unknown 🤔"
                }

                tvPredictionResult.text = "🪴 Prediction: $prediction"

                val timestamp = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault()).format(Date())

                // Show only one popup at a time
                popupList.clear()
                val popup = NotificationModel("Popup", message, timestamp)
                popupList.add(0, popup)
                popupAdapter.notifyDataSetChanged()
                rvPopups.scrollToPosition(0)
                btnClearPopups.visibility = View.VISIBLE

                // Save to history
                val newNotification = NotificationModel("Prediction Result 🌿", message, timestamp)
                NotificationStorage.saveNotification(this, newNotification)
                updateRecyclerView()

                onComplete()
            },
            { error ->
                tvPredictionResult.text = "❌ Error: ${error.message ?: "Server error"}"
                onComplete()
            }
        )
        queue.add(request)
    }

    private fun updateRecyclerView() {
        historyList = NotificationStorage.getNotifications(this).toMutableList()
        rvNotifications.adapter = NotificationAdapter(historyList)
    }
}
