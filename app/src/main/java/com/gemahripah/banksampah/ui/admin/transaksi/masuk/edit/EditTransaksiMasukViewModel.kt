package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from

class EditTransaksiMasukViewModel : ViewModel() {

    private val _jenisList = MutableLiveData<List<Sampah>>()
    val jenisList: LiveData<List<Sampah>> = _jenisList

    private val _penggunaList = MutableLiveData<List<Pengguna>>()
    val penggunaList: LiveData<List<Pengguna>> = _penggunaList

    suspend fun fetchJenisSampah() {
        val data = SupabaseProvider.client
            .from("sampah")
            .select()
            .decodeList<Sampah>()
        _jenisList.postValue(data)
    }

    suspend fun fetchPenggunaList() {
        val data = SupabaseProvider.client
            .from("pengguna")
            .select()
            .decodeList<Pengguna>()
        _penggunaList.postValue(data)
    }

    suspend fun updateTransaksi(
        transaksiId: String,
        userId: String,
        keterangan: String,
        detailList: List<DetailTransaksi>
    ) {
        SupabaseProvider.client
            .from("transaksi")
            .update({
                set("tskIdPengguna", userId)
                set("tskKeterangan", keterangan)
                set("tskTipe", "Masuk")
            }) {
                filter { eq("tskId", transaksiId) }
            }

        SupabaseProvider.client
            .from("detail_transaksi")
            .delete {
                filter { eq("dtlTskId", transaksiId) }
            }

        SupabaseProvider.client
            .from("detail_transaksi")
            .insert(detailList)
    }
}
