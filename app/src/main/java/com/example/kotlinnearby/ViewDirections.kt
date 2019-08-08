package com.example.kotlinnearby

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlinnearby.Common.Common
import com.example.kotlinnearby.Helper.DirectionJSONParser
import com.example.kotlinnearby.Remote.IGoogleApiService
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import dmax.dialog.SpotsDialog
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ViewDirections : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    lateinit var mService: IGoogleApiService

    lateinit var mCurrentMarker: Marker

    lateinit var polyline: Polyline?=null

    //location
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest
    lateinit var locationCallback: LocationCallback
    lateinit var mLastLocation: Location

    companion object {
        private const val MY_PERMISSION_CODE: Int = 1000
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
                    ), ViewDirections.MY_PERMISSION_CODE
                )
            else
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), ViewDirections.MY_PERMISSION_CODE
                )
            return false
        } else
            return true
    }

    @SuppressLint("RestrictedApi")
    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation = p0!!.lastLocation


                drawPath(mLastLocation, Common.currentResult!!.geometry!!.location)
                val markerOptions = MarkerOptions()
                    .position(LatLng(mLastLocation.longitude, mLastLocation.longitude))
                    .title("Your Position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

                mCurrentMarker = mMap!!.addMarker(markerOptions)

                mMap!!.moveCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            mLastLocation.latitude,
                            mLastLocation.longitude
                        )
                    )
                )
                mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12.0f))

                val destinationLatLng = LatLng(
                    Common.currentResult!!.geometry!!.location!!.latitude.toDouble(),
                    Common.currentResult!!.geometry!!.location!!.longitude.toDouble()
                )

                mMap!!.addMarker(
                    MarkerOptions().position(destinationLatLng)
                        .title(Common.currentResult!!.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

                drawPath(mLastLocation, Common.currentResult!!.geometry!!.location!!)
            }
        }
    }

    private fun drawPath(mLastLocation: Location?, location: Location?) {
        if (polyline != null)
            polyline!!.remove()

        val origin = StringBuilder(mLastLocation!!.latitude.toString())
            .append(",")
            .append(mLastLocation!!.longitude.toString())
            .toString()

        val destination = StringBuilder(location!!.latitude.toString())
            .append(",")
            .append(location!!.longitude.toString()).toString()

        mService.getDiretions(origin, destination)
            .enqueue(object : Callback<String> {
                override fun onFailure(call: Call<String>, t: Throwable) {
                    Log.d("EDMTDEC", t.message)
                }

                override fun onResponse(call: Call<String>, response: Response<String>) {
                    ParseTask().execute(response.body()!!.toString())
                }

            })


    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_directions)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            ViewDirections.MY_PERMISSION_CODE -> {
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->

            mLastLocation = location
            drawPath(mLastLocation, Common.currentResult!!.geometry!!.location)
            val markerOptions = MarkerOptions()
                .position(LatLng(mLastLocation.longitude, mLastLocation.longitude))
                .title("Your Position")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

            mCurrentMarker = mMap!!.addMarker(markerOptions)

            mMap!!.moveCamera(
                CameraUpdateFactory.newLatLng(
                    LatLng(
                        mLastLocation.latitude,
                        mLastLocation.longitude
                    )
                )
            )
            mMap!!.animateCamera(CameraUpdateFactory.zoomTo(12.0f))

            val destinationLatLng = LatLng(
                Common.currentResult!!.geometry!!.location!!.latitude.toDouble(),
                Common.currentResult!!.geometry!!.location!!.longitude.toDouble()
            )

            mMap!!.addMarker(
                MarkerOptions().position(destinationLatLng)
                    .title(Common.currentResult!!.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))

            drawPath(mLastLocation, Common.currentResult!!.geometry!!.location!!)

        }

    }

   inner class ParseTask : AsyncTask<String, Int, List<List<HashMap<String, String>>>>() {

        internal val waitingDilog:AlertDialog = SpotsDialog(this@ViewDirections)

       override fun onPreExecute() {
           super.onPreExecute()
           waitingDilog.show()
           waitingDilog.setMessage("Please waiting...")
       }

        override fun doInBackground(vararg params: String?): List<List<HashMap<String, String>>>? {
         val jsonObject: JSONObject
            var routes: List<List<HashMap<String, String>>>?=null
            try{
                jsonObject = JSONObject(params[0])
                val parser = DirectionJSONParser()
                routes = parser.parse(jsonObject)
            }catch (e:JSONException){
                e.printStackTrace()
            }
            return routes
        }

       override fun onPostExecute(result: List<List<HashMap<String, String>>>?) {
               super.onPostExecute(result)

           var points: ArrayList<LatLng>?=null
           var polylineOptions:PolylineOptions?=null

           for(i in result!!.indices){
               points = ArrayList()
               polylineOptions = PolylineOptions()

               var path = result[i]

               for(j in path.indices){
                   val point = path[j]
                   val lat = point["lng"]!!.toDouble()
                   val lng = point["lng"]!!.toDouble()
                   val position = LatLng(lat, lng)

                   points.add(position)
               }
               polylineOptions.addAll(points)
               polylineOptions.width(12f)
               polylineOptions.color(Color.RED)
               polylineOptions.geodesic(true)
           }
           polyline = mMap!!.addPolyline(polylineOptions)
           waitingDilog.dismiss()
       }
    }
}




