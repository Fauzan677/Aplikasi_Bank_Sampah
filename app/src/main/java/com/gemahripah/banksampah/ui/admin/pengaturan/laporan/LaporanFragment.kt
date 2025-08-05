package com.gemahripah.banksampah.ui.admin.pengaturan.laporan

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.gemahripah.banksampah.data.model.sampah.gabungan.SampahRelasi
import com.gemahripah.banksampah.databinding.FragmentLaporanBinding
import com.gemahripah.banksampah.ui.admin.AdminActivity
import com.gemahripah.banksampah.utils.NetworkUtil
import com.gemahripah.banksampah.utils.Reloadable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class LaporanFragment : Fragment(), Reloadable {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!
    private var loadingDialog: AlertDialog? = null
    private val supabase = SupabaseProvider.client
    private val formatterOutput = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.transaksi.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            tampilkanDialogKonfirmasi("Konfirmasi", "Apakah Anda ingin menyimpan laporan ini?") {
                ambilDataTransaksiLengkap()
            }
        }

        binding.setoran.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            tampilkanDialogKonfirmasi("Konfirmasi", "Apakah Anda ingin menyimpan laporan ini?") {
                ambilDataSetoran()
            }
        }

        binding.nasabah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            tampilkanDialogKonfirmasi("Konfirmasi", "Apakah Anda ingin menyimpan data nasabah?") {
                ambilDataNasabah()
            }
        }

        binding.sampah.setOnClickListener {
            if (!updateInternetCard()) return@setOnClickListener
            tampilkanDialogKonfirmasi("Konfirmasi", "Apakah Anda ingin menyimpan data sampah?") {
                ambilDataSampah()
            }
        }

        if (!updateInternetCard()) return
    }

    override fun reloadData() {
        if (!updateInternetCard()) return
    }

    private fun tampilkanDialogKonfirmasi(
        title: String,
        message: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ya") { _, _ -> onConfirm() }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun ambilDataSampah() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading()

            try {

                val hasil = withContext(Dispatchers.IO) {
                    val columns = Columns.raw(
                        """
                        created_at,
                        sphJenis,
                        sphSatuan,
                        sphHarga,
                        sphKeterangan,
                        sphKtgId (
                            ktgNama
                        )
                        """.trimIndent())

                    supabase.from("sampah")
                        .select(columns = columns)
                        .decodeList<SampahRelasi>()
                }

                val headers = listOf("Nomor", "Tanggal Dibuat", "Jenis", "Kategori", "Satuan", "Harga", "Keterangan")
                val dataRows = hasil.mapIndexed { index, item ->
                    listOf(
                        (index + 1),
                        try { ZonedDateTime.parse(item.created_at).format(formatterOutput) } catch (e: Exception) { "-" },
                        item.sphJenis ?: "",
                        item.sphKtgId?.ktgNama ?: "",
                        item.sphSatuan ?: "",
                        item.sphHarga ?: 0,
                        item.sphKeterangan ?: ""
                    )
                }

                val workbook = generateWorkbook("Data Sampah", headers, dataRows)
                withContext(Dispatchers.IO) {
                    val fileName = "Data_Sampah_${System.currentTimeMillis()}.xlsx"
                    val filePath = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    FileOutputStream(filePath).use { workbook.write(it) }
                    workbook.close()
                }

                Toast.makeText(requireContext(), "Data sampah berhasil disimpan ke Excel di folder Download", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Gagal ambil data sampah", Toast.LENGTH_SHORT).show()
            } finally {
                hideLoading()
            }
        }
    }

    private fun ambilDataNasabah() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading()

            try {
                val penggunaList = withContext(Dispatchers.IO) {
                    supabase.from("pengguna")
                        .select {
                            filter {
                                eq("pgnIsAdmin", false)
                            }
                        }
                        .decodeList<Pengguna>()
                }

                val headers = listOf(
                    "Nomor", "Nama", "Email", "Tanggal Terdaftar", "Total Setoran", "Total Saldo"
                )

                val dataRows = withContext(Dispatchers.IO) {
                    penggunaList.mapIndexed { index, pengguna ->
                        val tanggalFormatted = try {
                            ZonedDateTime.parse(pengguna.created_at).format(formatterOutput)
                        } catch (e: Exception) {
                            "-"
                        }

                        val totalSetoran = try {
                            SupabaseProvider.client.postgrest.rpc(
                                "hitung_total_jumlah_per_pengguna_masuk",
                                buildJsonObject { put("pgn_id_input", pengguna.pgnId) }
                            ).data.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) {
                            0.0
                        }

                        val totalSaldo = try {
                            SupabaseProvider.client.postgrest.rpc(
                                "hitung_saldo_pengguna",
                                buildJsonObject { put("pgn_id_input", pengguna.pgnId) }
                            ).data.toDoubleOrNull() ?: 0.0
                        } catch (e: Exception) {
                            0.0
                        }

                        listOf(
                            (index + 1),
                            pengguna.pgnNama ?: "-",
                            pengguna.pgnEmail ?: "-",
                            tanggalFormatted,
                            totalSetoran,
                            totalSaldo
                        )
                    }
                }

                val workbook = generateWorkbook("Laporan Nasabah", headers, dataRows)

                withContext(Dispatchers.IO) {
                    val fileName = "Laporan_Nasabah_${System.currentTimeMillis()}.xlsx"
                    val filePath = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    FileOutputStream(filePath).use { workbook.write(it) }
                    workbook.close()
                }
                Toast.makeText(requireContext(), "Laporan Nasabah disimpan di Download", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal ambil data nasabah: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    private fun ambilDataSetoran() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading()

            try {

                val hasilMasuk = withContext(Dispatchers.IO) {
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
                        """.trimIndent())

                    val hasil = supabase.from("detail_transaksi")
                        .select(columns = columns)
                        .decodeList<DetailTransaksiRelasi>()

                    hasil.filter { it.dtlTskId?.tskTipe == "Masuk" }
                }

                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Kategori", "Jenis Sampah", "Satuan", "Jumlah")

                val dataRows = hasilMasuk.mapIndexed { index, detail ->
                    val pengguna = detail.dtlTskId?.tskIdPengguna
                    val sampah = detail.dtlSphId
                    val kategori = sampah?.sphKtgId

                    val tanggalFormatted = try {
                        ZonedDateTime.parse(detail.created_at).format(formatterOutput)
                    } catch (e: Exception) {
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

                val workbook = generateWorkbook("Laporan Setoran", headers, dataRows)

                withContext(Dispatchers.IO) {
                    val fileName = "Laporan_Setoran_${System.currentTimeMillis()}.xlsx"
                    val filePath = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    FileOutputStream(filePath).use { workbook.write(it) }
                    workbook.close()
                }

                Toast.makeText(requireContext(), "Laporan Setoran berhasil disimpan di Download", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal ambil data setoran: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
    }

    private fun ambilDataTransaksiLengkap() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading()

            try {
                val transaksiList = withContext(Dispatchers.IO) {
                    val columns = Columns.raw("""
                    tskId,
                    created_at,
                    tskKeterangan,
                    tskTipe,
                    tskIdPengguna (
                        pgnNama,
                        pgnEmail
                    )
                """.trimIndent())

                    supabase.from("transaksi")
                        .select(columns = columns)
                        .decodeList<TransaksiRelasi>()
                }

                val headers = listOf("Nomor", "Tanggal", "Nama", "Email", "Tipe", "Total", "Keterangan")

                val dataRows = withContext(Dispatchers.IO) {
                    transaksiList.mapIndexed { index, transaksi ->
                        val total = try {
                            when (transaksi.tskTipe) {
                                "Masuk" -> SupabaseProvider.client.postgrest.rpc(
                                    "hitung_total_harga",
                                    buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                                ).data.toDoubleOrNull() ?: 0.0

                                "Keluar" -> SupabaseProvider.client.postgrest.rpc(
                                    "hitung_total_jumlah",
                                    buildJsonObject { put("tsk_id_input", transaksi.tskId) }
                                ).data.toDoubleOrNull() ?: 0.0

                                else -> 0.0
                            }
                        } catch (e: Exception) {
                            0.0
                        }

                        val tanggalFormatted = try {
                            ZonedDateTime.parse(transaksi.created_at).format(formatterOutput)
                        } catch (e: Exception) {
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
                }

                val workbook = generateWorkbook("Laporan Transaksi", headers, dataRows)

                withContext(Dispatchers.IO) {
                    val fileName = "Laporan_Transaksi_${System.currentTimeMillis()}.xlsx"
                    val filePath = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        fileName
                    )

                    FileOutputStream(filePath).use { workbook.write(it) }
                    workbook.close()
                }

                Toast.makeText(requireContext(), "File Excel berhasil dibuat di Download", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e("SUPABASE_ERROR", "Gagal mengambil data atau membuat file Excel: ${e.message}", e)
            } finally {
                hideLoading()
            }
        }
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

        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, title ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(title)
            cell.cellStyle = headerStyle
            sheet.setColumnWidth(index, 20 * 256)
        }

        data.forEachIndexed { rowIndex, rowData ->
            val row = sheet.createRow(rowIndex + 1)
            rowData.forEachIndexed { cellIndex, value ->
                val cell = row.createCell(cellIndex)
                when (value) {
                    is Number -> cell.setCellValue(value.toDouble())
                    is String -> cell.setCellValue(value)
                    else -> cell.setCellValue(value?.toString() ?: "")
                }
            }
        }

        return workbook
    }


    private fun showLoading() {
        if (loadingDialog?.isShowing == true) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)

        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        loadingDialog?.window?.setDimAmount(0.3f)
        loadingDialog?.show()
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
    }

    private fun updateInternetCard(): Boolean {
        val isConnected = NetworkUtil.isInternetAvailable(requireContext())
        val showCard = !isConnected
        (activity as? AdminActivity)?.showNoInternetCard(showCard)
        return isConnected
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}