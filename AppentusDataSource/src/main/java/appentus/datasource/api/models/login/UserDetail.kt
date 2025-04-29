package appentus.datasource.api.models.login

data class UserDetail(
    val userProfileID: Int,
    val userAccountID: Int,
    val firstName: String,
    val lastName: String,
    val streetAddress: String,
    val st: String,
    val zip: String,
    val phone: String,
    val groupName: String,
    val isBlacklisted: Boolean,
    val isEmailVerified: Boolean,
    val email: String,
    val getNotifications: Boolean,
    val authenticationType: String,
    val message: String,
    val token: String,
    val authToken: String,
    val dateCreated: String,
    val dateLastLogin: String,
    val accountType: Int,
    val description: String,
    val name: String,
    val status: Int
)
