package com.example.bpskota.uimaps

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bpskota.R
import com.example.bpskota.bpskp.model.KodeRumah
import com.example.bpskota.bpskp.model.KodeRumahLog
import com.example.bpskota.bpskp.model.KodeRumahRequest
import com.example.bpskota.bpskp.model.KodeRumahRetagRequest
import com.example.bpskota.bpskp.model.MasterWilayah
import com.example.bpskota.bpskp.repository.BpskpAuthSession
import com.example.bpskota.bpskp.repository.BpskpRepository
import com.example.bpskota.databinding.FragmentPetaKodeRumahBinding
import com.google.android.material.snackbar.Snackbar
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapView

class PetaKodeRumahFragment : Fragment() {

    // Inisialisasi variabel dan binding
    private var _binding: FragmentPetaKodeRumahBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: BpskpRepository
    private lateinit var authSession: BpskpAuthSession
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var selectedKodeRumah: KodeRumah? = null
    private val markers = mutableMapOf<Long, KodeRumah>()
    private val hasilPencarian = mutableListOf<KodeRumah>()

    // Cache data wilayah tanpa filter
    private val semuaDataKodeRumah = mutableListOf<KodeRumah>()
    private var filterKecamatan: String? = null
    private var filterKelurahan: String? = null
    private var filterRtRw: String? = null
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var mapStyleLoaded = false
    private var pendingLocation: Location? = null
    private var markerLokasiSaya: Marker? = null
    private var dialogDetail: AlertDialog? = null
    private var dialogRetag: AlertDialog? = null
    private var retagSedangDiproses = false

    // Konfigurasi warna UI
    private val warnaUtama = Color.rgb(249, 115, 22)
    private val warnaLatar = Color.rgb(246, 242, 238)
    private val warnaTeks = Color.rgb(35, 35, 35) // Ini untuk teks hitam
    private val warnaSekunder = Color.rgb(105, 105, 105)
    private val warnaGaris = Color.rgb(230, 225, 220)

    // Launcher request izin lokasi
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            ambilLokasiSaatIni()
        } else {
            tampilkanPesan("Izin lokasi diperlukan untuk menggunakan GPS.")
        }
    }

    // Pembuatan tampilan awal
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPetaKodeRumahBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Setup map dan komponen setelah layout diload
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = BpskpRepository()
        authSession = BpskpAuthSession(requireContext())
        Mapbox.getInstance(requireContext())

        setupRole()
        setupMapView(savedInstanceState)
        setupSearch()
        setupFilters()
        setupLocationButton()
        setupLogButton()
        setupNavigationButton()
        binding.tvStatusLokasi.text = "Lokasi GPS belum diperiksa"
        loadKodeRumah()
    }

    // Mengambil token sesi login
    private fun getAuthorization(): String? {
        val token = authSession.getToken()?.trim()?.takeIf { it.isNotEmpty() }
        return token?.let { "Bearer $it" }
    }

    // Menyesuaikan visibilitas berdasarkan role user
    private fun setupRole() {
        val role = authSession.getRole()?.trim()?.lowercase().orEmpty()
        binding.btnLihatLog.visibility = if (role == "pimpinan" || role == "ketua_tim") View.VISIBLE else View.GONE
    }

    // Setup mapbox viewer
    private fun setupMapView(savedInstanceState: Bundle?) {
        val container = binding.mapViewContainer
        val newMapView = MapView(requireContext())
        mapView = newMapView
        container.removeAllViews()
        container.addView(newMapView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        aturGesturePeta(newMapView)
        newMapView.onCreate(savedInstanceState)

        newMapView.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            mapboxMap = map
            map.cameraPosition = CameraPosition.Builder().target(LatLng(-7.7543, 113.2159)).zoom(10.0).build()
            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = false
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                if (_binding == null) return@setStyle
                mapStyleLoaded = true
                binding.layoutPetunjukPeta.visibility = View.VISIBLE
                tampilkanMarker()
                pendingLocation?.let { updateCameraToLocation(it) }
            }
            map.setOnMarkerClickListener { marker ->
                val rumah = markers[marker.id]
                if (rumah != null) {
                    selectedKodeRumah = rumah
                    binding.btnMulaiNavigasi.isEnabled = koordinatValid(rumah.latitude, rumah.longitude)
                    tampilkanDetailRumah(rumah)
                }
                true
            }
        }
    }

    // Inisialisasi event input pencarian
    private fun setupSearch() {
        binding.btnCariKodeRumah.setOnClickListener { jalankanPencarian() }
        binding.etCariKodeRumah.setOnEditorActionListener { _, actionId, event ->
            val submit = actionId == EditorInfo.IME_ACTION_SEARCH || (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (submit) {
                jalankanPencarian()
                true
            } else {
                false
            }
        }
    }

    // Memproses query pencarian
    private fun jalankanPencarian() {
        sembunyikanKeyboard()
        val query = binding.etCariKodeRumah.text?.toString()?.trim().orEmpty()
        if (query.isEmpty()) loadKodeRumah() else loadKodeRumah(query = query)
    }

    // Menyembunyikan keyboard virtual
    private fun sembunyikanKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etCariKodeRumah.windowToken, 0)
        binding.etCariKodeRumah.clearFocus()
    }

    // Melakukan fetch data rumah dari repo
    private fun loadKodeRumah(query: String? = null) {
        val authorization = getAuthorization()
        if (authorization == null) {
            tampilkanPesan("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        binding.btnCariKodeRumah.isEnabled = false
        binding.tvStatusLokasi.text = "Memuat data kode rumah..."
        repository.getKodeRumah(authorization = authorization, q = query, namaKecamatan = filterKecamatan, namaKelurahan = filterKelurahan, namaRtRw = filterRtRw, perPage = 100) { response, _, errorMessage ->
            if (_binding == null) return@getKodeRumah
            binding.btnCariKodeRumah.isEnabled = true
            if (response?.success == true) {
                hasilPencarian.clear()
                hasilPencarian.addAll(response.data)

                val tanpaFilter = query.isNullOrBlank() && filterKecamatan == null && filterKelurahan == null && filterRtRw == null
                if (tanpaFilter) {
                    semuaDataKodeRumah.clear()
                    semuaDataKodeRumah.addAll(response.data)
                }

                perbaruiStatusFilter()
                tampilkanMarker()

                val jumlahKoordinat = hasilPencarian.count { koordinatValid(it.latitude, it.longitude) }
                binding.tvStatusLokasi.text = "${hasilPencarian.size} data ditemukan • $jumlahKoordinat memiliki koordinat"

                if (hasilPencarian.isEmpty()) {
                    tampilkanPesan("Data kode rumah tidak ditemukan.")
                } else if (jumlahKoordinat == 0) {
                    tampilkanPesan("Data ditemukan, tetapi belum memiliki koordinat valid.")
                }
            } else {
                binding.tvStatusLokasi.text = "Gagal memuat data"
                tampilkanPesan(errorMessage ?: response?.message ?: "Gagal mengambil data kode rumah.")
            }
        }
    }

    // Memvalidasi latitude dan longitude
    private fun koordinatValid(latitude: Double?, longitude: Double?): Boolean {
        if (latitude == null || longitude == null) return false
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0 && !(latitude == 0.0 && longitude == 0.0)
    }

    // Memproses data rumah dan memasang marker ke peta
    private fun tampilkanMarker() {
        val map = mapboxMap ?: return
        if (!mapStyleLoaded || _binding == null) return
        map.clear()
        markers.clear()
        markerLokasiSaya = null
        selectedKodeRumah = null
        binding.btnMulaiNavigasi.isEnabled = false

        hasilPencarian.forEach { rumah ->
            val latitude = rumah.latitude
            val longitude = rumah.longitude
            if (!koordinatValid(latitude, longitude)) return@forEach

            val title = rumah.noBangunan?.takeIf { it.isNotBlank() } ?: rumah.idsubsls ?: "Kode rumah ${rumah.id}"
            val snippet = buildString {
                rumah.namaKelurahan?.takeIf { it.isNotBlank() }?.let { append(it) }
                rumah.namaRtRw?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" • ")
                    append(it)
                }
            }

            val marker = map.addMarker(MarkerOptions().position(LatLng(latitude!!, longitude!!)).title(title).snippet(snippet))
            markers[marker.id] = rumah
        }

        pendingLocation?.let { perbaruiMarkerLokasiSaya(it) }
        binding.layoutPetunjukPeta.visibility = if (markers.isEmpty()) View.VISIBLE else View.GONE

        if (markers.size == 1 && pendingLocation == null) {
            val rumah = markers.values.first()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(rumah.latitude!!, rumah.longitude!!), 17.0))
        }
    }

    // Render dan tampilkan popup informasi rumah
    private fun tampilkanDetailRumah(rumah: KodeRumah) {
        if (_binding == null) return
        dialogDetail?.dismiss()
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = buatBackground(Color.WHITE, 24)
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val ikon = TextView(context).apply {
            text = "⌂"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = buatBackground(warnaUtama, 16)
        }
        header.addView(ikon, LinearLayout.LayoutParams(dp(54), dp(54)))

        val headerText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        headerText.addView(teks("Detail Kode Rumah", 19f, warnaTeks, Typeface.BOLD))
        headerText.addView(teks("Informasi titik bangunan", 12f, warnaSekunder).apply { setPadding(0, dp(3), 0, 0) })
        header.addView(headerText, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(header)

        val kode = rumah.noBangunan?.takeIf { it.isNotBlank() } ?: "Rumah ${rumah.id}"
        val subSls = rumah.idsubsls ?: "-"
        val identitas = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = buatBackground(warnaLatar, 16)
        }
        identitas.addView(teks("NOMOR BANGUNAN", 10f, warnaSekunder, Typeface.BOLD))
        identitas.addView(teks(kode, 20f, warnaUtama, Typeface.BOLD).apply { setPadding(0, dp(3), 0, dp(2)) })
        identitas.addView(teks("ID SubSLS: $subSls", 12f, warnaTeks))
        val marginIdentitas = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(18) }
        root.addView(identitas, marginIdentitas)

        val detail = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        tambahBarisDetail(detail, "Kecamatan", rumah.namaKecamatan)
        tambahBarisDetail(detail, "Kelurahan", rumah.namaKelurahan)
        tambahBarisDetail(detail, "RT/RW", rumah.namaRtRw)
        tambahBarisDetail(detail, "Nama KK", rumah.namaKk)
        tambahBarisDetail(detail, "Nama usaha", rumah.namaUsaha)

        val koordinat = if (koordinatValid(rumah.latitude, rumah.longitude)) "${rumah.latitude}, ${rumah.longitude}" else "Belum tersedia"
        tambahBarisDetail(detail, "Koordinat aktif", koordinat)
        root.addView(detail)

        val catatan = teks("Jika titik rumah tidak sesuai lokasi sebenarnya atau ada perubahan data, silakan klik Ubah Data.", 12f, warnaSekunder)
        catatan.setPadding(0, dp(4), 0, dp(12))
        root.addView(catatan)

        val tombolNavigasi = buatTombol("Navigasi ke lokasi", warnaUtama, Color.WHITE)
        tombolNavigasi.isEnabled = koordinatValid(rumah.latitude, rumah.longitude)
        root.addView(tombolNavigasi, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)))

        val tombolUbah = buatTombol("✎  Ubah Data", Color.rgb(255, 244, 235), warnaUtama)
        val marginUbah = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(10) }
        root.addView(tombolUbah, marginUbah)

        val tombolTutup = TextView(context).apply {
            text = "Tutup"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(14), 0, dp(4))
            isClickable = true
            isFocusable = true
        }
        root.addView(tombolTutup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(root)
        }
        val dialog = AlertDialog.Builder(context).setView(scroll).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        tombolNavigasi.setOnClickListener {
            dialog.dismiss()
            bukaGoogleMaps(rumah)
        }

        tombolUbah.setOnClickListener {
            dialog.dismiss()
            tampilkanPilihanUbah(rumah)
        }

        tombolTutup.setOnClickListener { dialog.dismiss() }

        dialog.setOnDismissListener {
            if (dialogDetail === dialog) dialogDetail = null
        }

        dialogDetail = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        animasikanPopup(root)
    }

    // =====================================================
    // MENU PILIHAN UBAH DATA
    // =====================================================

    private fun tampilkanPilihanUbah(rumah: KodeRumah) {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(20))
            background = buatBackground(Color.WHITE, 20)
        }

        root.addView(teks("Pilih Aksi", 18f, warnaTeks, Typeface.BOLD).apply {
            setPadding(0, 0, 0, dp(16))
        })

        // Teks diubah menjadi warnaTeks (hitam)
        val btnInformasi = TextView(context).apply {
            text = "📝  Ubah Informasi Data"
            textSize = 15f
            setTextColor(warnaTeks)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = buatBackground(warnaLatar, 12)
            isClickable = true
            isFocusable = true
        }

        // Teks diubah menjadi warnaTeks (hitam)
        val btnRetag = TextView(context).apply {
            text = "⌖  Perbarui Koordinat GPS"
            textSize = 15f
            setTextColor(warnaTeks)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = buatBackground(warnaLatar, 12)
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            }
        }

        root.addView(btnInformasi)
        root.addView(btnRetag)

        val dialog = AlertDialog.Builder(context).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnInformasi.setOnClickListener {
            dialog.dismiss()
            tampilkanFormEdit(rumah)
        }

        btnRetag.setOnClickListener {
            dialog.dismiss()
            tampilkanKonfirmasiRetag(rumah)
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        animasikanPopup(root)
    }

    // =====================================================
    // FORM EDIT INFORMASI
    // =====================================================

    private fun tampilkanFormEdit(rumah: KodeRumah) {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = buatBackground(Color.WHITE, 20)
        }

        root.addView(teks("Ubah Informasi Data", 18f, warnaTeks, Typeface.BOLD))
        root.addView(teks("Sesuaikan data yang ingin diubah.", 12f, warnaSekunder).apply {
            setPadding(0, dp(4), 0, dp(16))
        })

        fun buatInput(hintText: String, textAwal: String?): android.widget.EditText {
            return android.widget.EditText(context).apply {
                hint = hintText
                setText(textAwal)
                textSize = 14f
                setTextColor(warnaTeks)          // <--- PASTIKAN TEKS KETIKAN HITAM
                setHintTextColor(warnaSekunder)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = buatBackground(warnaLatar, 12)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(12)
                }
            }
        }

        val etNoBangunan = buatInput("Nomor Bangunan", rumah.noBangunan)
        val etIdSubsls = buatInput("ID SubSLS", rumah.idsubsls)
        val etNamaKk = buatInput("Nama Kepala Keluarga", rumah.namaKk)
        val etNamaUsaha = buatInput("Nama Usaha", rumah.namaUsaha)

        val formContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(teks("Nomor Bangunan", 12f, warnaSekunder))
            addView(etNoBangunan)
            addView(teks("ID SubSLS", 12f, warnaSekunder))
            addView(etIdSubsls)
            addView(teks("Nama Kepala Keluarga", 12f, warnaSekunder))
            addView(etNamaKk)
            addView(teks("Nama Usaha", 12f, warnaSekunder))
            addView(etNamaUsaha)
        }

        val scroll = ScrollView(context).apply { addView(formContainer) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val btnSimpan = buatTombol("Simpan Perubahan", warnaUtama, Color.WHITE)
        val marginBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) }
        root.addView(btnSimpan, marginBtn)

        val btnBatal = TextView(context).apply {
            text = "Batal"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(12), 0, dp(0))
            isClickable = true
        }
        root.addView(btnBatal, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val dialog = AlertDialog.Builder(context).setView(root).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val req = KodeRumahRequest(
                noBangunan = etNoBangunan.text.toString().trim(),
                idsubsls = etIdSubsls.text.toString().trim(),
                namaKk = etNamaKk.text.toString().trim(),
                namaUsaha = etNamaUsaha.text.toString().trim()
            )

            if (req.noBangunan.isNullOrBlank() || req.idsubsls.isNullOrBlank()) {
                tampilkanPesan("Nomor Bangunan dan ID SubSLS wajib diisi!")
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."

            val auth = getAuthorization()
            if (auth == null) {
                tampilkanPesan("Sesi habis. Silakan login kembali.")
                return@setOnClickListener
            }

            repository.updateKodeRumah(auth, rumah.id, req) { response, _, errMsg ->
                if (_binding == null) return@updateKodeRumah
                if (response?.success == true) {
                    dialog.dismiss()
                    tampilkanPesan("Data berhasil diperbarui!")
                    loadKodeRumah()
                } else {
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "Simpan Perubahan"
                    tampilkanPesan(errMsg ?: response?.message ?: "Gagal memperbarui data.")
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), (resources.displayMetrics.heightPixels * 0.75f).toInt())
        animasikanPopup(root)
    }

    // =====================================================
    // FORM TAMBAH DATA (Via Long Click)
    // =====================================================

    // =====================================================
    // FORM TAMBAH DATA (Via Long Click)
    // =====================================================

    private fun tampilkanFormTambah() {
        val location = pendingLocation
        if (location == null) {
            tampilkanPesan("Lokasi GPS belum tersedia. Klik tombol Lokasi Saya sekali untuk mendapatkan sinyal GPS terlebih dahulu.")
            periksaIzinLokasi()
            return
        }

        if (!koordinatValid(location.latitude, location.longitude)) {
            tampilkanPesan("Sinyal GPS belum valid. Tunggu sebentar lalu coba lagi.")
            return
        }

        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = buatBackground(Color.WHITE, 20)
        }

        root.addView(teks("Tambah Data Baru", 18f, warnaTeks, Typeface.BOLD))
        root.addView(teks("Koordinat otomatis terisi dari GPS terkini.", 12f, warnaSekunder).apply {
            setPadding(0, dp(4), 0, dp(16))
        })

        fun buatInput(
            hintText: String,
            textAwal: String? = null,
            inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
        ): android.widget.EditText {
            return android.widget.EditText(context).apply {
                hint = hintText
                setText(textAwal)
                textSize = 14f
                setTextColor(warnaTeks)
                setHintTextColor(warnaSekunder)
                this.inputType = inputType
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = buatBackground(warnaLatar, 12)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }
        }

        // Dropdown wilayah menggunakan desain yang sama persis dengan EditText
        fun buatDropdown(): Spinner {
            return Spinner(context).apply {
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = buatBackground(warnaLatar, 12)

                val popupBg = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    setStroke(dp(2), warnaUtama)
                    cornerRadius = dp(12).toFloat()
                }
                setPopupBackgroundDrawable(popupBg)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            }
        }

        val spinnerKecamatan = buatDropdown()
        val spinnerKelurahan = buatDropdown()
        val spinnerRtRw = buatDropdown()

        fun buatAdapter(items: MutableList<String>): ArrayAdapter<String> {
            return object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTextColor(warnaTeks)
                    view.textSize = 14f
                    view.setPadding(0, 0, 0, 0)
                    return view
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(warnaTeks)
                    view.textSize = 14f
                    view.setPadding(dp(16), dp(14), dp(16), dp(14))
                    return view
                }
            }
        }

        val adapterKecamatan = buatAdapter(mutableListOf("Memuat kecamatan..."))
        val adapterKelurahan = buatAdapter(mutableListOf("Pilih kecamatan terlebih dahulu"))
        val adapterRtRw = buatAdapter(mutableListOf("Pilih kelurahan terlebih dahulu"))

        spinnerKecamatan.adapter = adapterKecamatan
        spinnerKelurahan.adapter = adapterKelurahan
        spinnerRtRw.adapter = adapterRtRw
        spinnerKecamatan.isEnabled = false
        spinnerKelurahan.isEnabled = false
        spinnerRtRw.isEnabled = false

        val dataKecamatan = mutableListOf<MasterWilayah>()
        val dataKelurahan = mutableListOf<MasterWilayah>()
        val dataRtRw = mutableListOf<MasterWilayah>()

        var kecamatanTerpilih: MasterWilayah? = null
        var kelurahanTerpilih: MasterWilayah? = null
        var rtRwTerpilih: MasterWilayah? = null
        var dialogForm: AlertDialog? = null

        // MEMINDAHKAN DEKLARASI INPUT ID SUBSLS KE ATAS AGAR BISA DIAKSES OLEH SPINNER
        val etIdSubsls = buatInput("ID SubSLS (Otomatis)").apply {
            isEnabled = false // Mematikan fungsi edit (Read Only)
            setTextColor(warnaSekunder) // Memberikan warna abu-abu agar terlihat read-only
        }

        fun isiDropdown(
            spinner: Spinner,
            adapter: ArrayAdapter<String>,
            placeholder: String,
            data: List<MasterWilayah>,
            listener: AdapterView.OnItemSelectedListener
        ) {
            spinner.onItemSelectedListener = null
            adapter.clear()
            adapter.add(placeholder)
            data.forEach { adapter.add(it.getNama()) }
            adapter.notifyDataSetChanged()
            spinner.setSelection(0, false)
            spinner.onItemSelectedListener = listener
        }

        var listenerKecamatan: AdapterView.OnItemSelectedListener? = null
        var listenerKelurahan: AdapterView.OnItemSelectedListener? = null
        var listenerRtRw: AdapterView.OnItemSelectedListener? = null

        fun resetDropdownRtRw() {
            dataRtRw.clear()
            rtRwTerpilih = null
            etIdSubsls.setText("") // Kosongkan Input ID SubSLS jika RT/RW direset
            isiDropdown(
                spinnerRtRw,
                adapterRtRw,
                "Pilih kelurahan terlebih dahulu",
                emptyList(),
                listenerRtRw ?: return
            )
            spinnerRtRw.isEnabled = false
        }

        fun resetDropdownKelurahan() {
            dataKelurahan.clear()
            kelurahanTerpilih = null
            isiDropdown(
                spinnerKelurahan,
                adapterKelurahan,
                "Pilih kecamatan terlebih dahulu",
                emptyList(),
                listenerKelurahan ?: return
            )
            spinnerKelurahan.isEnabled = false
            resetDropdownRtRw()
        }

        var muatKelurahan: ((Int) -> Unit)? = null
        var muatRtRw: ((Int) -> Unit)? = null

        listenerKecamatan = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                kecamatanTerpilih = null
                resetDropdownKelurahan()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                kecamatanTerpilih = dataKecamatan.getOrNull(position - 1)
                if (kecamatanTerpilih == null) {
                    resetDropdownKelurahan()
                } else {
                    muatKelurahan?.invoke(kecamatanTerpilih!!.id)
                }
            }
        }

        listenerKelurahan = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                kelurahanTerpilih = null
                resetDropdownRtRw()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                kelurahanTerpilih = dataKelurahan.getOrNull(position - 1)
                if (kelurahanTerpilih == null) {
                    resetDropdownRtRw()
                } else {
                    muatRtRw?.invoke(kelurahanTerpilih!!.id)
                }
            }
        }

        listenerRtRw = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                rtRwTerpilih = null
                etIdSubsls.setText("") // Kosongkan Input jika tidak ada RT/RW yang dipilih
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                rtRwTerpilih = dataRtRw.getOrNull(position - 1)

                // MENGISI ID SUBSLS SECARA OTOMATIS BERDASARKAN KODE WILAYAH DARI API
                val kode = rtRwTerpilih?.kodeWilayah ?: ""
                etIdSubsls.setText(kode)
            }
        }

        muatRtRw = { kelurahanId ->
            dataRtRw.clear()
            rtRwTerpilih = null
            etIdSubsls.setText("") // Bersihkan input saat memuat ulang RT/RW

            listenerRtRw?.let { listener ->
                isiDropdown(spinnerRtRw, adapterRtRw, "Memuat RT/RW...", emptyList(), listener)
            }

            spinnerRtRw.isEnabled = false

            repository.getMasterWilayah(level = "rtrw", status = "aktif", perPage = 100, page = 1, kelurahanId = kelurahanId) { response, errorMessage ->
                if (_binding == null || dialogForm?.isShowing != true || kelurahanTerpilih?.id != kelurahanId) return@getMasterWilayah

                val daftar = if (response?.success == true) response.data.orEmpty() else emptyList()
                dataRtRw.clear()
                dataRtRw.addAll(daftar)

                isiDropdown(
                    spinnerRtRw,
                    adapterRtRw,
                    if (daftar.isEmpty()) "RT/RW tidak tersedia" else "Pilih RT/RW",
                    daftar,
                    listenerRtRw!!
                )

                spinnerRtRw.isEnabled = daftar.isNotEmpty()

                if (response?.success != true) {
                    tampilkanPesan(errorMessage ?: response?.message ?: "Gagal memuat data RT/RW.")
                } else if (daftar.isEmpty()) {
                    tampilkanPesan("Tidak ada data RT/RW aktif pada kelurahan ini.")
                }
            }
        }

        muatKelurahan = { kecamatanId ->
            dataKelurahan.clear()
            kelurahanTerpilih = null
            resetDropdownRtRw()

            listenerKelurahan?.let { listener ->
                isiDropdown(spinnerKelurahan, adapterKelurahan, "Memuat kelurahan...", emptyList(), listener)
            }

            spinnerKelurahan.isEnabled = false

            repository.getMasterWilayah(level = "kelurahan", status = "aktif", perPage = 100, page = 1, kecamatanId = kecamatanId) { response, errorMessage ->
                if (_binding == null || dialogForm?.isShowing != true || kecamatanTerpilih?.id != kecamatanId) return@getMasterWilayah

                val daftar = if (response?.success == true) response.data.orEmpty() else emptyList()
                dataKelurahan.clear()
                dataKelurahan.addAll(daftar)

                isiDropdown(
                    spinnerKelurahan,
                    adapterKelurahan,
                    if (daftar.isEmpty()) "Kelurahan tidak tersedia" else "Pilih Kelurahan/Desa",
                    daftar,
                    listenerKelurahan!!
                )

                spinnerKelurahan.isEnabled = daftar.isNotEmpty()

                if (response?.success != true) {
                    tampilkanPesan(errorMessage ?: response?.message ?: "Gagal memuat data kelurahan.")
                } else if (daftar.isEmpty()) {
                    tampilkanPesan("Tidak ada data kelurahan aktif pada kecamatan ini.")
                }
            }
        }

        spinnerKecamatan.onItemSelectedListener = listenerKecamatan
        spinnerKelurahan.onItemSelectedListener = listenerKelurahan
        spinnerRtRw.onItemSelectedListener = listenerRtRw

        // Input text sisanya
        val etNoBangunan = buatInput("Nomor Bangunan (Wajib)")
        val etNamaKk = buatInput("Nama Kepala Keluarga (Opsional)")
        val etNamaUsaha = buatInput("Nama Usaha (Opsional)")
        val etLat = buatInput(
            "Latitude",
            location.latitude.toString(),
            android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        )
        val etLong = buatInput(
            "Longitude",
            location.longitude.toString(),
            android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        )

        val formContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            addView(teks("Wilayah Administrasi", 12f, warnaUtama, Typeface.BOLD).apply {
                setPadding(0, 0, 0, dp(8))
            })
            addView(spinnerKecamatan)
            addView(spinnerKelurahan)
            addView(spinnerRtRw)

            addView(teks("Identitas Bangunan", 12f, warnaUtama, Typeface.BOLD).apply {
                setPadding(0, dp(8), 0, dp(8))
            })
            addView(etIdSubsls) // Masukkan etIdSubsls yang sudah digeser ke atas tadi
            addView(etNoBangunan)
            addView(etNamaKk)
            addView(etNamaUsaha)

            addView(teks("Titik Koordinat", 12f, warnaUtama, Typeface.BOLD).apply {
                setPadding(0, dp(8), 0, dp(8))
            })
            addView(etLat)
            addView(etLong)
        }

        val scroll = ScrollView(context).apply { addView(formContainer) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val btnSimpan = buatTombol("Simpan Data Baru", warnaUtama, Color.WHITE)
        val marginBtn = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)).apply { topMargin = dp(12) }
        root.addView(btnSimpan, marginBtn)

        val btnBatal = TextView(context).apply {
            text = "Batal"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(12), 0, dp(0))
            isClickable = true
        }
        root.addView(btnBatal, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val dialog = AlertDialog.Builder(context).setView(root).create()
        dialogForm = dialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnBatal.setOnClickListener { dialog.dismiss() }

        btnSimpan.setOnClickListener {
            val req = KodeRumahRequest(
                namaKecamatan = kecamatanTerpilih?.namaKecamatan,
                namaKelurahan = kelurahanTerpilih?.namaKelurahan,
                namaRtRw = rtRwTerpilih?.namaWilayah,
                idsubsls = etIdSubsls.text.toString().trim(), // Nilai yang tersubmit tetap membaca dari EditText
                noBangunan = etNoBangunan.text.toString().trim(),
                namaKk = etNamaKk.text.toString().trim(),
                namaUsaha = etNamaUsaha.text.toString().trim(),
                latitude = etLat.text.toString().toDoubleOrNull(),
                longitude = etLong.text.toString().toDoubleOrNull()
            )

            if (req.namaKecamatan.isNullOrBlank() ||
                req.namaKelurahan.isNullOrBlank() ||
                req.namaRtRw.isNullOrBlank() ||
                req.idsubsls.isNullOrBlank() ||
                req.noBangunan.isNullOrBlank()
            ) {
                tampilkanPesan("Semua kolom wilayah dan identitas bangunan wajib diisi!")
                return@setOnClickListener
            }

            if (req.latitude == null || req.longitude == null) {
                tampilkanPesan("Koordinat Latitude dan Longitude tidak valid!")
                return@setOnClickListener
            }

            btnSimpan.isEnabled = false
            btnSimpan.text = "Menyimpan..."

            val auth = getAuthorization()
            if (auth == null) {
                tampilkanPesan("Sesi habis. Silakan login kembali.")
                return@setOnClickListener
            }

            repository.tambahKodeRumah(auth, req) { response, _, errMsg ->
                if (_binding == null) return@tambahKodeRumah
                if (response?.success == true) {
                    dialog.dismiss()
                    tampilkanPesan("Data rumah baru berhasil ditambahkan!")
                    loadKodeRumah()
                } else {
                    btnSimpan.isEnabled = true
                    btnSimpan.text = "Simpan Data Baru"
                    tampilkanPesan(errMsg ?: response?.message ?: "Gagal menambahkan data baru.")
                }
            }
        }

        dialog.show()
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            (resources.displayMetrics.heightPixels * 0.85f).toInt()
        )
        animasikanPopup(root)

        repository.getMasterWilayah(level = "kecamatan", status = "aktif", perPage = 100, page = 1) { response, errorMessage ->
            if (_binding == null || !dialog.isShowing) return@getMasterWilayah

            val daftar = if (response?.success == true) response.data.orEmpty() else emptyList()
            dataKecamatan.clear()
            dataKecamatan.addAll(daftar)
            isiDropdown(
                spinnerKecamatan,
                adapterKecamatan,
                if (daftar.isEmpty()) "Kecamatan tidak tersedia" else "Pilih Kecamatan",
                daftar,
                listenerKecamatan!!
            )
            spinnerKecamatan.isEnabled = daftar.isNotEmpty()

            if (response?.success != true) {
                tampilkanPesan(errorMessage ?: response?.message ?: "Gagal memuat data kecamatan.")
            } else if (daftar.isEmpty()) {
                tampilkanPesan("Tidak ada data kecamatan aktif.")
            }
        }
    }

    // Helper untuk membuat baris properti
    private fun tambahBarisDetail(parent: LinearLayout, judul: String, nilai: String?) {
        val baris = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }
        baris.addView(teks(judul, 11f, warnaSekunder))
        baris.addView(teks(nilai?.takeIf { it.isNotBlank() } ?: "-", 14f, warnaTeks, Typeface.NORMAL).apply { setPadding(0, dp(3), 0, 0) })
        parent.addView(baris)

        val divider = View(requireContext()).apply { setBackgroundColor(warnaGaris) }
        parent.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
    }

    // Helper untuk baris properti horizontal
    private fun tambahBarisHorizontal(parent: LinearLayout, judul: String, nilai: String?) {
        val baris = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }

        val tvJudul = teks(judul, 12f, warnaSekunder).apply {
            layoutParams = LinearLayout.LayoutParams(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val tvNilai = teks(nilai?.takeIf { it.isNotBlank() } ?: "-", 14f, warnaTeks, Typeface.NORMAL).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        baris.addView(tvJudul)
        baris.addView(tvNilai)
        parent.addView(baris)

        val divider = View(requireContext()).apply { setBackgroundColor(warnaGaris) }
        parent.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
    }

    // Memunculkan peringatan / konfirmasi sebelum mengubah koordinat (retag)
    private fun tampilkanKonfirmasiRetag(rumah: KodeRumah) {
        if (_binding == null) return
        val location = pendingLocation
        if (location == null) {
            tampilkanPesan("Lokasi GPS belum tersedia. Tekan tombol Lokasi Saya, tunggu GPS diperbarui, lalu coba Retagging kembali.")
            periksaIzinLokasi()
            return
        }

        val umurLokasi = System.currentTimeMillis() - location.time
        if (umurLokasi < 0 || umurLokasi > 120_000L) {
            tampilkanPesan("Data GPS sudah terlalu lama. Perbarui lokasi melalui tombol Lokasi Saya, lalu ulangi Retagging.")
            periksaIzinLokasi()
            return
        }

        if (!koordinatValid(location.latitude, location.longitude)) {
            tampilkanPesan("Koordinat GPS tidak valid.")
            return
        }

        if (!location.hasAccuracy() || location.accuracy > 100f) {
            tampilkanPesan("Akurasi GPS belum memadai (maksimal 100 meter). Tunggu GPS lebih akurat lalu coba lagi.")
            return
        }

        val isi = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(8))
            background = buatBackground(Color.WHITE, 20)
        }
        isi.addView(teks("Ajukan Retagging", 20f, warnaTeks, Typeface.BOLD))
        isi.addView(teks("Koordinat baru akan diambil dari GPS HP kamu. Pastikan kamu berada di lokasi rumah yang benar.", 13f, warnaSekunder).apply { setPadding(0, dp(8), 0, dp(16)) })

        val dataGps = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = buatBackground(warnaLatar, 14)
        }
        dataGps.addView(teks("KOORDINAT GPS BARU", 10f, warnaSekunder, Typeface.BOLD))
        dataGps.addView(teks("Lat: %.7f".format(location.latitude), 14f, warnaTeks, Typeface.BOLD).apply { setPadding(0, dp(6), 0, 0) })
        dataGps.addView(teks("Long: %.7f".format(location.longitude), 14f, warnaTeks, Typeface.BOLD).apply { setPadding(0, dp(3), 0, 0) })
        dataGps.addView(teks("Akurasi ±%.0f meter".format(location.accuracy), 12f, warnaSekunder).apply { setPadding(0, dp(6), 0, 0) })
        isi.addView(dataGps)

        isi.addView(teks("Rumah: ${rumah.noBangunan ?: rumah.idsubsls ?: rumah.id}", 13f, warnaTeks).apply { setPadding(0, dp(14), 0, dp(4)) })
        isi.addView(teks("Koordinat aktif tidak seharusnya berubah hanya karena pengajuan ini. Pastikan backend menangani pengajuan dan verifikasinya.", 12f, warnaSekunder).apply { setPadding(0, dp(4), 0, dp(8)) })

        val scroll = ScrollView(requireContext()).apply { addView(isi) }
        val dialog = AlertDialog.Builder(requireContext())
            .setView(scroll)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Ubah", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(warnaUtama)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(warnaSekunder)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                kirimRetagging(rumah, location, dialog)
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        }

        dialogRetag = dialog
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        animasikanPopup(isi)

        dialog.setOnDismissListener {
            if (dialogRetag === dialog) dialogRetag = null
        }
    }

    // Request pembaruan koordinat
    private fun kirimRetagging(rumah: KodeRumah, location: Location, dialog: AlertDialog) {
        if (retagSedangDiproses) return
        val authorization = getAuthorization()
        if (authorization == null) {
            tampilkanPesan("Sesi login tidak ditemukan. Silakan login kembali.")
            return
        }

        val umurLokasi = System.currentTimeMillis() - location.time
        if (umurLokasi < 0 || umurLokasi > 120_000L || !location.hasAccuracy() || location.accuracy > 100f || !koordinatValid(location.latitude, location.longitude)) {
            tampilkanPesan("GPS tidak lagi memenuhi syarat. Perbarui lokasi sebelum mengirim.")
            dialog.dismiss()
            periksaIzinLokasi()
            return
        }

        retagSedangDiproses = true
        val tombolKirim = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        tombolKirim.isEnabled = false
        tombolKirim.text = "Mengirim..."

        val request = KodeRumahRetagRequest(latitude = location.latitude, longitude = location.longitude)
        repository.retagKodeRumah(authorization = authorization, id = rumah.id, request = request) { response, _, errorMessage ->
            retagSedangDiproses = false
            if (_binding == null) return@retagKodeRumah

            if (response?.success == true) {
                dialog.dismiss()
                tampilkanPesan(response.message ?: "Pengajuan retagging berhasil dikirim.")
                loadKodeRumah()
            } else {
                tombolKirim.isEnabled = true
                tombolKirim.text = "Kirim Pengajuan"
                tampilkanPesan(errorMessage ?: response?.message ?: "Gagal mengirim pengajuan retagging.")
            }
        }
    }

    // Memberikan animasi saat popup tampil
    private fun animasikanPopup(view: View) {
        view.alpha = 0f
        view.scaleX = 0.94f
        view.scaleY = 0.94f
        view.translationY = dp(16).toFloat()
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_X, 0.94f, 1f),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, 0.94f, 1f),
                ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, dp(16).toFloat(), 0f)
            )
            duration = 240L
            start()
        }
    }

    // Helper textview
    private fun teks(isi: String, ukuran: Float, warna: Int, gaya: Int = Typeface.NORMAL): TextView {
        return TextView(requireContext()).apply {
            text = isi
            textSize = ukuran
            setTextColor(warna)
            typeface = Typeface.create("sans-serif", gaya)
        }
    }

    // Helper button
    private fun buatTombol(label: String, warnaLatar: Int, warnaTeks: Int): Button {
        return Button(requireContext()).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(warnaTeks)
            background = buatBackground(warnaLatar, 14)
            stateListAnimator = null
            minHeight = 0
            minimumHeight = 0
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
    }

    // Helper background
    private fun buatBackground(warna: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(warna)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    // Mengkonversi pixel
    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }

    // Melempar intent rute ke aplikasi Google Maps
    private fun bukaGoogleMaps(rumah: KodeRumah) {
        val latitude = rumah.latitude
        val longitude = rumah.longitude
        if (!koordinatValid(latitude, longitude)) {
            tampilkanPesan("Rumah ini belum memiliki koordinat yang valid.")
            return
        }

        val uriGoogleMaps = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
        try {
            val intentGoogleMaps = Intent(Intent.ACTION_VIEW, uriGoogleMaps).apply { setPackage("com.google.android.apps.maps") }
            startActivity(intentGoogleMaps)
        } catch (_: Exception) {
            val uriFallback = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uriFallback))
            } catch (_: Exception) {
                tampilkanPesan("Tidak dapat membuka aplikasi peta.")
            }
        }
    }

    // Inisiasi aksi untuk opsi dropdown wilayah
    private fun setupFilters() {
        perbaruiStatusFilter()

        binding.btnFilterKecamatan.setOnClickListener {
            val opsiKecamatan = (semuaDataKodeRumah + hasilPencarian).mapNotNull { it.namaKecamatan?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
            pilihFilter(judul = "Pilih Kecamatan", nilaiSaatIni = filterKecamatan, opsi = opsiKecamatan) { nilai ->
                if (filterKecamatan != nilai) {
                    filterKecamatan = nilai
                    filterKelurahan = null
                    filterRtRw = null
                    binding.btnFilterKecamatan.text = nilai ?: "Kecamatan"
                    binding.btnFilterDesa.text = "Desa/Kelurahan"
                    binding.btnFilterSls.text = "RT/RW/SLS"
                    perbaruiStatusFilter()
                    loadKodeRumah()
                }
            }
        }

        binding.btnFilterDesa.setOnClickListener {
            if (filterKecamatan.isNullOrBlank()) {
                tampilkanPesan("Pilih kecamatan terlebih dahulu.")
                return@setOnClickListener
            }
            val opsiKelurahan = (semuaDataKodeRumah + hasilPencarian).distinctBy { it.id }.filter { it.namaKecamatan.equals(filterKecamatan, ignoreCase = true) }.mapNotNull { it.namaKelurahan?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
            pilihFilter(judul = "Pilih Desa/Kelurahan", nilaiSaatIni = filterKelurahan, opsi = opsiKelurahan) { nilai ->
                if (filterKelurahan != nilai) {
                    filterKelurahan = nilai
                    filterRtRw = null
                    binding.btnFilterDesa.text = nilai ?: "Desa/Kelurahan"
                    binding.btnFilterSls.text = "RT/RW/SLS"
                    perbaruiStatusFilter()
                    loadKodeRumah()
                }
            }
        }

        binding.btnFilterSls.setOnClickListener {
            if (filterKecamatan.isNullOrBlank()) {
                tampilkanPesan("Pilih kecamatan terlebih dahulu.")
                return@setOnClickListener
            }
            if (filterKelurahan.isNullOrBlank()) {
                tampilkanPesan("Pilih kelurahan terlebih dahulu.")
                return@setOnClickListener
            }
            val opsiRtRw = (semuaDataKodeRumah + hasilPencarian).distinctBy { it.id }.filter { it.namaKecamatan.equals(filterKecamatan, ignoreCase = true) }.filter { it.namaKelurahan.equals(filterKelurahan, ignoreCase = true) }.mapNotNull { it.namaRtRw?.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
            pilihFilter(judul = "Pilih RT/RW/SLS", nilaiSaatIni = filterRtRw, opsi = opsiRtRw) { nilai ->
                if (filterRtRw != nilai) {
                    filterRtRw = nilai
                    binding.btnFilterSls.text = nilai ?: "RT/RW/SLS"
                    perbaruiStatusFilter()
                    loadKodeRumah()
                }
            }
        }
    }

    // Mengunci atau membuka dropdown tergantung filter yang terisi
    private fun perbaruiStatusFilter() {
        binding.btnFilterDesa.isEnabled = !filterKecamatan.isNullOrBlank()
        binding.btnFilterSls.isEnabled = !filterKecamatan.isNullOrBlank() && !filterKelurahan.isNullOrBlank()
    }

    // Menampilkan popup builder filter
    private fun pilihFilter(judul: String, nilaiSaatIni: String?, opsi: List<String>, onSelected: (String?) -> Unit) {
        if (opsi.isEmpty()) {
            tampilkanPesan("Tidak ada pilihan wilayah untuk filter ini.")
            return
        }
        val pilihan = listOf("Semua") + opsi
        val indexTerpilih = if (nilaiSaatIni == null) 0 else pilihan.indexOf(nilaiSaatIni).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle(judul)
            .setSingleChoiceItems(pilihan.toTypedArray(), indexTerpilih) { dialog, which ->
                val nilai = if (which == 0) null else pilihan[which]
                dialog.dismiss()
                onSelected(nilai)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // Menghubungkan fungsi tracker ke tombol
    private fun setupLocationButton() {
        locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.btnLokasiSaya.setOnClickListener { periksaIzinLokasi() }

        // Pemicu tambah data lewat long click
        binding.btnLokasiSaya.setOnLongClickListener {
            tampilkanFormTambah()
            true
        }
    }

    // Validasi permission secara realtime
    private fun periksaIzinLokasi() {
        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        ambilLokasiSaatIni()
    }

    // Start pemantauan lokasi terbaru menggunakan manager
    private fun ambilLokasiSaatIni() {
        val manager = locationManager ?: return
        val context = context ?: return
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            tampilkanPesan("Izin lokasi belum diberikan.")
            return
        }

        try {
            val providers = manager.getProviders(true).filter { it == LocationManager.GPS_PROVIDER || it == LocationManager.NETWORK_PROVIDER }
            if (providers.isEmpty()) {
                binding.tvStatusLokasi.text = "Aktifkan layanan lokasi perangkat"
                tampilkanPesan("GPS atau layanan lokasi perangkat belum aktif.")
                return
            }

            val lastKnown = providers.mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }

            if (lastKnown != null) {
                pendingLocation = lastKnown
                perbaruiMarkerLokasiSaya(lastKnown)
                updateCameraToLocation(lastKnown)
                binding.tvStatusLokasi.text = "Memperbarui lokasi GPS…"
            } else {
                binding.tvStatusLokasi.text = "Mencari lokasi GPS…"
            }

            locationListener?.let {
                try { manager.removeUpdates(it) } catch (_: SecurityException) {}
            }

            var firstFreshLocation = true
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (_binding == null) return
                    pendingLocation = location
                    if (firstFreshLocation) {
                        updateCameraToLocation(location)
                        firstFreshLocation = false
                    } else {
                        perbaruiMarkerLokasiSaya(location)
                    }
                    binding.tvStatusLokasi.text = "Lokasi saya: %.6f, %.6f (akurasi ±%.0f m)".format(location.latitude, location.longitude, location.accuracy)
                }

                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit
            }

            locationListener = listener
            providers.forEach { provider ->
                try { manager.requestLocationUpdates(provider, 2000L, 2f, listener) } catch (_: SecurityException) {}
            }
        } catch (_: SecurityException) {
            tampilkanPesan("Izin lokasi belum diberikan.")
        } catch (e: Exception) {
            tampilkanPesan(e.message ?: "Gagal mendapatkan lokasi perangkat.")
        }
    }

    // Mendaratkan kamera mapbox
    private fun updateCameraToLocation(location: Location) {
        pendingLocation = location
        perbaruiMarkerLokasiSaya(location)
        val map = mapboxMap ?: return
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17.0))
        if (_binding != null) {
            binding.tvStatusLokasi.text = "Lokasi saya: %.6f, %.6f (akurasi ±%.0f m)".format(location.latitude, location.longitude, location.accuracy)
        }
    }

    // Melakukan update view marker posisi sekarang
    private fun perbaruiMarkerLokasiSaya(location: Location) {
        val map = mapboxMap ?: return
        if (!mapStyleLoaded || _binding == null) return
        val position = LatLng(location.latitude, location.longitude)
        val existing = markerLokasiSaya

        if (existing != null) {
            existing.position = position
        } else {
            markerLokasiSaya = map.addMarker(MarkerOptions().position(position).title("Lokasi saya").snippet("Posisi perangkat berdasarkan GPS").icon(IconFactory.getInstance(requireContext()).defaultMarker()))
        }
    }

    // Listener interaksi riwayat
    private fun setupLogButton() {
        binding.btnLihatLog.setOnClickListener {
            val role = authSession.getRole()?.trim()?.lowercase().orEmpty()
            if (role != "pimpinan" && role != "ketua_tim") {
                tampilkanPesan("Kamu tidak memiliki akses ke menu riwayat.")
                return@setOnClickListener
            }
            loadLogKodeRumah()
        }
    }

    // Mengambil data log dari API backend
    private fun loadLogKodeRumah() {
        val authorization = getAuthorization()
        if (authorization == null) {
            tampilkanPesan("Sesi login tidak ditemukan.")
            return
        }
        repository.getLogKodeRumah(authorization = authorization, perPage = 50) { response, _, errorMessage ->
            if (_binding == null) return@getLogKodeRumah
            if (response?.success == true) {
                tampilkanDaftarLog(response.data)
            } else {
                tampilkanPesan(errorMessage ?: response?.message ?: "Gagal mengambil riwayat kode rumah.")
            }
        }
    }

    // Membuat tampilan list log menggunakan Card kustom dan Custom Dialog
    private fun tampilkanDaftarLog(logs: List<KodeRumahLog>) {
        if (logs.isEmpty()) {
            tampilkanPesan("Belum ada riwayat perubahan kode rumah.")
            return
        }

        val context = requireContext()
        val inflater = LayoutInflater.from(context)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(16))
            background = buatBackground(Color.WHITE, 24)
        }

        val headerText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        headerText.addView(teks("Riwayat Kode Rumah", 19f, warnaTeks, Typeface.BOLD))
        headerText.addView(teks("Daftar log perubahan data", 12f, warnaSekunder).apply { setPadding(0, dp(3), 0, 0) })
        root.addView(headerText)

        val scrollView = ScrollView(context).apply {
            isFillViewport = true
        }

        val containerList = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }

        logs.forEach { log ->
            val cardView = inflater.inflate(R.layout.item_wilayah, containerList, false)

            val tvJudul = cardView.findViewById<TextView>(R.id.tvJudul)
            val tvKategori = cardView.findViewById<TextView>(R.id.tvKategori)
            val tvKode = cardView.findViewById<TextView>(R.id.tvKode)
            val tvNilai = cardView.findViewById<TextView>(R.id.tvNilai)

            tvJudul.maxLines = 1
            tvJudul.ellipsize = android.text.TextUtils.TruncateAt.END
            tvKategori.maxLines = 1
            tvKategori.ellipsize = android.text.TextUtils.TruncateAt.END

            val judul = if (!log.noBangunan.isNullOrBlank()) "No: ${log.noBangunan}" else "ID: ${log.idsubsls ?: "-"}"
            tvJudul.text = judul
            tvKategori.text = log.namaPengguna ?: "Sistem"
            tvKode.text = log.aksi ?: "LOG"
            tvNilai.text = log.createdAt ?: "-"

            cardView.setOnClickListener {
                tampilkanDetailLog(log)
            }

            containerList.addView(cardView)
        }

        scrollView.addView(containerList)
        root.addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val tombolTutup = TextView(context).apply {
            text = "Tutup"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(14), 0, dp(4))
            isClickable = true
            isFocusable = true
        }
        root.addView(tombolTutup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))

        val dialog = AlertDialog.Builder(context)
            .setView(root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        tombolTutup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.95f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        animasikanPopup(root)
    }

    // Pembuatan popup desain modern untuk item list log
    private fun tampilkanDetailLog(log: KodeRumahLog) {
        val context = requireContext()
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(18))
            background = buatBackground(Color.WHITE, 24)
        }

        val headerText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        headerText.addView(teks("Detail Riwayat", 19f, warnaTeks, Typeface.BOLD))
        headerText.addView(teks("Informasi perubahan data", 12f, warnaSekunder).apply { setPadding(0, dp(3), 0, 0) })
        root.addView(headerText)

        val detail = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(8))
        }

        tambahBarisHorizontal(detail, "Aksi", log.aksi)
        tambahBarisHorizontal(detail, "ID SubSLS", log.idsubsls)
        tambahBarisHorizontal(detail, "No. Bangunan", log.noBangunan)
        tambahBarisHorizontal(detail, "Pengguna", log.namaPengguna)
        tambahBarisHorizontal(detail, "Waktu", log.createdAt)

        tambahBarisDetail(detail, "Keterangan", log.keterangan)
        tambahBarisDetail(detail, "Data sebelum", log.dataSebelum)
        tambahBarisDetail(detail, "Data sesudah", log.dataSesudah)

        root.addView(detail)

        val tombolTutup = TextView(context).apply {
            text = "Tutup"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(14), 0, dp(4))
            isClickable = true
            isFocusable = true
        }
        root.addView(tombolTutup, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(42)))

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(root)
        }

        val dialog = AlertDialog.Builder(context).setView(scroll).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        tombolTutup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        animasikanPopup(root)
    }

    // Menghubungkan fungsi ke klik
    private fun setupNavigationButton() {
        binding.btnMulaiNavigasi.isEnabled = false
        binding.btnMulaiNavigasi.setOnClickListener {
            val rumah = selectedKodeRumah
            if (rumah == null) {
                tampilkanPesan("Pilih marker rumah terlebih dahulu.")
                return@setOnClickListener
            }
            bukaGoogleMaps(rumah)
        }
    }

    // Menampilkan komponen snackbar
    private fun tampilkanPesan(pesan: String) {
        val root = _binding?.root ?: return
        Snackbar.make(root, pesan, Snackbar.LENGTH_LONG).show()
    }

    // Mencegah konflik pada layout yang bisa digulir
    private fun aturGesturePeta(mapView: MapView) {
        mapView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
    }

    // Integrasi mapbox lifecycle
    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onStop() {
        locationListener?.let {
            try { locationManager?.removeUpdates(it) } catch (_: SecurityException) {}
        }
        locationListener = null
        mapView?.onStop()
        super.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        dialogDetail?.dismiss()
        dialogDetail = null
        dialogRetag?.dismiss()
        dialogRetag = null
        locationListener?.let {
            try { locationManager?.removeUpdates(it) } catch (_: SecurityException) {}
        }
        locationListener = null
        binding.mapViewContainer.removeAllViews()
        mapView?.onDestroy()
        mapView = null
        mapboxMap = null
        markerLokasiSaya = null
        selectedKodeRumah = null
        pendingLocation = null
        mapStyleLoaded = false
        retagSedangDiproses = false

        semuaDataKodeRumah.clear()
        hasilPencarian.clear()
        markers.clear()
        _binding = null
        super.onDestroyView()
    }
}