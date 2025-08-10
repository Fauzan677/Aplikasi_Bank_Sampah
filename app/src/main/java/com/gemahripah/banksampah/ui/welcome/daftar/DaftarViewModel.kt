package com.gemahripah.banksampah.ui.welcome.daftar

import android.util.Log
import androidx.lifecycle.ViewModel
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class DaftarViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    suspend fun isNamaExists(nama: String): Boolean {
        return try {
            val normalizedNama = nama.trim()

            val response = client
                .from("pengguna")
                .select(columns = Columns.list("pgnNama")) {
                    filter {
                        eq("pgnNama", normalizedNama)
                    }
                }
                .decodeList<Pengguna>()

            Log.d("DaftarViewModel", "Hasil pencarian nama: $response")

            response.isNotEmpty()
        } catch (e: Exception) {
            Log.e("DaftarViewModel", "Gagal cek nama unik", e)
            false
        }
    }

    suspend fun registerUser(email: String, password: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun insertUserToDatabase(nama: String, email: String) {
        try {
            client.from("pengguna").insert(
                mapOf(
                    "pgnNama" to nama.trim(),
                    "pgnEmail" to email
                )
            )
        } catch (e: RestException) {
            Log.e("DaftarViewModel", "RestException saat insert: ${e.message}", e)

            val msg = e.message?.lowercase().orEmpty()
            if (msg.contains("duplicate") && msg.contains("pgnnama")) {
                throw IllegalStateException("Nama sudah digunakan")
            }
            throw Exception("Gagal menyimpan data pengguna")
        } catch (e: Exception) {
            Log.e("DaftarViewModel", "Exception umum saat insert: ${e.message}", e)
            throw Exception("Gagal menyimpan data pengguna")
        }
    }

    fun getCurrentUser() = client.auth.currentSessionOrNull()?.user
    suspend fun signOut() = client.auth.signOut()
}