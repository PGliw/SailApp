package com.pwr.sailapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.pwr.sailapp.R
import com.pwr.sailapp.data.Centre
import com.pwr.sailapp.ui.main.adapters.CentreAdapter
import com.pwr.sailapp.ui.main.dialogs.FilterDialogFragment
import com.pwr.sailapp.ui.main.dialogs.SortDialogFragment
import com.pwr.sailapp.viewModel.MainViewModel
import kotlinx.android.synthetic.main.fragment_rent_master.*
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class RentMasterFragment : Fragment(), CoroutineScope{

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    companion object {
        const val MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1
    }

    private lateinit var mainViewModel: MainViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_rent_master, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView_master.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context!!)
        recyclerView_master.hasFixedSize() // makes recycler view more efficient
        val adapter=
            CentreAdapter(context!!) { centre: Centre -> centreItemClicked(centre) }.apply { setCentres(ArrayList()) }
        recyclerView_master.adapter = adapter
        /*
        Wait until main view model fetches all data:
        1. Show loading bar (set visible) - Main thread
        2. Fetch centres data - IO thread
        3. Wait until centres data is fetched
        4. Hide loading bar
        5. Initialize recycler view
        6. Set all listeners
         */

        mainViewModel = ViewModelProviders.of(requireActivity()).get(MainViewModel::class.java)
        launch {
            linearLayout_loading.visibility = View.VISIBLE
            linearLayout_centres.visibility = View.GONE

            val operation = async(Dispatchers.IO) {
               mainViewModel.fetchCentres()
            }
            operation.await()

            linearLayout_loading.visibility = View.GONE
            linearLayout_centres.visibility = View.VISIBLE

            // Pass this (fragment) as owner - updating is bound to states of fragment's lifecycle
            mainViewModel.centres.observe(viewLifecycleOwner, Observer {
                adapter.setCentres(it)
            })




            search_view.setOnQueryTextListener(object :
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
            //        mainViewModel.search(newText) TODO implement searching
                    return false
                }
            })

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

            if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.d("Access coarse location", "Permission denied")

                ActivityCompat.requestPermissions(requireActivity(),
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    MY_PERMISSIONS_ACCESS_COARSE_LOCATION)
            }

            locateUser()

            button_location.setOnClickListener {locateUser()}

            button_sort.setOnClickListener {
                val sortDialog = SortDialogFragment()
                fragmentManager?.let { it1 -> sortDialog.show(it1, "Sort dialog") }
            }

            button_filter.setOnClickListener {
                val filterDialog = FilterDialogFragment()
                fragmentManager?.let { it1 -> filterDialog.show(it1, "Filter dialog") }
            }

        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            MY_PERMISSIONS_ACCESS_COARSE_LOCATION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                }
            }
            // ... other permission codes if necessary
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    private fun centreItemClicked(centre: Centre){
        mainViewModel.selectCentre(centre)
        findNavController().navigate(R.id.destination_rent_details) // TODO consider using nextAction and graph
    }

    private fun locateUser(){
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if(location != null){
                        mainViewModel.coordinates = Pair(location.latitude, location.longitude)
                        mainViewModel.calculateDistances(location)
                 //       mainViewModel.sort() TODO sorting
                    }
                    else {
                        // TODO inform user that couldn't fetch location
                    }
                }
                .addOnFailureListener{
                    Log.e("fusedLocationClient", "last location failure")
                    /*         if (it is ResolvableApiException){
                                 // Location settings are not satisfied, but this can be fixed
                                 // by showing the user a dialog.
                                 try {
                                     // Show the dialog by calling startResolutionForResult(),
                                     // and check the result in onActivityResult().
                                     it.startResolutionForResult(this@MainActivity,
                                         REQUEST_CHECK_SETTINGS)
                                 } catch (sendEx: IntentSender.SendIntentException) {
                                     // Ignore the error.
                                 }
                             }*/
                }
        } catch (e: SecurityException) {
            Log.e("SecurityException", "Missing location permission")
        }
    }



}
