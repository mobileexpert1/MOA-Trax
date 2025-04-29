package appentus.datasource.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import appentus.datasource.api.models.home.MapBaseBean
import com.iFollow.dataSource.local.convertors.Converters

@Database(
    entities = arrayOf(
        EntityRemoteKeys::class, EntityGeoPDF::class, EntitySavePdf::class, EntityMapFile::class,
        EntityMapTrackingFile::class, EntitySavedTrackingPdf::class,

        ), version = 8,exportSchema = true
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun daoRemoteKeys(): DaoRemoteKeys
    abstract fun daoGeoPdf(): DaoGeoPdf

    abstract fun daoSavePdf(): DaoSavePdf
    abstract fun daoSaveMapPdf(): DaoSaveMapPdf

    abstract fun daoTrackingSavePdf(): DaoTrackingSavePdf
    abstract fun daoSavedTrackingMapPdf(): DaoSavedTrackingMapPdf

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: buildDatabase(context).also {
                instance = it
            }
        }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, "appentus_orbis_inc")
                .allowMainThreadQueries().fallbackToDestructiveMigration().build()
    }

}