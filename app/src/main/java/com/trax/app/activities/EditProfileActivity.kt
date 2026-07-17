package com.trax.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.trax.app.databinding.ActivityEditProfileBinding


class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBarInset =
                insets.getInsets(WindowInsetsCompat.Type.statusBars()).top

            view.updatePadding(
                top = statusBarInset + 16
            )

            insets
        }


        initViews()
        initClicks()
    }

    private fun initViews() {

        val name = intent.getStringExtra("name") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        binding.tvName.text = name
        binding.etUsername.setText(name)
        binding.tvEmail.text = email
    }

    private fun initClicks() {

        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }
}