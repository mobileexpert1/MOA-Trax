package com.trax.app.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import appentus.datasource.api.BaseDataSource
import appentus.datasource.api.models.login.FacebookBean
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.trax.app.R
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.utils.Utility
import com.trax.app.viewModels.LoginViewModel
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.custom_progress_dialog.*
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


class LoginActivity : AppCompatActivity() {
    private val viewModel: LoginViewModel by viewModels()
    private val loader by lazy { ProgressView.getLoader(this) }
    public var currentUser: String ? =null
    private var context: Context? = null
    private val mFaceBookCallback: FacebookCallback<LoginResult> = object :
        FacebookCallback<LoginResult> {
        override fun onSuccess(result: LoginResult?) {
            val token = AccessToken.getCurrentAccessToken()
            val request = GraphRequest.newMeRequest(result!!.accessToken) { json: JSONObject, _: GraphResponse? ->
                    try {
                        val bean = Gson().fromJson<FacebookBean>(json.toString(), FacebookBean::class.java)
                        val params = hashMapOf(
                            "Email" to bean.email,
                            "Password" to "",
                            "AuthenticationType" to "facebook",
                            "AuthorizationKey" to token.token
                        )
                        viewModel.loginWithSocialMedia(params)!!.observe(this@LoginActivity, Observer {
                            when {
                                it!!.status == BaseDataSource.Resource.Status.SUCCESS -> {
                                    //Toast.makeText(this@LoginActivity,it.message,Toast.LENGTH_SHORT).show()
                                    if(it.data!!.model == null){
                                        loader.dismiss()
                                        Toast.makeText(this@LoginActivity,it.data!!.message,Toast.LENGTH_SHORT).show()
                                    }
                                    else{
                                        loader.dismiss()
                                        val token = viewModel.encrypt(it.data!!.model.token)
                                        PrefManager.putString(AppConstant.AUTH_TOKEN,token)
                                        PrefManager.putBoolean(AppConstant.IS_LOGIN,true)
                                        PrefManager.putString(AppConstant.USER_NAME,it.data!!.model.firstName+" "+it.data!!.model.lastName)
                                        PrefManager.putString(AppConstant.USER_EMAIL,it.data!!.model.email)
                                        PrefManager.putString(AppConstant.USER_PROFILE_ID,it.data!!.model.userProfileID.toString())
                                        PrefManager.putString(AppConstant.CURRENT_USER,it.data!!.model.userAccountID.toString())
                                        PrefManager.putString(AppConstant.USER_ACCOUNT_ID,it.data!!.model.userAccountID.toString())

                                      //  currentUser=it.data!!.model.userProfileID.toString();

                                        //AppConstant.authToken = it.data!!.model.token
                                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                        intent.putExtra("user",it.data!!.model.userAccountID.toString())
                                        startActivity(intent)
                                        finish()
                                    }

                                }
                                it.status == BaseDataSource.Resource.Status.ERROR -> {
                                    loader.dismiss()
                                    Toast.makeText(this@LoginActivity,it.message,Toast.LENGTH_SHORT).show()
                                }
                                it.status == BaseDataSource.Resource.Status.LOADING -> {
                                    loader.show()
                                }
                            }
                        })
                        LoginManager.getInstance().logOut()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            val parameter = Bundle()
            parameter.putString("fields", "id,name,email,gender,picture.type(large)")
            request.parameters = parameter
            request.executeAsync()
        }

        override fun onCancel() {
            loader.dismiss()
        }

        override fun onError(error: FacebookException?) {

        }

    }
    fun getLoginContext(): LoginActivity {
        return this
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        this.context = context
        viewModel.initGoogle(this)
        viewModel.initFb(mFaceBookCallback)
        initObserver()
        initClickListener()

        try {
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }

    private fun initClickListener() {

        llHideUnhidePass.setOnClickListener {

            if (viewModel.passVisible) {
                viewModel.passVisible = false
                ivUnhideHide.setImageDrawable(resources.getDrawable(R.drawable.ic_unhide))
                etPass.inputType = InputType.TYPE_CLASS_TEXT
                etPass.setSelection(etPass.text.length)

            } else {
                viewModel.passVisible = true
                ivUnhideHide.setImageDrawable(resources.getDrawable(R.drawable.ic_hide))
                etPass.inputType =
                    InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                etPass.setSelection(etPass.text.length)


            }
        }

        imageView.setOnClickListener {
            if (!AppConstant.CredentialsRemember){
                AppConstant.CredentialsRemember= true
                imageView.setBackgroundResource(R.drawable.ic_check)
            }
            else{
                AppConstant.CredentialsRemember = false
                imageView.setBackgroundResource(R.drawable.ic_uncheck)
            }
        }
        tvRememberMe.setOnClickListener {
            if (!AppConstant.CredentialsRemember){
                AppConstant.CredentialsRemember= true
                imageView.setBackgroundResource(R.drawable.ic_check)
            }
            else{
                AppConstant.CredentialsRemember = false
                imageView.setBackgroundResource(R.drawable.ic_uncheck)
            }
        }

        btnLogin.setOnClickListener {

        when {
            !etUsername.text.matches("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+".toRegex()) -> {
                Toast.makeText(this,"Please enter valid email address.",Toast.LENGTH_SHORT).show()
            }
            etPass.text.trim().isNullOrEmpty() -> {
                Toast.makeText(this,"Please enter valid password.",Toast.LENGTH_SHORT).show()
            }
            else -> {

                if(AppConstant.CredentialsRemember){
                    PrefManager.putString(AppConstant.EMAIL,etUsername.text.toString())
                    PrefManager.putString(AppConstant.PASSWORD,etPass.text.toString())
                    PrefManager.putString(AppConstant.ISREMEMBER,"1")
                }
                else{
                    PrefManager.putString(AppConstant.EMAIL,"")
                    PrefManager.putString(AppConstant.PASSWORD,"")
                    PrefManager.putString(AppConstant.ISREMEMBER,"0")
                }

                    viewModel.userLogin(etUsername.text.toString(),etPass.text.toString(),"orbis","abc")!!.observe(this,
                        Observer {
                            when {
                                it!!.status == BaseDataSource.Resource.Status.SUCCESS -> {
                                    if(it.data!!.model == null){
                                        loader.dismiss()
                                        Toast.makeText(this,it.data!!.message,Toast.LENGTH_SHORT).show()
                                    }
                                    else{
                                        loader.dismiss()
                                        val token = viewModel.encrypt(it.data!!.model.token)
                                        PrefManager.putString(AppConstant.AUTH_TOKEN,token)
                                        PrefManager.putBoolean(AppConstant.IS_LOGIN,true)
                                        PrefManager.putString(AppConstant.USER_NAME,it.data!!.model.name)
                                        PrefManager.putString(AppConstant.USER_EMAIL,it.data!!.model.email)
                                        PrefManager.putString(AppConstant.USER_ACCOUNT_ID,it.data!!.model.userAccountID.toString())
                                        PrefManager.putString(AppConstant.CURRENT_USER,it.data!!.model.userAccountID.toString())
                                        PrefManager.putString(AppConstant.USER_PROFILE_ID,it.data!!.model.userProfileID.toString())
                                        val intent = Intent(this, MainActivity::class.java)
                                        intent.putExtra("user",it.data!!.model.userAccountID.toString())
                                       // AppConstant.CURRENT_USER =it.data!!.model.userAccountID.toString()
                                        startActivity(intent)
                                        finish()
                                    }

                                }
                                it.status == BaseDataSource.Resource.Status.ERROR -> {
                                    loader.dismiss()
                                    Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
                                }
                                it.status == BaseDataSource.Resource.Status.LOADING -> {
                                    loader.show()
                                }
                            }

                        })
                }
            }
        }

        ivGoogle.setOnClickListener {
            etUsername.setText("")
            etPass.setText("")
            viewModel.googleLogin(this)
        }

        ivFace.setOnClickListener {
            etUsername.setText("")
            etPass.setText("")
            viewModel.fbLogin(this)

        }

    }

    private fun initObserver() {

        val email = PrefManager.getString(AppConstant.EMAIL).toString()
        val pass = PrefManager.getString(AppConstant.PASSWORD).toString()
        val isRemember = PrefManager.getString(AppConstant.ISREMEMBER).toString()
        if (email != ""){
            etUsername.setText(email)
        }
        if (pass != "") {
            etPass.setText(pass)
        }
        if(isRemember == "1"){
            imageView.setBackgroundResource(R.drawable.ic_check)
            btnLogin.background =
                ContextCompat.getDrawable(this, R.drawable.bg_gradient_btn_round_8)
        }
        else{
            imageView.setBackgroundResource(R.drawable.ic_uncheck)
            btnLogin.background =
                ContextCompat.getDrawable(this, R.drawable.theme_bg_solid_btn_grey_round_8)
        }

        etUsername.doOnTextChanged{text, start, before, count ->
            if (Utility.isValidEmail(etUsername.text.toString().trim()) == true) {
                btnLogin.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_gradient_btn_round_8)
            }
            else{
                btnLogin.background =
                    ContextCompat.getDrawable(this, R.drawable.theme_bg_solid_btn_grey_round_8)
            }
        }

        etPass.doOnTextChanged{text, start, before, count ->
            if(text.isNullOrEmpty()){
                btnLogin.background =
                    ContextCompat.getDrawable(this, R.drawable.theme_bg_solid_btn_grey_round_8)
            }
            else{
                btnLogin.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_gradient_btn_round_8)
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.callBackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == viewModel.RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let { it ->
                val params = hashMapOf<String, String>("Email" to it.email!!,
                        "Password" to "",
                        "AuthenticationType" to "google",
                        "AuthorizationKey" to it.id!!
                    )
                    viewModel.loginWithSocialMedia(params)!!.observe(this@LoginActivity, Observer {
                        when {
                            it!!.status == BaseDataSource.Resource.Status.SUCCESS -> {
                                if(it.data!!.model == null){
                                    loader.dismiss()
                                    Toast.makeText(this,it.data!!.message,Toast.LENGTH_SHORT).show()
                                }
                                else{
                                    loader.dismiss()
                                    val token = viewModel.encrypt(it.data!!.model.token)
                                    PrefManager.putString(AppConstant.AUTH_TOKEN,token)
                                    PrefManager.putBoolean(AppConstant.IS_LOGIN,true)
                                    PrefManager.putString(AppConstant.USER_NAME,it.data!!.model.firstName+" "+it.data!!.model.lastName)
                                    PrefManager.putString(AppConstant.USER_EMAIL,it.data!!.model.email)
                                    PrefManager.putString(AppConstant.USER_ACCOUNT_ID,it.data!!.model.userAccountID.toString())
                                    PrefManager.putString(AppConstant.USER_PROFILE_ID,it.data!!.model.userProfileID.toString())
                                    val intent = Intent(this, MainActivity::class.java)
                                    intent.putExtra("user",it.data!!.model.userAccountID.toString())

                                    startActivity(intent)
                                    finish()
                                }

                            }
                            it.status == BaseDataSource.Resource.Status.ERROR -> {
                                loader.dismiss()
                                Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
                            }
                            it.status == BaseDataSource.Resource.Status.LOADING -> {
                                loader.show()
                            }
                        }
                    })
                }
        }
        catch (e: ApiException) {
        }
    }
}