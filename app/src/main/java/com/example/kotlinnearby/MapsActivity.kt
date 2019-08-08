package com.example.kotlinnearby

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlinnearby.Common.Common
import com.example.kotlinnearby.Model.MyPlaces
import com.example.kotlinnearby.Remote.IGoogleApiService
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private lateinit var mLastLocation: Location
    private var mMarker: Marker? = null

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
    }

    //location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback

    lateinit var mService: IGoogleApiService
    internal lateinit var currencyPlaces: MyPlaces


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //inciar Service
        mService = Common.googleApiService

        //request permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest()
                buildLocationCallBack()

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
            }
        } else {
            buildLocationRequest()
            buildLocationCallBack()

            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        }

        button_navigation_view.setOnNavigationItemReselectedListener { item ->
            when (item.itemId) {
                R.id.action_hospital -> nearByPlace("Hospital")
                R.id.action_market -> nearByPlace("Market")
                R.id.action_restaurant -> nearByPlace("Restaurant")
                R.id.action_school -> nearByPlace("School")
            }
            true
        }
    }

    private fun nearByPlace(typePlace: String) {
        mMap.clear()

        var url = getUrl(latitude, longitude, typePlace)
        mService.getNearbyPalces(url)
            .enqueue(object : Callback<MyPlaces> {
                override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                    Toast.makeText(baseContext, "" + t!!.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<MyPlaces>, response: Response<MyPlaces>) {

                    currencyPlaces = response.body()!!
                    if (response.isSuccessful) {
                        for (i in 0 until response!!.body()!!.results!!.size) {
                            val markerOptions = MarkerOptions()
                            val googlePlaces = response.body()!!.results!![i]
                            val lat = googlePlaces.geometry!!.location!!.latitude
                            val lgn = googlePlaces.geometry!!.location!!.longitude
                            val placeName = googlePlaces.name
                            val latLng = LatLng(lat, lgn)

                            markerOptions.position(latLng)
                            markerOptions.title(placeName)
                            if (typePlace.equals("hospital"))
                                markerOptions.icon(
                                    BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_hospital)
                                )
                            else if (typePlace.equals("market"))
                                markerOptions.icon(
                                    BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_shopping)
                                )
                            else if (typePlace.equals("restaurant"))
                                markerOptions.icon(
                                    BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_restaurant)
                                )
                            else if (typePlace.equals("school"))
                                markerOptions.icon(
                                    BitmapDescriptorFactory
                                        .fromResource(R.drawable.ic_school)
                                )
                            else
                                markerOptions.icon(
                                    BitmapDescriptorFactory
                                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                                )

                            markerOptions.snippet(i.toString())
                            //add marker ao mapa
                            mMap!!.addMarker(markerOptions)
                            //move camera
                            mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(20f))

                        }
                    }
                }
            })
    }

    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {

        val googlePlaceUrl =
            StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?location=$latitude, $longitude")
        googlePlaceUrl.append("&radius=1000")
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&keyword=cruise&key=AIzaSyBJoNPupHKbidgeuIuxqMUQ4Z5myYDSiQ0")

        Log.d("URL_DEBUG", googlePlaceUrl.toString())

        return googlePlaceUrl.toString()

    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSION_CODE
                )
            else
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), MY_PERMISSION_CODE
                )
            return false
        } else
            return true
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation = p0!!.lastLocation
                if (mMarker != null) {
                    mMarker!!.remove()
                }
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude

                val latLng = LatLng(latitude, longitude)
                val markerOptions = MarkerOptions().position(latLng)
                    .title("Your position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                mMarker = mMap!!.addMarker(markerOptions)

                // move camera
                mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11f))

            }

        }
    }

    @SuppressLint("RestrictedApi")
    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSION_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                    )
                        if (checkLocationPermission()) {
                            buildLocationRequest()
                            buildLocationCallBack()

                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                            fusedLocationProviderClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper()
                            )

                            mMap!!.isMyLocationEnabled = true
                        }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //inicializar o Google Service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {

                mMap!!.isMyLocationEnabled = true
            }
        } else
            mMap!!.isMyLocationEnabled = true

        //Habilitando o zoom
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap!!.setOnMarkerClickListener { marker ->
            Common.currentResult = currencyPlaces!!.results!![Integer.parseInt(marker.snippet)]
            startActivity(Intent(this@MapsActivity, ViewPlace::class.java))
            true
        }

    }
}

