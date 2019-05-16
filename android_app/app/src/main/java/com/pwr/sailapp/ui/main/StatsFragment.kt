package com.pwr.sailapp.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer

import com.pwr.sailapp.R
import com.pwr.sailapp.data.network.sail.ConnectivityInterceptorImpl
import com.pwr.sailapp.data.network.sail.SailAppApiService
import com.pwr.sailapp.data.network.sail.SailNetworkDataSourceImpl
import kotlinx.android.synthetic.main.fragment_stats.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class StatsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val apiService = SailAppApiService(ConnectivityInterceptorImpl(requireContext()))
        val sailNetworkDataSource = SailNetworkDataSourceImpl(apiService)
        sailNetworkDataSource.downloadedAllUserRentals.observe(this, Observer {
            textView_stats_fragment.text = it.toString()
        })

        GlobalScope.launch(Dispatchers.Main) {
            sailNetworkDataSource.fetchAllUserRentals(1)
        }
    }


}
