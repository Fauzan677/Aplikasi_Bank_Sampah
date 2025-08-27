package com.gemahripah.banksampah.ui.admin.transaksi

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.transaksi.DetailTransaksi
import com.gemahripah.banksampah.data.model.transaksi.RiwayatTransaksi
import com.gemahripah.banksampah.data.model.transaksi.Transaksi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class TransaksiPagingSource(
    private val client: SupabaseClient,
    private val query: String?,
    private val startDate: String?, // UTC ISO, inklusif (>=)
    private val endDate: String?    // UTC ISO, eksklusif (<)
) : PagingSource<Int, RiwayatTransaksi>() {

    private val ID_LOCALE = Locale("id","ID")
    private val TGL_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy", ID_LOCALE)
    private val JAM_FMT   = DateTimeFormatter.ofPattern("HH.mm",       ID_LOCALE)

    private fun Any?.toBigDecimalOrNull(): BigDecimal? = when (this) {
        null -> null
        is BigDecimal -> this
        is Number -> BigDecimal(this.toString())
        is String -> this.takeIf { it.isNotBlank() }?.let { BigDecimal(it) }
        else -> runCatching { BigDecimal(toString()) }.getOrNull()
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RiwayatTransaksi> = try {
        val page = params.key ?: 1
        val pageSize = params.loadSize
        val offset = ((page - 1) * pageSize).toLong()

        // --- Query ke server dengan filter tanggal yang benar ---
        val transaksiList = client.postgrest.from("transaksi")
            .select {
                order("created_at", Order.DESCENDING)

                filter {
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
                client.postgrest.rpc(
                    "hitung_total_jumlah",
                    buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                ).data.toBigDecimalOrNull()
            } else null

            val totalHarga = if (transaksi.tskTipe == "Keluar") {
                client.from("detail_transaksi")
                    .select { filter { eq("dtlTskId", transaksi.tskId!!) } }
                    .decodeList<DetailTransaksi>()
                    .fold(BigDecimal.ZERO) { acc, row -> acc + (row.dtlNominal ?: BigDecimal.ZERO) }
            } else {
                client.postgrest.rpc(
                    "hitung_total_harga",
                    buildJsonObject { put("tsk_id_input", transaksi.tskId!!) }
                ).data.toBigDecimalOrNull()
            }

            val tanggalFormatted = try {
                val raw = transaksi.created_at ?: ""

                when {
                    // ISO 8601 dengan offset (timestamptz), contoh: 2025-05-25T10:15:30.123Z / +07:00
                    raw.contains("Z") || raw.contains("+") -> {
                        val odt   = OffsetDateTime.parse(raw)
                        val local = odt.atZoneSameInstant(java.time.ZoneId.systemDefault())
                        "${local.format(TGL_FMT)}, ${local.format(JAM_FMT)}"
                    }

                    // LocalDateTime tanpa offset, contoh: 2025-05-25T10:15:30
                    raw.length > 10 && raw[10] == 'T' -> {
                        val ldt   = java.time.LocalDateTime.parse(raw)
                        val zdt   = ldt.atZone(java.time.ZoneId.systemDefault())
                        "${zdt.format(TGL_FMT)}, ${zdt.format(JAM_FMT)}"
                    }

                    // Hanya tanggal (kolom DATE lama)
                    raw.length == 10 -> {
                        java.time.LocalDate.parse(raw).format(TGL_FMT)
                    }

                    else -> "-"
                }
            } catch (_: Exception) { "-" }

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