package com.trax.app.activities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.trax.app.R
import com.trax.app.base.BaseActivity
import com.trax.app.databinding.ActivityMapSavedTrackBinding
import com.trax.app.databinding.BottomsheetRemoveTrackBinding
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.SingleTrackViewModel

class MapSavedTrackActivity : BaseActivity() {

    private lateinit var binding: ActivityMapSavedTrackBinding

    private val viewModel: SingleTrackViewModel by viewModels()

    private var trackId = 0

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    private lateinit var trackingDotManager: PointAnnotationManager

    private lateinit var pointAnnotationManager: PointAnnotationManager

    private var startMarkerAdded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapSavedTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()

        trackingDotManager =
            binding.mapView.annotations.createPointAnnotationManager()

        pointAnnotationManager =
            binding.mapView.annotations.createPointAnnotationManager()

        trackId = intent.getIntExtra("trackId", 0)

        initObserver()

        callSingleTrackApi(trackId)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.rlDelete.setOnClickListener {
            showRemoveTrackBottomSheet()
        }
    }

    private fun initObserver() {

        viewModel.singleTrackSuccess.observe(this) {

            it?.data?.let { track ->

                binding.tvTitle.text =
                    track.properties?.trackName

                binding.tvLocation.text =
                    "${track.properties?.countyName}, ${track.properties?.stateAbbrev}"

                binding.tvTrackLocation.text =
                    "${track.properties?.countyName}, ${track.properties?.stateAbbrev}"

                binding.tvDate.text =
                    track.properties?.startedAtUtc

                binding.tvDistanceValue.text =
                    "${track.properties?.totalDistanceMeters} km"

                binding.tvDurationValue.text =
                    "${track.properties?.durationSeconds} sec"

                track.geometry?.coordinates?.let {

                    drawTrack(it)
                }
            }
        }

        viewModel.deleteTrackSuccess.observe(this) {

            it?.let {

                if (it.success) {

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.apiError.observe(this) {

            Log.e("ERROR", it.toString())
        }

        viewModel.isLoading.observe(this) {

            if (it == true)
                loader.show()
            else
                loader.dismiss()
        }
    }

    private fun drawTrack(
        coordinates: List<List<Double>>
    ) {

        if (coordinates.isEmpty()) return

        val points = coordinates.map {

            Point.fromLngLat(
                it[0],
                it[1]
            )
        }

        addTrackingDots(points)

        if (points.isNotEmpty()) {

            addStartMarker(points.first())
        }

        zoomToTrack(points)
    }

    private fun addTrackingDots(
        points: List<Point>
    ) {

        trackingDotManager.deleteAll()

        points.forEach {

            trackingDotManager.create(

                PointAnnotationOptions()
                    .withPoint(it)
                    .withIconImage(trackingDotBitmap)
                    .withIconSize(1.2)
            )
        }
    }

    private fun addStartMarker(
        point: Point
    ) {

        if (startMarkerAdded)
            return

        val bitmap =
            bitmapFromDrawableRes(
                R.drawable.ic_start_marker
            ) ?: return

        pointAnnotationManager.create(

            PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.0)
        )

        startMarkerAdded = true
    }

    private fun zoomToTrack(
        points: List<Point>
    ) {

        val camera =
            binding.mapView.mapboxMap.cameraForCoordinates(
                points,
                EdgeInsets(
                    120.0,
                    120.0,
                    120.0,
                    120.0
                ),
                null,
                null
            )

        binding.mapView.mapboxMap.setCamera(camera)
    }

    private fun bitmapFromDrawableRes(
        drawableRes: Int
    ): Bitmap? {

        val drawable =
            ContextCompat.getDrawable(
                this,
                drawableRes
            ) ?: return null

        val bitmap =
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(bitmap)

        drawable.setBounds(
            0,
            0,
            canvas.width,
            canvas.height
        )

        drawable.draw(canvas)

        return bitmap
    }

    private fun createTrackingDot(): Bitmap {

        val size = 32

        val bitmap =
            Bitmap.createBitmap(
                size,
                size,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(bitmap)

        val paint = Paint().apply {

            color = Color.parseColor("#007F56")

            isAntiAlias = true

            style = Paint.Style.FILL
        }

        canvas.drawCircle(
            size / 2f,
            size / 2f,
            10f,
            paint
        )

        return bitmap
    }

    private val trackingDotBitmap by lazy {

        createTrackingDot()
    }

    private fun callSingleTrackApi(
        trackId: Int
    ) {

        val token =
            PrefManager.getString(AppConstant.AUTH_TOKEN)

        viewModel.getSingleTrack(
            token,
            trackId
        )
    }

    private fun setupInsets() {

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->

            // Status bar
            val statusBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            binding.statusBarSpace.layoutParams.height = statusBarHeight
            binding.statusBarSpace.requestLayout()

            // Navigation bar
            val navigationBarHeight =
                insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

            // Apply bottom padding so buttons stay above navigation bar
            binding.bottomCard.setPadding(
                binding.bottomCard.paddingLeft,
                binding.bottomCard.paddingTop,
                binding.bottomCard.paddingRight,
                navigationBarHeight + dpToPx(16)
            )

            insets
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // remove track
    private fun showRemoveTrackBottomSheet() {

        val dialog = BottomSheetDialog(this)

        val sheetBinding =
            BottomsheetRemoveTrackBinding.inflate(layoutInflater)

        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvCancel.setOnClickListener {
            dialog.dismiss()
        }

        sheetBinding.tvRemove.setOnClickListener {

            dialog.dismiss()

            callDeleteTrackApi()
        }

        dialog.show()
    }

    //** delete track api
    private fun callDeleteTrackApi() {

        val token =
            PrefManager.getString(AppConstant.AUTH_TOKEN)

        viewModel.deleteTrack(
            token,
            trackId
        )
    }
}