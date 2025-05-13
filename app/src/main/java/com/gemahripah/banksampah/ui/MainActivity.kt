package com.gemahripah.banksampah.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.data.remote.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.ActivityMainBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val supabase = SupabaseProvider.client

        lifecycleScope.launch {
            supabase.auth.loadFromStorage() // tunggu session dimuat dulu secara eksplisit

            val session = supabase.auth.currentSessionOrNull()
            val user = session?.user

            Log.d("SupabaseSession", "Session setelah load: $session")
            Log.d("SupabaseSession", "User setelah load: $user")

            if (user != null) {
                val userId = user.id

                val response = supabase.postgrest
                    .from("pengguna")
                    .select {
                        filter {
                            eq("pgnId", userId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<Pengguna>()


                val isAdmin = response?.pgnIsAdmin ?: false

                val intent = if (isAdmin) {
                    Intent(this@MainActivity, AdminActivity::class.java)
                } else {
                    Intent(this@MainActivity, NasabahActivity::class.java)
                }

                try {
                    Toast.makeText(this@MainActivity, "Login berhasil", Toast.LENGTH_SHORT).show()

                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } catch (e: Exception) {
                    Log.e("LoginDebug", "Gagal startActivity: ${e.localizedMessage}")
                }
            } else {
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