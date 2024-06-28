package com.example.parkly

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.parkly.models.ParkingSpot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class AddParkingSpotActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addparkingspot)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)

        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val priceEditText = findViewById<EditText>(R.id.priceEditText)
        val freeSpotsEditText = findViewById<EditText>(R.id.freeSpotsEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val price = priceEditText.text.toString().toDoubleOrNull()
            val freeSpots = freeSpotsEditText.text.toString().toIntOrNull()

            if (name.isEmpty() || price == null || freeSpots == null) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else {
                addParkingSpot(name, price, freeSpots)
            }
        }
    }

    private fun addParkingSpot(name: String, price: Double, freeSpots: Int) {

        val spot = ParkingSpot(name,price, latitude, longitude, freeSpots)

        db.collection("parking-spots").document(name).set(spot)
            .addOnSuccessListener {
                Toast.makeText(this, "Parking spot added!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("AddParkingSpotActivity", "Failed to add parking spot", e)
                Toast.makeText(this, "Failed to add parking spot!", Toast.LENGTH_SHORT).show()
            }
    }
}
