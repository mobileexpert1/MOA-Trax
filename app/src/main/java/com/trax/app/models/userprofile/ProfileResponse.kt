package com.trax.app.models.userprofile.response


data class ProfileResponse(
    val statusCode: Int,
    val message: String,
    val model: UserProfile?
)

data class UserProfile(
    val userProfileID: Int,
    val userAccountID: Int,
    val firstName: String?,
    val lastName: String?,
    val streetAddress: String?,
    val city: String?,
    val st: String?,
    val zip: String?,
    val phone: String?,
    val groupName: String?,
    val clubName: String?,
    val email: String?,
    val getNotifications: Boolean,
    val authenticationType: String?,
    val isUserProfileComplete: Boolean,
    val stateName: String?,
    val status: Int,
    val dateCreated: String?
)
