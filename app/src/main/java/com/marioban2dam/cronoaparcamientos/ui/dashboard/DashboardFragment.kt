package com.marioban2dam.cronoaparcamientos.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.marioban2dam.cronoaparcamientos.Parking
import com.marioban2dam.cronoaparcamientos.databinding.FragmentDashboardBinding
import com.marioban2dam.cronoaparcamientos.databinding.ItemLayoutBinding
import com.marioban2dam.cronoaparcamientos.ui.home.SharedViewModel

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var authUser: FirebaseUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        ViewModelProvider(this).get(DashboardViewModel::class.java)
        val sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedViewModel.getUser().observe(viewLifecycleOwner) { user ->
            authUser = user

            if (user != null) {
                val base: DatabaseReference = FirebaseDatabase.getInstance().reference
                val users = base.child("users")
                val uid = users.child(authUser.uid)
                val parkings = uid.child("parkings")

                val options = FirebaseRecyclerOptions.Builder<Parking>()
                    .setQuery(parkings, Parking::class.java)
                    .setLifecycleOwner(viewLifecycleOwner)
                    .build()

                val adapter = ParkingAdapter(options)
                binding.rvParkings.adapter = adapter
                binding.rvParkings.layoutManager = LinearLayoutManager(requireContext())
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class ParkingAdapter(options: FirebaseRecyclerOptions<Parking>) :
        FirebaseRecyclerAdapter<Parking, ParkingAdapter.ParkingViewHolder>(options) {

        override fun onBindViewHolder(
            holder: ParkingViewHolder, position: Int, model: Parking
        ) {
            holder.binding.txtDescripcio.text = model.description
            holder.binding.txtAdreca.text = model.location
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParkingViewHolder {
            val binding = ItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ParkingViewHolder(binding)
        }

        class ParkingViewHolder(val binding: ItemLayoutBinding) :
            RecyclerView.ViewHolder(binding.root)
    }
}