package appentus.datasource.api.models.login

import com.google.gson.annotations.SerializedName

data class FacebookBean(
    @SerializedName("email")
    val email: String,
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("picture")
    val picture: Picture
) {
    data class Picture(
        @SerializedName("data")
        val `data`: Data
    ) {
        data class Data(
            @SerializedName("height")
            val height: Int,
            @SerializedName("is_silhouette")
            val isSilhouette: Boolean,
            @SerializedName("url")
            val url: String,
            @SerializedName("width")
            val width: Int
        )
    }
}