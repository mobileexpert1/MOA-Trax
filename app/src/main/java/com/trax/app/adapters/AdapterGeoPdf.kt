package com.trax.app.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import appentus.datasource.api.models.home.MapBaseBean
import com.trax.app.R
import kotlinx.android.synthetic.main.rvgeo_pdf.view.*

class AdapterGeoPdf(
    private val context: Context,
    val pdfList: List<MapBaseBean>,
    private val userIdList: ArrayList<String>,

    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.rvgeo_pdf, parent, false)
        val vh = ItemHolder(view)
        return vh
    }

    class ItemHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(entityGeoPDF: MapBaseBean,userIdList:ArrayList<String>) {

            itemView.apply {

                if(entityGeoPDF.mapFiles.isNullOrEmpty()){
                    tvProductName.text = entityGeoPDF.displayName
                    NoMapCV.visibility = View.VISIBLE
                    tvNotAvailable.visibility = View.VISIBLE
                    rv_map_files.visibility=View.GONE
                    //  Log.e("MapVisibility = null", entityGeoPDF.displayName.toString() + " Size== 0 ")
                 } else {
                    tvNotAvailable.visibility = View.GONE
                    NoMapCV.visibility = View.GONE
                    tvProductName.text = entityGeoPDF.displayName
                    rv_map_files.visibility=View.VISIBLE
                    tvNotAvailable.visibility = View.GONE
                    var productName1:String

                    // Log.e("MapVisibility", entityGeoPDF.displayName.toString() +"  Size " + entityGeoPDF.mapFiles.size.toString())

                    if(entityGeoPDF.productName!=null){
                        productName1=entityGeoPDF.productName!!
                    } else{
                        productName1=""
                    }
                 val distinct = entityGeoPDF.mapFiles.distinct().toList()

                 // Log.e("entityGeoPDF.mapFiles  ","  Size = ${entityGeoPDF.mapFiles.size}")

                    rv_map_files.adapter = AdapterMapfiles(context, entityGeoPDF.productID,productName1,entityGeoPDF.productNo!!,entityGeoPDF.displayName!!,distinct,userIdList)
                    Log.e("AdapterMapfiles","AdapterMapfiles"+productName1+entityGeoPDF.productNo+entityGeoPDF.displayName+userIdList)
                    rv_map_files.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)

                }
            }
        }
       /* fun bind( pdfList : List<MapBaseBean>) {
            itemView.apply {

                if(entityGeoPDF.mapFiles.isNullOrEmpty()){
                    tvProductName.text = entityGeoPDF.displayName
                    tvNotAvailable.visibility = View.VISIBLE
                  //  Log.e("MapVisibility = null", entityGeoPDF.displayName.toString() + " Size== 0 ")
                 } else {
                    tvNotAvailable.visibility = View.GONE
                    tvProductName.text = entityGeoPDF.displayName
                    var productName1:String
                    // Log.e("MapVisibility", entityGeoPDF.displayName.toString() +"  Size " + entityGeoPDF.mapFiles.size.toString())

                    if(entityGeoPDF.productName!=null){
                        productName1=entityGeoPDF.productName!!
                    } else{
                        productName1=""
                    }
                 val distinct = entityGeoPDF.mapFiles.distinct().toList()
                  Log.e("entityGeoPDF.mapFiles  ","  Size = ${entityGeoPDF.mapFiles.size}")
                    rv_map_files.adapter = AdapterMapfiles(context, entityGeoPDF.productID,productName1,entityGeoPDF.productNo!!,entityGeoPDF.displayName!!,entityGeoPDF.mapFiles)
                    rv_map_files.layoutManager = LinearLayoutManager(context,RecyclerView.VERTICAL,false)

                }
            }
        }*/
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        (holder as ItemHolder).bind(pdfList[position],userIdList)


        Log.e("@@","main list :"+pdfList[position]);


    }

    override fun getItemCount(): Int {
        return pdfList.size
    }

}