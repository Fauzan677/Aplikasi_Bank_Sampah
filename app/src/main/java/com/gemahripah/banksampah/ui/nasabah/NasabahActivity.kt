package com.gemahripah.banksampah.ui.nasabah

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.ActivityNasabahBinding
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable

class NasabahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNasabahBinding
    private val viewModel: NasabahViewModel by viewModels()
    private var pengguna: Pengguna? = null

    private val navHostFragment: NavHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_nasabah) as NavHostFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initBinding()
        retrievePengguna()
        setupViewModel()
        setupNavigation()
    }

    private fun initBinding() {
        binding = ActivityNasabahBinding.inflate(layoutInflater)
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
    }

    private fun retrievePengguna() {
        @Suppress("DEPRECATION")
        pengguna = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PENGGUNA", Pengguna::class.java)
        } else {
            intent.getParcelableExtra("EXTRA_PENGGUNA")
        }
    }

    private fun setupViewModel() {
        viewModel.setPengguna(pengguna)
    }

    private fun setupNavigation() {
        val navController = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_nasabah) as NavHostFragment).navController
        val navView: BottomNavigationView = binding.navView

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

        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.menu.findItem(destination.id)?.isChecked = true
        }
    }

    fun showNoInternetCard(show: Boolean) {
        binding.noConnectionCard.visibility = if (show) View.VISIBLE else View.GONE
        binding.navHostFragmentActivityNasabah.visibility = if (show) View.GONE else View.VISIBLE
    }

    fun showLoading(show: Boolean) {
        binding.loading.visibility = if (show) View.VISIBLE else View.GONE
    }
}