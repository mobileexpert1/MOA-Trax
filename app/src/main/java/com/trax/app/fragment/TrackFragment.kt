package com.trax.app.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.trax.app.activities.ProfileActivity
import com.trax.app.adapters.track.MonthAdapter
import com.trax.app.base.BaseFragment
import com.trax.app.databinding.FragmentTrackBinding
import com.trax.app.models.track.TrackMonthModel
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.getValue

class TrackFragment : BaseFragment() {

    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthAdapter: MonthAdapter

    private val viewModel: HomeViewModel by viewModels()

    private var isFirstLoad = true

    private val loader by lazy {
        ProgressView.getLoader(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentTrackBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObserver()
        initClicks()
    }

    override fun onResume() {
        super.onResume()

        if (isFirstLoad) {
            isFirstLoad = false
        }

        callTrackApi()
    }

    private fun callTrackApi(){

        val token =
            PrefManager.getString(AppConstant.AUTH_TOKEN)

        viewModel.getTracks(
            token,
            1,
            10
        )
    }

    private fun initObserver() {

        viewModel.trackSuccess.observe(viewLifecycleOwner) { response ->

            response?.data?.features?.let { list ->

                binding.tvSessionCount.text =
                    "${list.size} recorded sessions"

                if (list.size > 0){
                    val groupedList = list
                        .sortedByDescending {
                            it.properties?.startedAtUtc
                        }
                        .groupBy {

                            getMonthYear(
                                it.properties?.startedAtUtc
                            )
                        }
                        .map {

                            TrackMonthModel(
                                month = it.key,
                                tracks = ArrayList(
                                    it.value.sortedByDescending { track ->
                                        track.properties?.startedAtUtc
                                    }
                                )
                            )
                        }

                    monthAdapter.updateList(groupedList)
                } else{
                    binding.tvNoDataFound.visibility = View.VISIBLE
                    binding.rvTracks.visibility = View.GONE
                }
            }
        }

        viewModel.apiError.observe(viewLifecycleOwner) {

        //    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {

            if (it)
                loader.show()
            else
                loader.dismiss()
        }
    }


    private fun initClicks() {

        binding.btnSettings.setOnClickListener {

            startActivity(
                Intent(requireContext(), ProfileActivity::class.java)
            )
        }
    }

    private fun setupRecyclerView() {


        monthAdapter = MonthAdapter(arrayListOf())

        binding.rvTracks.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvTracks.adapter =
            monthAdapter

    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun getMonthYear(date: String?): String {

        if (date.isNullOrEmpty())
            return ""

        return try {

            val input =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                )

            val output =
                SimpleDateFormat(
                    "MMMM yyyy",
                    Locale.getDefault()
                )

            output.format(input.parse(date)!!)

        } catch (e: Exception) {
            ""
        }
    }



}