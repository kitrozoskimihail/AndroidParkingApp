package com.example.parkly.models

data class ParkingSpot(
    val name: String,
    val price: Double,
    val latitude: Double,
    val longitude: Double,
    val freeSpots: Int
)

