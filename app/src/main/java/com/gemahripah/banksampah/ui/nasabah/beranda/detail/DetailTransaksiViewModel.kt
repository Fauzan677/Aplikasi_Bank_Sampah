package com.gemahripah.banksampah.ui.nasabah.beranda.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DetailTransaksiViewModel : ViewModel() {

    private val client = SupabaseProvider.client

    private val _detailList = MutableLiveData<List<DetailTransaksiRelasi>>()
    val detailList: LiveData<List<DetailTransaksiRelasi>> = _detailList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadDetailTransaksi(idTransaksi: Long) {
        _isLoading.postValue(true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val columns = Columns.raw(
                    """
                    dtlId,
                    dtlJumlah,
                    dtlSphId (
                        sphJenis
                    )
                    """.trimIndent()
                )

                val response = client
                    .from("detail_transaksi")
                    .select(columns) {
                        filter { eq("dtlTskId", idTransaksi) }
                    }

                val detailList = Json.decodeFromString<List<DetailTransaksiRelasi>>(response.data.toString())

                val enriched = detailList.mapNotNull { detail ->
                    detail.dtlId?.let { dtlId ->
                        try {
                            val hargaResponse = client.postgrest.rpc(
                                "hitung_harga_detail",
                                buildJsonObject {
                                    put("dtl_id_input", dtlId)
                                }
                            )
                            val harga = hargaResponse.data.toDoubleOrNull() ?: 0.0
                            detail.copy(hargaDetail = harga)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                _detailList.postValue(enriched)
            } catch (e: Exception) {
                Log.e("DetailTransaksiVM", "Gagal ambil detail: ${e.message}", e)
                _detailList.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}