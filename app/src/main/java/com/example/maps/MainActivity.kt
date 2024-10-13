package com.example.maps


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.app.AlertDialog
import android.media.MediaPlayer
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

        private lateinit var fusedLocationClient: FusedLocationProviderClient
        private var mGoogleMap: GoogleMap? = null
        private var lastKnownLocation: Location? = null
        private var isTrackingEnabled: Boolean = false
        private lateinit var locationCallback: LocationCallback
        private lateinit var reference: DatabaseReference
        private var polyline: Polyline? = null

        private lateinit var popupMenu: PopupMenu
        private lateinit var bottomNavigationView: BottomNavigationView

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var drawtoggle: ActionBarDrawerToggle

        val deviceName = fetchDeviceName().replace("[^a-zA-Z0-9]".toRegex(), "")

        var deviceview : String = ""




        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            mediaPlayer = MediaPlayer.create(this, R.raw.ringtone)

            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)


            val popupMenu = PopupMenu(this@MainActivity, findViewById(R.id.devices))




            val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_Layout)
            val navView = findViewById<NavigationView>(R.id.navView)


            drawtoggle = ActionBarDrawerToggle(this, drawerLayout,R.string.open,R.string.close)
            drawerLayout.addDrawerListener(drawtoggle)
            drawtoggle.syncState()
            supportActionBar?.setDisplayHomeAsUpEnabled(true)








            fun toggleDrawer(view: View) {
                val drawerLayout: DrawerLayout = findViewById(R.id.drawer_Layout)
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }


            navView.setNavigationItemSelectedListener {
                when(it.itemId){
                    R.id.item1 -> Toast.makeText(applicationContext,"Permission allowed location",Toast.LENGTH_SHORT).show()

                    R.id.item2 -> Toast.makeText(applicationContext,"FireBase enabled",Toast.LENGTH_SHORT).show()

                    R.id.item3 -> Toast.makeText(applicationContext,"Sychronizing",Toast.LENGTH_SHORT).show()

                }
                true
            }


            bottomNavigationView = findViewById(R.id.bottomNavigationView)



            bottomNavigationView.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.maptype -> {
                       
                        val popupMenu = PopupMenu(this@MainActivity, findViewById(R.id.maptype))
                        popupMenu.menuInflater.inflate(R.menu.map_menu, popupMenu.menu)
                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.normal_map -> {
                                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                                    true
                                }
                                R.id.hybrid_map -> {
                                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
                                    true
                                }
                                R.id.satellite_map -> {
                                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                                    true
                                }
                                R.id.terrain_map -> {
                                    mGoogleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                                    true
                                }
                                else -> false
                            }
                        }
                        popupMenu.show()

                        true
                    }

                    R.id.ring -> {
                        // Handle ring selection
                        mediaPlayer?.start()
                        true
                    }


                    R.id.Batterypower -> {
                        mediaPlayer?.pause()
                        val dref = FirebaseDatabase.getInstance().reference.child("user").child(deviceview)

                        getBatteryAndSSID(dref) { batteryLevel, ssid ->
                            // Use the batteryLevel and ssid values here
                            Log.d("FirebaseData", "Battery Level: $batteryLevel, SSID: $ssid")
                            item.title = "$batteryLevel %"
                        }

                        true
                    }

                    R.id.NetWork -> {

                        // Handle network selection
                        val dref = FirebaseDatabase.getInstance().reference.child("user").child(deviceview)

                        getBatteryAndSSID(dref) { batteryLevel, ssid ->
                            // Use the batteryLevel and ssid values here
                            Log.d("FirebaseData", "Battery Level: $batteryLevel, SSID: $ssid")
                            item.title = "$ssid"
                        }

                        true
                        true
                    }
                    R.id.devices -> {

                        showPopupMenu { selectedItemName ->
                            // Use the selectedItemName here
                            Log.d("Selected Item", "Name: $selectedItemName")
                            deviceview = selectedItemName
                            Toast.makeText(
                                this,
                                "Device : $selectedItemName",
                                Toast.LENGTH_SHORT
                            ).show()

                            val ref = FirebaseDatabase.getInstance().getReference("user").child(selectedItemName)

                            getLocationData(ref) { latitude, longitude ->
                                mGoogleMap?.clear()
                                // Inside this block, you have access to the latitude and longitude values
                                Log.d("LocationData", "Latitude: $latitude, Longitude: $longitude")

                                // Call your function to mark the location here
                                markLocation(mGoogleMap!!, latitude, longitude)
                            }
                        }
                        true
                    }

                    else -> {
                        mediaPlayer?.stop() // Stop the sound when a different item is selected
                        mediaPlayer?.release() // Release the MediaPlayer resources
                        mediaPlayer = null
                        false
                    }
                }
            }




            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            reference = FirebaseDatabase.getInstance().getReference("user")



            // Check if permissions are granted or need to be requested
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request permissions if not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    REQUEST_LOCATION_PERMISSION
                )
            } else {
                // Check if the user has previously denied the permissions
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                    shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                ) {
                    // Explain to the user why the permissions are needed
                    AlertDialog.Builder(this)
                        .setTitle("Location Permissions")
                        .setMessage("This app requires location permissions to function properly.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            // Request the permissions again
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ),
                                REQUEST_LOCATION_PERMISSION
                            )
                        }
                        .create()
                        .show()
                } else {
                    // Permissions are granted or denied with "Don't ask again"
                    // You can handle this as needed
                    getCurrentLocation(this)
                }
            }


            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.FOREGROUND_SERVICE),
                    REQUEST_FOREGROUND_SERVICE_PERMISSION_CODE
                )
            }


            val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
            mapFragment.getMapAsync(this)











        }


        override fun onOptionsItemSelected(item: MenuItem): Boolean {

            if(drawtoggle.onOptionsItemSelected(item)){
                return true
            }

            return super.onOptionsItemSelected(item)

        }


        private fun createLocationCallback() {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult ?: return
                    val batteryPercentage = getBatteryPercentage(applicationContext)
                    val wifiSSID = getWifiSSID(applicationContext)

                    for (location in locationResult.locations) {
                        if (isTrackingEnabled) {
                            saveLocationToFirebase(reference,deviceName,location.latitude, location.longitude,batteryPercentage,wifiSSID)
                        }
                    }
                }
            }
        }

        private fun showPopupMenu(itemSelectedCallback: (String) -> Unit) {
            val databaseReference = FirebaseDatabase.getInstance().reference.child("user")

            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val popupMenu = PopupMenu(this@MainActivity, findViewById(R.id.devices))
                    for (nodeSnapshot in dataSnapshot.children) {
                        val nodeName = nodeSnapshot.key ?: ""
                        popupMenu.menu.add(nodeName)
                    }
                    popupMenu.setOnMenuItemClickListener { item ->
                        val selectedItemName = item.title.toString()
                        itemSelectedCallback(selectedItemName)
                        true
                    }
                    popupMenu.show()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                }
            })
        }



        override fun onResume() {
            super.onResume()
            startLocationUpdates()
        }

        override fun onPause() {
            super.onPause()
            stopLocationUpdates()
        }

        private fun startLocationUpdates() {
            isTrackingEnabled = true
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            createLocationCallback()

            val locationRequest = LocationRequest.create().apply {
                interval = 4000
                fastestInterval = 2000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }

        private fun stopLocationUpdates() {
            isTrackingEnabled = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        override fun onMapReady(googleMap: GoogleMap) {
            mGoogleMap = googleMap

            if (lastKnownLocation != null) {
                val latLng = LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
                mGoogleMap?.addMarker(MarkerOptions().position(latLng).title("You").icon(
                    BitmapDescriptorFactory.fromResource(R.drawable.pin)))

            }

            drawPolylineFromFirebase()




        }


    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

        private fun drawPolylineFromFirebase() {
            val database = FirebaseDatabase.getInstance()
            val reference = database.getReference("locations").child("PathLocation")


            reference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val coordinatesList = mutableListOf<LatLng>()

                    for (snapshot in dataSnapshot.children) {
                        val latitude = snapshot.child("latitude").getValue(Double::class.java)
                        val longitude = snapshot.child("longitude").getValue(Double::class.java)

                        if (latitude != null && longitude != null) {
                            coordinatesList.add(LatLng(latitude, longitude))
                        }
                    }

                    if (coordinatesList.size >= 2) {
                        val polylineOptions = PolylineOptions().clickable(true)
                        for (coordinate in coordinatesList) {
                            polylineOptions.add(coordinate)
                        }

                        polyline?.remove()
                        polyline = mGoogleMap?.addPolyline(polylineOptions
                            .endCap(RoundCap())
                            .startCap(RoundCap())
                            .color(ContextCompat.getColor(this@MainActivity, R.color.black))
                            .width(12f)
                            .jointType(JointType.ROUND)
                        )
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w(TAG, "loadPost:onCancelled", databaseError.toException())
                }
            })
        }

        private fun getCurrentLocation(mainActivity: MainActivity) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        lastKnownLocation = location
                        val latitude = location.latitude
                        val longitude = location.longitude
                        Toast.makeText(
                            this,
                            "Latitude: $latitude, Longitude: $longitude",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to get location: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        fun getBatteryPercentage(context: Context): Int {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }

        fun getWifiSSID(context: Context): String {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo: WifiInfo = wifiManager.connectionInfo
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo.ssid
            } else {
                wifiInfo.ssid.replace("\"", "")
            }
        }


        fun fetchDeviceName(): String {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model.capitalize()
            } else {
                manufacturer.capitalize() + " " + model
            }
        }

        fun markLocation(map: GoogleMap, latitude: Double, longitude: Double) {
            val location = LatLng(latitude, longitude)
            map.addMarker(MarkerOptions().position(location).title(deviceview).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin3)))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }


        private val permissions = arrayOf(
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        private val REQUEST_PERMISSION_CODE = 123

        private fun requestPermissions() {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.FOREGROUND_SERVICE
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permissions
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    REQUEST_PERMISSION_CODE
                )
            }
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == REQUEST_PERMISSION_CODE) {
                // Check if all permissions are granted
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All permissions granted, start the foreground service
                    // Start your foreground service here
                } else {
                    // Permission denied, handle accordingly (e.g., show a message)
                    Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }
        }



        private fun changemap(itemId: Int){
            when(itemId){
                R.id.normal_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL

                R.id.satellite_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE

                R.id.hybrid_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID

                R.id.terrain_map -> mGoogleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
        }



        data class LocationData(val latitude: Double, val longitude: Double)

        companion object {
            const val REQUEST_LOCATION_PERMISSION = 1001
            private val REQUEST_FOREGROUND_SERVICE_PERMISSION_CODE = 1001

            const val TAG = "MainActivity"
        }
}