package com.trax.app.viewModels

import android.util.Base64
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.myoutdoor.agent.retrofit.ApiClient
import com.myoutdoor.agent.retrofit.ResponseHandler
import com.trax.app.models.login.request.LoginRequest
import com.trax.app.models.login.request.SocialLoginRequest
import com.trax.app.models.login.response.LoginResponse
import com.trax.app.utils.AppConstant
import kotlinx.coroutines.launch
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class LoginViewModel : BaseViewModel() {

    //==============================================================================
    // Variables
    //==============================================================================

    var passVisible = true
    val loginSuccess = MutableLiveData<LoginResponse?>()
    val socialLoginSuccess = MutableLiveData<LoginResponse?>()

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Dispatches a standard user login credentials request.
    //--------------------------------------------------
    fun loginRequest(
        email: String,
        password: String,
        deviceToken: String,
        deviceType: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClient()!!.loginRequest(
                    LoginRequest(
                        Email = email,
                        Password = password,
                        AuthenticationType = "orbis",
                        deviceToken = deviceToken,
                        deviceType = deviceType
                    )
                )

                isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.model != null) {
                        loginSuccess.value = body
                    } else {
                        apiError.value = body?.message ?: "Something went wrong"
                    }
                } else {
                    apiError.value = response.message()
                }
            } catch (e: Exception) {
                isLoading.value = false
                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message ?: "Network Error"
            }
        }
    }

    //--------------------------------------------------
    // Dispatches a social authentication login payload.
    //--------------------------------------------------
    fun socialLoginRequest(
        email: String,
        authKey: String,
        authType: String,
        deviceToken: String,
        deviceType: Int
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val response = ApiClient.getApiClient()!!.socialLoginRequest(
                    SocialLoginRequest(
                        Email = email,
                        AuthorizationKey = authKey,
                        AuthenticationType = authType,
                        deviceToken = deviceToken,
                        deviceType = deviceType
                    )
                )

                isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.model != null) {
                        socialLoginSuccess.value = body
                    } else {
                        apiError.value = body?.message ?: "Something went wrong"
                    }
                } else {
                    apiError.value = response.message()
                }
            } catch (e: Exception) {
                isLoading.value = false
                apiError.value = ResponseHandler()
                    .handleException<String>(e)
                    .message ?: "Network Error"
            }
        }
    }

    //==============================================================================
    // Cryptography Utility Functions
    //==============================================================================

    //--------------------------------------------------
    // Encrypts a text string using AES/CBC/PKCS7Padding specifications.
    //--------------------------------------------------
    fun encrypt(strToEncrypt: String): String? {
        return try {
            val ivParameterSpec = IvParameterSpec(
                Base64.decode(AppConstant.iv, Base64.DEFAULT)
            )

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(
                AppConstant.secretKey.toCharArray(),
                Base64.decode(AppConstant.salt, Base64.DEFAULT),
                10000,
                256
            )
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)

            Base64.encodeToString(
                cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)),
                Base64.DEFAULT
            )
        } catch (e: Exception) {
            null
        }
    }

    //--------------------------------------------------
    // Decrypts an AES/CBC/PKCS7Padding encrypted string.
    //--------------------------------------------------
    fun decrypt(strToDecrypt: String): String? {
        return try {
            val ivParameterSpec = IvParameterSpec(
                Base64.decode(AppConstant.iv, Base64.DEFAULT)
            )

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec = PBEKeySpec(
                AppConstant.secretKey.toCharArray(),
                Base64.decode(AppConstant.salt, Base64.DEFAULT),
                10000,
                256
            )
            val tmp = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

            String(
                cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT))
            )
        } catch (e: Exception) {
            null
        }
    }
}