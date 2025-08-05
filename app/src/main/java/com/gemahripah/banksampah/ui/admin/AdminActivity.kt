package com.gemahripah.banksampah.ui.admin

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.ActivityAdminBinding
import com.gemahripah.banksampah.utils.Reloadable
import androidx.core.view.isVisible
import com.gemahripah.banksampah.utils.NetworkUtil

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()

    private val navHostFragment: NavHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_admin) as NavHostFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.noConnectionCard.setOnClickListener {
            binding.noConnectionCard.visibility = View.GONE
            showLoading(true)

            Handler(Looper.getMainLooper()).postDelayed({
                if (NetworkUtil.isInternetAvailable(this)) {
                    showNoInternetCard(false)

                    val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                    if (currentFragment is Reloadable) {
                        currentFragment.reloadData()
                    }
                } else {
                    showNoInternetCard(true)
                }

                showLoading(false)
            }, 1000)
        }

        getPenggunaFromIntent()
        setupBottomNavigation()
        observeNavigation()
    }

    private fun getPenggunaFromIntent() {
        val pengguna = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PENGGUNA", Pengguna::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("EXTRA_PENGGUNA")
        }

        pengguna?.let {
            viewModel.setPengguna(it)
        }
    }

    private fun setupBottomNavigation() {
        val navView: BottomNavigationView = binding.navView
        val navController = navHostFragment.navController

        navView.setOnItemSelectedListener { item ->
            val destinationId = item.itemId
            val currentDestinationId = navController.currentDestination?.id

            if (currentDestinationId == destinationId) {
                val isConnected = NetworkUtil.isInternetAvailable(this)
                val isNoConnectionVisible = binding.noConnectionCard.isVisible

                when {
                    !isConnected && !isNoConnectionVisible -> {
                        showNoInternetCard(true)
                    }

                    isConnected && !isNoConnectionVisible -> {
                        // Ada koneksi & card belum ditampilkan → langsung reload
                        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                        if (currentFragment is Reloadable) {
                            currentFragment.reloadData()
                        }
                    }

                    isConnected && isNoConnectionVisible -> {
                        // Ada koneksi & card sedang ditampilkan → loading → reload → sembunyikan card
                        binding.noConnectionCard.visibility = View.GONE
                        showLoading(true)

                        Handler(Looper.getMainLooper()).postDelayed({
                            val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                            if (currentFragment is Reloadable) {
                                currentFragment.reloadData()
                            }
                            showNoInternetCard(false)
                            showLoading(false)
                        }, 1000)
                    }

                    !isConnected && isNoConnectionVisible -> {
                        // Tidak ada koneksi & card sudah ditampilkan → loading sebentar
                        binding.noConnectionCard.visibility = View.GONE
                        showLoading(true)

                        Handler(Looper.getMainLooper()).postDelayed({
                            showNoInternetCard(true)
                            showLoading(false)
                        }, 1000)
                    }
                }

                return@setOnItemSelectedListener true
            }

            navController.popBackStack(navController.graph.startDestinationId, false)
            navController.navigate(destinationId)

            true
        }
    }

    private fun observeNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_admin) as NavHostFragment
        val navController = navHostFragment.navController
        val navView: BottomNavigationView = binding.navView

        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.menu.findItem(destination.id)?.isChecked = true
        }
    }

    fun showNoInternetCard(show: Boolean) {
        binding.noConnectionCard.visibility = if (show) View.VISIBLE else View.GONE
        binding.navHostFragmentActivityAdmin.visibility = if (show) View.GONE else View.VISIBLE
    }

    fun showLoading(show: Boolean) {
        binding.loading.visibility = if (show) View.VISIBLE else View.GONE
    }

}