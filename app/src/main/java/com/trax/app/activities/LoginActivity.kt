package com.trax.app.activities

import android.content.Intent

import android.os.Bundle

import android.text.InputType
import android.util.Log

import android.widget.Toast

import androidx.activity.viewModels

import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat

import androidx.core.widget.doOnTextChanged

import com.trax.app.R
import com.trax.app.base.BaseActivity

import com.trax.app.databinding.ActivityLoginBinding

import com.trax.app.progressBar.ProgressView

import com.trax.app.utils.AppConstant

import com.trax.app.utils.PrefManager

import com.trax.app.utils.Utility

import com.trax.app.viewModels.LoginViewModel

class LoginActivity : BaseActivity() {


    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels()

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var deviceToken = PrefManager.getString(AppConstant.DEVICE_TOKEN)
        Log.e("call","DEVICE TOKEN:: "+deviceToken)

        initObserver()
        initClickListener()
    }

    private fun initClickListener() {

        binding.llHideUnhidePass.setOnClickListener {

            if (viewModel.passVisible) {

                viewModel.passVisible = false

                binding.ivUnhideHide.setImageResource(
                    R.drawable.ic_unhide
                )

                binding.etPass.inputType =
                    InputType.TYPE_CLASS_TEXT

            } else {

                viewModel.passVisible = true

                binding.ivUnhideHide.setImageResource(
                    R.drawable.ic_hide
                )

                binding.etPass.inputType =
                    InputType.TYPE_CLASS_TEXT or
                            InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            binding.etPass.setSelection(
                binding.etPass.text?.length ?: 0
            )
        }

        binding.imageView.setOnClickListener {
            toggleRememberMe()
        }

        binding.tvRememberMe.setOnClickListener {
            toggleRememberMe()
        }

        binding.btnLogin.setOnClickListener {

            when {

                !Utility.isValidEmail(
                    binding.etUsername.text.toString().trim()
                )!! -> {

                    Toast.makeText(
                        this,
                        "Please enter valid email address.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                binding.etPass.text.toString().trim().isEmpty() -> {

                    Toast.makeText(
                        this,
                        "Please enter valid password.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {

                    if (AppConstant.CredentialsRemember) {

                        PrefManager.putString(
                            AppConstant.EMAIL,
                            binding.etUsername.text.toString()
                        )

                        PrefManager.putString(
                            AppConstant.PASSWORD,
                            binding.etPass.text.toString()
                        )

                        PrefManager.putString(
                            AppConstant.ISREMEMBER,
                            "1"
                        )

                    } else {

                        PrefManager.putString(
                            AppConstant.EMAIL,
                            ""
                        )

                        PrefManager.putString(
                            AppConstant.PASSWORD,
                            ""
                        )

                        PrefManager.putString(
                            AppConstant.ISREMEMBER,
                            "0"
                        )
                    }

                    viewModel.loginRequest(
                        email = binding.etUsername.text.toString(),
                        password = binding.etPass.text.toString(),
                        deviceToken =  PrefManager.getString(AppConstant.DEVICE_TOKEN),
                        deviceType = 2
                    )
                }
            }
        }
    }

    private fun initObserver() {

        val email =
            PrefManager.getString(AppConstant.EMAIL) ?: ""

        val pass =
            PrefManager.getString(AppConstant.PASSWORD) ?: ""

        val isRemember =
            PrefManager.getString(AppConstant.ISREMEMBER) ?: "0"

        binding.etUsername.setText(email)
        binding.etPass.setText(pass)

        if (isRemember == "1") {

            AppConstant.CredentialsRemember = true

            binding.imageView.setBackgroundResource(
                R.drawable.ic_check
            )

        } else {

            AppConstant.CredentialsRemember = false

            binding.imageView.setBackgroundResource(
                R.drawable.ic_uncheck
            )
        }

        viewModel.isLoading.observe(this) {

            if (it == true) {
                loader.show()
            } else {
                loader.dismiss()
            }
        }

        viewModel.apiError.observe(this) {

            Toast.makeText(
                this,
                it,
                Toast.LENGTH_SHORT
            ).show()
        }

        /*
        viewModel.loginSuccess.observe(this) { response ->

            response?.model?.let { user ->

                val token =
                    viewModel.encrypt(user.token)

                PrefManager.putString(
                    AppConstant.AUTH_TOKEN,
                    token
                )

                PrefManager.putBoolean(
                    AppConstant.IS_LOGIN,
                    true
                )

                PrefManager.putString(
                    AppConstant.USER_NAME,
                    user.name
                )

                PrefManager.putString(
                    AppConstant.USER_EMAIL,
                    user.email
                )

                PrefManager.putString(
                    AppConstant.USER_ACCOUNT_ID,
                    user.userAccountID.toString()
                )

                PrefManager.putString(
                    AppConstant.CURRENT_USER,
                    user.userAccountID.toString()
                )

                PrefManager.putString(
                    AppConstant.USER_PROFILE_ID,
                    user.userProfileID.toString()
                )

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    ).apply {

                        putExtra(
                            "user",
                            user.userAccountID.toString()
                        )
                    }
                )

                finish()
            }
        }
         */

        viewModel.loginSuccess.observe(this) { response ->

            response?.model?.let { token ->

                PrefManager.putString(
                    AppConstant.AUTH_TOKEN,
                    token
                )

                PrefManager.putBoolean(
                    AppConstant.IS_LOGIN,
                    true
                )

                startActivity(
                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

                finish()
            }
        }

        binding.etUsername.doOnTextChanged { _, _, _, _ ->
            updateLoginButton()
        }

        binding.etPass.doOnTextChanged { _, _, _, _ ->
            updateLoginButton()
        }
    }

    private fun toggleRememberMe() {

        AppConstant.CredentialsRemember =
            !AppConstant.CredentialsRemember

        binding.imageView.setBackgroundResource(
            if (AppConstant.CredentialsRemember)
                R.drawable.ic_check
            else
                R.drawable.ic_uncheck
        )
    }

    private fun updateLoginButton() {

        val isValid =
            Utility.isValidEmail(
                binding.etUsername.text.toString()
            ) == true &&
                    binding.etPass.text.toString()
                        .isNotEmpty()

        binding.btnLogin.background =
            ContextCompat.getDrawable(
                this,
                if (isValid)
                    R.drawable.bg_gradient_btn_round_8
                else
                    R.drawable.theme_bg_solid_btn_grey_round_8
            )
    }

}

