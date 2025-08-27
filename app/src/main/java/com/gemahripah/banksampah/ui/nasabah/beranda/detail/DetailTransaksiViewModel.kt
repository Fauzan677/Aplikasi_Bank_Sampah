package com.gemahripah.banksampah.ui.nasabah.beranda.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class DetailTransaksiViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _detailList = MutableLiveData<List<DetailTransaksiRelasi>>()
    val detailList: LiveData<List<DetailTransaksiRelasi>> = _detailList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    /**
     * Jika tipe transaksi = "Keluar", panggil dengan skipFetch = true agar tidak memuat detail.
     */
    fun loadDetailTransaksi(idTransaksi: Long, skipFetch: Boolean = false) {
        _isLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (skipFetch) {
                    _detailList.postValue(emptyList())
                    return@launch
                }

                // Ambil langsung jumlah, nominal, dan relasi sampah (jenis & satuan)
                val columns = Columns.raw(
                    """
                    dtlId,
                    dtlJumlah,
                    dtlNominal,
                    dtlSphId (
                        sphJenis,
                        sphSatuan
                    )
                    """.trimIndent()
                )

                val details = client
                    .from("detail_transaksi")
                    .select(columns) {
                        filter { eq("dtlTskId", idTransaksi) }
                    }
                    .decodeList<DetailTransaksiRelasi>()

                _detailList.postValue(details)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("DetailTransaksiVM", "Gagal ambil detail: ${e.message}", e)
                _detailList.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}