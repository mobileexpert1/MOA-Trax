package com.trax.app.activities


import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView

import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import appentus.datasource.api.models.home.LatLng
import appentus.datasource.api.models.home.LatLngData
import appentus.datasource.api.models.home.MapFileJsonBean
import appentus.datasource.local.*
import com.google.android.gms.common.api.GoogleApiClient
import com.google.gson.Gson
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.LocationProvider
import com.trax.app.utils.PermissionHelper
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.VmTiff
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_tiff.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Math.abs
import kotlin.math.roundToInt


class TiffActivity : AppCompatActivity(), LocationProvider.EasyLocationCallback {
    private val TAG = "TiffActivity"
    var loginActivity: LoginActivity = LoginActivity()
    private val vmTiff: VmTiff by viewModels()
    lateinit var locationProvider: LocationProvider
    var offsetX = 0.0f
    var offsetY = 0.0f
    var mapId = 0

    var productId = 0
    var displayName = ""
    var productNo = ""
    var productName = ""

    var latLngString = ""
    var trackPath = true
    var isUpdate = true
    var userCanSaveTracking = false
    var isNotDrown = true
    var latLngData = LatLngData()
    var currentLatLngData = LatLngData()
    var appDB: AppDatabase? = null
    val pdfLists = ArrayList<String>()
    private val loader by lazy { ProgressView.getLoader(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tiff)
        loginActivity=loginActivity.getLoginContext()

        if (PermissionHelper.hasLocationPermission(this@TiffActivity)) {
            enableLocationProvider()
        } else {
            PermissionHelper.requestLocationPermission(this@TiffActivity)
        }
        initViews()
    }

    private fun initViews() {

        ivBack.setOnClickListener {
          showSavePdfDialog()

            /*if (checkLocationInBounds(
                    bottom = vmTiff.minLatLon[0],
                    left = vmTiff.minLatLon[1],
                    top = vmTiff.maxLatLon[0],
                    right = vmTiff.maxLatLon[1]
                )
            ) {
                if (userCanSaveTracking) {
                    showSavePdfDialog()
                } else {
                    finish()
                }
            } else {
                finish()
            }*/
        }


        mapId = intent.getIntExtra("mapID", 0)

        productId = intent.getIntExtra("productId", 0)
        productName = intent.getStringExtra("productName").toString()
        productNo = intent.getStringExtra("productNo").toString()
        displayName = intent.getStringExtra("displayName").toString()

        latLngString = intent.getStringExtra("latLngJson").toString()
        tvSelectedPlace.text = intent.getStringExtra("FILE_NAME").toString()
        val downloadedFile = File(
            intent.getStringExtra("PATH").toString(),
            intent.getStringExtra("FILE_NAME").toString()
        )
        appDB = MyApp.getInstance()!!.getAppDatabase()
        showPdfFromFile(downloadedFile)

        val mapJson = intent.getStringExtra("MAP_FILE_JSON").toString()
        val mapbean = Gson().fromJson(mapJson, MapFileJsonBean::class.java)
        vmTiff.getLocationData(mapbean)

        print("latLngString: $latLngString")




        imageZoomOut.setOnClickListener {
            //pdfView.scrollTo(200,200)
            /*pdfView.resetZoomWithAnimation()
            pdfView.isFitEachPage*/
            pdfView.zoomWithAnimation(1f);

        }


//        ivToggle.setOnToggled1Listener(object : OnToggledListener {
//            override fun onSwitched(toggleableView: ToggleableView?, isOn: Boolean) {
//                AConstant.isUpdate = isOn
//                isUpdate = true
//                //isUpdate = isOn
//            }
//        })


    }


    fun drawSavedTrack() {

        if (latLngString.isNotEmpty()) {
            latLngData = Gson().fromJson(latLngString, LatLngData::class.java)

            print("latLngData: $latLngData")

            for (i in 0 until latLngData.latLngList.size) {

//                if (checkLocationInBounds(
//                                bottom = vmTiff.minLatLon[0],
//                                left = vmTiff.minLatLon[1],
//                                top = vmTiff.maxLatLon[0],
//                                right = vmTiff.maxLatLon[1]
//                        )) {

                val yFactorCurrent =
                    (latLngData.latLngList[i].lat!! - vmTiff.minLatLon[1]) / (vmTiff.maxLatLon[1] - vmTiff.minLatLon[1])
                val xFactorCurrent =
                    (vmTiff.maxLatLon[0] - latLngData.latLngList[i].lng!!) / (vmTiff.maxLatLon[0] - vmTiff.minLatLon[0])
//                    if (trackPath) {
                pdfView.addPointToPath(xFactorCurrent, yFactorCurrent, true)
//                    }
            }
//            }


        }


    }


    private fun showPdfFromFile(file: File) {

        pdfView.fromFile(file)
            .password(null)
            .defaultPage(0)
            .enableSwipe(true)
            .swipeHorizontal(true)
            .enableDoubletap(true)
            .pages(0)


            .setonMoveListener { rect ->
                offsetX = rect!!.left
                offsetY = rect.right
                val xFactor = (((pdfView.width / 2) - rect.left) / rect.width())
                val yFactor = (((pdfView.height / 2) - rect.top) / rect.height())

                val latitude =
                    vmTiff.maxLatLon[1] - (yFactor * (vmTiff.maxLatLon[1] - vmTiff.minLatLon[1]))
                val longitude =
                    vmTiff.minLatLon[0] + (xFactor * abs(vmTiff.maxLatLon[0] - vmTiff.minLatLon[0]))

                print("lat: ${latitude}, lng: ${longitude}")
                tvSelectedLocation.text = "${latitude},${longitude}"
            }
            .onPageError { page, _ ->
                Toast.makeText(
                    this@TiffActivity,
                    "Error at page: $page", Toast.LENGTH_LONG
                ).show()
            }
            .load()
        pdfView.minZoom = 50f
    }

    private fun enableLocationProvider() {

        locationProvider = LocationProvider.Builder(this@TiffActivity)
            .setInterval(500)
            .setFastestInterval(200)
            .setListener(this)
            .build()
        lifecycle.addObserver(locationProvider)
    }

    var curLatitude = 0.0
    var curLongitude = 0.0


    fun getDistance(latitude: Double, longitude: Double): Int {


        val locationA = Location("Point A")
        locationA.latitude = curLatitude
        locationA.longitude = curLongitude
        val locationB = Location("Point B")
        locationB.latitude = latitude
        locationB.longitude = longitude
        val distance = locationA.distanceTo(locationB)
        print("Distance: $distance")
        return distance.roundToInt()
    }


    override fun onGoogleAPIClient(googleApiClient: GoogleApiClient?, message: String?) {

    }

    override fun onLocationUpdated(latitude: Double, longitude: Double) {
        if (curLatitude != latitude || curLongitude != longitude) {

            if (curLatitude != latitude) {
                val data = LatLng()
                data.lat = latitude
                data.lng = longitude
                currentLatLngData.latLngList.add(data)
            }
            if (!userCanSaveTracking) {

                if (curLatitude != 0.0) {
                    if (getDistance(latitude, longitude) >= 2) {
                        userCanSaveTracking = true
                    }
                }
            }

            curLatitude = latitude
            curLongitude = longitude

            if (!pdfView.isPdfFileNull) {
                if (isNotDrown) {
                    isNotDrown = false
                    drawSavedTrack()
                }
                startPathInPdf()

//                if (AConstant.isUpdate){
//                }
//                else{
//                    markStopLocationInPdf()
//                }
            }
        }
    }

    private fun startPathInPdf() {
        if (checkLocationInBounds(
                bottom = vmTiff.minLatLon[0],
                left = vmTiff.minLatLon[1],
                top = vmTiff.maxLatLon[0],
                right = vmTiff.maxLatLon[1]
            )
        ) {

            val yFactorCurrent =
                (curLatitude - vmTiff.minLatLon[1]) / (vmTiff.maxLatLon[1] - vmTiff.minLatLon[1])
            val xFactorCurrent =
                (vmTiff.maxLatLon[0] - curLongitude) / (vmTiff.maxLatLon[0] - vmTiff.minLatLon[0])
            if (trackPath) {
                pdfView.addPointToPath(xFactorCurrent, yFactorCurrent, false)
            }
            pdfView.setCurrentRunningLocation(xFactorCurrent, yFactorCurrent, pdfView)
        }
    }

    private fun markStopLocationInPdf() {
        if (checkLocationInBounds(
                bottom = vmTiff.minLatLon[0],
                left = vmTiff.minLatLon[1],
                top = vmTiff.maxLatLon[0],
                right = vmTiff.maxLatLon[1]

            )
        ) {
            val yFactorCurrent =
                (curLatitude - vmTiff.minLatLon[1]) / (vmTiff.maxLatLon[1] - vmTiff.minLatLon[1])
            val xFactorCurrent =
                (vmTiff.maxLatLon[0] - curLongitude) / (vmTiff.maxLatLon[0] - vmTiff.minLatLon[0])
            if (isUpdate) {
                pdfView.markedStopLocation(xFactorCurrent, yFactorCurrent)
                isUpdate = false
            }
        }
    }


    private fun checkLocationInBounds(
        top: Double,
        right: Double,
        bottom: Double,
        left: Double
    ): Boolean {
        return curLatitude > left && curLatitude < right && curLongitude < top && curLongitude > bottom
    }


    override fun onLocationUpdateRemoved() {

    }


    override fun onDestroy() {
        locationProvider.removeUpdates()
        lifecycle.removeObserver(locationProvider)
        super.onDestroy()
    }


    private fun showSavePdfDialog() {
        var uID: String = PrefManager.getString(AppConstant.CURRENT_USER).toString()
        pdfLists.add(uID)
        Log.e("pdfLista","list...."+ pdfLists.add(uID))
        val alertDialog = AlertDialog.Builder(this)
            .setMessage("Do you want to save these Tracks?")
            .setPositiveButton("YES") { dialog, which -> showEnterNameDialog() }
            .setNegativeButton("NO") { dialog, which ->
                finish()
            }
            .setOnDismissListener({ dialog: DialogInterface? -> })
        alertDialog.show()
    }

    private fun showEnterNameDialog() {
        val dialogBuilder = Dialog(this)
        dialogBuilder.setContentView(R.layout.dialog_pdf_name)
        val editText = dialogBuilder.findViewById<View>(R.id.edtPdfName) as EditText
        val txtSubmit = dialogBuilder.findViewById<View>(R.id.txtOK) as TextView

        txtSubmit.setOnClickListener {

            if (editText.text.toString().isEmpty()) {
                editText.error = "please enter Tracks name"
                editText.requestFocus()
                return@setOnClickListener
            }

            val data = appDB?.daoSavedTrackingMapPdf()
                ?.getSavedTrackingMapPdfByName(editText.text.toString())

            if (data != null) {
                if (data.mapFileName!! == editText.text.toString()) {
                    editText.error = "File Name Already Exists"
                    editText.requestFocus()
                    return@setOnClickListener
                }
            }


            val json: String = Gson().toJson(currentLatLngData)

            GlobalScope.launch {


//                appDB?.daoTrackingSavePdf()?.insertTrackingPdf(
//                    EntitySavedTrackingPdf(
//                        productId = productId,
//                        productName =  productName,
//                        productNo = productNo,
//                        displayName =displayName
//                    )
//                )

//CW
 //var uID: String = PrefManager.getString(AppConstant.CURRENT_USER).toString()
 var uID: String = PrefManager.getString(AppConstant.CURRENT_USER).toString()

                /*var uID: String = AppConstant.CURRENT_USER.toString()
                Log.e(">>","uID ::"+uID)*/;
                //change
                val userLoginList = ArrayList<String>()


                userLoginList.add(uID)
                if(!intent.getStringArrayListExtra("userIdList").isNullOrEmpty())
                {
                    userLoginList.addAll(intent.getStringArrayListExtra("userIdList")!!)

                }
                val distinct = userLoginList.distinct()
               val uList = ArrayList<String>()
               uList.addAll(distinct)
                appDB?.daoSavePdf()?.updateId(productId,uList)
                appDB?.daoSavePdf()?.updateSaveMapData(productId,true)
                appDB?.daoSavedTrackingMapPdf()?.insertTrackingMapPdf(
                    EntityMapTrackingFile(
                        mapId = mapId,
                        productId = productId,
                        mapPath = intent.getStringExtra("PATH").toString(),
                        mapFileURL = "url",
                        mapFileName = editText.text.toString(),
                        mapInfoJason = intent.getStringExtra("MAP_FILE_JSON").toString(),
                        status = 1,
                        latLngJsonString = json,
                        userIdList = uList
                    )
                )
            }
            finish()
            dialogBuilder.dismiss()
        }
        dialogBuilder.show()
    }

    fun localData(){


    }


}