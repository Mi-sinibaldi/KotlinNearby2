package com.example.kotlinnearby

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.example.kotlinnearby.Common.Common
import com.example.kotlinnearby.Model.PlaceDetail
import com.example.kotlinnearby.Remote.IGoogleApiService
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_view_place.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder

class ViewPlace : AppCompatActivity() {

    internal lateinit var mService: IGoogleApiService
    var mPlace: PlaceDetail? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_place)

        mService = Common.googleApiService

        place_name.text = ""
        place_address.text = ""
        place_open_hour.text = ""

        btn_show_map.setOnClickListener {
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mPlace!!.result!!.url))
            startActivity(mapIntent)
        }

        btn_view_directions.setOnClickListener {
            val viewDirections = Intent(this@ViewPlace, ViewDirections::class.java)
            startActivity(viewDirections)

        }

        if (Common.currentResult!!.photo != null && Common.currentResult!!.photo!!.size > 0)
            Picasso.with(this)
                .load(getPhotoOfPlace(Common.currentResult!!.photo!![0].photo_referece!!, 1000))
                .into(photo)

        //load rating
        if (Common.currentResult!!.rating != null)
            rating_bar.rating = Common.currentResult!!.rating.toFloat()
        else
            rating_bar.visibility = View.GONE

        // load open hours
        if (Common.currentResult!!.opening_hours != null)
            place_open_hour.text = "Open now : " + Common.currentResult!!.opening_hours!!.open_now
        else
            place_open_hour.visibility = View.GONE

        mService.getDetailPlace(getPlaceDetailUrl(Common.currentResult!!.place_id!!))
            .enqueue(object : Callback<PlaceDetail> {
                override fun onFailure(call: Call<PlaceDetail>, t: Throwable) {
                    Toast.makeText(baseContext, " " + t.message, Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<PlaceDetail>, response: Response<PlaceDetail>) {
                    mPlace = response!!.body()
                    place_address.text = mPlace!!.result!!.formatted_address
                    place_name.text = mPlace!!.result!!.name
                }

            })
    }

    private fun getPlaceDetailUrl(place_id: String): String {

        var url = StringBuilder("https://maps.googleapis.com/maps/api/place/details/json")
        url.append("?placeid=$place_id")
        url.append("&key=AIzaSyBJoNPupHKbidgeuIuxqMUQ4Z5myYDSiQ0")
        return url.toString()

    }

    private fun getPhotoOfPlace(photo_referece: String, maxWidth: Int): String {
        var url = StringBuilder("https://maps.googleapis.com/maps/api/place/photo")
        url.append("?maxwidth=$maxWidth")
        url.append("&photoreference=$photo_referece")
        url.append("&key=AIzaSyBJoNPupHKbidgeuIuxqMUQ4Z5myYDSiQ0")
        return url.toString()

    }
}
