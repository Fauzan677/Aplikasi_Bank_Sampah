package com.gemahripah.banksampah.admin

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_admin) as NavHostFragment
        val navController = navHostFragment.navController

        navView.setOnItemSelectedListener { item ->
            val destinationId = item.itemId

            // Hapus seluruh back stack (kembali ke awal)
            navController.popBackStack(navController.graph.startDestinationId, false)

            // Lalu langsung navigate ke tujuan
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