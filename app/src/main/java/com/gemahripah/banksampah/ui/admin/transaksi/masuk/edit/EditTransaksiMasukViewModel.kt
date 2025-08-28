package com.gemahripah.banksampah.ui.admin.transaksi.masuk.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.Sampah
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class EditTransaksiMasukViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Master data
    private val _jenisList = MutableStateFlow<List<String>>(emptyList())
    val jenisList: StateFlow<List<String>> = _jenisList.asStateFlow()

    private val _namaToIdMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val namaToIdMap: StateFlow<Map<String, Long>> = _namaToIdMap.asStateFlow()

    private val _jenisToSatuanMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val jenisToSatuanMap: StateFlow<Map<String, String>> = _jenisToSatuanMap.asStateFlow()

    // UI one-shot events
    private val _toast = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    private val _navigateBack = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateBack: SharedFlow<Unit> = _navigateBack

    private val _jenisToHargaMap = MutableStateFlow<Map<String, BigDecimal>>(emptyMap())
    val jenisToHargaMap: StateFlow<Map<String, BigDecimal>> = _jenisToHargaMap.asStateFlow()

    private val DECIMAL_SCALE = 2

    private fun BigDecimal.hasMoreThanTwoDecimals(): Boolean =
        this.stripTrailingZeros().scale() > DECIMAL_SCALE

    /** Paksa skala = 2. Akan throw jika butuh pembulatan (>2 desimal). */
    private fun BigDecimal.enforceScale2(): BigDecimal =
        this.stripTrailingZeros().setScale(DECIMAL_SCALE, RoundingMode.UNNECESSARY)


    /** Load master ‘sampah’ sekali */
    fun loadSampah() {
        if (_jenisList.value.isNotEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val data = client.from("sampah").select().decodeList<Sampah>()

                val listJenis = data.mapNotNull { it.sphJenis?.takeIf { s -> s.isNotBlank() } }

                _namaToIdMap.value      = data.associate { (it.sphJenis ?: "Unknown") to (it.sphId ?: 0L) }
                _jenisToSatuanMap.value = data.associate { (it.sphJenis ?: "Unknown") to (it.sphSatuan ?: "Unit") }
                _jenisToHargaMap.value  = data.associate {
                    (it.sphJenis ?: "Unknown") to BigDecimal.valueOf((it.sphHarga ?: 0L).toLong())
                }

                // <- set ini TERAKHIR supaya prefill dapat map yang sudah terisi
                _jenisList.value = listJenis
            } catch (e: Exception) {
                viewModelScope.launch { _toast.emit("Gagal memuat jenis transaksi") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Satuan untuk label jumlah */
    fun getSatuan(jenis: String?): String {
        val j = jenis ?: return "Unit"
        val map = jenisToSatuanMap.value
        return map[j] ?: map.entries.firstOrNull { it.key.equals(j, true) }?.value ?: "Unit"
    }

    fun getAvailableJenis(currentJenis: String?, selectedSemua: Set<String>): List<String> {
        val all = jenisList.value
        val withoutSelected = all.filter { it.isNotBlank() && it !in selectedSemua }
        val keepCurrent = currentJenis?.takeIf { it.isNotBlank() }
        return (withoutSelected + listOfNotNull(keepCurrent)).distinct()
    }

    /** Submit update transaksi + replace detail (IO di ViewModel) */
    fun submitEditTransaksiMasuk(
        transaksiId: Long,
        userId: String,
        keterangan: String,
        main: Pair<String, BigDecimal>,
        tambahan: List<Pair<String, BigDecimal>>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // ===== VALIDASI 2 DESIMAL MAKS =====
                val allQty = sequenceOf(main.second) + tambahan.asSequence().map { it.second }
                if (allQty.any { it.hasMoreThanTwoDecimals() }) {
                    _isLoading.value = false
                    _toast.emit("Format jumlah tidak valid (maksimal 2 angka di belakang koma)")
                    return@launch
                }

                // total LAMA sebelum dihapus
                val oldTotal = getOldTotalNominal(transaksiId)

                // update header
                client.from("transaksi").update({
                    set("tskIdPengguna", userId)
                    set("tskKeterangan", keterangan)
                    set("tskTipe", "Masuk")
                }) { filter { eq("tskId", transaksiId) } }

                // ganti detail
                deleteOldDetails(transaksiId)
                insertNewDetails(transaksiId, main, tambahan)

                // total BARU dari input + harga master
                val newTotal = calcNewTotal(main, tambahan)

                val delta = newTotal.subtract(oldTotal)
                updateSaldoPenggunaDelta(userId, delta)

                _toast.emit("Data berhasil diperbarui")
                _navigateBack.emit(Unit)
            } catch (e: Exception) {
                _toast.emit("Gagal memperbarui data, periksa koneksi internet")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun getOldTotalNominal(transaksiId: Long): BigDecimal {
        val list = client
            .from("detail_transaksi")
            .select { filter { eq("dtlTskId", transaksiId) } }
            .decodeList<DetailTransaksi>()
        var total = BigDecimal.ZERO
        list.forEach { d -> total = total.add(d.dtlNominal ?: BigDecimal.ZERO) }
        return total
    }

    private fun calcNewTotal(main: Pair<String, BigDecimal>, tambahan: List<Pair<String, BigDecimal>>): BigDecimal {
        var total = BigDecimal.ZERO
        fun add(jenis: String, qtyRaw: BigDecimal) {
            val qty = qtyRaw.enforceScale2() // <= pastikan skala 2
            if (qty <= BigDecimal.ZERO) return
            val harga = jenisToHargaMap.value[jenis] ?: BigDecimal.ZERO
            total = total.add(qty.multiply(harga))
        }
        add(main.first, main.second)
        tambahan.forEach { (j, q) -> add(j, q) }
        return total
    }

    private suspend fun updateSaldoPenggunaDelta(userId: String, delta: BigDecimal) {
        if (delta == BigDecimal.ZERO) return
        val pengguna = client
            .from("pengguna")
            .select { filter { eq("pgnId", userId) } }
            .decodeSingle<Pengguna>()
        val saldoBaru = (pengguna.pgnSaldo ?: BigDecimal.ZERO).add(delta)
        client.from("pengguna").update(mapOf("pgnSaldo" to saldoBaru)) { filter { eq("pgnId", userId) } }
    }


    private suspend fun deleteOldDetails(transaksiId: Long) {
        client
            .from("detail_transaksi")
            .delete {
                filter { eq("dtlTskId", transaksiId) }
            }
    }

    private suspend fun insertNewDetails(
        transaksiId: Long,
        main: Pair<String, BigDecimal>,
        tambahan: List<Pair<String, BigDecimal>>
    ) {
        val mapId    = namaToIdMap.value
        val mapHarga = jenisToHargaMap.value

        suspend fun insertOne(jenis: String, jumlahRaw: BigDecimal) {
            val sphId = mapId[jenis] ?: return
            val jumlah = jumlahRaw.enforceScale2() // <= pastikan skala 2
            if (jumlah <= BigDecimal.ZERO) return
            val harga   = mapHarga[jenis] ?: BigDecimal.ZERO
            val nominal = jumlah.multiply(harga)

            val detail = DetailTransaksi(
                dtlTskId = transaksiId,
                dtlSphId = sphId,
                dtlJumlah = jumlah,
                dtlNominal = nominal
            )
            client.from("detail_transaksi").insert(detail)
        }

        insertOne(main.first, main.second)
        tambahan.forEach { (j, n) -> insertOne(j, n) }
    }
}