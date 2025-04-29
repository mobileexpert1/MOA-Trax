package appentus.datasource.api

import appentus.datasource.BuildConfig
import appentus.datasource.api.models.home.AllMapBaseBean
import appentus.datasource.api.models.login.LoginBase
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AppApiService {

    @POST(BASE_URL_2 + "api/account/Authorize")
    suspend fun userLogin(
        @Body requestBody: RequestBody?
    ): Response<LoginBase>

    @GET(BASE_URL_2 + "api/property/mapfiles")
    suspend fun getAllMaps(
        @Header("Authorization") header: String
    ): Response<AllMapBaseBean>

    @POST(BASE_URL_2 + "api/account/Authorize")
    suspend fun loginWithSocialMedia(
        @Body requestBody: RequestBody?
    ): Response<LoginBase>

    companion object {

        val API_LOGIN = "login"
        val API_GET_MAPS = "mapfiles"



        // private const val BASE_URL = "http://18.189.188.114:3000/"

     //  private const val BASE_URL = "https://services.myoutdooragent.com/"

//        private const val BASE_URL = "https://datav2.myoutdooragent.com/"
        private const val BASE_URL = "https://services.myoutdooragent.com/"
        private const val BASE_URL_2 = BASE_URL
      //  private const val BASE_URL_2 = BuildConfig.API_URL

        fun create(): AppApiService {
            val logger = HttpLoggingInterceptor()
            logger.level = HttpLoggingInterceptor.Level.BODY
            val clientLogger = OkHttpClient.Builder()


            if(BuildConfig.DEBUG){
                clientLogger .addInterceptor(logger)
            }

            val client = clientLogger.build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AppApiService::class.java)
        }

    }

}