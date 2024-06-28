package com.example.parkly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.io.IOException
import java.util.*

class MapsActivity : FragmentActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        db = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val buttonMainActivity: Button = findViewById(R.id.button_main_activity)
        buttonMainActivity.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val buttonFreeSpots: Button = findViewById(R.id.button_free_spots)
        buttonFreeSpots.setOnClickListener {
            showFreeParkingSpots()
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 10f))

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                val userLocation = LatLng(location.latitude, location.longitude)
                val skopjeCoordinates = LatLng(41.9980, 21.4252)
                mMap.addMarker(
                    MarkerOptions()
                        .position(skopjeCoordinates)
                        .title("Your Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(skopjeCoordinates, 11f))
            }
        }

        loadParkingSpots()
    }

    private fun loadParkingSpots() {
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
                        val snippet = "Price: ${document.data["price"]}, Free Spots: ${document.data["freeSpots"]}"
                        val location = LatLng(latitude, longitude)
                        mMap.addMarker(MarkerOptions().position(location).title(name).snippet(snippet))
                    }
                    Log.d("", list.toString())
                } else {
                    Toast.makeText(this, "Failed to load parking spots", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun searchLocation(location: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addressList = geocoder.getFromLocationName(location, 1)
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val latLng = LatLng(address.latitude, address.longitude)
                    mMap.addMarker(MarkerOptions().position(latLng).title(location))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                } else {
                    Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFreeParkingSpots() {
        mMap.clear()

        db.collection("parking-spots")
            .whereEqualTo("price", 0)
            .get()
            .addOnCompleteListener(OnCompleteListener<QuerySnapshot> { task ->
                if (task.isSuccessful) {
                    val list = mutableListOf<String>()
                    for (document in task.result!!) {
                        list.add(document.data.toString())
                        val latitude = document.data["latitude"] as Double
                        val longitude = document.data["longitude"] as Double
                        val name = document.data["name"] as String
                        val snippet = "Price: ${document.data["price"]}, Free Spots: ${document.data["freeSpots"]}"
                        val location = LatLng(latitude, longitude)
                        mMap.addMarker(MarkerOptions().position(location).title(name).snippet(snippet))
                    }
                    Log.d("", list.toString())
                } else {
                    Toast.makeText(this, "Failed to load free parking spots", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                onMapReady(mMap)
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
