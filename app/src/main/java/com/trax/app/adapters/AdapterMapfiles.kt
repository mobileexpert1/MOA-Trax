package com.trax.app.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import appentus.datasource.api.models.home.MapFilesDetails
import appentus.datasource.local.EntityMapFile
import appentus.datasource.local.EntitySavePdf
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.activities.TiffActivity
import com.trax.app.utils.AppConstant
import kotlinx.android.synthetic.main.rv_geo_pdf.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class AdapterMapfiles(
    private val context: Context,
    private val productId: Int,
    private val productName: String,
    private val productNo: String,
    private val displayName: String,
    private val pdfMapList: List<MapFilesDetails>,
    private val idList: ArrayList<String>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_geo_pdf, parent, false)
        val vh = ItemHolder(view)
        return vh
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appDB = MyApp.getInstance()!!.getAppDatabase()
        fun bind(
            mapFiles: MapFilesDetails,
            productName: String,
            productNo: String,
            displayName: String,
            productId: Int,idList:ArrayList<String>
        ) {
            itemView.apply {
                tvFileName.text = mapFiles.mapFileName.split(".")[0]
                val uIdList : ArrayList<String> =java.util.ArrayList()
                uIdList.addAll(idList)
                PRDownloader.initialize(context)

                GlobalScope.launch {
                    val allMapPdf = appDB.daoSaveMapPdf().getSaveMapPdf(mapFiles.mapFileID)
                    if (allMapPdf != null) {
                        when (allMapPdf.status) {
                            null -> {
                                btnCancel.visibility = View.GONE
                                btnDownload.setBackgroundResource(R.drawable.bg_gradient_btn_round_download)
                                btnDownload.text = "Download"
                            }
                            1 -> {
                                btnCancel.visibility = View.GONE
                                btnDownload.setBackgroundResource(R.drawable.bg_open_background)
                                btnDownload.text = "Open"

                            }
                            0 -> {
                                btnCancel.visibility = View.GONE
                                btnDownload.setBackgroundResource(R.drawable.bg_gradient_btn_round_download)
                                btnDownload.text = "Download"
                            }
                            else -> {
                                btnCancel.visibility = View.GONE
                                btnDownload.setBackgroundResource(R.drawable.bg_gradient_btn_round_download)
                                btnDownload.text = "Download"
                            }
                        }
                    }
                }

                btnDownload.setOnClickListener {
                    GlobalScope.launch {
                        val allMapPdf = appDB.daoSaveMapPdf().getSaveMapPdf(mapFiles.mapFileID)
                        Log.e("@@","mapFileID :"+mapFiles.mapFileID);
                        if (allMapPdf != null) {
                            if (allMapPdf.status == null || allMapPdf.status == 0) {
                                downloadPdfFromInternet(
                                    productId,
                                    productName,
                                    productNo,
                                    displayName,
                                    mapFiles.mapFileID,
                                    mapFiles.mapFile,
                                    mapFiles.mapFileName,
                                    mapFiles.mapInfoJson,
                                    getCachePath(context),
                                    context,
                                    btnCancel,
                                    btnDownload,uIdList
                                )
                            } else if (allMapPdf.status == 1) {
                                val downloadedFile = File(allMapPdf.mapPath, allMapPdf.mapFileName)
                                val intent = Intent(context, TiffActivity::class.java)

                                intent.putExtra("productName", productName)
                                intent.putExtra("productNo", productNo)
                                intent.putExtra("displayName", displayName)

                                intent.putExtra("productId", productId)
                                intent.putExtra("mapID", allMapPdf.mapId)
                                intent.putExtra("latLngJson", allMapPdf.latLngJsonString)
                                intent.putExtra("PATH", allMapPdf.mapPath)
                                intent.putExtra("FILE_NAME", allMapPdf.mapFileName)
                                intent.putExtra("MAP_FILE_JSON", allMapPdf.mapInfoJason)

                                context.startActivity(intent)
                            }
                        } else {
                            downloadPdfFromInternet(
                                productId,
                                productName,
                                productNo,
                                displayName,
                                mapFiles.mapFileID,
                                mapFiles.mapFile,
                                mapFiles.mapFileName,
                                mapFiles.mapInfoJson,
                                getCachePath(context),
                                context,
                                btnCancel,
                                btnDownload,
                                uIdList
                            )
                        }
                    }

                }
                btnCancel.setOnClickListener {
                    btnDownload.setBackgroundResource(R.drawable.bg_gradient_btn_round_download)
                    btnDownload.text = "Download"
                    btnCancel.visibility = View.GONE
                }
            }
        }

        fun getCachePath(context: Context): String {
            return context.filesDir.absolutePath
        }

        private fun downloadPdfFromInternet(
            productId: Int,
            productName: String,
            productNo: String,
            displayName: String,
            mapId: Int,
            url: String,
            mapFileName: String,
            mapJson: String,
            dirPath: String,
            context: Context,
            btnCancel: ImageView,
            btnDownload: TextView,
            uIdList: ArrayList<String>
        )


        {



            GlobalScope.launch(Dispatchers.Main) {
                btnDownload.setBackgroundResource(R.drawable.bg_btn_processing)
                btnDownload.text = "Processing"
                btnDownload.setTextColor(Color.WHITE);
                btnCancel.visibility = View.VISIBLE
            }
            AppConstant.DOWNLOAD_ID = mapFileName
            PRDownloader.download(
                url,
                dirPath,
                mapFileName
            ).build()
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        btnCancel.visibility = View.GONE
                        btnDownload.setBackgroundResource(R.drawable.bg_open_background)
                        btnDownload.text = "Open"
                        btnDownload.setTextColor(ContextCompat.getColor(context,R.color.black))
                        Toast.makeText(context, "Download Complete", Toast.LENGTH_LONG).show()
                        val downloadedFile = File(dirPath, mapFileName)

                        // progressBar.visibility = View.GONE
                        val appDB = MyApp.getInstance()!!.getAppDatabase()
                        GlobalScope.launch {

                            appDB.daoSaveMapPdf().insertMapPdf(
                                EntityMapFile(
                                    mapId = mapId,
                                    productId = productId,
                                    mapPath = dirPath,
                                    mapFileURL = url,
                                    mapFileName = mapFileName,
                                    mapInfoJason = mapJson,
                                    status = 1
                                )
                            )
//CW

                            appDB.daoSavePdf().insertPdf(
                                EntitySavePdf(
                                    productId = productId,
                                    productName = productName,
                                    productNo = productNo,
                                    displayName = displayName,
                                    userIdList = uIdList
                                )
                            )

                        }

                        val intent = Intent(context, TiffActivity::class.java)

                        intent.putExtra("productName", productName)
                        intent.putExtra("productNo", productNo)
                        intent.putExtra("displayName", displayName)
                        intent.putExtra("productId", productId)

                        intent.putExtra("mapID", mapId)
                        intent.putExtra("latLngJson", "")
                        intent.putExtra("PATH", dirPath)
                        intent.putExtra("FILE_NAME", mapFileName)
                        intent.putExtra("MAP_FILE_JSON", mapJson)

                        context.startActivity(intent)

                    }

                    override fun onError(error: com.downloader.Error?) {
                        Toast.makeText(
                            context,
                            "Error in downloading file : $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                })

        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ItemHolder).bind(
            pdfMapList[position],
            productName,
            productNo,
            displayName,
            productId,idList
        )

    }

    override fun getItemCount(): Int {
        return pdfMapList.size
    }

}