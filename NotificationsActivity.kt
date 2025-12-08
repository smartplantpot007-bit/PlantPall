package com.example.plantpall

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.database.*
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

    // Firebase database reference
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        rvNotifications = findViewById(R.id.rvNotifications)
        rvPopups = findViewById(R.id.rvPopups)
        btnPredict = findViewById(R.id.btnPredict)
        tvPredictionResult = findViewById(R.id.tvPredictionResult)
        btnClearPopups = findViewById(R.id.btnClearPopups)

        // Firebase reference
        database = FirebaseDatabase.getInstance(
            "https://plantpal-f-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("sensorData")

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

            val originalTint = btnPredict.backgroundTintList
            val originalTextColor = btnPredict.currentTextColor

            btnPredict.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#388E3C"))
            btnPredict.setTextColor(Color.WHITE)
            btnPredict.text = "Predicting..."

            // Fetch latest sensor data from Firebase before prediction
            fetchLatestSensorData { soilMoisture, ambientTemp, soilTemp, humidity ->
                sendPredictionRequest(soilMoisture, ambientTemp, soilTemp, humidity) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        btnPredict.backgroundTintList = originalTint
                        btnPredict.setTextColor(originalTextColor)
                        btnPredict.text = "Predict Now"
                        btnPredict.isEnabled = true
                    }, 200)
                }
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

    // Fetch latest sensor data from Firebase
    private fun fetchLatestSensorData(onComplete: (Float, Float, Float, Float) -> Unit) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.getValue(SensorData::class.java)
                    if (data != null) {
                        val soilMoisture1 = data.soilMoisture1 // or average of M1 & M2
                        val soilMoisture2= data.soilMoisture2
                        val soilTemp = data.soilTemperature
                        val humidity = data.humidity
                        onComplete(soilMoisture1, soilMoisture2, soilTemp, humidity)
                    } else {
                        Toast.makeText(this@NotificationsActivity, "Sensor data unavailable", Toast.LENGTH_SHORT).show()
                        onComplete(0f, 0f, 0f, 0f)
                    }
                } else {
                    Toast.makeText(this@NotificationsActivity, "No sensor data found", Toast.LENGTH_SHORT).show()
                    onComplete(0f, 0f, 0f, 0f)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificationsActivity, "Firebase error: ${error.message}", Toast.LENGTH_SHORT).show()
                onComplete(0f, 0f, 0f, 0f)
            }
        })
    }

    private fun sendPredictionRequest(
        soilMoisture: Float,
        ambientTemp: Float,
        soilTemp: Float,
        humidity: Float,
        onComplete: () -> Unit
    ) {
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

                popupList.clear()
                val popup = NotificationModel("Popup", message, timestamp)
                popupList.add(0, popup)
                popupAdapter.notifyDataSetChanged()
                rvPopups.scrollToPosition(0)
                btnClearPopups.visibility = View.VISIBLE

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
