package com.gemahripah.banksampah.ui.nasabah

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.ActivityNasabahBinding

class NasabahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNasabahBinding
    private val viewModel: NasabahViewModel by viewModels()
    private var pengguna: Pengguna? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBinding()
        retrievePengguna()
        setupViewModel()
        setupNavigation()
    }

    private fun initBinding() {
        binding = ActivityNasabahBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
            navController.popBackStack(navController.graph.startDestinationId, false)
            if (navController.currentDestination?.id != destinationId) {
                navController.navigate(destinationId)
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.menu.findItem(destination.id)?.isChecked = true
        }
    }
}