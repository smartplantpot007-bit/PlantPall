package com.example.plantpall

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import android.animation.AnimatorInflater
import android.widget.LinearLayout
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

    private lateinit var database: DatabaseReference

    private var latestSensorData: SensorData? = null
    private var latestTemp: Float = 0f
    private var latestHumidity: Float = 0f
    private var latestUV: Float = 0f

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_homepg)

        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        val sideMenu = findViewById<LinearLayout>(R.id.sideMenu) 
        val ivCloseMenu = findViewById<ImageView>(R.id.ivCloseMenu)

        // --- HIDE MENU AT START ---
        sideMenu.post {
            sideMenu.translationX = sideMenu.width.toFloat()
        }

        // --- OPEN MENU ---
        ivMenu.setOnClickListener {
            sideMenu.animate().translationX(0f).setDuration(300).start()
        }

        // --- CLOSE MENU ---
        ivCloseMenu.setOnClickListener {
            sideMenu.animate().translationX(sideMenu.width.toFloat()).setDuration(300).start()
        }

        setupGauges()

        database = FirebaseDatabase.getInstance(
            "https://plantpal-f-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("sensorData")

        fetchFirebaseData()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fetchLocationAndWeather()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // ---- NEW ICONS ----
        val ivNotification = findViewById<ImageView>(R.id.ivNotification)
        val ivProfile = findViewById<ImageView>(R.id.ivProfile)

        // 🍔 MENU ICON ROTATION ANIMATION
        val rotateAnim = AnimatorInflater.loadAnimator(this, R.animator.rotate_menu)
        ivMenu.setOnClickListener {
            rotateAnim.setTarget(ivMenu)
            rotateAnim.start()
            sideMenu.animate().translationX(0f).setDuration(300).start()
        }

        // 🔔 Notification Screen
        ivNotification.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        // 👤 Profile Screen
        ivProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupGauges() {
        speedometer1 = findViewById(R.id.gauge1)
        speedometer2 = findViewById(R.id.gauge2)
        speedometer3 = findViewById(R.id.gauge3)
        speedometer3.setMaxRange(50f)
    }

    private fun fetchFirebaseData() {
        Log.d("FirebaseCheck", "Connecting to Firebase...")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val data = snapshot.getValue(SensorData::class.java)
                    if (data != null) {
                        latestSensorData = data
                        Log.d("FirebaseData", "Fetched: $data")

                        speedometer1.setSpeed(data.soilMoisture1)
                        speedometer2.setSpeed(data.soilMoisture2)
                        speedometer3.setSpeed(data.soilTemperature)

                        latestUV = data.uv

                        findViewById<TextView>(R.id.tvMoist1).text = "Soil M1: ${data.soilMoisture1}"
                        findViewById<TextView>(R.id.tvMoist2).text = "Soil M2: ${data.soilMoisture2}"
                        findViewById<TextView>(R.id.tvSoilTemp).text = "Soil Temp: ${data.soilTemperature}°C"
                        findViewById<TextView>(R.id.tvUV).text = "UV Index: ${data.uv}"
                    }
                } else {
                    Log.d("FirebaseCheck", "No data in Firebase")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseCheck", "Firebase error: ${error.message}")
            }
        })
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) fetchLocationAndWeather()
            else Toast.makeText(this, "Location permission required.", Toast.LENGTH_SHORT).show()
        }

    private fun fetchLocationAndWeather() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

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
                    val main = response.optJSONObject("main")
                    val weatherArray = response.optJSONArray("weather")
                    val wind = response.optJSONObject("wind")

                    val temperature = main?.optDouble("temp", 0.0)?.toFloat() ?: 0f
                    val humidity = main?.optInt("humidity", 0)?.toFloat() ?: 0f
                    latestTemp = temperature
                    latestHumidity = humidity

                    val weatherCondition = weatherArray?.optJSONObject(0)
                        ?.optString("description", "N/A") ?: "N/A"
                    val windSpeed = wind?.optDouble("speed", 0.0) ?: 0.0
                    val visibility = response.optInt("visibility", 0) / 1000

                    findViewById<TextView>(R.id.city).text = cityName
                    findViewById<TextView>(R.id.temperature).text = "$temperature°C"
                    findViewById<TextView>(R.id.humidity).text = "$humidity%"
                    findViewById<TextView>(R.id.condition).text = weatherCondition
                    findViewById<TextView>(R.id.wind).text = "$windSpeed m/s"
                    findViewById<TextView>(R.id.visibility).text = "$visibility km"

                } catch (_: Exception) {
                }
            },
            { _ ->
                Toast.makeText(this, "Error fetching weather data", Toast.LENGTH_SHORT).show()
            }
        )
        queue.add(request)
    }
}
