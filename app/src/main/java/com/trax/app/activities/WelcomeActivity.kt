package com.trax.app.activities

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.trax.app.databinding.ActivityWelcomeBinding
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.LoginViewModel
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class WelcomeActivity : AppCompatActivity() {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityWelcomeBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    //--------------------------------------------------
    // Handles Google Sign-In results.
    //--------------------------------------------------
    private val launcher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.let {
                    val authKey = it.idToken
                        ?: it.serverAuthCode
                        ?: it.id
                        ?: ""

                    viewModel.socialLoginRequest(
                        email = it.email ?: "",
                        authType = "google",
                        authKey = authKey,
                        deviceToken = PrefManager.getString(AppConstant.DEVICE_TOKEN),
                        deviceType = 2
                    )
                }
            } catch (e: Exception) {
                Log.e("GoogleLogin", e.toString())
            }
        }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes window decorations, configurations, and insets.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initGoogle()
        initObservers()
        initClicks()
        printHashKey()

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutBottom) { view, insets ->
            val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navBar.bottom + 20.dpToPx()
            )
            insets
        }
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Configures Google Sign-In client options.
    //--------------------------------------------------
    private fun initGoogle() {
        val gso = GoogleSignInOptions.Builder(
            GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    //--------------------------------------------------
    // Initializes click listeners.
    //--------------------------------------------------
    private fun initClicks() {
        binding.rlGoogle.setOnClickListener {
            launcher.launch(googleSignInClient.signInIntent)
        }

        binding.rlEmail.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
            finish()
        }
    }

    //--------------------------------------------------
    // Initializes LiveData observers.
    //--------------------------------------------------
    private fun initObservers() {
        viewModel.isLoading.observe(this) {
            if (it == true) {
                loader.show()
            } else {
                loader.dismiss()
            }
        }

        viewModel.apiError.observe(this) {
            // Error logs can be observed or processed here.
        }

        viewModel.socialLoginSuccess.observe(this) { response ->
            val token = response?.model
            if (!token.isNullOrEmpty()) {
                PrefManager.putString(AppConstant.AUTH_TOKEN, token)
                PrefManager.putBoolean(AppConstant.IS_LOGIN, true)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }

    //==============================================================================
    // Utility Functions
    //==============================================================================

    //--------------------------------------------------
    // Converts dp density values into pixels.
    //--------------------------------------------------
    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    //--------------------------------------------------
    // Prints signature hash keys for console configuration.
    //--------------------------------------------------
    private fun printHashKey() {
        try {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d(
                    "KeyHash",
                    Base64.encodeToString(
                        md.digest(),
                        Base64.DEFAULT
                    )
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }
}