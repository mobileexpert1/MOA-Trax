package com.trax.app.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.trax.app.activities.ProfileActivity
import com.trax.app.adapters.track.MonthAdapter
import com.trax.app.base.BaseFragment
import com.trax.app.databinding.FragmentTrackBinding
import com.trax.app.models.track.TrackFeature
import com.trax.app.models.track.TrackMonthModel
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.OfflineTrackManager
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class TrackFragment : BaseFragment() {

    //==============================================================================
    // Variables
    //==============================================================================

    private var _binding: FragmentTrackBinding? = null
    private val binding get() = _binding!!

    private lateinit var monthAdapter: MonthAdapter
    private val viewModel: HomeViewModel by viewModels()
    private var isFirstLoad = true

    private val loader by lazy {
        ProgressView.getLoader(requireActivity())
    }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Inflates the fragment layout.
    //--------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrackBinding.inflate(inflater, container, false)
        return binding.root
    }

    //--------------------------------------------------
    // Initializes the fragment views, click listeners, and observers.
    //--------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initObservers()
        initClicks()
    }

    //--------------------------------------------------
    // Performs offline synchronization checks and reloads tracks lists when visible.
    //--------------------------------------------------
    override fun onResume() {
        super.onResume()

        OfflineTrackManager.syncOfflineTracks(requireContext())
        callTrackApi()
        loadAndDisplayTracks(null)
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Sets up the recycler view layout manager and adapter.
    //--------------------------------------------------
    private fun setupRecyclerView() {
        monthAdapter = MonthAdapter(arrayListOf())
        binding.rvTracks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTracks.adapter = monthAdapter
    }

    //--------------------------------------------------
    // Initializes click listeners.
    //--------------------------------------------------
    private fun initClicks() {
        binding.btnSettings.setOnClickListener {
            startActivity(
                Intent(requireContext(), ProfileActivity::class.java)
            )
        }
    }

    //--------------------------------------------------
    // Initializes LiveData observers.
    //--------------------------------------------------
    private fun initObservers() {
        viewModel.trackSuccess.observe(viewLifecycleOwner) { response ->
            if (response != null && response.success) {
                PrefManager.saveGetTracksResponse(response)
            }
            loadAndDisplayTracks(response?.data?.features)
        }

        viewModel.apiError.observe(viewLifecycleOwner) {
            loadAndDisplayTracks(null)
        }

        viewModel.unauthorizedError.observe(viewLifecycleOwner) { isUnauthorized ->
            if (isUnauthorized == true) {
                activity?.let { com.trax.app.utils.SessionManager.showSessionExpiredDialog(it) }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            if (it) {
                if (isFirstLoad) {
                    loader.show()
                }
            } else {
                loader.dismiss()
                isFirstLoad = false
            }
        }
    }

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Calls the active track list fetch API.
    //--------------------------------------------------
    private fun callTrackApi() {
        val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
        viewModel.getTracks(token, 1, 10)
    }

    //==============================================================================
    // Utility & Calculations Functions
    //==============================================================================

    //--------------------------------------------------
    // Combines, processes, and displays online and offline track items list.
    //--------------------------------------------------
    private fun loadAndDisplayTracks(onlineFeatures: List<TrackFeature>?) {
        val effectiveOnlineFeatures = if (!onlineFeatures.isNullOrEmpty()) {
            onlineFeatures
        } else {
            PrefManager.getCachedGetTracksResponse()?.data?.features
        }

        val offlineTracks = OfflineTrackManager.getOfflineTracks(requireContext())
        val offlineFeatures = offlineTracks.map { OfflineTrackManager.convertToTrackFeature(it) }

        val combinedList = ArrayList<TrackFeature>()
        if (!effectiveOnlineFeatures.isNullOrEmpty()) {
            combinedList.addAll(effectiveOnlineFeatures)
        }
        combinedList.addAll(offlineFeatures)

        binding.tvSessionCount.text = "${combinedList.size} recorded sessions"

        if (combinedList.isNotEmpty()) {
            binding.tvNoDataFound.visibility = View.GONE
            binding.rvTracks.visibility = View.VISIBLE

            val sortedFeatures = combinedList.sortedByDescending {
                parseTimestamp(it.properties?.startedAtUtc)
            }

            val monthMap = LinkedHashMap<String, ArrayList<TrackFeature>>()
            sortedFeatures.forEach { track ->
                val monthKey = getMonthYear(track.properties?.startedAtUtc)
                val displayKey = if (monthKey.isBlank()) "Recent Tracks" else monthKey
                monthMap.getOrPut(displayKey) { ArrayList() }.add(track)
            }

            val groupedList = monthMap.map { entry ->
                TrackMonthModel(
                    month = entry.key,
                    tracks = entry.value
                )
            }

            monthAdapter.updateList(groupedList)
        } else {
            binding.tvNoDataFound.visibility = View.VISIBLE
            binding.rvTracks.visibility = View.GONE
        }
    }

    //--------------------------------------------------
    // Formats and parses custom string date times into millisecond timestamps.
    //--------------------------------------------------
    private fun parseTimestamp(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "MMM dd, yyyy HH:mm:ss",
            "MMM dd, yyyy"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                val date = sdf.parse(dateStr)
                if (date != null) return date.time
            } catch (e: Exception) {
                // try next
            }
        }
        return 0L
    }

    //--------------------------------------------------
    // Extracts the month name and year configuration string.
    //--------------------------------------------------
    private fun getMonthYear(date: String?): String {
        if (date.isNullOrEmpty()) return ""

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

            val parsedDate = try {
                input.parse(date)
            } catch (e: Exception) {
                val ts = parseTimestamp(date)
                if (ts > 0) java.util.Date(ts) else null
            }

            if (parsedDate != null) output.format(parsedDate) else ""
        } catch (e: Exception) {
            ""
        }
    }

    //==============================================================================
    // Cleanup Methods
    //==============================================================================

    //--------------------------------------------------
    // Cleans up the view binding references.
    //--------------------------------------------------
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}