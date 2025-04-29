package com.trax.app.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import appentus.datasource.api.BaseDataSource
import appentus.datasource.api.models.home.MapBaseBean
import appentus.datasource.local.EntitySavePdf
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.adapters.AdapterGeoPdf
import com.trax.app.local_adapter.LocalAdapterGeoPdf
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.LocationProvider
import com.trax.app.utils.PermissionHelper
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.HomeViewModel
import com.trax.app.viewModels.LoginViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottomsheet_logout_dialog.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow


class MainActivity : AppCompatActivity(), LocationProvider.EasyLocationCallback {

    private val viewModel: HomeViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    var cmdConvertGeoPdfToTIF = mutableListOf<String>()
    private val TAG = "MainActivity"
    lateinit var locationProvider: LocationProvider
    private val loader by lazy { ProgressView.getLoader(this) }
    var isTracks = false
    val appDB = MyApp.getInstance()!!.getAppDatabase()
     var currentUser: String = ""
    private var productId : Int =0
    private val userList: ArrayList<String> = ArrayList()
    private val CREDENTIAL_PICKER_REQUEST = 1
    /**
     * Internal Files Directory.(File System private to the Application)
     * This will not be available to the user or other com directly.
     * */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val userAccountId = PrefManager.getString(AppConstant.USER_ACCOUNT_ID)
        userList.add(userAccountId)

        if (PermissionHelper.hasLocationPermission(this@MainActivity)) {
            enableLocationProvider()
        } else {
            PermissionHelper.requestLocationPermission(this@MainActivity)
        }

        initViews()
        initListeners()
        if (isNetworkConnected()) {
            checkNetwork()
        } else {
            getSavedMaps(false)
        }
    }

    override fun onResume() {
        super.onResume()

        /*Log.e("@@","PrefManager :: USER_ACCOUNT_ID == )"+PrefManager.getString(AppConstant.USER_ACCOUNT_ID))
      Log.e("@@"," appDB.daoSavePdf().loadSavedTracks list )"+appDB.daoSavePdf().loadSavedTracks(
          PrefManager.getString(AppConstant.USER_ACCOUNT_ID).toString().length.toString()
      ))*/

        // if(appDB.daoSavePdf().loadSavedTracks(PrefManager.getString(AppConstant.USER_ACCOUNT_ID).toString())!!.isEmpty())
        //  {
        /*for (i in appDB.daoSavePdf().loadSavedTracks(productId = productId).toString()) {
        if (appDB.daoSavePdf().loadSavedTracks(productId)!!.isNullOrEmpty()) {*/
           if (appDB.daoSavedTrackingMapPdf().getAllSavedTrackingMap()!!.isEmpty()) {

            Log.e("@@", "GONE :: isTracks ");
            ll_my_tracks.visibility = View.GONE
            isTracks = false
        } else {
            Log.e("@@", "VISIBLE :: isTracks ");
            ll_my_tracks.visibility = View.VISIBLE
        }

   // }
        if (isTracks) {
            getSavedMaps(true)
        } else {
            checkNetwork()
        }
    }

    private fun checkNetwork() {
        if (isNetworkConnected()) {
            getAllMaps()
        } else {
            getSavedMaps(false)

        }

      /*  lifecycleScope.launchWhenResumed {
            networkAvailableFlow().collectLatest { networkIsAvailable ->
                println(networkIsAvailable)
                if (networkIsAvailable) {
                    getAllMaps()
                } else {
                    getSavedMaps(false)

                }
            }
        }*/

    }

    private fun getSavedMaps(isTrack: Boolean) {


//            if (isTracks) {
//          val pdfList = ArrayList<EntitySavePdf>()
//
//                pdfList.clear()
//                for (i in appDB.daoTrackingSavePdf().getAllSavedTrackingPdf()!!.indices){
//
//                    val data = EntitySavePdf(
//                        productId = appDB.daoTrackingSavePdf().getAllSavedTrackingPdf()!![i].productId,
//                        productName = appDB.daoTrackingSavePdf().getAllSavedTrackingPdf()!![i].productName,
//                        productNo = appDB.daoTrackingSavePdf().getAllSavedTrackingPdf()!![i].productNo,
//                        displayName = appDB.daoTrackingSavePdf().getAllSavedTrackingPdf()!![i].displayName,
//                    )
//                    pdfList.add(data)
//                }
//
//
//
//
//                rvGeoPdf.adapter =
//                    LocalAdapterGeoPdf(this@MainActivity,pdfList,isTracks)
//
//            } else {

        //rvGeoPdf.adapter = LocalAdapterGeoPdf(this@MainActivity, appDB.daoSavePdf().getAllSavePdf()!!, isTrack)
      //  tvTrackTitle.visibility=View.VISIBLE

        //        }


        val pdfList = ArrayList<EntitySavePdf>()

        pdfList.clear()
       currentUser=  PrefManager.getString(AppConstant.USER_ACCOUNT_ID).toString()
        for (i in appDB.daoSavePdf().getAllSavePdf()!!.indices) {
            Log.e("ddkgdgd","${appDB.daoSavePdf().getAllSavePdf()!![i]}")
//            if(appDB.daoSavePdf().getAllSavePdf()!![i].isSavedTracking) {
                if (appDB.daoSavePdf().getAllSavePdf()!![i].userIdList!!.contains(currentUser)) {
                        pdfList.add(appDB.daoSavePdf().getAllSavePdf()!![i])
                    productId = pdfList.get(i).productId
//                }
            }

        }

                rvGeoPdf.adapter = LocalAdapterGeoPdf(this@MainActivity, pdfList, isTrack)



    }


    fun updateAdapter() {
        getSavedMaps(true)
       if (appDB.daoSavedTrackingMapPdf().getAllSavedTrackingMap()!!.isEmpty()) {
        /*for (i in appDB.daoSavePdf().loadSavedTracks(productId = productId).toString()) {
            if (appDB.daoSavePdf().loadSavedTracks(productId)!!..isNullOrEmpty()) {*/
                //   if(appDB.daoSavePdf().loadSavedTracks(PrefManager.getString(AppConstant.USER_ACCOUNT_ID).toString())!!.isEmpty()) {
                ll_my_tracks.visibility = View.GONE
                isTracks = false
                checkNetwork()
            } else {
                ll_my_tracks.visibility = View.VISIBLE
            }
       // }
    }


//    @ExperimentalCoroutinesApi
//    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
//    fun networkAvailableFlow(): Flow<Boolean> = callbackFlow {
//        val callback =
//            object : ConnectivityManager.NetworkCallback() {
//                override fun onAvailable(network: Network) {
//                    offer(true)
//                }
//
//                override fun onLost(network: Network) {
//                    offer(false)
//                }
//
//                override fun onUnavailable() {
//                    offer(false)
//                }
//            }
//
//        val manager = getSystemService(Context.CONNECTIVITY_SERVICE)
//                as ConnectivityManager
//        manager.registerNetworkCallback(NetworkRequest.Builder().run {
//            addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//            build()
//        }, callback)
//        awaitClose {
//            manager.unregisterNetworkCallback(callback)
//        }
//    }

    private fun getAllMaps() {
        val token = loginViewModel.decrypt(PrefManager.getString(AppConstant.AUTH_TOKEN))
        val userAccountId = PrefManager.getString(AppConstant.USER_ACCOUNT_ID)
        val userProfileId = PrefManager.getString(AppConstant.USER_PROFILE_ID)
        Log.d("PrefValues", "Token: $token, Account ID: $userAccountId, Profile ID: $userProfileId")

        viewModel.getAllMaps(token!!)!!.observe(this, Observer {
            when {
                it!!.status == BaseDataSource.Resource.Status.SUCCESS -> {

                    loader.dismiss()
                    if (it.data!!.model == null) {
                        ll_noDatafound.visibility = View.VISIBLE
                        clToolbar.visibility = View.GONE
                        rvGeoPdf.visibility = View.GONE
                    } else {
                        rvGeoPdf.visibility = View.VISIBLE
                        ll_noDatafound.visibility = View.GONE
                        clToolbar.visibility = View.VISIBLE
                         val pdfList : ArrayList<MapBaseBean> = ArrayList()
                         pdfList.addAll(it.data!!.model)
//                        val userAccountId = PrefManager.getString(AppConstant.USER_ACCOUNT_ID)
//                        userList.add(userAccountId)
//                        val userIdList : ArrayList<String> = ArrayList()
//                        userIdList.add(intent.getStringExtra("user")!!.toString())
                        rvGeoPdf.adapter = AdapterGeoPdf(this@MainActivity, pdfList,userList)
                        Log.e("sbhscjsca","slkcdnsv $userList pdf $pdfList ")


                    }
                }
                it.status == BaseDataSource.Resource.Status.ERROR -> {
                    loader.dismiss()
                    ll_noDatafound.visibility = View.VISIBLE
                    clToolbar.visibility = View.GONE
                    rvGeoPdf.visibility = View.GONE
                }
                it.status == BaseDataSource.Resource.Status.LOADING -> {
                    loader.show()
                }
            }
        })
    }

    private fun enableLocationProvider() {
        locationProvider = LocationProvider.Builder(this@MainActivity)
            .setInterval(500)
            .setFastestInterval(200)
            .setListener(this)
            .build()


        lifecycle.addObserver(locationProvider)
    }

    private fun initListeners() {
//        live-login@orbisinc.com/Welcome2022

                ll_signout.setOnClickListener {
                    val dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
                    dialog.setContentView(R.layout.bottomsheet_logout_dialog)

                    dialog.btnLogout.setOnClickListener {
                        if (isInternetConnected(this)) {
                            PrefManager.putBoolean(AppConstant.IS_LOGIN, false)
                            PrefManager.clearKey(AppConstant.CURRENT_USER)
                            // Uncomment and modify these lines as needed based on your use case
                            /*
                            if (!currentUser.equals(User)) {
                                appDB.daoGeoPdf().clearGeoPDF()
                                appDB.daoSaveMapPdf().clearSavePdf()
                                appDB.daoRemoteKeys().clearRemoteKeys()
                                appDB.daoSavePdf().clearSavePdf()
                                appDB.daoTrackingSavePdf().clearSavedTrackingPdf()
                                appDB.daoSavedTrackingMapPdf().clearTrackingSavePdf()
                                // PrefManager.clear()
                            }
                            */

                            AppConstant.CredentialsRemember = AppConstant.CredentialsRemember
                            val intent = Intent(this, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else {
                            Toast.makeText(this, "Please connect to the internet to sign out.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    dialog.btnCancel.setOnClickListener {
                        dialog.dismiss()
                    }
                    dialog.show()
                }

/*
        ll_signout.setOnClickListener {
            val dialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
            dialog.setContentView(R.layout.bottomsheet_logout_dialog)
            dialog.btnLogout.setOnClickListener {
                PrefManager.putBoolean(AppConstant.IS_LOGIN, false)

                PrefManager.clearKey(AppConstant.CURRENT_USER)


*/
/*if(!currentUser.equals(User))
{
    appDB.daoGeoPdf().clearGeoPDF()
    appDB.daoSaveMapPdf().clearSavePdf()
    appDB.daoRemoteKeys().clearRemoteKeys()
    appDB.daoSavePdf().clearSavePdf()
    appDB.daoTrackingSavePdf().clearSavedTrackingPdf()
    appDB.daoSavedTrackingMapPdf().clearTrackingSavePdf()
   // PrefManager.clear()

}*//*



                AppConstant.CredentialsRemember = AppConstant.CredentialsRemember
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            dialog.btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            dialog.show()
        }
*/

        ll_my_maps.setOnClickListener {
            isTracks = false
            checkNetwork()
            //tvTrackTitle.visibility=View.GONE
            locationIV.setImageDrawable(resources.getDrawable(R.drawable.ic_location));
            trackIV.setImageDrawable(resources.getDrawable(R.drawable.track_unselected));
        }

        ll_my_tracks.setOnClickListener {
            isTracks = true
            getSavedMaps(true)
           // tvTrackTitle.visibility=View.VISIBLE
            locationIV.setImageDrawable(resources.getDrawable(R.drawable.ic_location_unselected));
            trackIV.setImageDrawable(resources.getDrawable(R.drawable.track_on));
        }

    }

    private fun initViews() {
        currentUser= intent.getStringExtra("user").toString()
        tvName.text = PrefManager.getString(AppConstant.USER_NAME)
        currentUser=  PrefManager.getString(AppConstant.USER_ACCOUNT_ID).toString()
        tvNameNoMap.text = PrefManager.getString(AppConstant.USER_NAME)

        rvGeoPdf.layoutManager =
            LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PermissionHelper.PERMISSION_CODE -> {
                if (PermissionHelper.hasLocationPermission(this@MainActivity)) {
                    enableLocationProvider()
                } else {
                    PermissionHelper.requestLocationPermission(this@MainActivity)
                }
            }
        }
    }

    override fun onDestroy() {
        locationProvider = LocationProvider.Builder(this@MainActivity)
            .setInterval(500)
            .setFastestInterval(200)
            .setListener(this)
            .build()
        lifecycle.removeObserver(locationProvider)
        super.onDestroy()
    }

    override fun onGoogleAPIClient(googleApiClient: GoogleApiClient?, message: String?) {

    }

    override fun onLocationUpdated(latitude: Double, longitude: Double) {

    }

    override fun onLocationUpdateRemoved() {

    }

    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }


    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
}