package com.marioban2dam.cronoaparcamientos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.marioban2dam.cronoaparcamientos.databinding.ActivityMainBinding
import com.marioban2dam.cronoaparcamientos.ui.home.SharedViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private lateinit var sharedViewModel: SharedViewModel

    var signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
        if (result.resultCode == RESULT_OK) {
            var user = FirebaseAuth.getInstance().currentUser as FirebaseUser
            sharedViewModel.setUser(user)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        val mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedViewModel.setFusedLocationClient(mFusedLocationClient)

        sharedViewModel.checkPermission.observe(this) { checkPermission() }

        locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val fineLocationGranted: Boolean = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)
            val coarseLocationGranted: Boolean = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            if (fineLocationGranted) {
                sharedViewModel.startTrackingLocation(false)
            } else if (coarseLocationGranted) {
                sharedViewModel.startTrackingLocation(false)
            } else {
                Toast.makeText(this, "No permissions granted (Main Activity)", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onStart() {
        super.onStart()

        var auth: FirebaseAuth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            val signInIntent: Intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(
                    arrayListOf(
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.GoogleBuilder().build()
                    )
                ).build()
            signInLauncher.launch(signInIntent)
        } else {
            sharedViewModel.setUser(auth.currentUser!!)
        }
    }

    fun checkPermission() {
        Log.d("PERMISSIONS", "CHECK Permissions (Main Activity)")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("PERMISSIONS", "Request Permissions (Main Activity)")
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            sharedViewModel.startTrackingLocation(false)
        }
    }
}