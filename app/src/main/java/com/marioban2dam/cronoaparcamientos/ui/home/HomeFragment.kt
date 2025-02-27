package com.marioban2dam.cronoaparcamientos.ui.home

        import android.app.Activity
        import android.content.Intent
        import android.graphics.Bitmap
        import android.location.Location
        import android.os.Bundle
        import android.provider.MediaStore
        import android.text.Editable
        import android.util.Base64
        import android.util.Log
        import android.view.LayoutInflater
        import android.view.View
        import android.view.ViewGroup
        import androidx.fragment.app.Fragment
        import androidx.lifecycle.ViewModelProvider
        import com.google.android.gms.location.FusedLocationProviderClient
        import com.google.firebase.auth.FirebaseAuth
        import com.google.firebase.auth.FirebaseUser
        import com.google.firebase.database.DatabaseReference
        import com.google.firebase.database.FirebaseDatabase
        import com.marioban2dam.cronoaparcamientos.Parking
        import com.marioban2dam.cronoaparcamientos.databinding.FragmentHomeBinding
        import java.io.ByteArrayOutputStream

        class HomeFragment : Fragment() {

            private var _binding: FragmentHomeBinding? = null
            private val binding get() = _binding!!

            lateinit var lastLocation: Location
            lateinit var fusedLocationClient: FusedLocationProviderClient

            lateinit var authUser: FirebaseUser
            private val REQUEST_IMAGE_CAPTURE = 1

            override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

                val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

                _binding = FragmentHomeBinding.inflate(inflater, container, false)
                val root: View = binding.root

                sharedViewModel.currentLatLng.observe(viewLifecycleOwner) { latlng ->
                    binding.txtLatitud.setText(latlng.latitude.toString())
                    binding.txtLongitud.setText(latlng.longitude.toString())
                }

                sharedViewModel.currentAddress.observe(viewLifecycleOwner) { address ->
                    val newString = String.format("Address: %s \n Time: %tr", address, System.currentTimeMillis())
                    binding.txtDireccio.text = Editable.Factory.getInstance().newEditable(newString)
                }

                sharedViewModel.buttonText.observe(viewLifecycleOwner) { s ->
                    binding.buttonGetLocation.text = s
                }

                binding.buttonGetLocation.setOnClickListener { _ ->
                    sharedViewModel.switchTrackingLocation()
                    Log.d("DEBUG", "Clicked Button Get Location (HomeFragment)")
                }

                sharedViewModel.getUser().observe(viewLifecycleOwner) { user ->
                    authUser = user
                }

                binding.buttonNotify.setOnClickListener { button ->
                    val parking = Parking (
                        latitude = binding.txtLatitud.text.toString(),
                        longitude = binding.txtLongitud.text.toString(),
                        location = binding.txtDireccio.text.toString(),
                        description = binding.txtDescripcio.text.toString()
                    )

                    val base: DatabaseReference = FirebaseDatabase.getInstance().reference
                    val users = base.child("users")
                    val uid = users.child(authUser.uid)
                    val incidencies = uid.child("parkings")
                    val reference = incidencies.push()
                    reference.setValue(parking)
                }

                binding.buttonTakePhoto.setOnClickListener {
                    dispatchTakePictureIntent()
                }

                return root
            }

            private fun dispatchTakePictureIntent() {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                    takePictureIntent.resolveActivity(requireActivity().packageManager)?.also {
                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                    }
                }
            }

            override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
                super.onActivityResult(requestCode, resultCode, data)
                if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    binding.imageView.setImageBitmap(imageBitmap)
                    saveImageToDatabase(imageBitmap)
                }
            }

            private fun saveImageToDatabase(bitmap: Bitmap) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageData = baos.toByteArray()
                val imageDataString = Base64.encodeToString(imageData, Base64.DEFAULT) // Convert to Base64 String

                val base = FirebaseDatabase.getInstance().reference
                val users = base.child("users")
                val uid = users.child(FirebaseAuth.getInstance().uid!!)
                val images = uid.child("images")
                val newImageRef = images.push()
                newImageRef.setValue(imageDataString)
            }

            override fun onDestroyView() {
                super.onDestroyView()
                _binding = null
            }
        }