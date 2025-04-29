package com.trax.app.local_adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import appentus.datasource.local.EntityMapFile
import appentus.datasource.local.EntitySavePdf
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.activities.MainActivity
import kotlinx.android.synthetic.main.rvgeo_pdf.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class LocalAdapterGeoPdf(
    private val context: Context,
    private val pdfList: List<EntitySavePdf>,
    val isTracks: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rvgeo_pdf, parent, false)
       // val view = LayoutInflater.from(parent.context).inflate(R.layout.rvgeo_pdf, parent, false)
        val vh = ItemHolder(view)
        return vh
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(entityGeoPDF: EntitySavePdf, isTracks: Boolean) {

            itemView.apply {
//CW
                //tvProductName.text = entityGeoPDF.displayName!!.split(".")[0]
                 tvProductName.text = "My Tracks"

                Log.e("@@","tvProductName ::"+tvProductName.text);
                val appDB = MyApp.getInstance()!!.getAppDatabase()
                val list = ArrayList<EntityMapFile>()
                if (isTracks) {
                    list.clear()

                    for (i in appDB.daoSavedTrackingMapPdf()
                        .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!!.indices) {
                        for (j in entityGeoPDF.userIdList!!) {

                            Log.e("@@", "j  user :"+j)
                            Log.e("@@", "user id  :"+appDB.daoSavedTrackingMapPdf().getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].userIdList).toString()
                                                           // if (j.equals(appDB.daoSavedTrackingMapPdf().getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].userId)) {

                             for (k in appDB.daoSavedTrackingMapPdf().getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].userIdList!!) {
                                 if (j == k) {

                                     val data = EntityMapFile(
                                         mapId = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].mapId,

                                         productId = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].productId,

                                         mapPath = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].mapPath,

                                         mapFileURL = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].mapFileURL,

                                         mapFileName = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].mapFileName,

                                         mapInfoJason = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].mapInfoJason,

                                         status = 1,

                                         latLngJsonString = appDB.daoSavedTrackingMapPdf()
                                             .getAllSavedTrackingMapPdf(entityGeoPDF.productId)!![i].latLngJsonString

                                     )
                                     Log.e("@@", "list ::${list.size}")
                                     list.add(data)

                                 }
                             }

                        }
                    }

                    list.reverse()
                } else {

                    list.clear()
                    list.addAll(appDB.daoSaveMapPdf().getAllSaveMapPdf(entityGeoPDF.productId)!!)

                }



                rv_map_files.adapter = LocalAdapterMapFiles(
                    context,
                    entityGeoPDF.productName!!,
                    entityGeoPDF.productNo!!,
                    entityGeoPDF.displayName!!,
                    list,
                    isTracks,
                    entityGeoPDF.userIdList!!
                )

                rv_map_files.layoutManager = LinearLayoutManager(
                    context,
                    RecyclerView.VERTICAL, false
                )

            }
        }




    }



    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ItemHolder).bind(pdfList[position], isTracks)
    }

    override fun getItemCount(): Int {
        Log.e("pdfList","item count "+pdfList.size);
        return pdfList.size
    }

}