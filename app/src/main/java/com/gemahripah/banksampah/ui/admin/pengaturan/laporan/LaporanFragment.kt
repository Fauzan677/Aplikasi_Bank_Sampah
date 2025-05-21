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
import androidx.appcompat.app.AlertDialog
import com.gemahripah.banksampah.data.model.pengguna.Pengguna
import com.gemahripah.banksampah.databinding.FragmentLaporanBinding
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LaporanFragment : Fragment() {

    private var loadingDialog: AlertDialog? = null
    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transaksi.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda ingin menyimpan laporan ini?")
                .setPositiveButton("Ya") { _, _ -> ambilDataTransaksiLengkap() }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.setoran.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda ingin menyimpan laporan ini?")
                .setPositiveButton("Ya") { _, _ -> ambilDataSetoran() }
                .setNegativeButton("Batal", null)
                .show()
        }

        binding.nasabah.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda ingin menyimpan data nasabah?")
                .setPositiveButton("Ya") { _, _ -> ambilDataNasabah() }
                .setNegativeButton("Batal", null)
                .show()
        }

    }

    private fun ambilDataNasabah() {
        lifecycleScope.launch {
            showLoading()
            try {
                val penggunaList = SupabaseProvider.client
                    .from("pengguna")
                    .select {
                        filter {
                            eq("pgnIsAdmin", false)
                        }
                    }
                    .decodeList<Pengguna>()

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Laporan Nasabah")

                val headerStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                }

                val headers = listOf(
                    "Nomor", "Nama", "Email", "Tanggal Terdaftar", "Total Setoran", "Total Saldo"
                )
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, title ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(title)
                    cell.cellStyle = headerStyle
                }

                penggunaList.forEachIndexed { index, pengguna ->
                    val row = sheet.createRow(index + 1)

                    // Nomor
                    row.createCell(0).setCellValue((index + 1).toDouble())

                    // Nama & Email
                    row.createCell(1).setCellValue(pengguna.pgnNama ?: "-")
                    row.createCell(2).setCellValue(pengguna.pgnEmail ?: "-")

                    // Tanggal Terdaftar
                    val tanggalFormatted = try {
                        val zonedDate = ZonedDateTime.parse(pengguna.created_at)
                        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")).format(zonedDate)
                    } catch (e: Exception) {
                        "-"
                    }
                    row.createCell(3).setCellValue(tanggalFormatted)

                    // Total Setoran
                    val totalSetoranResult = try {
                        SupabaseProvider.client.postgrest.rpc(
                            "hitung_total_jumlah_per_pengguna_masuk",
                            buildJsonObject {
                                put("pgn_id_input", pengguna.pgnId)
                            }
                        )
                    } catch (e: Exception) {
                        null
                    }
                    val totalSetoran = totalSetoranResult?.data?.toString()?.toDoubleOrNull() ?: 0.0
                    row.createCell(4).setCellValue(totalSetoran)

                    // Total Saldo
                    val totalSaldoResult = try {
                        SupabaseProvider.client.postgrest.rpc(
                            "hitung_saldo_pengguna",
                            buildJsonObject {
                                put("pgn_id_input", pengguna.pgnId)
                            }
                        )
                    } catch (e: Exception) {
                        null
                    }
                    val totalSaldo = totalSaldoResult?.data?.toString()?.toDoubleOrNull() ?: 0.0
                    row.createCell(5).setCellValue(totalSaldo)
                }

                (0 until headers.size).forEach {
                    sheet.setColumnWidth(it, 20 * 256)
                }

                val fileName = "Laporan_Nasabah_${System.currentTimeMillis()}.xlsx"
                val filePath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

                FileOutputStream(filePath).use { workbook.write(it) }
                workbook.close()

                Toast.makeText(requireContext(), "Laporan Nasabah disimpan di Download", Toast.LENGTH_LONG).show()
                Log.d("LaporanFragment", "File nasabah disimpan: ${filePath.absolutePath}")
            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal ambil data nasabah: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    private fun ambilDataSetoran() {
        lifecycleScope.launch {
            showLoading()
            try {
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

                val hasil = SupabaseProvider.client
                    .from("detail_transaksi")
                    .select(columns = columns)
                    .decodeList<DetailTransaksiRelasi>()

                val hasilMasuk = hasil.filter { it.dtlTskId?.tskTipe == "Masuk" }

                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("Laporan Setoran")

                val headerStyle = workbook.createCellStyle().apply {
                    val font = workbook.createFont()
                    font.bold = true
                    setFont(font)
                }

                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Kategori", "Jenis Sampah", "Satuan", "Jumlah")
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { index, title ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(title)
                    cell.cellStyle = headerStyle
                }

                hasilMasuk.forEachIndexed { index, detail ->
                    val row = sheet.createRow(index + 1)

                    val pengguna = detail.dtlTskId?.tskIdPengguna
                    val sampah = detail.dtlSphId
                    val kategori = sampah?.sphKtgId

                    row.createCell(0).setCellValue((index + 1).toDouble())

                    val tanggalFormatted = try {
                        val zonedDate = ZonedDateTime.parse(detail.created_at)
                        DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")).format(zonedDate)
                    } catch (e: Exception) {
                        "-"
                    }
                    row.createCell(1).setCellValue(tanggalFormatted)

                    row.createCell(2).setCellValue(pengguna?.pgnNama ?: "-")
                    row.createCell(3).setCellValue(pengguna?.pgnEmail ?: "-")
                    row.createCell(4).setCellValue(kategori?.ktgNama ?: "-")
                    row.createCell(5).setCellValue(sampah?.sphJenis ?: "-")
                    row.createCell(6).setCellValue(sampah?.sphSatuan ?: "-")
                    row.createCell(7).setCellValue(detail.dtlJumlah ?: 0.0)
                }

                (0 until headers.size).forEach { sheet.setColumnWidth(it, 20 * 256) }

                val fileName = "Laporan_Setoran_${System.currentTimeMillis()}.xlsx"
                val filePath = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

                FileOutputStream(filePath).use { outputStream ->
                    workbook.write(outputStream)
                }
                workbook.close()

                Toast.makeText(requireContext(), "Laporan Setoran berhasil disimpan di Download", Toast.LENGTH_LONG).show()
                Log.d("LaporanFragment", "File disimpan: ${filePath.absolutePath}")
            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal ambil data setoran: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    private fun ambilDataTransaksiLengkap() {
        lifecycleScope.launch {
            showLoading()
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
            } finally {
                hideLoading()
            }
        }
    }

    private fun showLoading() {
        if (loadingDialog?.isShowing == true) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)

        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.window?.setDimAmount(0.3f)
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }
}