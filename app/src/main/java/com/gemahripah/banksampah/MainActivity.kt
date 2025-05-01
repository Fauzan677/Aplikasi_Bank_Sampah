package com.gemahripah.banksampah

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.admin.AdminActivity
import com.gemahripah.banksampah.data.datastore.SessionPreference
import com.gemahripah.banksampah.data.remote.Pengguna
import com.gemahripah.banksampah.databinding.ActivityMainBinding
import com.gemahripah.banksampah.nasabah.NasabahActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sessionPreference: SessionPreference

    companion object {
        lateinit var supabase: SupabaseClient
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supabase = createSupabaseClient(
            supabaseUrl = "https://gxqnvejigdthwlkeshks.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imd4cW52ZWppZ2R0aHdsa2VzaGtzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDIwNTI5MTAsImV4cCI6MjA1NzYyODkxMH0.L3sdec1qUrZ66VS91Invpb9X60kmNvXgwKDazxklH3w"
        ) {
            install(Postgrest)
            install(Auth)
            install(Storage)
        }

        sessionPreference = SessionPreference.getInstance(this)

        lifecycleScope.launch {
            // Cek session dari DataStore
            sessionPreference.getSession().collect { session ->
                if (session != null) {
                    // Jika session ditemukan, periksa apakah isAdmin bernilai true
                    if (session.isAdmin) {
                        // Jika isAdmin true, arahkan ke AdminActivity
                        val intent = Intent(this@MainActivity, AdminActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // Jika bukan admin, arahkan ke NasabahActivity
                        val intent = Intent(this@MainActivity, NasabahActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // Tidak ada session, lanjutkan ke layar login
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
        }
    }
}