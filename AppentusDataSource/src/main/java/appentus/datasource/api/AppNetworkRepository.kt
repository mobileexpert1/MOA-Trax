package appentus.datasource.api

import okhttp3.RequestBody

public class AppNetworkRepository() : BaseDataSource() {

    var apiService = AppApiService.create()

    fun userLogin(tag: String, requestBody: RequestBody?) =
        performOperation(tag) {
            getResult(tag) {
                apiService.userLogin(requestBody)
            }
        }

    fun getAllMaps(tag: String,token:String) =
        performOperation(tag) {
            getResult(tag) {
                apiService.getAllMaps(token)
            }
        }
    fun loginWithSocialMedia(tag: String, requestBody: RequestBody?) =
        performOperation(tag) {
            getResult(tag) {
                apiService.loginWithSocialMedia(requestBody)
            }
        }


}