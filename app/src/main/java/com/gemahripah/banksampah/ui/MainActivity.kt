package com.gemahripah.banksampah.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ActivityMainBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val supabase = SupabaseProvider.client

        lifecycleScope.launch {
            supabase.auth.loadFromStorage()

            val session = supabase.auth.currentSessionOrNull()
            val user = session?.user

            val isSessionValid = session != null && session.expiresAt > Clock.System.now()

            if (isSessionValid && user != null) {
                val userId = user.id

                try {
                    val pengguna = supabase.postgrest
                        .from("pengguna")
                        .select {
                            filter { eq("pgnId", userId) }
                            limit(1)
                        }
                        .decodeSingleOrNull<Pengguna>()

                    if (pengguna == null) {
                        // Hapus sesi karena data pengguna tidak ditemukan
                        supabase.auth.clearSession()
                        Toast.makeText(this@MainActivity, "Data pengguna tidak ditemukan", Toast.LENGTH_SHORT).show()
                        enableMainUI()
                        return@launch
                    }

                    val isAdmin = pengguna.pgnIsAdmin ?: false

                    val intent = if (isAdmin) {
                        Intent(this@MainActivity, AdminActivity::class.java)
                    } else {
                        Intent(this@MainActivity, NasabahActivity::class.java).apply {
                            putExtra("EXTRA_PENGGUNA", pengguna)
                        }
                    }

                    Toast.makeText(this@MainActivity, "Login berhasil", Toast.LENGTH_SHORT).show()
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } catch (e: Exception) {
                    Log.e("LoginDebug", "Gagal ambil data pengguna: ${e.localizedMessage}")
                    supabase.auth.clearSession()
                    enableMainUI()
                }
            } else {
                supabase.auth.clearSession()
                enableMainUI()
            }
        }
    }

    private fun enableMainUI() {
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

}