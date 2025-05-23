package com.gemahripah.banksampah.ui.welcome.masuk

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.data.datastore.SessionPreference
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.remote.SessionData
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import com.gemahripah.banksampah.databinding.FragmentMasukBinding
import com.gemahripah.banksampah.ui.nasabah.NasabahActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class MasukFragment : Fragment() {

    private var _binding: FragmentMasukBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MasukViewModel by viewModels()
    private lateinit var sessionPreference: SessionPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMasukBinding.inflate(inflater, container, false)
        sessionPreference = SessionPreference.getInstance(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aksi saat tombol back ditekan
        binding.back.setOnClickListener {
            findNavController().navigate(R.id.action_masukFragment_to_landingFragment)
        }

        // Aksi saat tombol daftar ditekan (Sign In)
        binding.masuk.setOnClickListener {
            val email = binding.email.text.toString().trim()
            val password = binding.password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Email dan Password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    SupabaseProvider.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }

                    val session = SupabaseProvider.client.auth.currentSessionOrNull()

                    if (session != null) {
                        val userId = session.user?.id
                        val accessToken = session.accessToken

                        if (userId != null && accessToken != null) {
                            val response = SupabaseProvider.client.postgrest
                                .from("pengguna")
                                .select {
                                    filter {
                                        eq("pgnId", userId)
                                    }
                                    limit(1)
                                }
                                .decodeSingleOrNull<Pengguna>()


                            val isAdmin = response?.pgnIsAdmin ?: false

                            val sessionData = SessionData(userId, accessToken, isAdmin)
                            sessionPreference.saveSession(sessionData)

                            val intent = if (isAdmin) {
                                Intent(requireContext(), AdminActivity::class.java)
                            } else {
                                Intent(requireContext(), NasabahActivity::class.java).apply {
                                    putExtra("EXTRA_PENGGUNA", response) // pastikan 'response' adalah objek dari class Pengguna
                                }
                            }

                            try {
                                Toast.makeText(requireContext(), "Login berhasil", Toast.LENGTH_SHORT).show()

                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)

                            } catch (e: Exception) {
                                Log.e("LoginDebug", "Gagal startActivity: ${e.localizedMessage}")
                            }
                        } else {
                            Toast.makeText(requireContext(), "User ID tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal login", Toast.LENGTH_SHORT).show()
                        Log.d("LoginDebug", "Session null setelah login")
                    }

                } catch (e: BadRequestRestException) {
                    Toast.makeText(requireContext(), "Email atau Password salah", Toast.LENGTH_LONG).show()
                    Log.e("LoginDebug", "BadRequest: ${e.message}")
                } catch (e: RestException) {
                    Toast.makeText(requireContext(), "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("LoginDebug", "RestException: ${e.message}")
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal login: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    Log.e("LoginDebug", "Exception: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}