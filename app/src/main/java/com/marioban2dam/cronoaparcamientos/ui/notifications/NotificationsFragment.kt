package com.marioban2dam.cronoaparcamientos.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.marioban2dam.cronoaparcamientos.Parking
import com.marioban2dam.cronoaparcamientos.databinding.FragmentNotificationsBinding
import com.marioban2dam.cronoaparcamientos.ui.home.SharedViewModel

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var authUser: FirebaseUser
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.notification_background ) as? SupportMapFragment
        mapFragment?.getMapAsync { googleMap ->
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request location permissions if not granted
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1
                )
                return@getMapAsync
            }
            googleMap.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    googleMap.addMarker(
                        MarkerOptions().position(currentLatLng).title("You are here")
                    )
                }
            }

            sharedViewModel.currentLatLng.observe(viewLifecycleOwner) { latLng ->
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                googleMap.animateCamera(cameraUpdate)
                sharedViewModel.currentLatLng.removeObservers(viewLifecycleOwner)
            }

            val auth = FirebaseAuth.getInstance()
            val base = FirebaseDatabase.getInstance().reference
            val users = base.child("users")
            val uid = users.child(auth.uid!!)
            val parkings = uid.child("parkings")

            parkings.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                    val parking = dataSnapshot.getValue(Parking::class.java)
                    if (parking != null) {
                        val position =
                            LatLng(parking.latitude!!.toDouble(), parking.longitude!!.toDouble())
                        googleMap.addMarker(
                            MarkerOptions()
                                .title(parking.description)
                                .snippet(parking.location)
                                .position(position)
                        )
                    }
                }

                override fun onChildChanged(
                    dataSnapshot: DataSnapshot,
                    previousChildName: String?
                ) {
                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
                override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}