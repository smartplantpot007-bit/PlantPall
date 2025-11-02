package com.example.plantpall

import android.os.Bundle
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

        // Retrieve sensor values
        soilMoisture = intent.getFloatExtra("Soil_Moisture", 0f)
        ambientTemp = intent.getFloatExtra("Ambient_Temperature", 0f)
        soilTemp = intent.getFloatExtra("Soil_Temperature", 0f)
        humidity = intent.getFloatExtra("Humidity", 0f)

        // Setup history list
        historyList = NotificationStorage.getNotifications(this).toMutableList()
        rvNotifications.layoutManager = LinearLayoutManager(this)
        historyAdapter = NotificationAdapter(historyList)
        rvNotifications.adapter = historyAdapter

        // Setup popup list
        rvPopups.layoutManager = LinearLayoutManager(this)
        popupAdapter = PopupAdapter(popupList)
        rvPopups.adapter = popupAdapter

        // Swipe-to-remove for popups (also remove from storage)
        val swipePopupHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val removed = popupList.getOrNull(pos)
                if (removed != null) {
                    NotificationStorage.deleteNotificationMatching(this@NotificationsActivity, removed.message, removed.time)
                    popupAdapter.removeAt(pos)
                    updateRecyclerView()
                }
            }
        }
        ItemTouchHelper(swipePopupHandler).attachToRecyclerView(rvPopups)

        // Swipe-to-remove for history
        val swipeHistoryHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                NotificationStorage.deleteNotificationAt(this@NotificationsActivity, pos)
                updateRecyclerView()
            }
        }
        ItemTouchHelper(swipeHistoryHandler).attachToRecyclerView(rvNotifications)

        // Predict button
        btnPredict.setOnClickListener { sendPredictionRequest() }

        // Clear all popups + history
        btnClearPopups.setOnClickListener {
            popupAdapter.clearAll()
            NotificationStorage.clearAll(this)
            updateRecyclerView()
        }

        btnClearPopups.visibility = View.VISIBLE
    }

    private fun sendPredictionRequest() {
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

                // Save prediction to history (not shown as duplicate popup)
                val newNotification = NotificationModel("Prediction Result 🌿", message, timestamp)
                NotificationStorage.saveNotification(this, newNotification)

                // Only show ONE popup (clear previous)
                popupList.clear()
                val popup = NotificationModel("Popup", message, timestamp)
                popupList.add(popup)
                popupAdapter.notifyDataSetChanged()

                btnClearPopups.visibility = View.VISIBLE
                updateRecyclerView()
            },
            { error ->
                tvPredictionResult.text = "❌ Error: ${error.message ?: "Server error"}"
            }
        )
        queue.add(request)
    }

    private fun updateRecyclerView() {
        historyList = NotificationStorage.getNotifications(this).toMutableList()
        rvNotifications.adapter = NotificationAdapter(historyList)
    }
}
