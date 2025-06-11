package com.gemahripah.banksampah.ui.nasabah

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.ActivityNasabahBinding

class NasabahActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNasabahBinding
    private var pengguna: Pengguna? = null
    private lateinit var viewModel: NasabahViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        pengguna = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("EXTRA_PENGGUNA", Pengguna::class.java)
        } else {
            intent.getParcelableExtra("EXTRA_PENGGUNA")
        }

        viewModel = ViewModelProvider(this)[NasabahViewModel::class.java]
        viewModel.setPengguna(pengguna)

        binding = ActivityNasabahBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_nasabah) as NavHostFragment
        val navController = navHostFragment.navController

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