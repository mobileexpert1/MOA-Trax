package com.trax.app.viewModels

import android.app.Activity
import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import appentus.datasource.api.AppApiService
import appentus.datasource.api.AppNetworkRepository
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.trax.app.MyApp
import com.trax.app.R
import com.trax.app.utils.AppConstant
import com.trax.app.utils.Utility
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class LoginViewModel() : ViewModel() {

    val RC_SIGN_IN = 1111
    var passVisible = true
    private val networkRepository = AppNetworkRepository()
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var callBackManager: CallbackManager

    fun initGoogle(activity: Activity) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.client_outh_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun googleLogin(activity: Activity) {
        val signInIntent = mGoogleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        mGoogleSignInClient.signOut()
    }


    fun initFb(mFacebookCallback: FacebookCallback<LoginResult>) {
        callBackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().registerCallback(callBackManager, mFacebookCallback)
    }


    fun fbLogin(activity: Activity) {
        LoginManager.getInstance().loginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK

        LoginManager.getInstance().logInWithReadPermissions(
            activity,
            listOf("public_profile","email")
        )
    }

    fun loginWithSocialMedia(map: HashMap<String, String>) = MyApp.getInstance()?.getNetworkRepo()?.loginWithSocialMedia(
        AppApiService.API_LOGIN,
        Utility.convertToRequestBody(map)
    )

    fun userLogin(email:String,pass:String,type:String,key:String) = MyApp.getInstance()?.getNetworkRepo()?.userLogin(AppApiService.API_LOGIN, Utility.convertToRequestBody(mapOf(
        "Email" to email,
        "Password" to pass,
        "AuthenticationType" to type,
        "AuthorizationKey" to key
    )))

    //encrypt

    fun encrypt(strToEncrypt: String) :  String?
    {
        try
        {
            val ivParameterSpec = IvParameterSpec(Base64.decode(AppConstant.iv, Base64.DEFAULT))

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(AppConstant.secretKey.toCharArray(), Base64.decode(AppConstant.salt, Base64.DEFAULT), 10000, 256)
            val tmp = factory.generateSecret(spec)
            val secretKey =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            return Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
        }
        catch (e: Exception)
        {
            println("Error while encrypting: $e")
        }
        return null
    }

    fun decrypt(strToDecrypt : String) : String? {
        try
        {

            val ivParameterSpec =  IvParameterSpec(Base64.decode(AppConstant.iv, Base64.DEFAULT))

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(AppConstant.secretKey.toCharArray(), Base64.decode(AppConstant.salt, Base64.DEFAULT), 10000, 256)
            val tmp = factory.generateSecret(spec);
            val secretKey =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
            return  String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)))
        }
        catch (e : Exception) {
            println("Error while decrypting: $e");
        }
        return null
    }
}