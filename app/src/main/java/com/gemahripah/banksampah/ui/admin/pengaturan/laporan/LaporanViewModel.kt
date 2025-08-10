package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Environment
import android.util.Log
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.TransaksiRelasi
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LaporanViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client

    private val formatterOutput = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

    // UI state/events
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    /** Laporan: DATA SAMPAH */
    fun exportSampah() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val hasil = client.from("sampah")
                    .select(
                        columns = Columns.raw(
                            """
                            created_at,
                            sphJenis,
                            sphSatuan,
                            sphHarga,
                            sphKeterangan,
                            sphKtgId (
                                ktgNama
                            )
                            """.trimIndent()
                        )
                    )
                    .decodeList<SampahRelasi>()

                val headers = listOf("Nomor", "Tanggal Dibuat", "Jenis", "Kategori", "Satuan", "Harga", "Keterangan")
                val dataRows = hasil.mapIndexed { index, item ->
                    listOf(
                        index + 1,
                        try { ZonedDateTime.parse(item.created_at).format(formatterOutput) } catch (_: Exception) { "-" },
                        item.sphJenis ?: "",
                        item.sphKtgId?.ktgNama ?: "",
                        item.sphSatuan ?: "",
                        item.sphHarga ?: 0,
                        item.sphKeterangan ?: ""
                    )
                }

                saveWorkbook(
                    sheetName = "Data Sampah",
                    filePrefix = "Data_Sampah",
                    headers = headers,
                    data = dataRows
                )
                _toast.emit("Data sampah berhasil disimpan di folder Download")
            } catch (e: Exception) {
                Log.e("LaporanVM", "exportSampah gagal: ${e.message}", e)
                _toast.emit("Gagal ambil data sampah")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Laporan: NASABAH */
    fun exportNasabah() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val penggunaList = client.from("pengguna")
                    .select {
                        filter { eq("pgnIsAdmin", false) }
                    }
                    .decodeList<Pengguna>()

                val headers = listOf("Nomor", "Nama", "Email", "Tanggal Terdaftar", "Total " +
                        "Setoran (Kg)", "Total Saldo (Rp)")

                val dataRows = penggunaList.mapIndexed { index, pengguna ->
                    val tanggalFormatted = try {
                        ZonedDateTime.parse(pengguna.created_at).format(formatterOutput)
                    } catch (_: Exception) {
                        "-"
                    }

                    val totalSetoran = try {
                        client.postgrest.rpc(
                            "hitung_total_jumlah_per_pengguna_masuk",
                            buildJsonObject { put("pgn_id_input", pengguna.pgnId) }
                        ).data.toDoubleOrNull() ?: 0.0
                    } catch (_: Exception) {
                        0.0
                    }

                    val totalSaldo = try {
                        client.postgrest.rpc(
                            "hitung_saldo_pengguna",
                            buildJsonObject { put("pgn_id_input", pengguna.pgnId) }
                        ).data.toDoubleOrNull() ?: 0.0
                    } catch (_: Exception) {
                        0.0
                    }

                    listOf(
                        index + 1,
                        pengguna.pgnNama ?: "-",
                        pengguna.pgnEmail ?: "-",
                        tanggalFormatted,
                        totalSetoran,
                        totalSaldo
                    )
                }

                saveWorkbook(
                    sheetName = "Laporan Nasabah",
                    filePrefix = "Laporan_Nasabah",
                    headers = headers,
                    data = dataRows
                )
                _toast.emit("Laporan Nasabah berhasil disimpan di folder Download")
            } catch (e: Exception) {
                Log.e("LaporanVM", "exportNasabah gagal: ${e.message}", e)
                _toast.emit("Gagal ambil data nasabah")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Laporan: SETORAN (detail_transaksi bertipe Masuk) */
    fun exportSetoran() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val hasilMasuk = run {
                    val columns = Columns.raw(
                        """
                        dtlJumlah,
                        created_at,
                        dtlSphId (
                            sphJenis,
                            sphSatuan,
                            sphKtgId (
                                ktgNama
                            )
                        ),
                        dtlTskId (
                            tskTipe,
                            tskIdPengguna (
                                pgnNama,
                                pgnEmail
                            )
                        )
                        """.trimIndent()
                    )
                    client.from("detail_transaksi")
                        .select(columns = columns)
                        .decodeList<DetailTransaksiRelasi>()
                        .filter { it.dtlTskId?.tskTipe == "Masuk" }
                }

                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Kategori", "Jenis Sampah", "Satuan", "Jumlah")

                val dataRows = hasilMasuk.mapIndexed { index, detail ->
                    val pengguna = detail.dtlTskId?.tskIdPengguna
                    val sampah = detail.dtlSphId
                    val kategori = sampah?.sphKtgId

                    val tanggalFormatted = try {
                        ZonedDateTime.parse(detail.created_at).format(formatterOutput)
                    } catch (_: Exception) {
                        "-"
                    }

                    listOf(
                        index + 1,
                        tanggalFormatted,
                        pengguna?.pgnNama ?: "-",
                        pengguna?.pgnEmail ?: "-",
                        kategori?.ktgNama ?: "-",
                        sampah?.sphJenis ?: "-",
                        sampah?.sphSatuan ?: "-",
                        detail.dtlJumlah ?: 0.0
                    )
                }

                saveWorkbook(
                    sheetName = "Laporan Setoran",
                    filePrefix = "Laporan_Setoran",
                    headers = headers,
                    data = dataRows
                )
                _toast.emit("Laporan Setoran berhasil disimpan di folder Download")
            } catch (e: Exception) {
                Log.e("LaporanVM", "exportSetoran gagal: ${e.message}", e)
                _toast.emit("Gagal ambil data setoran")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Laporan: TRANSAKSI (lengkap) */
    fun exportTransaksiLengkap() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val transaksiList = client.from("transaksi")
                    .select(
                        columns = Columns.raw(
                            """
                            tskId,
                            created_at,
                            tskKeterangan,
                            tskTipe,
                            tskIdPengguna (
                                pgnNama,
                                pgnEmail
                            )
                            """.trimIndent()
                        )
                    )
                    .decodeList<TransaksiRelasi>()

                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Tipe", "Total (Rp)",
                    "Keterangan")

                val dataRows = transaksiList.mapIndexed { index, transaksi ->
                    val total = try {
                        when (transaksi.tskTipe) {
                            "Masuk" -> client.postgrest.rpc(
                                "hitung_total_harga",
                                buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                            ).data.toDoubleOrNull() ?: 0.0

                            "Keluar" -> client.postgrest.rpc(
                                "hitung_total_jumlah",
                                buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                            ).data.toDoubleOrNull() ?: 0.0

                            else -> 0.0
                        }
                    } catch (_: Exception) {
                        0.0
                    }

                    val tanggalFormatted = try {
                        ZonedDateTime.parse(transaksi.created_at).format(formatterOutput)
                    } catch (_: Exception) {
                        "-"
                    }

                    listOf(
                        index + 1,
                        tanggalFormatted,
                        transaksi.tskIdPengguna?.pgnNama ?: "-",
                        transaksi.tskIdPengguna?.pgnEmail ?: "-",
                        transaksi.tskTipe ?: "-",
                        total,
                        transaksi.tskKeterangan ?: "-"
                    )
                }

                saveWorkbook(
                    sheetName = "Laporan Transaksi",
                    filePrefix = "Laporan_Transaksi",
                    headers = headers,
                    data = dataRows
                )
                _toast.emit("Laporan Transaksi berhasil disimpan di folder Download")
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("LaporanVM", "exportTransaksiLengkap gagal: ${e.message}", e)
                _toast.emit("Gagal mengambil data atau membuat file Excel")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ===== Helpers =====

    private suspend fun saveWorkbook(
        sheetName: String,
        filePrefix: String,
        headers: List<String>,
        data: List<List<Any?>>
    ) = withContext(Dispatchers.IO) {
        val workbook = generateWorkbook(sheetName, headers, data)
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.xlsx"
        val filePath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )
        FileOutputStream(filePath).use { workbook.write(it) }
        workbook.close()
    }

    private fun generateWorkbook(
        sheetName: String,
        headers: List<String>,
        data: List<List<Any?>>
    ): Workbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(sheetName)

        val headerFont = workbook.createFont().apply { bold = true }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }
        val dataStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            verticalAlignment = VerticalAlignment.CENTER
        }

        // Header
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(index, 20 * 256)
        }

        // Data
        data.forEachIndexed { rowIndex, rowData ->
            val row = sheet.createRow(rowIndex + 1)
            rowData.forEachIndexed { cellIndex, value ->
                val cell = row.createCell(cellIndex)
                when (value) {
                    is Number -> cell.setCellValue(value.toDouble())
                    is String -> cell.setCellValue(value)
                    else -> cell.setCellValue(value?.toString() ?: "")
                }
                cell.cellStyle = dataStyle
            }
        }

        return workbook
    }
}
