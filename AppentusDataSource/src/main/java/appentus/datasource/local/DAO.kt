package appentus.datasource.local

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DaoGeoPdf {

    @Query("SELECT * FROM geoPDF")
    fun getPdfList(): List<EntityGeoPDF>

    @Query("SELECT * FROM geoPDF")
    fun getPdfListLiveData(): LiveData<List<EntityGeoPDF>>

    @Query("SELECT * FROM geoPDF where created = :created")
    fun getPdf(created: String): EntityGeoPDF

    @Query("SELECT * FROM geoPDF")
    fun getPdfPagingSource(): PagingSource<Int, EntityGeoPDF>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(geoPdf: EntityGeoPDF)

    @Query("DELETE FROM geoPDF")
    fun clearGeoPDF()

    @Query("UPDATE geoPDF SET remoteTiffPath = :remoteTiffPath,status = :i WHERE created = :created")
    fun updatePdfStatusDownloading(created: String, remoteTiffPath: String, i: Int)

    @Query("UPDATE geoPDF SET localTiffPath = :localTiffPath,status = :i WHERE created = :created")
    fun updatePdfStatusConverting(created: String, localTiffPath: String, i: Int)

    @Query("UPDATE geoPDF SET localPngPath = :localPngPath,status = :i WHERE created = :created")
    fun updatePdfStatusReady(created: String, localPngPath: String, i: Int)

}

@Dao
interface DaoRemoteKeys {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertAllRemoteKeys(remoteKey: List<EntityRemoteKeys>)

    @Query("SELECT * FROM remoteKeys WHERE repoId = :id")
     fun getRemoteKeys(id: String): EntityRemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE repoId = :id")
    fun getRemoteKeyFor(id: Int): EntityRemoteKeys?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insert(remoteKey: EntityRemoteKeys)

    @Query("DELETE FROM remoteKeys")
     fun clearRemoteKeys()
}

@Dao
interface DaoSavePdf {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertPdf(entitySavePdf: EntitySavePdf)

    @Query("SELECT * FROM allPdfs WHERE productId = :id")
     fun getSavePdf(id: Int): EntitySavePdf?

    @Query("SELECT * FROM allPdfs")
    fun getAllSavePdf(): List<EntitySavePdf>?


//    @Query("select isSavedTracking from allPdfs where isSavedTracking = 1 AND userId= :userId" )
//    fun loadSavedTracks(userId: String): Boolean?

//    @Query("SELECT * from allPdfs  WHERE isSavedTracking = 1 AND productId = :productId" )
//    fun loadSavedTracks(productId: Int): List<EntitySavePdf>?



    @Query("DELETE FROM allPdfs")
     fun clearSavePdf()

    @Query("UPDATE allPdfs set isSavedTracking= :isSavedTracking  WHERE productId = :productId")
     fun updateSaveMapData(productId: Int, isSavedTracking: Boolean)

    @Query("UPDATE allPdfs set userIdList= :userIdList  WHERE productId = :productId")
     fun updateId(productId: Int, userIdList: ArrayList<String>?)
}

@Dao
interface DaoSaveMapPdf {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertMapPdf(entitySavePdf: EntityMapFile)

    @Query("SELECT * FROM allMapPdfs WHERE mapId = :id")
     fun getSaveMapPdf(id: Int): EntityMapFile?

    @Query("UPDATE allMapPdfs set latLngJsonString= :latLng  WHERE mapId = :id")
     fun updateSaveMapData(id: Int, latLng: String)

    @Query("SELECT * FROM allMapPdfs WHERE productId = :id")
    fun getAllSaveMapPdf(id: Int): List<EntityMapFile>?

    @Query("DELETE FROM allMapPdfs")
     fun clearSavePdf()

}


@Dao
interface DaoTrackingSavePdf {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun insertTrackingPdf(entitySavePdf: EntitySavedTrackingPdf)

    @Query("SELECT * FROM allTrackingPdfs WHERE productId = :id")
     fun getSavedTrackingPdf(id: Int): EntitySavedTrackingPdf?

    @Query("SELECT * FROM allTrackingPdfs")
    fun getAllSavedTrackingPdf(): List<EntitySavedTrackingPdf>?

    @Query("DELETE FROM allTrackingPdfs")
     fun clearSavedTrackingPdf()

}


@Dao
interface DaoSavedTrackingMapPdf {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrackingMapPdf(entitySavePdf: EntityMapTrackingFile)

    @Query("SELECT * FROM allMapTrackingPdfs WHERE mapId = :id")
     fun getSavedTrackingMapPdf(id: Int): EntityMapTrackingFile?

    @Query("UPDATE allMapTrackingPdfs set latLngJsonString= :latLng  WHERE mapId = :id")
     fun updateSavedTrackingMapData(id: Int, latLng: String)

    @Query("SELECT * FROM allMapTrackingPdfs WHERE productId = :id")
    fun getAllSavedTrackingMapPdf(id: Int): List<EntityMapTrackingFile>?

    @Query("SELECT * FROM allMapTrackingPdfs")
    fun getAllSavedTrackingMap(): List<EntityMapTrackingFile>?

    @Query("SELECT * FROM allMapTrackingPdfs WHERE mapFileName = :name")
    fun getSavedTrackingMapPdfByName(name: String):EntityMapTrackingFile?

    @Query("DELETE FROM allMapTrackingPdfs WHERE mapFileName = :name")
     fun deleteTrackingSavePdf(name:String)

    @Query("DELETE FROM allMapTrackingPdfs")
     fun clearTrackingSavePdf()



}
