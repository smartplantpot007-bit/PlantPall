package com.example.plantpall

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.*

class HomepgActivity : AppCompatActivity() {

    private val apiKey = "f4af1fdf4f1f7d20fd7b45b17e4042bf"

    private lateinit var speedometer1: SpeedometerView
    private lateinit var speedometer2: SpeedometerView
    private lateinit var speedometer3: SpeedometerView
    private lateinit var speedometer4: SpeedometerView

    private lateinit var database: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepg)

        setupGauges()

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("sensorData")



        // Fetch Firebase sensor data in real time
        fetchFirebaseData()

        // Keep your weather API logic untouched
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val ivProfile = findViewById<ImageView>(R.id.ivnotif)
        ivProfile.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupGauges() {
        speedometer1 = findViewById(R.id.gauge1)
        speedometer2 = findViewById(R.id.gauge2)
        speedometer3 = findViewById(R.id.gauge3)
        speedometer4 = findViewById(R.id.gauge4)
    }

    // 🔹 Real-time Firebase data fetching
    // 🔹 Real-time Firebase data fetching (fixed for your structure)
    private fun fetchFirebaseData() {
        Log.d("FirebaseCheck", "Connecting to Firebase...")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("FirebaseCheck", "Snapshot exists: ${snapshot.exists()}")
                Log.d("FirebaseCheck", "Raw data: ${snapshot.value}")

                if (snapshot.exists()) {
                    val data = snapshot.getValue(SensorData::class.java)
                    if (data != null) {
                        Log.d("FirebaseCheck", "Parsed data: $data")

                        speedometer1.setSpeed(data.soilMoisture1)
                        speedometer2.setSpeed(data.soilMoisture2)
                        speedometer3.setSpeed((data.soilTemperature / 50f) * 100f)
                        speedometer4.setSpeed(data.uv)

                        findViewById<TextView>(R.id.tvMoist1).text = "Soil M1: ${data.soilMoisture1}"
                        findViewById<TextView>(R.id.tvMoist2).text = "Soil M2: ${data.soilMoisture2}"
                        findViewById<TextView>(R.id.tvSoilTemp).text = "Soil Temp: ${data.soilTemperature}°C"
                        findViewById<TextView>(R.id.tvUV).text = "UV Index: ${data.uv}"
                    } else {
                        Log.d("FirebaseCheck", "Data parsed as null")
                    }
                } else {
                    Log.d("FirebaseCheck", "Snapshot is empty")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCheck", "Firebase error: ${error.message}")
            }
        })
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchLocationAndWeather()
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_SHORT).show()
            }
        }

    private fun fetchLocationAndWeather() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                fetchWeather(location.latitude, location.longitude)
            } else {
                Toast.makeText(this, "Unable to get location.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        val url =
            "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

        val queue = Volley.newRequestQueue(this)
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val cityName = response.optString("name", "Unknown City")
                    val mainObject = response.optJSONObject("main")
                    val weatherArray = response.optJSONArray("weather")
                    val windObject = response.optJSONObject("wind")

                    val temperature = mainObject?.optDouble("temp", 0.0) ?: 0.0
                    val humidity = mainObject?.optInt("humidity", 0) ?: 0
                    val weatherCondition = weatherArray?.optJSONObject(0)
                        ?.optString("description", "N/A") ?: "N/A"
                    val windSpeed = windObject?.optDouble("speed", 0.0) ?: 0.0
                    val visibility = response.optInt("visibility", 0) / 1000

                    findViewById<TextView>(R.id.city).text = cityName
                    findViewById<TextView>(R.id.temperature).text = "$temperature°C"
                    findViewById<TextView>(R.id.humidity).text = "$humidity%"
                    findViewById<TextView>(R.id.condition).text = weatherCondition
                    findViewById<TextView>(R.id.wind).text = "$windSpeed m/s"
                    findViewById<TextView>(R.id.visibility).text = "$visibility km"

                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show()
                }
            },
            { _ ->
                Toast.makeText(this, "Error fetching weather data", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}
