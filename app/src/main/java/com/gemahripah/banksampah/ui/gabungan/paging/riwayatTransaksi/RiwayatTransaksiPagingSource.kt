package com.gemahripah.banksampah.ui.gabungan.paging.riwayatTransaksi

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RiwayatTransaksiPagingSource(
    private val pgnId: String,
    private val startDate: String? = null,
    private val endDate: String? = null
) : PagingSource<Int, RiwayatTransaksi>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RiwayatTransaksi> {
        val page = params.key ?: 0
        val pageSize = params.loadSize
        val offset = page * pageSize

        return try {
            // RiwayatTransaksiPagingSource.load(...)
            val transaksiList = SupabaseProvider.client
                .from("transaksi")
                .select {
                    order("created_at", Order.DESCENDING)
                    filter {
                        eq("tskIdPengguna", pgnId)

                        when {
                            !startDate.isNullOrEmpty() && !endDate.isNullOrEmpty() -> and {
                                gte("created_at", startDate) // inklusif
                                lt ("created_at", endDate)   // eksklusif
                            }
                            !startDate.isNullOrEmpty() -> gte("created_at", startDate)
                            !endDate.isNullOrEmpty()   -> lt ("created_at", endDate)
                        }
                    }
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<Transaksi>()

            val list = transaksiList.map { transaksi ->
                val pengguna = SupabaseProvider.client.from("pengguna")
                    .select { filter { eq("pgnId", transaksi.tskIdPengguna!!) } }
                    .decodeSingle<Pengguna>()

                val totalBerat = if (transaksi.tskTipe == "Masuk") {
                    SupabaseProvider.client.postgrest.rpc(
                        "hitung_total_jumlah",
                        buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                    ).data.toDoubleOrNull()
                } else null

                val totalHarga = if (transaksi.tskTipe == "Keluar") {
                    SupabaseProvider.client.from("detail_transaksi")
                        .select { filter { eq("dtlTskId", transaksi.tskId!!) } }
                        .decodeList<DetailTransaksi>()
                        .sumOf { it.dtlJumlah ?: 0.0 }
                } else {
                    SupabaseProvider.client.postgrest.rpc(
                        "hitung_total_harga",
                        buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                    ).data.toDoubleOrNull()
                }

                val (tanggalFormatted, hari) = try {
                    val dateTime = OffsetDateTime.parse(transaksi.created_at)
                    val tanggalFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id"))
                    val hariFormat = DateTimeFormatter.ofPattern("EEEE", Locale("id"))
                    dateTime.format(tanggalFormat) to dateTime.format(hariFormat)
                } catch (e: Exception) {
                    "Tanggal tidak valid" to "Hari tidak valid"
                }

                RiwayatTransaksi(
                    tskId = transaksi.tskId!!,
                    tskIdPengguna = transaksi.tskIdPengguna,
                    nama = pengguna.pgnNama ?: "Tidak Diketahui",
                    tanggal = tanggalFormatted,
                    tipe = transaksi.tskTipe ?: "Masuk",
                    tskKeterangan = transaksi.tskKeterangan,
                    totalBerat = totalBerat,
                    totalHarga = totalHarga,
                    hari = hari,
                    createdAt = transaksi.created_at ?: ""
                )
            }

            val nextKey = if (list.size < pageSize) null else page + 1

            LoadResult.Page(
                data = list,
                prevKey = if (page == 0) null else page - 1,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, RiwayatTransaksi>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }
}