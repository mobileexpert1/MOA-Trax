package com.trax.app.activities

import android.content.Intent
import android.os.Bundle
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

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var logoutDialog: BottomSheetDialog? = null
    private var deleteDialog: BottomSheetDialog? = null

    private val loader by lazy {
        ProgressView.getLoader(this)
    }

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes binding, applies window status bar top padding, and triggers profile api.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBar + 16)
            insets
        }

        initObservers()
        initClicks()
        callUserProfileApi()
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Initializes click listeners.
    //--------------------------------------------------
    private fun initClicks() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.tvEdit.setOnClickListener {
            navigateToEditProfile()
        }

        binding.ivEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.cardLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.cardOfflineMaps.setOnClickListener {
            startActivity(Intent(this, OfflineDownloadedMapsActivity::class.java))
        }

        binding.cardDelete.setOnClickListener {
            showDeleteDialog()
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
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }

        viewModel.unauthorizedError.observe(this) { isUnauthorized ->
            if (isUnauthorized == true) {
                com.trax.app.utils.SessionManager.showSessionExpiredDialog(this)
            }
        }

        viewModel.logoutSuccess.observe(this) { response ->
            response?.let {
                if (it.statusCode == 200) {
                    clearUserSessionAndRedirect()
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
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
                binding.tvEmail.text = user.email ?: ""
                binding.tvStorage.text = "124 MB · 1 maps · 4 tracks"

                PrefManager.putString(AppConstant.USER_NAME, fullName)
                PrefManager.putString(AppConstant.USER_EMAIL, user.email ?: "")
                PrefManager.putString(AppConstant.USER_PROFILE_ID, user.userProfileID.toString())
                PrefManager.putString(AppConstant.USER_ACCOUNT_ID, user.userAccountID.toString())
            }
        }

        viewModel.deleteSuccess.observe(this) { response ->
            response?.let {
                if (it.statusCode == 200) {
                    deleteDialog?.dismiss()
                    clearUserSessionAndRedirect()
                } else {
                    Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //==============================================================================
    // API Calls
    //==============================================================================

    //--------------------------------------------------
    // Dispatches user profile fetch API request.
    //--------------------------------------------------
    private fun callUserProfileApi() {
        val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
        viewModel.getProfile(token)
    }

    //==============================================================================
    // Navigation / Dialog Functions
    //==============================================================================

    //--------------------------------------------------
    // Routes user to EditProfileActivity screen.
    //--------------------------------------------------
    private fun navigateToEditProfile() {
        val intent = Intent(this, EditProfileActivity::class.java).apply {
            putExtra("name", binding.tvName.text.toString())
            putExtra("email", binding.tvEmail.text.toString())
        }
        startActivity(intent)
    }

    //--------------------------------------------------
    // Clears stored user session tokens and redirects to LoginActivity.
    //--------------------------------------------------
    private fun clearUserSessionAndRedirect() {
        PrefManager.putBoolean(AppConstant.IS_LOGIN, false)
        PrefManager.clearKey(AppConstant.CURRENT_USER)
        PrefManager.clearKey(AppConstant.AUTH_TOKEN)

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    //--------------------------------------------------
    // Renders the logout bottom sheet dialog.
    //--------------------------------------------------
    private fun showLogoutDialog() {
        logoutDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        val dialogBinding = BottomsheetLogoutDialogBinding.inflate(layoutInflater)
        logoutDialog?.setContentView(dialogBinding.root)

        dialogBinding.btnLogout.setOnClickListener {
            logoutDialog?.dismiss()
            val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
            val deviceToken = PrefManager.getString(AppConstant.DEVICE_TOKEN)
            viewModel.logoutApi(token, deviceToken)
        }

        dialogBinding.btnCancel.setOnClickListener {
            logoutDialog?.dismiss()
        }

        logoutDialog?.show()
    }

    //--------------------------------------------------
    // Renders the delete account bottom sheet dialog.
    //--------------------------------------------------
    private fun showDeleteDialog() {
        deleteDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        val dialogBinding = BottomsheetDeleteDialogBinding.inflate(layoutInflater)
        deleteDialog?.setContentView(dialogBinding.root)

        dialogBinding.btnDeleteAccount.setOnClickListener {
            val token = PrefManager.getString(AppConstant.AUTH_TOKEN)
            viewModel.deleteApi(token)
        }

        dialogBinding.btnCancel.setOnClickListener {
            deleteDialog?.dismiss()
        }

        deleteDialog?.show()
    }
}