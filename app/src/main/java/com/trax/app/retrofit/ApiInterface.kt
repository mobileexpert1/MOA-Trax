package appentus.datasource.api

import com.trax.app.models.delete.DeleteTrackResponse
import com.trax.app.models.delete.DeleteUserResponse
import com.trax.app.models.home.license.LicenseResponse
import com.trax.app.models.live_track.save_track.SaveTrackRequest
import com.trax.app.models.live_track.save_track.SaveTrackResponse
import com.trax.app.models.login.request.LoginRequest
import com.trax.app.models.login.request.SocialLoginRequest
import com.trax.app.models.login.response.LoginResponse
import com.trax.app.models.logout.LogoutResponse
import com.trax.app.models.track.GetTracksResponse
import com.trax.app.models.track.single_track.GetSingleTrackResponse
import com.trax.app.models.userprofile.response.ProfileResponse
import com.trax.app.retrofit.Constants
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    //--------------------------------------------------
    // What the function does: Submits credentials to login the user.
    // When it is called: Triggered when standard credentials login is requested.
    // Why it is required: Validates login credentials and returns user token.
    //--------------------------------------------------
    @POST(Constants.LOGIN)
    suspend fun loginRequest(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    //--------------------------------------------------
    // What the function does: Submits OAuth credentials payload for social authorization.
    // When it is called: Triggered when social OAuth login is requested.
    // Why it is required: Validates third-party social auth credentials.
    //--------------------------------------------------
    @POST(Constants.LOGIN)
    suspend fun socialLoginRequest(
        @Body request: SocialLoginRequest
    ): Response<LoginResponse>

    //--------------------------------------------------
    // What the function does: Fetches details of the active user profile.
    // When it is called: Triggered upon loading the Profile view.
    // Why it is required: Retrieves name, email, and storage info.
    //--------------------------------------------------
    @GET(Constants.USER_PROFILE)
    suspend fun getUserProfile(
    ): Response<ProfileResponse>

    //--------------------------------------------------
    // What the function does: Revokes session tokens and signs out the user.
    // When it is called: Triggered when user logs out.
    // Why it is required: Securely ends sessions on server databases.
    //--------------------------------------------------
    @POST(Constants.LOGOUT)
    suspend fun logout(
        @Header("DeviceToken") deviceToken: String
    ): Response<LogoutResponse>

    //--------------------------------------------------
    // What the function does: Retrieves properties active leases info lists.
    // When it is called: Triggered when mapping leases list items in Home views.
    // Why it is required: Checks active land access licenses.
    //--------------------------------------------------
    @GET(Constants.LICENSE)
    suspend fun getLicenses(): Response<LicenseResponse>

    //--------------------------------------------------
    // What the function does: Requests server to delete the active user account.
    // When it is called: Triggered when delete account confirms.
    // Why it is required: Permanently deletes user registration profiles.
    //--------------------------------------------------
    @POST(Constants.DELETE_USER)
    suspend fun deleteUserRequest(
    ): Response<DeleteUserResponse>

    //--------------------------------------------------
    // What the function does: Uploads and persists coordinate walking tracks logs.
    // When it is called: Triggered when saving live recorded routes.
    // Why it is required: Saves tracked walking lines details to remote storage.
    //--------------------------------------------------
    @POST(Constants.SAVE_TRACK)
    suspend fun saveTrack(
        @Body request: SaveTrackRequest
    ): Response<SaveTrackResponse>

    //--------------------------------------------------
    // What the function does: Fetches paginated tracked routes history lists.
    // When it is called: Triggered when viewing tracks tab listing.
    // Why it is required: Loads historically recorded tracking routes list items.
    //--------------------------------------------------
    @GET(Constants.GET_TRACKS)
    suspend fun getTracks(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Response<GetTracksResponse>

    //--------------------------------------------------
    // What the function does: Fetches details matching a single track record.
    // When it is called: Triggered when viewing single saved map details.
    // Why it is required: Resolves individual coordinates sets for map plots.
    //--------------------------------------------------
    @GET(Constants.GET_TRACK)
    suspend fun getSingleTrack(
        @Path("trackId") trackId: Int
    ): Response<GetSingleTrackResponse>

    //--------------------------------------------------
    // What the function does: Requests removal of the specified track.
    // When it is called: Triggered inside single track deletes confirmations.
    // Why it is required: Deletes the target path from database indexes.
    //--------------------------------------------------
    @POST(Constants.DELETE_TRACK)
    suspend fun deleteTrack(
        @Path("trackId") trackId: Int
    ): Response<DeleteTrackResponse>

    //--------------------------------------------------
    // What the function does: Requests a PDF / PNG image export stream.
    // When it is called: Triggered during saved track image downloads.
    // Why it is required: Streams file bytes representing track graphics outputs.
    //--------------------------------------------------
    @GET(Constants.PDF_TRACK)
    suspend fun pdfTrack(
        @Path("trackId") trackId: Int
    ): Response<ResponseBody>
}