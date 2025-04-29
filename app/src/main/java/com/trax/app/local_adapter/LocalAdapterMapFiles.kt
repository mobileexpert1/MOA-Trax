package com.trax.app.local_adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import appentus.datasource.local.EntityMapFile
import com.downloader.PRDownloader
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.activities.MainActivity
import com.trax.app.activities.TiffActivity
import kotlinx.android.synthetic.main.rv_geo_pdf.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class LocalAdapterMapFiles(
    private val context: Context,
    private val productName: String,
    private val productNo: String,
    private val displayName: String,
    private val pdfList: List<EntityMapFile>,
    private val isTracks: Boolean,
    private val userIdList: ArrayList<String>,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rv_geo_pdf, parent, false)
        val vh = ItemHolder(view)
        return vh
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appDB = MyApp.getInstance()!!.getAppDatabase()
        fun bind(
            context: Context,
            mapFiles: EntityMapFile,
            productName: String,
            productNo: String,
            displayName: String,
            isTracks: Boolean,userIdList: ArrayList<String>
        ) {
            itemView.apply {
                if (isTracks) {
                    imgDelete.visibility = View.VISIBLE
                } else {
                    imgDelete.visibility = View.GONE
                }

              tvFileName.text = mapFiles.mapFileName!!.split(".")[0]


                PRDownloader.initialize(context)

                GlobalScope.launch {

                    var allMapPdf: EntityMapFile? = null
                    if (isTracks) {
                        allMapPdf = appDB.daoSaveMapPdf().getSaveMapPdf(mapFiles.mapId)
                    } else {

                        val data =
                            appDB.daoSavedTrackingMapPdf().getSavedTrackingMapPdf(mapFiles.mapId)
                        if(data!=null)
                        allMapPdf = EntityMapFile(
                            mapId = data.mapId,
                            productId = data.productId,
                            mapPath = data.mapPath,
                            mapFileURL = data.mapFileURL,
                            mapFileName = data.mapFileName,
                            mapInfoJason = data.mapInfoJason,
                            status = data.status,
                            latLngJsonString = data.latLngJsonString
                        )
                    }


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
                                btnDownload.setTextColor(resources.getColor(R.color.black))

                            }
                            0 -> {
                                btnCancel.visibility = View.GONE
                                btnDownload.setBackgroundResource(R.drawable.bg_gradient_btn_round_download)
                                btnDownload.text = "Download"
                            }
                        }
                    }
                    else {
                        btnCancel.visibility = View.GONE
                        btnDownload.setBackgroundResource(R.drawable.bg_open_background)
                        btnDownload.text = "Open"
                        btnDownload.setTextColor(resources.getColor(R.color.black))
                    }
                }

                btnDownload.setOnClickListener {
                    GlobalScope.launch {
                        val allMapPdf = appDB.daoSaveMapPdf().getSaveMapPdf(mapFiles.mapId)
                        if (allMapPdf != null) {
                            if (allMapPdf.status == 1) {

                                val downloadedFile = File(allMapPdf.mapPath, allMapPdf.mapFileName)
                                val intent = Intent(context, TiffActivity::class.java)
                                intent.putExtra("productName", productName)
                                intent.putExtra("productNo", productNo)
                                intent.putExtra("displayName", displayName)
                                intent.putExtra("productId", mapFiles.productId)


                                intent.putExtra("mapID", mapFiles.mapId)
                                intent.putExtra("latLngJson", mapFiles.latLngJsonString)
                                intent.putExtra("PATH", allMapPdf.mapPath)
                                intent.putExtra("FILE_NAME", allMapPdf.mapFileName)
                                intent.putExtra("MAP_FILE_JSON", allMapPdf.mapInfoJason)
                                intent.putStringArrayListExtra("userIdList",userIdList)
                                context.startActivity(intent)
                            }
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



    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        (holder as ItemHolder).bind(
            context,
            pdfList[position],
            productName,
            productNo,
            displayName,
            isTracks,userIdList
        )


        (holder as ItemHolder).itemView.imgDelete.setOnClickListener {
            showSavePdfDialog(context, pdfList[position].mapFileName!!)
        }

    }


    private fun showSavePdfDialog(context: Context, name: String) {
        val alertDialog = AlertDialog.Builder(context)
            .setMessage("Are you sure want to delete this Track?")
            .setPositiveButton("YES") { dialog, which -> deletePdf(name) }
            .setNegativeButton("NO") { dialog, which -> dialog.dismiss() }
            .setOnDismissListener({ dialog: DialogInterface? -> })
        alertDialog.show()
    }

     private fun deletePdf(name: String) {

        GlobalScope.launch {
            MyApp.getInstance()!!.getAppDatabase().daoSavedTrackingMapPdf()
                .deleteTrackingSavePdf(name)
        }
         (context as MainActivity).updateAdapter()
     }


    override fun getItemCount(): Int {
        return pdfList.size
    }

}