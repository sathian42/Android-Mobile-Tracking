package com.example.maps

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


fun saveLocationToFirebase(
    reference: DatabaseReference,
    devicename: String,
    latitude: Double,
    longitude: Double,
    Btprc: Int?,
    wifissid: String
) {
    val key = reference.push().key
    key?.let {
        val locationData = MainActivity.LocationData(latitude, longitude)

       FirebaseDatabase.getInstance().getReference("user").child(devicename).child("PathLocation").push().setValue(locationData)
        FirebaseDatabase.getInstance().getReference("user").child(devicename).child("lastLocation").setValue(locationData)
        FirebaseDatabase.getInstance().getReference("user").child(devicename).child("Battery level").setValue(Btprc)
        FirebaseDatabase.getInstance().getReference("user").child(devicename).child("ssid").setValue(wifissid)

    }
}


fun getLocationData(ref: DatabaseReference, callback: (Double, Double) -> Unit) {
    ref.child("lastLocation").addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val latitude = dataSnapshot.child("latitude").getValue(Double::class.java)
            val longitude = dataSnapshot.child("longitude").getValue(Double::class.java)

            if (latitude != null && longitude != null) {
                callback(latitude, longitude)
            } else {
                // Handle null values
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    })
}

fun getBatteryAndSSID(reference: DatabaseReference, callback: (Int, String) -> Unit) {
    reference.addListenerForSingleValueEvent(object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            val batteryLevel = dataSnapshot.child("Battery level").getValue(Int::class.java) ?: 0
            val ssid = dataSnapshot.child("ssid").getValue(String::class.java) ?: "No Connection"

            callback(batteryLevel, ssid)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Handle database error
        }
    })
}





