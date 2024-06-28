package com.example.parkly

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.io.IOException
import java.util.Locale

class AdminActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        db = FirebaseFirestore.getInstance()

        val buttonMainActivity: Button = findViewById(R.id.button_main_activity)
        buttonMainActivity.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val searchView: SearchView = findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchLocation(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val initialPosition = LatLng(41.9981, 21.4254)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 11f))

        loadMap()

        mMap.setOnMapClickListener { latLng ->
            showAddParkingSpotDialog(latLng)
        }

        mMap.setOnMarkerClickListener(this)
    }

    private fun showAddParkingSpotDialog(latLng: LatLng) {
        AlertDialog.Builder(this)
            .setTitle("Add Parking Spot")
            .setMessage("Do you want to add a new parking spot on this location?")
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(this, AddParkingSpotActivity::class.java).apply {
                    putExtra("latitude", latLng.latitude)
                    putExtra("longitude", latLng.longitude)
                }
                startActivity(intent)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(location, 1)
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList?.get(0)
                    val latLng = address?.let { LatLng(it.latitude, address.longitude) }
                    latLng?.let { MarkerOptions().position(it).title(location) }
                        ?.let { mMap.addMarker(it) }
                    latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 11f) }
                        ?.let { mMap.animateCamera(it) }
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun showMarkerOptionsDialog(marker: Marker) {
        val snippet = marker.snippet
        var freeSpots = snippet?.split(",")?.get(1)?.replace("Free Spots: ", "")?.trim()?.toInt() ?: 0

        val message = "Parking Spot: ${marker.title}\nFree Spots: $freeSpots"

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Parking Spot Options")

        val dialogLayout = layoutInflater.inflate(R.layout.activity_options, null)
        builder.setView(dialogLayout)

        val dialogMessage = dialogLayout.findViewById<TextView>(R.id.dialog_message)
        val freeSpotsCount = dialogLayout.findViewById<TextView>(R.id.free_spots_count)
        val incrementButton = dialogLayout.findViewById<Button>(R.id.increment_button)
        val decrementButton = dialogLayout.findViewById<Button>(R.id.decrement_button)

        dialogMessage.text = message
        freeSpotsCount.text = freeSpots.toString()

        incrementButton.setOnClickListener {
            freeSpots++
            freeSpotsCount.text = freeSpots.toString()
            updateFreeSpots(marker, freeSpots)
        }

        decrementButton.setOnClickListener {
            if (freeSpots > 0) {
                freeSpots--
                freeSpotsCount.text = freeSpots.toString()
                updateFreeSpots(marker, freeSpots)
            } else {
                Toast.makeText(this, "Free spots cannot be less than 0", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setPositiveButton("Edit") { _, _ ->
            val intent = Intent(this, EditParkingSpotActivity::class.java).apply {
                putExtra("latitude", marker.position.latitude)
                putExtra("longitude", marker.position.longitude)
                putExtra("name", marker.title)
                putExtra("snippet", marker.snippet)
            }
            startActivity(intent)
        }

        builder.setNegativeButton("Delete") { _, _ ->
            deleteParkingSpot(marker)
        }

        builder.setNeutralButton("Cancel", null)
        builder.show()
    }

    private fun updateFreeSpots(marker: Marker, newFreeSpots: Int) {
        db.collection("parking-spots")
            .whereEqualTo("latitude", marker.position.latitude)
            .whereEqualTo("longitude", marker.position.longitude)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        db.collection("parking-spots").document(document.id)
                            .update("freeSpots", newFreeSpots)
                            .addOnSuccessListener {
                                marker.snippet = "Price: ${document.data["price"]}, Free Spots: $newFreeSpots"
                                Toast.makeText(this, "Free spots updated", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error updating free spots", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error finding parking spot", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun deleteParkingSpot(marker: Marker) {
        db.collection("parking-spots")
            .whereEqualTo("latitude", marker.position.latitude)
            .whereEqualTo("longitude", marker.position.longitude)
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot> { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        db.collection("parking-spots").document(document.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Parking spot deleted", Toast.LENGTH_SHORT).show()
                                marker.remove()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Error deleting parking spot", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Error finding parking spot", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun loadMap(){
        mMap.clear()

        db.collection("parking-spots")
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot> { task ->
                if (task.isSuccessful) {
                    val list = mutableListOf<String>()
                    for (document in task.result!!) {
                        list.add(document.data.toString())
                        val latitude = document.data["latitude"] as Double
                        val longitude = document.data["longitude"] as Double
                        val name = document.data["name"] as String
                        val snippet =
                            "Price: ${document.data["price"]}, Free Spots: ${document.data["freeSpots"]}"
                        val location = LatLng(latitude, longitude)
                        mMap.addMarker(MarkerOptions().position(location).title(name).snippet(snippet))
                    }
                    Log.d("", list.toString())
                } else {
                    Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        selectedMarker = marker
        showMarkerOptionsDialog(marker)
        return true
    }

    override fun onResume() {
        super.onResume()
        try {
            loadMap()
        } catch (_: Exception) {
        }
    }
}
