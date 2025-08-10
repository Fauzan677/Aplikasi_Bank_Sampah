package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TransaksiPagingSource(
    private val client: SupabaseClient,
    private val query: String?,
    private val startDate: String?, // UTC ISO, inklusif (>=)
    private val endDate: String?    // UTC ISO, eksklusif (<)
) : PagingSource<Int, RiwayatTransaksi>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RiwayatTransaksi> = try {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        val offset = ((page - 1) * pageSize).toLong()

        // --- Query ke server dengan filter tanggal yang benar ---
        val transaksiList = client.postgrest.from("transaksi")
            .select {
                order("created_at", Order.DESCENDING)

                filter {
                    // filter user pemilik transaksi (kalau ada)
                    // eq("tskIdPengguna", <isi jika perlu>)

                    when {
                        !startDate.isNullOrEmpty() && !endDate.isNullOrEmpty() -> and {
                            gte("created_at", startDate) // inklusif
                            lt ("created_at", endDate)   // eksklusif
                        }
                        !startDate.isNullOrEmpty() -> gte("created_at", startDate)
                        !endDate.isNullOrEmpty()   -> lt ("created_at", endDate)
                    }
                }

                range(offset..<(offset + pageSize))
            }
            .decodeList<Transaksi>()

        // (opsional) filter nama di client (lebih aman kalau bisa join di server)
        val transaksiFilteredByName = if (!query.isNullOrBlank()) {
            transaksiList.filter { t ->
                val pengguna = client.postgrest.from("pengguna")
                    .select { filter { eq("pgnId", t.tskIdPengguna!!) } }
                    .decodeSingle<Pengguna>()
                pengguna.pgnNama?.contains(query, ignoreCase = true) == true
            }
        } else transaksiList

        val data = transaksiFilteredByName.map { transaksi ->
            val pengguna = client.postgrest.from("pengguna")
                .select { filter { eq("pgnId", transaksi.tskIdPengguna!!) } }
                .decodeSingle<Pengguna>()

            val totalBerat = if (transaksi.tskTipe == "Masuk") {
                client.postgrest.rpc("hitung_total_jumlah", buildJsonObject {
                    put("tsk_id_input", transaksi.tskId)
                }).data.toDoubleOrNull()
            } else null

            val totalHarga = if (transaksi.tskTipe == "Keluar") {
                val detail = client.postgrest.from("detail_transaksi")
                    .select { filter { eq("dtlTskId", transaksi.tskId!!) } }
                    .decodeList<DetailTransaksi>()
                detail.sumOf { it.dtlJumlah ?: 0.0 }
            } else {
                client.postgrest.rpc("hitung_total_harga", buildJsonObject {
                    put("tsk_id_input", transaksi.tskId)
                }).data.toDoubleOrNull()
            }

            val tanggalFormatted = try {
                OffsetDateTime.parse(transaksi.created_at)
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("id")))
            } catch (_: Exception) { "Tanggal tidak valid" }

            RiwayatTransaksi(
                tskId = transaksi.tskId!!,
                tskIdPengguna = transaksi.tskIdPengguna,
                nama = pengguna.pgnNama ?: "Tidak Diketahui",
                tanggal = tanggalFormatted,
                tipe = transaksi.tskTipe ?: "Masuk",
                tskKeterangan = transaksi.tskKeterangan,
                totalBerat = totalBerat,
                totalHarga = totalHarga,
                hari = null,
                createdAt = transaksi.created_at ?: ""
            )
        }

        LoadResult.Page(
            data = data,
            prevKey = if (page == 1) null else page - 1,
            nextKey = if (data.isEmpty()) null else page + 1
        )
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<Int, RiwayatTransaksi>): Int? {
        return state.anchorPosition?.let { pos ->
            state.closestPageToPosition(pos)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(pos)?.nextKey?.minus(1)
        }
    }
}