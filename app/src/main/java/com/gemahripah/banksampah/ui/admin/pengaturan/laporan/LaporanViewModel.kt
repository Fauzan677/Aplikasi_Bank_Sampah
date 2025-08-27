package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.TransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Locale

class LaporanViewModel : ViewModel() {

    private val client get() = SupabaseProvider.client
    private val formatterOutput = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _toast = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toast: SharedFlow<String> = _toast

    // =========================
    // Filter rentang tanggal (dipakai HANYA utk Transaksi & Setoran)
    // =========================
    data class DateRange(val start: LocalDate, val end: LocalDate)
    private val _dateRange = MutableStateFlow<DateRange?>(null)
    val dateRange: StateFlow<DateRange?> = _dateRange.asStateFlow()

    fun setDateRangeByEpoch(startMillis: Long, endMillis: Long) {
        val z = ZoneId.systemDefault()
        val s = Instant.ofEpochMilli(startMillis).atZone(z).toLocalDate()
        val e = Instant.ofEpochMilli(endMillis).atZone(z).toLocalDate()
        _dateRange.value = if (e.isBefore(s)) DateRange(e, s) else DateRange(s, e)
    }

    fun clearDateRange() {
        _dateRange.value = null
        viewModelScope.launch { _toast.emit("Filter tanggal dihapus") }
    }

    /** Ubah DateRange -> batas waktu ISO (lokal TZ) untuk gte/lte di PostgREST */
    private fun currentRangeIso(): Pair<String, String>? {
        val r = _dateRange.value ?: return null
        val zone = ZoneId.systemDefault()
        val startIso = r.start.atStartOfDay(zone).toOffsetDateTime().toString()
        // end of day (inklusif) → 23:59:59.999999999
        val endIso = r.end.plusDays(1).atStartOfDay(zone).minusNanos(1).toOffsetDateTime().toString()
        return startIso to endIso
    }

    // =========================
    // PREVIEW signal (HTML disimpan di VM)
    // =========================
    data class PreviewSignal(val title: String)
    private val _preview = MutableSharedFlow<PreviewSignal>(extraBufferCapacity = 1)
    val preview: SharedFlow<PreviewSignal> = _preview

    private val _lastHtml = MutableStateFlow<String?>(null)
    val lastHtml: StateFlow<String?> = _lastHtml.asStateFlow()

    // Simpan struktur data untuk ekspor tanpa fetch ulang
    private sealed interface BuiltReport {
        data class Simple(
            val sheetName: String,
            val filePrefix: String,
            val headers: List<String>,
            val rows: List<List<Any?>>,
            val decimalColumns: List<Int> = emptyList()
        ) : BuiltReport
        data class PivotSetoran(
            val sheetName: String,
            val filePrefix: String,
            val fixedHeaders: List<String>,
            val katToJenis: LinkedHashMap<String, List<String>>,
            val rows: List<List<Any?>>
        ) : BuiltReport
        data class SampahMerged(
            val sheetName: String,
            val filePrefix: String,
            val grouped: LinkedHashMap<String, MutableList<SampahRelasi>>
        ) : BuiltReport
    }
    private var lastBuilt: BuiltReport? = null

    // =========================
    //  PREVIEW (server-side filter utk Transaksi & Setoran)
    // =========================

    fun previewTransaksi() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val rangeIso = currentRangeIso()

                val transaksiList = client.from("transaksi")
                    .select(
                        columns = Columns.raw(
                            """
                            tskId, created_at, tskKeterangan, tskTipe,
                            tskIdPengguna ( pgnNama, pgnEmail, pgnRekening )
                            """.trimIndent()
                        )
                    ) {
                        if (rangeIso != null) {
                            filter {
                                gte("created_at", rangeIso.first)
                                lte("created_at", rangeIso.second)
                            }
                        }
                    }
                    .decodeList<TransaksiRelasi>()
                    .sortedBy { parseInstantOrMax(it.created_at) }   // ✅ urut paling awal dulu


                val headers = listOf("Nomor","Tanggal","No Rekening","Nama","Email","Transaksi Masuk","Transaksi Keluar","Keterangan")
                val rows = transaksiList.mapIndexed { index, trx ->
                    val tanggal = try { ZonedDateTime.parse(trx.created_at).format(formatterOutput) } catch (_: Exception) { "-" }
                    val (masuk: Any, keluar: Any) = when (trx.tskTipe) {
                        "Masuk" -> {
                            val nilai = try {
                                client.postgrest.rpc(
                                    "hitung_total_harga",
                                    buildJsonObject { put("tsk_id_input", trx.tskId) }
                                ).data.toDoubleOrNull()
                            } catch (_: Exception) { null }
                            (nilai ?: "-") to "-"
                        }
                        "Keluar" -> {
                            val nilai = try {
                                client.from("detail_transaksi")
                                    .select(columns = Columns.list("dtlNominal")) {
                                        filter { eq("dtlTskId", trx.tskId ?: -1) }
                                        limit(1)
                                    }
                                    .decodeList<DetailTransaksiRelasi>()
                                    .firstOrNull()?.dtlNominal?.toPlainString()?.toDoubleOrNull()
                            } catch (_: Exception) { null }
                            "-" to (nilai ?: "-")
                        }
                        else -> "-" to "-"
                    }
                    listOf(
                        index + 1, tanggal,
                        trx.tskIdPengguna?.pgnRekening.orDashIfBlank(),
                        trx.tskIdPengguna?.pgnNama.orDashIfBlank(),
                        trx.tskIdPengguna?.pgnEmail.orDashIfBlank(),
                        masuk, keluar,
                        trx.tskKeterangan.orDashIfBlank()
                    )
                }

                lastBuilt = BuiltReport.Simple("Laporan Transaksi","Laporan_Transaksi", headers, rows, decimalColumns = listOf(5,6))
                _lastHtml.value = wrapHtml(buildHtmlSimpleTable("Laporan Transaksi", headers, rows, listOf(5,6)))
                _preview.emit(PreviewSignal("Laporan Transaksi"))
            } catch (e: Exception) {
                Log.e("LaporanVM","previewTransaksi gagal: ${e.message}", e)
                _toast.emit("Gagal memuat preview transaksi")
            } finally { _isLoading.value = false }
        }
    }

    fun previewNasabah() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // TANPA filter tanggal
                val list = client.from("pengguna")
                    .select { filter { eq("pgnIsAdmin", false) } }
                    .decodeList<Pengguna>()
                    .sortedBy { it.pgnNama?.lowercase(Locale.ROOT) ?: "\uFFFF" }

                val headers = listOf("Nomor","Tanggal Terdaftar","No Rekening","Nama","Alamat","Email","Total Setoran (Kg)","Saldo (Rp)")
                val rows = list.mapIndexed { index, p ->
                    val tgl = try {
                        LocalDate.parse(p.created_at)
                            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id","ID")))
                    } catch (_: Exception) {
                        try { ZonedDateTime.parse(p.created_at).format(formatterOutput) } catch (_: Exception) { "-" }
                    }
                    val totalKg = try {
                        client.postgrest.rpc(
                            "hitung_total_jumlah_per_pengguna",
                            buildJsonObject { put("pgn_id_input", p.pgnId) }
                        ).data.toDoubleOrNull() ?: 0.0
                    } catch (_: Exception) { 0.0 }
                    val saldo = try { p.pgnSaldo?.toPlainString()?.toDouble() ?: 0.0 } catch (_: Exception) { 0.0 }

                    listOf(
                        index + 1,
                        tgl,
                        p.pgnRekening.orDashIfBlank(),
                        p.pgnNama.orDashIfBlank(),
                        p.pgnAlamat.orDashIfBlank(),
                        p.pgnEmail.orDashIfBlank(),
                        totalKg, saldo
                    )
                }

                lastBuilt = BuiltReport.Simple("Laporan Nasabah","Laporan_Nasabah", headers, rows, decimalColumns = listOf(6,7))
                _lastHtml.value = wrapHtml(buildHtmlSimpleTable("Laporan Nasabah", headers, rows, listOf(6,7)))
                _preview.emit(PreviewSignal("Laporan Nasabah"))
            } catch (e: Exception) {
                _toast.emit("Gagal memuat preview nasabah")
            } finally { _isLoading.value = false }
        }
    }

    fun previewSetoran() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val rangeIso = currentRangeIso()

                val detailMasuk = client.from("detail_transaksi")
                    .select(
                        columns = Columns.raw(
                            """
                            dtlJumlah,
                            dtlSphId ( sphJenis, sphKtgId ( ktgNama ) ),
                            dtlTskId!inner (
                                tskId, created_at, tskTipe,
                                tskIdPengguna ( pgnNama, pgnEmail )
                            )
                            """.trimIndent()
                        )
                    ) {
                        filter {
                            eq("dtlTskId.tskTipe", "Masuk")
                            if (rangeIso != null) {
                                gte("dtlTskId.created_at", rangeIso.first)
                                lte("dtlTskId.created_at", rangeIso.second)
                            }
                        }
                    }
                    .decodeList<DetailTransaksiRelasi>()

                val katToJenis = linkedMapOf<String, LinkedHashSet<String>>()

                detailMasuk.forEach { d ->
                    val kn = d.dtlSphId?.sphKtgId?.ktgNama
                    val kategori = if (kn.isNullOrBlank() || kn.equals("EMPTY", true)) "Lainnya" else kn
                    val j = d.dtlSphId?.sphJenis?.takeIf { it.isNotBlank() && !it.equals("EMPTY", true) } ?: return@forEach
                    katToJenis.getOrPut(kategori) { LinkedHashSet() }.add(j)
                }
                val ordered = LinkedHashMap<String, List<String>>().apply { katToJenis.forEach { (k,v) -> put(k, v.toList()) } }

                val fixed = listOf("Nomor","Tanggal","Nama","Email")
                val grouped = detailMasuk.groupBy { it.dtlTskId?.tskId ?: -1L }
                val sortedGroups = grouped.entries.sortedBy { e ->
                    val created = e.value.firstOrNull()?.dtlTskId?.created_at
                    parseInstantOrMax(created)
                }

                val rows = sortedGroups.mapIndexed { index, entry ->
                    val sample = entry.value.first()
                    val tanggal = try {
                        ZonedDateTime.parse(sample.dtlTskId?.created_at).format(formatterOutput)
                    } catch (_: Exception) { "-" }
                    val nama  = sample.dtlTskId?.tskIdPengguna?.pgnNama.orDashIfBlank()
                    val email = sample.dtlTskId?.tskIdPengguna?.pgnEmail.orDashIfBlank()

                    val perJenis = mutableMapOf<String, Double>()
                    entry.value.forEach { d ->
                        val jenis = d.dtlSphId?.sphJenis ?: return@forEach
                        val jml = d.dtlJumlah?.toPlainString()?.toDoubleOrNull() ?: 0.0
                        perJenis[jenis] = (perJenis[jenis] ?: 0.0) + jml
                    }

                    val nilai = buildList {
                        ordered.forEach { (_, jenisList) -> jenisList.forEach { add(perJenis[it] ?: 0.0) } }
                    }
                    listOf(index + 1, tanggal, nama, email) + nilai
                }

                lastBuilt = BuiltReport.PivotSetoran("Laporan Setoran","Laporan_Setoran", fixed, ordered, rows)
                _lastHtml.value = wrapHtml(buildHtmlPivotSetoran("Laporan Setoran", fixed, ordered, rows))
                _preview.emit(PreviewSignal("Laporan Setoran"))
            } catch (e: Exception) {
                Log.e("LaporanVM","previewSetoran gagal: ${e.message}", e)
                _toast.emit("Gagal memuat preview setoran")
            } finally { _isLoading.value = false }
        }
    }

    fun previewSampah() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // TANPA filter tanggal
                val hasil = client.from("sampah")
                    .select(
                        columns = Columns.raw(
                            """
                            sphJenis, sphSatuan, sphHarga, sphKeterangan,
                            sphKtgId ( ktgNama )
                            """.trimIndent()
                        )
                    )
                    .decodeList<SampahRelasi>()

                val grouped = linkedMapOf<String, MutableList<SampahRelasi>>()
                hasil.forEach { s ->
                    val kn = s.sphKtgId?.ktgNama
                    val kat = if (kn.isNullOrBlank() || kn.equals("EMPTY", true)) "Lainnya" else kn
                    grouped.getOrPut(kat) { mutableListOf() }.add(s)
                }

                lastBuilt = BuiltReport.SampahMerged("Data Sampah","Data_Sampah", grouped)
                _lastHtml.value = wrapHtml(buildHtmlSampahMerged(grouped))
                _preview.emit(PreviewSignal("Data Sampah"))
            } catch (e: Exception) {
                _toast.emit("Gagal memuat preview sampah")
            } finally { _isLoading.value = false }
        }
    }

    /** Simpan file dari data yang sudah di-preview (tanpa hit ulang) */
    fun saveCurrentPreviewAsExcel() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                when (val b = lastBuilt) {
                    is BuiltReport.Simple       -> saveWorkbook(b.sheetName, b.filePrefix, b.headers, b.rows, b.decimalColumns)
                    is BuiltReport.PivotSetoran -> saveWorkbookDirect(generateWorkbookSetoranPivot(b.sheetName, b.katToJenis, b.rows), b.filePrefix)
                    is BuiltReport.SampahMerged -> saveWorkbookDirect(generateWorkbookSampahMerged(b.sheetName, b.grouped), b.filePrefix)
                    null -> { _toast.emit("Tidak ada data untuk disimpan"); return@launch }
                }
                _toast.emit("Berhasil disimpan di folder Download")
            } catch (e: Exception) {
                Log.e("LaporanVM","saveCurrentPreviewAsExcel gagal: ${e.message}", e)
                _toast.emit("Gagal menyimpan file")
            } finally { _isLoading.value = false }
        }
    }

    // =========================
    //  Helpers Excel
    // =========================

    private suspend fun saveWorkbook(
        sheetName: String,
        filePrefix: String,
        headers: List<String>,
        data: List<List<Any?>>,
        decimalColumns: List<Int> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val workbook = generateWorkbook(sheetName, headers, data, decimalColumns)
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.xlsx"
        val filePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        FileOutputStream(filePath).use { workbook.write(it) }
        workbook.close()
    }

    private fun generateWorkbook(
        sheetName: String,
        headers: List<String>,
        data: List<List<Any?>>,
        decimalColumns: List<Int> = emptyList()
    ): Workbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(sheetName)

        val headerFont = workbook.createFont().apply { bold = true }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont); alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }
        val dataStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }
        val decimalStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
            dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
        }

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(index, 20 * 256)
        }

        data.forEachIndexed { r, rowData ->
            val row = sheet.createRow(r + 1)
            rowData.forEachIndexed { c, v ->
                val cell = row.createCell(c)
                when (v) {
                    is Number -> { cell.setCellValue(v.toDouble()); cell.cellStyle = if (c in decimalColumns) decimalStyle else dataStyle }
                    is String -> { cell.setCellValue(v); cell.cellStyle = dataStyle }
                    else -> { cell.setCellValue(v?.toString() ?: ""); cell.cellStyle = dataStyle }
                }
            }
        }
        return workbook
    }

    private suspend fun saveWorkbookDirect(workbook: Workbook, filePrefix: String) = withContext(Dispatchers.IO) {
        val fileName = "${filePrefix}_${System.currentTimeMillis()}.xlsx"
        val filePath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        FileOutputStream(filePath).use { workbook.write(it) }
        workbook.close()
    }

    private fun generateWorkbookSetoranPivot(
        sheetName: String,
        katToJenis: LinkedHashMap<String, List<String>>,
        data: List<List<Any?>>
    ): Workbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(sheetName)

        val headerFont = workbook.createFont().apply { bold = true }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont); alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }
        val dataStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }
        val decimalStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
            dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
        }

        val top = sheet.createRow(0)
        val sub = sheet.createRow(1)

        val fixed = listOf("Nomor", "Tanggal", "Nama", "Email")
        var col = 0
        fixed.forEachIndexed { idx, title ->
            val cTop = top.createCell(col)
            cTop.setCellValue(title)
            cTop.cellStyle = headerStyle
            sheet.addMergedRegion(CellRangeAddress(0, 1, col, col))
            sheet.setColumnWidth(col, (if (idx == 1) 16 else 14) * 256)
            col++
        }

        katToJenis.forEach { (kategori, jenisList) ->
            if (jenisList.isEmpty()) return@forEach
            val start = col
            jenisList.forEach { jenis ->
                val cSub = sub.createCell(col)
                cSub.setCellValue(jenis)
                cSub.cellStyle = headerStyle
                sheet.setColumnWidth(col, 12 * 256)
                col++
            }
            val cTop = top.createCell(start)
            cTop.setCellValue(kategori)
            cTop.cellStyle = headerStyle
            sheet.addMergedRegion(CellRangeAddress(0, 0, start, col - 1))
        }

        data.forEachIndexed { rIdx, rowVals ->
            val row = sheet.createRow(rIdx + 2)
            rowVals.forEachIndexed { cIdx, v ->
                val cell = row.createCell(cIdx)
                when (v) {
                    is Number -> { cell.setCellValue(v.toDouble()); cell.cellStyle = if (cIdx >= 4) decimalStyle else dataStyle }
                    is String -> { cell.setCellValue(v); cell.cellStyle = dataStyle }
                    else -> { cell.setCellValue(v?.toString() ?: ""); cell.cellStyle = dataStyle }
                }
            }
        }
        return workbook
    }

    private fun generateWorkbookSampahMerged(
        sheetName: String,
        grouped: LinkedHashMap<String, MutableList<SampahRelasi>>
    ): Workbook {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet(sheetName)

        val headerFont = workbook.createFont().apply { bold = true }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont); alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }
        val dataStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER; verticalAlignment = VerticalAlignment.CENTER
        }

        val headers = listOf("Nomor", "Kategori", "Jenis Sampah", "Satuan", "Harga", "Keterangan")
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { i, t ->
            val c = headerRow.createCell(i)
            c.setCellValue(t); c.cellStyle = headerStyle
            sheet.setColumnWidth(i, when (i) {
                0 -> 8 * 256; 1 -> 18 * 256; 2 -> 24 * 256; 3 -> 12 * 256; 4 -> 14 * 256; else -> 28 * 256
            })
        }

        var rowIdx = 1
        var nomor = 1
        grouped.forEach { (kategori, list) ->
            val start = rowIdx
            list.forEach { s ->
                val r = sheet.createRow(rowIdx++)
                r.createCell(0).apply { setCellValue(nomor++.toDouble()); cellStyle = dataStyle }
                r.createCell(1).apply { } // merge setelah loop
                r.createCell(2).apply { setCellValue(s.sphJenis.orDashIfBlank());   cellStyle = dataStyle }
                r.createCell(3).apply { setCellValue(s.sphSatuan.orDashIfBlank());  cellStyle = dataStyle }
                r.createCell(4).apply { setCellValue((s.sphHarga ?: 0).toDouble()); cellStyle = dataStyle }
                r.createCell(5).apply { setCellValue(s.sphKeterangan.orDashIfBlank()); cellStyle = dataStyle }
            }
            val end = rowIdx - 1
            sheet.getRow(start).getCell(1).apply { setCellValue(kategori); cellStyle = dataStyle }
            if (end > start) sheet.addMergedRegion(CellRangeAddress(start, end, 1, 1))
        }
        return workbook
    }

    // =========================
    //  Helpers HTML (preview)
    // =========================

    private fun esc(s: String) = s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")

    private fun fmt(value: Any?, dec: Boolean): String = when (value) {
        null -> "-"
        is Number -> if (dec) String.format(Locale("id","ID"), "%,.2f", value.toDouble()) else value.toString()
        else -> esc(value.toString())
    }

    private fun buildHtmlSimpleTable(
        title: String,
        headers: List<String>,
        rows: List<List<Any?>>,
        decimalColumns: List<Int> = emptyList()
    ): String {
        val thead = buildString {
            append("<thead><tr>")
            headers.forEach { append("<th>").append(esc(it)).append("</th>") }
            append("</tr></thead>")
        }
        val tbody = buildString {
            append("<tbody>")
            rows.forEach { r ->
                append("<tr>")
                r.forEachIndexed { i, v -> append("<td>").append(fmt(v, i in decimalColumns)).append("</td>") }
                append("</tr>")
            }
            append("</tbody>")
        }
        return """<h3 class="title">${esc(title)}</h3><table class="tbl">$thead$tbody</table>"""
    }

    private fun buildHtmlPivotSetoran(
        title: String,
        fixedHeaders: List<String>,
        katToJenis: LinkedHashMap<String, List<String>>,
        rows: List<List<Any?>>
    ): String {
        val top = buildString {
            append("<tr>")
            fixedHeaders.forEach { append("""<th rowspan="2">""").append(esc(it)).append("</th>") }
            katToJenis.forEach { (kat, jenis) -> append("""<th colspan="${jenis.size}">""").append(esc(kat)).append("</th>") }
            append("</tr>")
        }
        val sub = buildString {
            append("<tr>")
            katToJenis.values.forEach { jl -> jl.forEach { append("<th>").append(esc(it)).append("</th>") } }
            append("</tr>")
        }
        val body = buildString {
            append("<tbody>")
            rows.forEach { r ->
                append("<tr>")
                r.forEachIndexed { idx, v -> append("<td>").append(fmt(v, idx >= fixedHeaders.size)).append("</td>") }
                append("</tr>")
            }
            append("</tbody>")
        }
        return """<h3 class="title">${esc(title)}</h3><table class="tbl"><thead>$top$sub</thead>$body</table>"""
    }

    private fun buildHtmlSampahMerged(
        grouped: LinkedHashMap<String, MutableList<SampahRelasi>>
    ): String {
        val headers = listOf("Nomor","Kategori","Jenis Sampah","Satuan","Harga","Keterangan")
        val thead = buildString {
            append("<thead><tr>")
            headers.forEach { append("<th>").append(esc(it)).append("</th>") }
            append("</tr></thead>")
        }
        var nomor = 1
        val tbody = buildString {
            append("<tbody>")
            grouped.forEach { (kat, list) ->
                list.forEachIndexed { idx, s ->
                    append("<tr>")
                    append("<td>").append(nomor++).append("</td>")
                    if (idx == 0) append("""<td rowspan="${list.size}">""").append(esc(kat)).append("</td>")
                    append("<td>").append(esc(s.sphJenis.orDashIfBlank())).append("</td>")
                    append("<td>").append(esc(s.sphSatuan.orDashIfBlank())).append("</td>")
                    append("<td>").append(fmt(s.sphHarga ?: 0, false)).append("</td>")
                    append("<td>").append(esc(s.sphKeterangan.orDashIfBlank())).append("</td>")
                    append("</tr>")
                }
            }
            append("</tbody>")
        }
        return """<h3 class="title">Data Sampah</h3><table class="tbl">$thead$tbody</table>"""
    }

    private fun wrapHtml(body: String) = """
      <!doctype html><html><head>
      <meta name="viewport" content="width=device-width,initial-scale=1"/>
      <style>
        body{font-family:sans-serif;margin:0;padding:12px;}
        .title{margin:8px 0 12px 0;}
        table.tbl{border-collapse:collapse;width:100%;}
        th,td{border:1px solid #ddd;padding:6px;text-align:center;white-space:nowrap;}
        thead th{background:#FFC000;position:sticky;top:0;z-index:2;}
        thead tr:nth-child(2) th{top:36px;}
        tbody tr:nth-child(even){background:#fafafa;}
        /* HAPUS .container overflow */
      </style></head>
      <body>$body</body></html>
    """.trimIndent()

    private fun parseInstantOrMax(s: String?): Instant =
        try { ZonedDateTime.parse(s).toInstant() }
        catch (_: Exception) { Instant.ofEpochMilli(Long.MAX_VALUE) } // yang gagal parse taruh di bawah

    private fun String?.orDashIfBlank(): String =
        if (this == null || this.isBlank() || this.equals("EMPTY", true)) "-" else this
}