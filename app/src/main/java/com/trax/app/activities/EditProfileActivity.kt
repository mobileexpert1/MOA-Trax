package com.trax.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.trax.app.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    //==============================================================================
    // Variables
    //==============================================================================

    private lateinit var binding: ActivityEditProfileBinding

    //==============================================================================
    // Lifecycle Methods
    //==============================================================================

    //--------------------------------------------------
    // Initializes binding, configures window status bar top padding, views, and clicks.
    //--------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusBarInset + 16)
            insets
        }

        initViews()
        initClicks()
    }

    //==============================================================================
    // Initializations
    //==============================================================================

    //--------------------------------------------------
    // Renders active user info from intent extras parameters.
    //--------------------------------------------------
    private fun initViews() {
        val name = intent.getStringExtra("name") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        binding.tvName.text = name
        binding.etUsername.setText(name)
        binding.tvEmail.text = email
    }

    //--------------------------------------------------
    // Initializes click listeners.
    //--------------------------------------------------
    private fun initClicks() {
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}