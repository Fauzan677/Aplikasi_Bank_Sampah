package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.gemahripah.banksampah.R
import com.gemahripah.banksampah.data.model.transaksi.gabungan.DetailTransaksiRelasi
import com.gemahripah.banksampah.data.model.transaksi.gabungan.TransaksiRelasi
import com.gemahripah.banksampah.data.supabase.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.os.Environment
import android.widget.Toast
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LaporanFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_laporan, container, false)
    }

    // Di dalam LaporanFragment.kt
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardTransaksi = view.findViewById<CardView>(R.id.transaksi)

        cardTransaksi.setOnClickListener {
            ambilDataTransaksiLengkap()
        }
    }

    private fun ambilDataTransaksiLengkap() {
        lifecycleScope.launch {
            try {
                val columns = Columns.raw(
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

                val hasil = SupabaseProvider.client
                    .from("transaksi")
                    .select(columns = columns)
                    .decodeList<TransaksiRelasi>()

                // Buat workbook dan sheet baru
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Laporan Transaksi")

                // Buat header style bold
                val headerStyle: CellStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                }

                // Buat header row
                val headerRow = sheet.createRow(0)
                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Tipe", "Total", "Keterangan")
                headers.forEachIndexed { index, title ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(title)
                    cell.cellStyle = headerStyle as XSSFCellStyle?
                }

                // Isi data mulai dari baris 1
                hasil.forEachIndexed { index, transaksi ->
                    val row = sheet.createRow(index + 1)

                    // Hitung total
                    val totalResult = when (transaksi.tskTipe) {
                        "Masuk" -> SupabaseProvider.client
                            .postgrest
                            .rpc(
                                "hitung_total_harga",
                                buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                            )
                        "Keluar" -> SupabaseProvider.client
                            .postgrest
                            .rpc(
                                "hitung_total_jumlah",
                                buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                            )
                        else -> null
                    }
                    val totalDouble = totalResult?.data?.toString()?.toDoubleOrNull() ?: 0.0

                    // Nomor
                    row.createCell(0).setCellValue((index + 1).toDouble())

                    // Tanggal
                    val tanggalString = transaksi.created_at

                    val formatterOutput = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

                    val tanggalFormatted = try {
                        if (tanggalString != null) {
                            val zonedDate = ZonedDateTime.parse(tanggalString) // Gunakan parser bawaan
                            formatterOutput.format(zonedDate)
                        } else {
                            "-"
                        }
                    } catch (e: Exception) {
                        "-"
                    }


                    row.createCell(1).setCellValue(tanggalFormatted)

                    // Nama
                    row.createCell(2).setCellValue(transaksi.tskIdPengguna?.pgnNama ?: "-")

                    // Email
                    row.createCell(3).setCellValue(transaksi.tskIdPengguna?.pgnEmail ?: "-")

                    // Tipe
                    row.createCell(4).setCellValue(transaksi.tskTipe ?: "-")

                    // Total
                    row.createCell(5).setCellValue(totalDouble)

                    // Keterangan
                    row.createCell(6).setCellValue(transaksi.tskKeterangan ?: "-")
                }

                // Autosize kolom supaya pas konten
                (0..6).forEach { sheet.setColumnWidth(it, 20 * 256) }

                // Simpan file Excel di folder Download (pastikan sudah permission storage)
                val fileName = "Laporan_Transaksi_${System.currentTimeMillis()}.xlsx"
                val filePath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

                FileOutputStream(filePath).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                Toast.makeText(requireContext(), "File Excel berhasil dibuat di Download", Toast.LENGTH_LONG).show()
                Log.d("LaporanFragment", "File Excel disimpan: ${filePath.absolutePath}")

            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal mengambil data atau membuat file Excel: ${e.message}", e)
            }
        }
    }

}