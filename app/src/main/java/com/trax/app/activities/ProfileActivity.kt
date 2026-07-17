package com.trax.app.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trax.app.R
import com.trax.app.databinding.ActivityProfileBinding
import com.trax.app.databinding.BottomsheetDeleteDialogBinding
import com.trax.app.databinding.BottomsheetLogoutDialogBinding
import com.trax.app.progressBar.ProgressView
import com.trax.app.utils.AppConstant
import com.trax.app.utils.PrefManager
import com.trax.app.viewModels.ProfileViewModel

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding

    private val viewModel: ProfileViewModel by viewModels()

    private var logoutDialog: BottomSheetDialog? = null

    private var deleteDialog: BottomSheetDialog? = null

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->

            val statusBar =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            view.updatePadding(
                top = statusBar + 16
            )

            insets
        }

        initObserver()

        initClicks()

        callUserProfileApi()
    }

    //** all user profile api **//
    private fun callUserProfileApi(){
        var token = PrefManager.getString(AppConstant.AUTH_TOKEN)
        viewModel.getProfile(token)
    }

    private fun initObserver() {

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

        viewModel.logoutSuccess.observe(this) { response ->

            response?.let {

                if (it.statusCode == 200) {

                    PrefManager.putBoolean(
                        AppConstant.IS_LOGIN,
                        false
                    )

                    PrefManager.clearKey(
                        AppConstant.CURRENT_USER
                    )

                    PrefManager.clearKey(
                        AppConstant.AUTH_TOKEN
                    )

                    val intent = Intent(
                        this,
                        LoginActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        viewModel.profileSuccess.observe(this) { response ->

            response?.model?.let { user ->

                val fullName = listOfNotNull(
                    user.firstName?.takeIf { it.isNotBlank() },
                    user.lastName?.takeIf { it.isNotBlank() }
                ).joinToString(" ")

                binding.tvName.text = fullName
                binding.tvUserName.text = fullName

                binding.tvEmail.text =
                    user.email ?: ""

                binding.tvStorage.text =
                    "124 MB · 1 maps · 4 tracks"

                // Save latest profile if required

                PrefManager.putString(
                    AppConstant.USER_NAME,
                    "${user.firstName} ${user.lastName}"
                )

                PrefManager.putString(
                    AppConstant.USER_EMAIL,
                    user.email ?: ""
                )

                PrefManager.putString(
                    AppConstant.USER_PROFILE_ID,
                    user.userProfileID.toString()
                )

                PrefManager.putString(
                    AppConstant.USER_ACCOUNT_ID,
                    user.userAccountID.toString()
                )
            }
        }

        viewModel.deleteSuccess.observe(this) { response ->

            response?.let {

                if (it.statusCode == 200) {

                    deleteDialog?.dismiss()

                    PrefManager.putBoolean(
                        AppConstant.IS_LOGIN,
                        false
                    )

                    PrefManager.clearKey(
                        AppConstant.CURRENT_USER
                    )

                    PrefManager.clearKey(
                        AppConstant.AUTH_TOKEN
                    )

                    val intent = Intent(
                        this,
                        LoginActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun initClicks() {

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvEdit.setOnClickListener {

            val intent = Intent(this, EditProfileActivity::class.java)

            intent.putExtra(
                "name",
                binding.tvName.text.toString()
            )

            intent.putExtra(
                "email",
                binding.tvEmail.text.toString()
            )

            startActivity(intent)
        }

        binding.ivEditProfile.setOnClickListener {

            val intent = Intent(this, EditProfileActivity::class.java)

            intent.putExtra(
                "name",
                binding.tvName.text.toString()
            )

            intent.putExtra(
                "email",
                binding.tvEmail.text.toString()
            )

            startActivity(intent)
        }

        binding.cardLogout.setOnClickListener {

            showLogoutDialog()
        }
    
        binding.cardDelete.setOnClickListener {

            showDeleteDialog()
        }
    }

    //** show logout dialog
    private fun showLogoutDialog() {

        logoutDialog = BottomSheetDialog(
            this,
            R.style.AppBottomSheetDialogTheme
        )

        val dialogBinding =
            BottomsheetLogoutDialogBinding.inflate(layoutInflater)

        logoutDialog?.setContentView(dialogBinding.root)

        dialogBinding.btnLogout.setOnClickListener {

            logoutDialog?.dismiss()

            val token =
                PrefManager.getString(AppConstant.AUTH_TOKEN)

            val deviceToken =
                PrefManager.getString(AppConstant.DEVICE_TOKEN)

            viewModel.logoutApi(
                token,
                deviceToken
            )
        }

        dialogBinding.btnCancel.setOnClickListener {
            logoutDialog?.dismiss()
        }

        logoutDialog?.show()
    }

    //** show delete dialog
    private fun showDeleteDialog() {

        deleteDialog = BottomSheetDialog(
            this,
            R.style.AppBottomSheetDialogTheme
        )

        val dialogBinding =
            BottomsheetDeleteDialogBinding.inflate(layoutInflater)

        deleteDialog?.setContentView(dialogBinding.root)

        dialogBinding.btnDeleteAccount.setOnClickListener {

            val token =
                PrefManager.getString(AppConstant.AUTH_TOKEN)

            viewModel.deleteApi(
                token
            )
        }

        dialogBinding.btnCancel.setOnClickListener {
            deleteDialog?.dismiss()
        }

        deleteDialog?.show()
    }
}