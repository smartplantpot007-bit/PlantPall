package com.example.plantpall

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
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

class HomepgActivity : AppCompatActivity() {

    private val apiKey = "f4af1fdf4f1f7d20fd7b45b17e4042bf"

    private lateinit var speedometer1: SpeedometerView
    private lateinit var speedometer2: SpeedometerView
    private lateinit var speedometer3: SpeedometerView
    private lateinit var speedometer4: SpeedometerView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepg)

        setupGauges()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupGauges() {
        speedometer1 = findViewById(R.id.gauge1)
        speedometer2 = findViewById(R.id.gauge2)
        speedometer3 = findViewById(R.id.gauge3)
        speedometer4 = findViewById(R.id.gauge4)

        // Example usage - you can update these based on your app's data
        speedometer1.setSpeed(70f)
        speedometer2.setSpeed(45f)
        speedometer3.setSpeed(90f)
        speedometer4.setSpeed(30f)
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

                    // Optionally map real weather values to gauges
                    speedometer1.setSpeed(temperature.toFloat().coerceIn(0f, 100f))
                    speedometer2.setSpeed(humidity.toFloat().coerceIn(0f, 100f))
                    speedometer3.setSpeed((windSpeed * 10).toFloat().coerceIn(0f, 100f))
                    speedometer4.setSpeed((visibility * 10).toFloat().coerceIn(0f, 100f))

                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing weather data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                Toast.makeText(this, "Error fetching weather data", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}
