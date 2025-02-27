package com.marioban2dam.cronoaparcamientos.ui.home

import android.annotation.SuppressLint
import android.app.Application
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseUser
import java.util.ArrayList
import java.util.Locale
import java.util.concurrent.Executors

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    val app = application

    private val user = MutableLiveData<FirebaseUser>()
    fun getUser(): LiveData<FirebaseUser> {
        return user
    }

    fun setUser(passedUser: FirebaseUser) {
        user.postValue(passedUser)
    }

    val currentAddress = MutableLiveData<String>()
    val currentLatLng = MutableLiveData<LatLng>()
    val checkPermission = MutableLiveData<String>()
    val buttonText = MutableLiveData<String>()


    var mTrackingLocation = false

    lateinit var mFusedLocationClient: FusedLocationProviderClient
    fun setFusedLocationClient(fusedLocationProviderClient: FusedLocationProviderClient) {
        this.mFusedLocationClient = fusedLocationProviderClient
    }

    val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            locationResult.lastLocation?.let { fetchAddress(it) }
        }
    }

    fun getLocationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000).build()
    }

    fun switchTrackingLocation() {
        if (!mTrackingLocation) {
            startTrackingLocation(needsChecking = true)
        } else {
            stopTrackingLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun startTrackingLocation(needsChecking: Boolean) {
        if (needsChecking) {
            checkPermission.postValue("Check")
        } else {
            mFusedLocationClient.requestLocationUpdates(
                getLocationRequest(),
                mLocationCallback,
                null
            )
            currentAddress.postValue("Loading")
            mTrackingLocation = true

            buttonText.value = "Stop "
        }
    }

    fun stopTrackingLocation() {
        if (mTrackingLocation) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            mTrackingLocation = false

            buttonText.value = "Start"
        }
    }

    fun fetchAddress(location: Location) {

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val geocoder = Geocoder(app.applicationContext, Locale.getDefault())

        executor.execute {
            val addresses: List<Address>?
            var resultMessage = ""
            try {
                addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1
                )
                val latLng = LatLng(location.longitude, location.latitude)
                currentLatLng.postValue(latLng)

                val address = addresses?.get(0)
                val addressParts = ArrayList<String>()
                if (address != null) {
                    for (i in 0..address.maxAddressLineIndex) {
                        addressParts.add(address.getAddressLine(i))
                    }
                }
                resultMessage = TextUtils.join("\n", addressParts)
                handler.post {
                    if (mTrackingLocation) {
                        currentAddress.postValue(resultMessage)
                    }
                }


            } catch (e: Exception) {

                Log.e("ERR", e.message ?: "Error")
            }

        }

    }

}