package com.example.plantpall

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class SampleActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var tvSoilMoisture: TextView
    private lateinit var tvSoilTemp: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvUV: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        // Bind views
        tvSoilMoisture = findViewById(R.id.tvSoilMoisture)
        tvSoilTemp = findViewById(R.id.tvSoilTemp)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvUV = findViewById(R.id.tvUV)

        // Reference to "sensorData" node
        database = FirebaseDatabase.getInstance().getReference("sensorData")

        // Listen for the latest child and live updates
        database.orderByChild("timestamp").limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@SampleActivity, "No data found", Toast.LENGTH_SHORT).show()
                        return
                    }

                    // Get the latest child (Firebase generates random keys)
                    val latestChild = snapshot.children.last()

                    // Debug: show raw snapshot
                    Toast.makeText(this@SampleActivity, latestChild.value.toString(), Toast.LENGTH_LONG).show()

                    // Extract values safely
                    val soilMoisture = latestChild.child("soilMoisture").value?.toString() ?: "N/A"
                    val soilTemp     = latestChild.child("soilTemperature").value?.toString() ?: "N/A"
                    val humidity     = latestChild.child("humidity").value?.toString() ?: "N/A"
                    val uv           = latestChild.child("uv").value?.toString() ?: "N/A"

                    // Display in TextViews
                    tvSoilMoisture.text = "Soil Moisture: $soilMoisture"
                    tvSoilTemp.text     = "Soil Temperature: $soilTemp"
                    tvHumidity.text     = "Humidity: $humidity"
                    tvUV.text           = "UV: $uv"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SampleActivity, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
