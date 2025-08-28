package com.gemahripah.banksampah.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.databinding.ActivityMainBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cek koneksi & session awal
        if (NetworkUtil.isInternetAvailable(this)) {
            viewModel.checkSession()
        } else {
            binding.loading.isVisible = false
            binding.noConnectionCard.isVisible = true
        }

        // Tombol retry koneksi
        binding.noConnectionCard.setOnClickListener {
            binding.noConnectionCard.isVisible = false
            binding.loading.isVisible = true

            lifecycleScope.launch {
                delay(1000)
                if (NetworkUtil.isInternetAvailable(this@MainActivity)) {
                    viewModel.checkSession()
                } else {
                    binding.loading.isVisible = false
                    binding.noConnectionCard.isVisible = true
                }
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loading.isVisible = isLoading
        }

        viewModel.pengguna.observe(this) { pengguna ->
            if (pengguna != null) {
                val next = if (pengguna.pgnIsAdmin == true) {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, NasabahActivity::class.java)
                }.apply {
                    putExtra("EXTRA_PENGGUNA", pengguna)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }

                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                startActivity(next)
            } else {
                // Tampilkan konten landing/login bila tidak ada session
                binding.layoutKonten.visibility = View.VISIBLE
            }
        }

        viewModel.error.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
