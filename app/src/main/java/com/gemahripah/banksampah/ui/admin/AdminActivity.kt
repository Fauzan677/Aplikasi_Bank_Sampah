package com.gemahripah.banksampah.ui.admin

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.ActivityAdminBinding
import com.gemahripah.banksampah.utils.Reloadable
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.utils.NetworkUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val viewModel: AdminViewModel by viewModels()

    private val navHostFragment: NavHostFragment by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_admin) as NavHostFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.noConnectionCard.setOnClickListener {
            binding.noConnectionCard.visibility = View.GONE
            showLoading(true)

            lifecycleScope.launch {
                kotlinx.coroutines.delay(1000)
                if (NetworkUtil.isInternetAvailable(this@AdminActivity)) {
                    showNoInternetCard(false)
                    (navHostFragment.childFragmentManager.primaryNavigationFragment as? Reloadable)
                        ?.reloadData()
                } else {
                    showNoInternetCard(true)
                }
                showLoading(false)
            }
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
                        (navHostFragment.childFragmentManager.primaryNavigationFragment as? Reloadable)
                            ?.reloadData()
                    }
                    isConnected && isNoConnectionVisible -> {
                        binding.noConnectionCard.visibility = View.GONE
                        showLoading(true)
                        lifecycleScope.launch {
                            delay(1000)
                            (navHostFragment.childFragmentManager.primaryNavigationFragment as? Reloadable)
                                ?.reloadData()
                            showNoInternetCard(false)
                            showLoading(false)
                        }
                    }
                    !isConnected && isNoConnectionVisible -> {
                        binding.noConnectionCard.visibility = View.GONE
                        showLoading(true)
                        lifecycleScope.launch {
                            delay(1000)
                            showNoInternetCard(true)
                            showLoading(false)
                        }
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