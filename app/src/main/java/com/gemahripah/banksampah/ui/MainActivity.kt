package com.gemahripah.banksampah.ui

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.databinding.ActivityMainBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import com.gemahripah.banksampah.utils.NetworkUtil

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (NetworkUtil.isInternetAvailable(this)) {
            viewModel.checkSession()
        } else {
            binding.loading.visibility = View.GONE
            binding.noConnectionCard.visibility = View.VISIBLE
        }

        binding.noConnectionCard.setOnClickListener {
            binding.noConnectionCard.visibility = View.GONE
            binding.loading.visibility = View.VISIBLE

            Handler(Looper.getMainLooper()).postDelayed({
                if (NetworkUtil.isInternetAvailable(this)) {
                    viewModel.checkSession()
                } else {
                    binding.loading.visibility = View.GONE
                    binding.noConnectionCard.visibility = View.VISIBLE
                }
            }, 1000)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { isLoading ->
            binding.loading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.pengguna.observe(this) { pengguna ->
            if (pengguna != null) {
                val intent = if (pengguna.pgnIsAdmin == true) {
                    Intent(this, AdminActivity::class.java)
                } else {
                    Intent(this, NasabahActivity::class.java)
                }

                intent.putExtra("EXTRA_PENGGUNA", pengguna)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                Toast.makeText(this, "Login berhasil", Toast.LENGTH_SHORT).show()
                startActivity(intent)
            } else {
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