package com.example.bpskota.uipegawai

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.PegawaiTersedia
import com.example.bpskota.bpskp.model.PenugasanResponse
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentPegawaiPenugasanBinding
import java.util.Locale

class PegawaiPenugasanFragment : Fragment() {

    private var _binding: FragmentPegawaiPenugasanBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BpskpRepository
    private lateinit var session: BpskpAuthSession
    private lateinit var penugasanAdapter: PenugasanAdapter

    private val daftarPenugasan = mutableListOf<PenugasanResponse>()
    private val rekomendasiMenunggu = mutableSetOf<Long>()

    private var sedangMemuat = false
    private var sedangMemuatPegawai = false
    private var dialogSedangDiproses: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPegawaiPenugasanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = BpskpRepository()
        session = BpskpAuthSession(requireContext())

        setupRecyclerView()
        muatPenugasan()
        muatRekomendasiMenunggu()
    }

    private fun setupRecyclerView() {
        penugasanAdapter = PenugasanAdapter()
        binding.rvPenugasanPegawai.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = penugasanAdapter
            setHasFixedSize(false)
        }
        tampilkanKondisiKosong(false)
    }

    private fun muatPenugasan() {
        if (!isAdded || _binding == null) return

        repository.getPenugasan(session = session, search = null, status = null) { response, error ->
            if (!isAdded || _binding == null) return@getPenugasan

            if (response?.data != null) {
                daftarPenugasan.clear()
                daftarPenugasan.addAll(response.data)
                penugasanAdapter.notifyDataSetChanged()
                tampilkanKondisiKosong(daftarPenugasan.isEmpty())
            } else {
                daftarPenugasan.clear()
                penugasanAdapter.notifyDataSetChanged()
                tampilkanKondisiKosong(true)
                Toast.makeText(requireContext(), error ?: "Gagal memuat penugasan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun muatRekomendasiMenunggu() {
        if (!isAdded || _binding == null) return

        repository.getRekomendasi(session = session, status = "menunggu_review") { data, _ ->
            if (!isAdded || _binding == null) return@getRekomendasi
            rekomendasiMenunggu.clear()
            data.orEmpty().forEach { if (it.penugasanId > 0L) rekomendasiMenunggu.add(it.penugasanId) }
            penugasanAdapter.notifyDataSetChanged()
        }
    }

    private fun tampilkanKondisiKosong(kosong: Boolean) {
        _binding?.let {
            it.layoutKosongPenugasan.visibility = if (kosong) View.VISIBLE else View.GONE
            it.rvPenugasanPegawai.visibility = if (kosong) View.GONE else View.VISIBLE
        }
    }

    private fun dp(nilai: Int): Int = (nilai * resources.displayMetrics.density).toInt()

    private fun buatTextView(ukuran: Float, warna: Int, tebal: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            textSize = ukuran
            setTextColor(warna)
            if (tebal) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }
        }
    }

    // =========================================================
    // ANIMASI POPUP DIALOG
    // =========================================================

    private fun animateDialog(dialog: AlertDialog, view: View) {
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val width = (resources.displayMetrics.widthPixels * 0.92f).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        view.alpha = 0f
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(280)
            .setInterpolator(OvershootInterpolator(1.15f))
            .start()
    }

    // =========================================================
    // RESPONS PENUGASAN
    // =========================================================

    private fun konfirmasiRespons(item: PenugasanResponse, statusRespons: String) {
        if (!isAdded || _binding == null || sedangMemuat) return
        val idPenugasan = item.id ?: return

        if (statusRespons == "diterima") {
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(22), dp(20), dp(22), dp(20))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(20).toFloat()
                }
            }
            container.addView(buatTextView(18f, Color.parseColor("#111827"), true).apply { text = "Terima Penugasan" })
            container.addView(buatTextView(14f, Color.parseColor("#4B5563")).apply {
                text = "Apakah kamu yakin ingin menerima penugasan \"${item.perihal ?: "-"}\"?"
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
            })

            val dialog = AlertDialog.Builder(requireContext())
                .setView(container)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Ya, Terima") { _, _ ->
                    kirimResponsPenugasan(idPenugasan, "diterima", null)
                }
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#15803D"))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6B7280"))
            }

            dialog.show()
            animateDialog(dialog, container)
        } else if (statusRespons == "ditolak") {
            tampilkanDialogAlasanPenolakan(item, idPenugasan)
        }
    }

    private fun tampilkanDialogAlasanPenolakan(item: PenugasanResponse, idPenugasan: Long) {
        if (!isAdded || _binding == null || sedangMemuat) return

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }

        container.addView(buatTextView(18f, Color.parseColor("#111827"), true).apply { text = "Alasan Penolakan" })
        container.addView(buatTextView(14f, Color.parseColor("#4B5563")).apply {
            text = "Perihal: ${item.perihal ?: "-"}\nNomor surat: ${item.nomorSurat ?: "-"}\n\nAlasan penolakan wajib diisi."
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                bottomMargin = dp(14)
            }
        })

        val inputAlasan = EditText(requireContext()).apply {
            hint = "Tuliskan alasan penolakan..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F9FAFB"))
                setStroke(dp(1), Color.parseColor("#E5E7EB"))
                cornerRadius = dp(12).toFloat()
            }
        }
        container.addView(inputAlasan)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Kirim Penolakan", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#DC2626"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6B7280"))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val alasan = inputAlasan.text.toString().trim()
                if (alasan.isBlank()) {
                    inputAlasan.error = "Alasan penolakan wajib diisi."
                    inputAlasan.requestFocus()
                    return@setOnClickListener
                }
                dialog.dismiss()
                kirimResponsPenugasan(idPenugasan, "ditolak", alasan)
            }
        }

        dialog.show()
        animateDialog(dialog, container)
    }

    private fun kirimResponsPenugasan(idPenugasan: Long, statusRespons: String, catatanRespon: String?) {
        if (!isAdded || _binding == null || sedangMemuat) return

        sedangMemuat = true
        penugasanAdapter.notifyDataSetChanged()

        tampilkanDialogProses(if (statusRespons == "diterima") "Menyimpan penerimaan..." else "Menyimpan penolakan...")

        repository.responPenugasan(
            session = session,
            id = idPenugasan,
            status = statusRespons,
            catatanRespon = catatanRespon
        ) { response, error ->
            if (!isAdded || _binding == null) {
                sedangMemuat = false
                tutupDialogProses()
                return@responPenugasan
            }

            sedangMemuat = false
            tutupDialogProses()

            if (response?.data != null) {
                Toast.makeText(requireContext(), response.message ?: "Respons berhasil disimpan.", Toast.LENGTH_LONG).show()
                muatPenugasan()
            } else {
                penugasanAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), error ?: response?.message ?: "Gagal menyimpan respons.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================
    // REKOMENDASI PEGAWAI
    // =========================================================

    private fun mulaiRekomendasi(item: PenugasanResponse) {
        if (!isAdded || _binding == null || sedangMemuat || sedangMemuatPegawai) return
        val idPenugasan = item.id ?: return

        val status = item.status.orEmpty()
        if (!status.equals("diterima", true) && !status.equals("menunggu_respon", true)) {
            Toast.makeText(requireContext(), "Rekomendasi hanya dapat diajukan ketika penugasan menunggu respons atau sudah diterima.", Toast.LENGTH_LONG).show()
            return
        }

        if (idPenugasan in rekomendasiMenunggu) {
            Toast.makeText(requireContext(), "Penugasan ini sudah memiliki rekomendasi yang menunggu review.", Toast.LENGTH_LONG).show()
            return
        }

        sedangMemuatPegawai = true
        tampilkanDialogProses("Memuat daftar pegawai...")

        repository.getPegawaiPengganti(session = session, idPenugasan = idPenugasan) { pegawai, error ->
            if (!isAdded || _binding == null) {
                sedangMemuatPegawai = false
                tutupDialogProses()
                return@getPegawaiPengganti
            }

            sedangMemuatPegawai = false
            tutupDialogProses()

            if (pegawai == null) {
                Toast.makeText(requireContext(), error ?: "Gagal memuat daftar pegawai.", Toast.LENGTH_LONG).show()
                return@getPegawaiPengganti
            }

            val pilihanPegawai = pegawai.filter { it.id > 0 && it.role.equals("pegawai", ignoreCase = true) }
            if (pilihanPegawai.isEmpty()) {
                Toast.makeText(requireContext(), "Tidak ada pegawai pengganti yang tersedia.", Toast.LENGTH_SHORT).show()
                return@getPegawaiPengganti
            }

            tampilkanDialogRekomendasi(item, idPenugasan, pilihanPegawai)
        }
    }

    private fun tampilkanDialogRekomendasi(
        item: PenugasanResponse,
        idPenugasan: Long,
        daftarPegawai: List<PegawaiTersedia>
    ) {
        if (!isAdded || _binding == null) return

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }

        container.addView(buatTextView(18f, Color.parseColor("#111827"), true).apply { text = "Rekomendasikan Pegawai" })
        container.addView(buatTextView(14f, Color.parseColor("#4B5563")).apply {
            text = "Perihal: ${item.perihal ?: "-"}\n\nPilih pegawai yang direkomendasikan:"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(8)
                bottomMargin = dp(14)
            }
        })

        val spinnerPegawai = Spinner(requireContext()).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F9FAFB"))
                setStroke(dp(1), Color.parseColor("#E5E7EB"))
                cornerRadius = dp(10).toFloat()
            }
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        spinnerPegawai.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            daftarPegawai.map { it.nama }
        )
        container.addView(spinnerPegawai, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))

        val labelAlasan = buatTextView(13f, Color.parseColor("#374151"), true).apply {
            text = "Alasan rekomendasi"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(14)
                bottomMargin = dp(6)
            }
        }
        container.addView(labelAlasan)

        val inputAlasan = EditText(requireContext()).apply {
            hint = "Jelaskan alasan rekomendasi..."
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            gravity = Gravity.TOP
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F9FAFB"))
                setStroke(dp(1), Color.parseColor("#E5E7EB"))
                cornerRadius = dp(12).toFloat()
            }
        }
        container.addView(inputAlasan)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(container)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Kirim Rekomendasi", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F97316"))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#6B7280"))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val posisi = spinnerPegawai.selectedItemPosition
                val alasan = inputAlasan.text.toString().trim()

                if (posisi !in daftarPegawai.indices) {
                    Toast.makeText(requireContext(), "Pilih pegawai yang direkomendasikan.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (alasan.length < 5) {
                    inputAlasan.error = "Alasan minimal 5 karakter."
                    inputAlasan.requestFocus()
                    return@setOnClickListener
                }

                dialog.dismiss()
                kirimRekomendasi(idPenugasan, daftarPegawai[posisi].id, alasan)
            }
        }

        dialog.show()
        animateDialog(dialog, container)
    }

    private fun kirimRekomendasi(idPenugasan: Long, pegawaiId: Int, alasan: String) {
        if (!isAdded || _binding == null || sedangMemuat) return

        sedangMemuat = true
        penugasanAdapter.notifyDataSetChanged()

        tampilkanDialogProses("Mengirim rekomendasi...")

        repository.buatRekomendasi(
            session = session,
            penugasanId = idPenugasan,
            pegawaiRekomendasiId = pegawaiId,
            alasan = alasan
        ) { response, error ->
            if (!isAdded || _binding == null) {
                sedangMemuat = false
                tutupDialogProses()
                return@buatRekomendasi
            }

            sedangMemuat = false
            tutupDialogProses()

            if (response?.success == true) {
                rekomendasiMenunggu.add(idPenugasan)
                Toast.makeText(requireContext(), response.message ?: "Rekomendasi berhasil dikirim.", Toast.LENGTH_LONG).show()
                muatPenugasan()
                muatRekomendasiMenunggu()
            } else {
                penugasanAdapter.notifyDataSetChanged()
                Toast.makeText(requireContext(), error ?: response?.message ?: "Gagal mengirim rekomendasi.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun tampilkanDialogProses(pesan: String) {
        if (!isAdded || _binding == null) return
        tutupDialogProses()
        dialogSedangDiproses = AlertDialog.Builder(requireContext()).setMessage(pesan).setCancelable(false).create()
        dialogSedangDiproses?.show()
    }

    private fun tutupDialogProses() {
        dialogSedangDiproses?.dismiss()
        dialogSedangDiproses = null
    }

    // =========================================================
    // DETAIL PENUGASAN
    // =========================================================

    private fun tampilkanDetail(item: PenugasanResponse) {
        val scroll = ScrollView(requireContext()).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(24).toFloat()
            }
        }
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(22))
        }
        scroll.addView(container)

        container.addView(buatTextView(20f, Color.parseColor("#111827"), true).apply {
            text = "Detail Penugasan"
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) }
        })

        val details = listOf(
            "Perihal" to item.perihal,
            "Nomor Surat" to item.nomorSurat,
            "Ditugaskan Oleh" to item.namaPengirim,
            "Ditugaskan Kepada" to item.namaPenerima,
            "Waktu Penugasan" to item.waktuPenugasan,
            "Catatan Penugasan" to item.catatanPenugasan,
            "Status" to (item.status?.replace("_", " ")?.uppercase(Locale.ROOT) ?: "-"),
            "Waktu Respons" to item.waktuRespon,
            "Catatan Respons" to item.catatanRespon
        )

        details.forEach { (label, value) ->
            if (!value.isNullOrBlank()) {
                container.addView(buatTextView(12f, Color.parseColor("#6B7280"), true).apply {
                    text = label
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) }
                })
                container.addView(buatTextView(14f, Color.parseColor("#111827")).apply { text = value })
            }
        }

        val dialog = AlertDialog.Builder(requireContext()).setView(scroll).setPositiveButton("Tutup", null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#F97316")) }
        dialog.show()
        animateDialog(dialog, scroll)
    }

    // =========================================================
    // RECYCLERVIEW ADAPTER (MENGGUNAKAN item_user.xml)
    // =========================================================

    private inner class PenugasanAdapter : RecyclerView.Adapter<PenugasanAdapter.PenugasanViewHolder>() {

        inner class PenugasanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPerihal: TextView = itemView.findViewById(R.id.tvUserName)
            val tvNomorSurat: TextView = itemView.findViewById(R.id.tvUserUsername)
            val tvStatus: TextView = itemView.findViewById(R.id.tvUserRole)
            val tvPengirim: TextView = itemView.findViewById(R.id.tvKegiatanTanggal)
            val tvWaktu: TextView = itemView.findViewById(R.id.tvKegiatanJam)
            val tvCatatan: TextView = itemView.findViewById(R.id.tvKegiatanLokasi)

            val btnTerima: TextView = itemView.findViewById(R.id.btnEditUser)
            val btnTolak: TextView = itemView.findViewById(R.id.btnDeleteUser)

            val btnRekomendasi: TextView = TextView(itemView.context).apply {
                text = "Rekomendasi"
                textSize = 13f
                setTextColor(Color.parseColor("#E65100"))
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setPadding(dp(16), 0, dp(16), 0)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FFEDCC"))
                    cornerRadius = dp(6).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(36)
                ).apply {
                    marginStart = dp(8)
                }
            }

            init {
                // Menyisipkan tombol Rekomendasi di sebelah tombol Terima/Tolak
                (btnTerima.parent as? ViewGroup)?.addView(btnRekomendasi)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PenugasanViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return PenugasanViewHolder(view)
        }

        override fun onBindViewHolder(holder: PenugasanViewHolder, position: Int) {
            val item = daftarPenugasan[position]
            val status = item.status ?: "tidak diketahui"

            // 1. Perihal & Nomor Surat
            holder.tvPerihal.text = item.perihal ?: "Perihal tidak tersedia"
            holder.tvNomorSurat.text = "Nomor Surat: ${item.nomorSurat ?: "-"}"

            // 2. Ditugaskan oleh
            holder.tvPengirim.text = "Ditugaskan oleh: ${item.namaPengirim ?: "-"}"
            holder.tvPengirim.visibility = View.VISIBLE

            // 3. Waktu Penugasan
            holder.tvWaktu.text = "Waktu: ${item.waktuPenugasan ?: "-"}"
            holder.tvWaktu.visibility = View.VISIBLE

            // 4. Catatan Penugasan (Jika Kosong -> View.GONE)
            val catatan = item.catatanPenugasan?.takeIf { it.isNotBlank() }
            if (catatan != null) {
                holder.tvCatatan.visibility = View.VISIBLE
                holder.tvCatatan.text = "Catatan: $catatan"
            } else {
                holder.tvCatatan.visibility = View.GONE
            }

            // 5. Badge Status Color
            holder.tvStatus.text = status.replace("_", " ").uppercase(Locale.ROOT)
            val (warnaLatar, warnaTeks) = when (status.lowercase(Locale.ROOT)) {
                "menunggu_respon" -> Color.parseColor("#FFF7ED") to Color.parseColor("#E65100")
                "diterima" -> Color.parseColor("#DCFCE7") to Color.parseColor("#15803D")
                "ditolak" -> Color.parseColor("#FEE2E2") to Color.parseColor("#B91C1C")
                else -> Color.parseColor("#F3F4F6") to Color.parseColor("#4B5563")
            }

            holder.tvStatus.background = GradientDrawable().apply {
                setColor(warnaLatar)
                cornerRadius = dp(6).toFloat()
            }
            holder.tvStatus.setTextColor(warnaTeks)

            // 6. Tombol Aksi
            holder.btnTerima.text = "Terima"
            holder.btnTolak.text = "Tolak"

            val menungguRespons = status.equals("menunggu_respon", ignoreCase = true)
            holder.btnTerima.visibility = if (menungguRespons) View.VISIBLE else View.GONE
            holder.btnTolak.visibility = if (menungguRespons) View.VISIBLE else View.GONE

            val idPenugasan = item.id
            val adaRekomendasiMenunggu = idPenugasan != null && idPenugasan in rekomendasiMenunggu
            val bisaRekomendasi = status.equals("diterima", ignoreCase = true) || status.equals("menunggu_respon", ignoreCase = true)

            holder.btnRekomendasi.visibility = if (bisaRekomendasi && !adaRekomendasiMenunggu) View.VISIBLE else View.GONE

            val parentBarisTombol = holder.btnTerima.parent as? View
            parentBarisTombol?.visibility = if (menungguRespons || (bisaRekomendasi && !adaRekomendasiMenunggu)) View.VISIBLE else View.GONE

            holder.btnTerima.isEnabled = !sedangMemuat
            holder.btnTolak.isEnabled = !sedangMemuat
            holder.btnRekomendasi.isEnabled = !sedangMemuat && !sedangMemuatPegawai

            holder.btnTerima.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && !sedangMemuat) {
                    konfirmasiRespons(daftarPenugasan[pos], "diterima")
                }
            }

            holder.btnTolak.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && !sedangMemuat) {
                    konfirmasiRespons(daftarPenugasan[pos], "ditolak")
                }
            }

            holder.btnRekomendasi.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION && !sedangMemuat && !sedangMemuatPegawai) {
                    mulaiRekomendasi(daftarPenugasan[pos])
                }
            }

            holder.itemView.setOnClickListener {
                val pos = holder.adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    tampilkanDetail(daftarPenugasan[pos])
                }
            }

            // 7. Animasi Masuk Item Card
            holder.itemView.animate().cancel()
            holder.itemView.alpha = 0f
            holder.itemView.translationY = dp(20).toFloat()
            holder.itemView.scaleX = 0.96f
            holder.itemView.scaleY = 0.96f
            holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(OvershootInterpolator(1.1f))
                .setStartDelay((position % 6) * 35L)
                .start()
        }

        override fun getItemCount(): Int = daftarPenugasan.size
    }

    override fun onDestroyView() {
        tutupDialogProses()
        sedangMemuat = false
        sedangMemuatPegawai = false
        if (_binding != null) {
            binding.rvPenugasanPegawai.adapter = null
        }
        _binding = null
        super.onDestroyView()
    }
}