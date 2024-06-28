package com.example.parkly

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditParkingSpotActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var editTextName: EditText
    private lateinit var editTextPrice: EditText
    private lateinit var editTextFreeSpots: EditText
    private lateinit var buttonSave: Button

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editparkingspot)

        db = FirebaseFirestore.getInstance()

        editTextName = findViewById(R.id.editTextName)
        editTextPrice = findViewById(R.id.editTextPrice)
        editTextFreeSpots = findViewById(R.id.editTextFreeSpots)
        buttonSave = findViewById(R.id.buttonSave)

        latitude = intent.getDoubleExtra("latitude", 0.0)
        longitude = intent.getDoubleExtra("longitude", 0.0)
        val name = intent.getStringExtra("name")
        val snippet = intent.getStringExtra("snippet")

        val price = snippet?.split(",")?.get(0)?.replace("Price: ", "")?.trim()
        val freeSpots = snippet?.split(",")?.get(1)?.replace("Free Spots: ", "")?.trim()

        editTextName.setText(name)
        editTextPrice.setText(price)
        editTextFreeSpots.setText(freeSpots)

        buttonSave.setOnClickListener {
            saveParkingSpot()
        }
    }

    private fun saveParkingSpot() {
        val name = editTextName.text.toString().trim()
        val price = editTextPrice.text.toString().trim()
        val freeSpots = editTextFreeSpots.text.toString().trim()

        if (name.isEmpty() || price.isEmpty() || freeSpots.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("parking-spots")
            .whereEqualTo("latitude", latitude)
            .whereEqualTo("longitude", longitude)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        db.collection("parking-spots").document(document.id)
                            .update(
                                mapOf(
                                    "name" to name,
                                    "price" to price,
                                    "freeSpots" to freeSpots
                                )
                            )
                            .addOnSuccessListener {
                                Toast.makeText(this, "Parking spot updated", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error updating parking spot", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error finding parking spot", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
