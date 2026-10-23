
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
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.bpskota.bpskp.model.KodeRumah
import com.example.bpskota.bpskp.model.KodeRumahLog
import com.example.bpskota.bpskp.model.KodeRumahRetagRequest
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

    private var _binding: FragmentPetaKodeRumahBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BpskpRepository
    private lateinit var authSession: BpskpAuthSession

    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null

    private var selectedKodeRumah: KodeRumah? = null

    private val markers = mutableMapOf<Long, KodeRumah>()
    private val hasilPencarian = mutableListOf<KodeRumah>()

    // Cache data wilayah dari pemuatan data tanpa filter.
    // Daftar Kecamatan tidak lagi bergantung pada hasil filter aktif.
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

    private val warnaUtama = Color.rgb(249, 115, 22)
    private val warnaLatar = Color.rgb(246, 242, 238)
    private val warnaTeks = Color.rgb(35, 35, 35)
    private val warnaSekunder = Color.rgb(105, 105, 105)
    private val warnaGaris = Color.rgb(230, 225, 220)

    // =====================================================
    // IZIN LOKASI
    // =====================================================

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                ambilLokasiSaatIni()
            } else {
                tampilkanPesan(
                    "Izin lokasi diperlukan untuk menggunakan GPS."
                )
            }
        }

    // =====================================================
    // LIFECYCLE FRAGMENT
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetaKodeRumahBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
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

    // =====================================================
    // SESSION DAN ROLE
    // =====================================================

    private fun getAuthorization(): String? {
        val token = authSession.getToken()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        return token?.let { "Bearer $it" }
    }

    private fun setupRole() {
        val role = authSession.getRole()
            ?.trim()
            ?.lowercase()
            .orEmpty()

        binding.btnLihatLog.visibility =
            if (role == "pimpinan" || role == "ketua_tim") {
                View.VISIBLE
            } else {
                View.GONE
            }
    }

    // =====================================================
    // MAP
    // =====================================================

    private fun setupMapView(savedInstanceState: Bundle?) {
        val container = binding.mapViewContainer

        val newMapView = MapView(requireContext())
        mapView = newMapView

        container.removeAllViews()

        container.addView(
            newMapView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        aturGesturePeta(newMapView)
        newMapView.onCreate(savedInstanceState)

        newMapView.getMapAsync { map ->
            if (_binding == null) return@getMapAsync

            mapboxMap = map

            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(-7.7543, 113.2159))
                .zoom(10.0)
                .build()

            map.uiSettings.isCompassEnabled = true
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isTiltGesturesEnabled = false

            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {
                if (_binding == null) return@setStyle

                mapStyleLoaded = true
                binding.layoutPetunjukPeta.visibility = View.VISIBLE

                tampilkanMarker()

                pendingLocation?.let {
                    updateCameraToLocation(it)
                }
            }

            map.setOnMarkerClickListener { marker ->
                val rumah = markers[marker.id]

                if (rumah != null) {
                    selectedKodeRumah = rumah
                    binding.btnMulaiNavigasi.isEnabled =
                        koordinatValid(rumah.latitude, rumah.longitude)

                    tampilkanDetailRumah(rumah)
                }

                // Marker lokasi saya tidak membuka detail rumah.
                true
            }
        }
    }

    // =====================================================
    // PENCARIAN
    // =====================================================

    private fun setupSearch() {
        binding.btnCariKodeRumah.setOnClickListener {
            jalankanPencarian()
        }

        binding.etCariKodeRumah.setOnEditorActionListener {
                _: TextView,
                actionId: Int,
                event: KeyEvent? ->

            val submit =
                actionId == EditorInfo.IME_ACTION_SEARCH ||
                        (
                                event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                                        event.action == KeyEvent.ACTION_DOWN
                                )

            if (submit) {
                jalankanPencarian()
                true
            } else {
                false
            }
        }
    }

    private fun jalankanPencarian() {
        sembunyikanKeyboard()

        val query = binding.etCariKodeRumah.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (query.isEmpty()) {
            loadKodeRumah()
        } else {
            loadKodeRumah(query = query)
        }
    }

    private fun sembunyikanKeyboard() {
        val imm = requireContext().getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            binding.etCariKodeRumah.windowToken,
            0
        )

        binding.etCariKodeRumah.clearFocus()
    }

    // =====================================================
    // AMBIL DATA KODE RUMAH
    // =====================================================

    private fun loadKodeRumah(query: String? = null) {
        val authorization = getAuthorization()

        if (authorization == null) {
            tampilkanPesan(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        binding.btnCariKodeRumah.isEnabled = false
        binding.tvStatusLokasi.text = "Memuat data kode rumah..."

        repository.getKodeRumah(
            authorization = authorization,
            q = query,
            namaKecamatan = filterKecamatan,
            namaKelurahan = filterKelurahan,
            namaRtRw = filterRtRw,
            perPage = 100
        ) { response, _, errorMessage ->

            if (_binding == null) return@getKodeRumah

            binding.btnCariKodeRumah.isEnabled = true

            if (response?.success == true) {
                hasilPencarian.clear()
                hasilPencarian.addAll(response.data)

                /*
                 * Simpan data wilayah hanya dari permintaan awal
                 * yang tidak menggunakan pencarian atau filter.
                 *
                 * Saat pengguna memilih Kecamatan tertentu,
                 * hasil filter tidak akan menimpa cache ini.
                 */
                val tanpaFilter =
                    query.isNullOrBlank() &&
                            filterKecamatan == null &&
                            filterKelurahan == null &&
                            filterRtRw == null

                if (tanpaFilter) {
                    semuaDataKodeRumah.clear()
                    semuaDataKodeRumah.addAll(response.data)
                }

                perbaruiStatusFilter()
                tampilkanMarker()

                val jumlahKoordinat = hasilPencarian.count {
                    koordinatValid(it.latitude, it.longitude)
                }

                binding.tvStatusLokasi.text =
                    "${hasilPencarian.size} data ditemukan • " +
                            "$jumlahKoordinat memiliki koordinat"

                if (hasilPencarian.isEmpty()) {
                    tampilkanPesan("Data kode rumah tidak ditemukan.")
                } else if (jumlahKoordinat == 0) {
                    tampilkanPesan(
                        "Data ditemukan, tetapi belum memiliki koordinat valid."
                    )
                }
            } else {
                binding.tvStatusLokasi.text = "Gagal memuat data"

                tampilkanPesan(
                    errorMessage
                        ?: response?.message
                        ?: "Gagal mengambil data kode rumah."
                )
            }
        }
    }

    private fun koordinatValid(
        latitude: Double?,
        longitude: Double?
    ): Boolean {
        if (latitude == null || longitude == null) return false
        if (!latitude.isFinite() || !longitude.isFinite()) return false

        return latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0)
    }

    // =====================================================
    // MARKER RUMAH
    // =====================================================

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

            if (!koordinatValid(latitude, longitude)) {
                return@forEach
            }

            val title = rumah.noBangunan
                ?.takeIf { it.isNotBlank() }
                ?: rumah.idsubsls
                ?: "Kode rumah ${rumah.id}"

            val snippet = buildString {
                rumah.namaKelurahan
                    ?.takeIf { it.isNotBlank() }
                    ?.let { append(it) }

                rumah.namaRtRw
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        if (isNotEmpty()) append(" • ")
                        append(it)
                    }
            }

            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(latitude!!, longitude!!))
                    .title(title)
                    .snippet(snippet)
            )

            markers[marker.id] = rumah
        }

        pendingLocation?.let {
            perbaruiMarkerLokasiSaya(it)
        }

        binding.layoutPetunjukPeta.visibility =
            if (markers.isEmpty()) View.VISIBLE else View.GONE

        if (markers.size == 1 && pendingLocation == null) {
            val rumah = markers.values.first()

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(rumah.latitude!!, rumah.longitude!!),
                    17.0
                )
            )
        }
    }

    // =====================================================
    // POPUP DETAIL MODERN
    // =====================================================

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

        header.addView(
            ikon,
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        val headerText = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        headerText.addView(
            teks(
                "Detail Kode Rumah",
                19f,
                warnaTeks,
                Typeface.BOLD
            )
        )

        headerText.addView(
            teks(
                "Informasi titik bangunan",
                12f,
                warnaSekunder
            ).apply {
                setPadding(0, dp(3), 0, 0)
            }
        )

        header.addView(
            headerText,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        root.addView(header)

        val kode = rumah.noBangunan
            ?.takeIf { it.isNotBlank() }
            ?: "Rumah ${rumah.id}"

        val subSls = rumah.idsubsls ?: "-"

        val identitas = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = buatBackground(warnaLatar, 16)
        }

        identitas.addView(
            teks(
                "NOMOR BANGUNAN",
                10f,
                warnaSekunder,
                Typeface.BOLD
            )
        )

        identitas.addView(
            teks(
                kode,
                20f,
                warnaUtama,
                Typeface.BOLD
            ).apply {
                setPadding(0, dp(3), 0, dp(2))
            }
        )

        identitas.addView(
            teks(
                "ID SubSLS: $subSls",
                12f,
                warnaTeks
            )
        )

        val marginIdentitas = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(18)
        }

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

        val koordinat = if (
            koordinatValid(rumah.latitude, rumah.longitude)
        ) {
            "${rumah.latitude}, ${rumah.longitude}"
        } else {
            "Belum tersedia"
        }

        tambahBarisDetail(detail, "Koordinat aktif", koordinat)

        root.addView(detail)

        val catatan = teks(
            "Jika titik rumah tidak sesuai lokasi sebenarnya, gunakan Retagging untuk mengajukan koordinat dari GPS HP.",
            12f,
            warnaSekunder
        )

        catatan.setPadding(0, dp(4), 0, dp(12))
        root.addView(catatan)

        val tombolNavigasi = buatTombol(
            "Navigasi ke lokasi",
            warnaUtama,
            Color.WHITE
        )

        tombolNavigasi.isEnabled =
            koordinatValid(rumah.latitude, rumah.longitude)

        root.addView(
            tombolNavigasi,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val tombolRetag = buatTombol(
            "⌖  Retagging dengan GPS",
            Color.rgb(255, 244, 235),
            warnaUtama
        )

        val marginRetag = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(48)
        ).apply {
            topMargin = dp(10)
        }

        root.addView(tombolRetag, marginRetag)

        val tombolTutup = TextView(context).apply {
            text = "Tutup"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(warnaSekunder)
            setPadding(0, dp(14), 0, dp(4))
            isClickable = true
            isFocusable = true
        }

        root.addView(
            tombolTutup,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(42)
            )
        )

        val scroll = ScrollView(context).apply {
            isFillViewport = true
            addView(root)
        }

        val dialog = AlertDialog.Builder(context)
            .setView(scroll)
            .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        tombolNavigasi.setOnClickListener {
            dialog.dismiss()
            bukaGoogleMaps(rumah)
        }

        tombolRetag.setOnClickListener {
            dialog.dismiss()
            tampilkanKonfirmasiRetag(rumah)
        }

        tombolTutup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            if (dialogDetail === dialog) {
                dialogDetail = null
            }
        }

        dialogDetail = dialog
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        animasikanPopup(root)
    }

    private fun tambahBarisDetail(
        parent: LinearLayout,
        judul: String,
        nilai: String?
    ) {
        val baris = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }

        baris.addView(
            teks(
                judul,
                11f,
                warnaSekunder
            )
        )

        baris.addView(
            teks(
                nilai?.takeIf { it.isNotBlank() } ?: "-",
                14f,
                warnaTeks,
                Typeface.NORMAL
            ).apply {
                setPadding(0, dp(3), 0, 0)
            }
        )

        parent.addView(baris)

        val divider = View(requireContext()).apply {
            setBackgroundColor(warnaGaris)
        }

        parent.addView(
            divider,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        )
    }

    // =====================================================
    // RETAGGING DENGAN GPS HP
    // =====================================================

    private fun tampilkanKonfirmasiRetag(rumah: KodeRumah) {
        if (_binding == null) return

        val location = pendingLocation

        if (location == null) {
            tampilkanPesan(
                "Lokasi GPS belum tersedia. Tekan tombol Lokasi Saya, tunggu GPS diperbarui, lalu coba Retagging kembali."
            )
            periksaIzinLokasi()
            return
        }

        val umurLokasi = System.currentTimeMillis() - location.time

        if (umurLokasi < 0 || umurLokasi > 120_000L) {
            tampilkanPesan(
                "Data GPS sudah terlalu lama. Perbarui lokasi melalui tombol Lokasi Saya, lalu ulangi Retagging."
            )
            periksaIzinLokasi()
            return
        }

        if (!koordinatValid(location.latitude, location.longitude)) {
            tampilkanPesan("Koordinat GPS tidak valid.")
            return
        }

        if (!location.hasAccuracy() || location.accuracy > 100f) {
            tampilkanPesan(
                "Akurasi GPS belum memadai (maksimal 100 meter). Tunggu GPS lebih akurat lalu coba lagi."
            )
            return
        }

        val isi = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(18), dp(22), dp(8))
            background = buatBackground(Color.WHITE, 20)
        }

        isi.addView(
            teks(
                "Ajukan Retagging",
                20f,
                warnaTeks,
                Typeface.BOLD
            )
        )

        isi.addView(
            teks(
                "Koordinat baru akan diambil dari GPS HP kamu. Pastikan kamu berada di lokasi rumah yang benar.",
                13f,
                warnaSekunder
            ).apply {
                setPadding(0, dp(8), 0, dp(16))
            }
        )

        val dataGps = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = buatBackground(warnaLatar, 14)
        }

        dataGps.addView(
            teks(
                "KOORDINAT GPS BARU",
                10f,
                warnaSekunder,
                Typeface.BOLD
            )
        )

        dataGps.addView(
            teks(
                "Lat: %.7f".format(location.latitude),
                14f,
                warnaTeks,
                Typeface.BOLD
            ).apply {
                setPadding(0, dp(6), 0, 0)
            }
        )

        dataGps.addView(
            teks(
                "Long: %.7f".format(location.longitude),
                14f,
                warnaTeks,
                Typeface.BOLD
            ).apply {
                setPadding(0, dp(3), 0, 0)
            }
        )

        dataGps.addView(
            teks(
                "Akurasi ±%.0f meter".format(location.accuracy),
                12f,
                warnaSekunder
            ).apply {
                setPadding(0, dp(6), 0, 0)
            }
        )

        isi.addView(dataGps)

        isi.addView(
            teks(
                "Rumah: ${rumah.noBangunan ?: rumah.idsubsls ?: rumah.id}",
                13f,
                warnaTeks
            ).apply {
                setPadding(0, dp(14), 0, dp(4))
            }
        )

        isi.addView(
            teks(
                "Koordinat aktif tidak seharusnya berubah hanya karena pengajuan ini. Pastikan backend menangani pengajuan dan verifikasinya.",
                12f,
                warnaSekunder
            ).apply {
                setPadding(0, dp(4), 0, dp(8))
            }
        )

        val scroll = ScrollView(requireContext()).apply {
            addView(isi)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(scroll)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Kirim Pengajuan", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(warnaUtama)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(warnaSekunder)

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    kirimRetagging(rumah, location, dialog)
                }

            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }

        dialogRetag = dialog
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        animasikanPopup(isi)

        dialog.setOnDismissListener {
            if (dialogRetag === dialog) {
                dialogRetag = null
            }
        }
    }

    private fun kirimRetagging(
        rumah: KodeRumah,
        location: Location,
        dialog: AlertDialog
    ) {
        if (retagSedangDiproses) return

        val authorization = getAuthorization()

        if (authorization == null) {
            tampilkanPesan(
                "Sesi login tidak ditemukan. Silakan login kembali."
            )
            return
        }

        // Periksa kembali umur dan akurasi GPS saat pengguna mengonfirmasi.
        val umurLokasi = System.currentTimeMillis() - location.time

        if (
            umurLokasi < 0 ||
            umurLokasi > 120_000L ||
            !location.hasAccuracy() ||
            location.accuracy > 100f ||
            !koordinatValid(location.latitude, location.longitude)
        ) {
            tampilkanPesan(
                "GPS tidak lagi memenuhi syarat. Perbarui lokasi sebelum mengirim."
            )
            dialog.dismiss()
            periksaIzinLokasi()
            return
        }

        retagSedangDiproses = true

        val tombolKirim =
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)

        tombolKirim.isEnabled = false
        tombolKirim.text = "Mengirim..."

        val request = KodeRumahRetagRequest(
            latitude = location.latitude,
            longitude = location.longitude
        )

        repository.retagKodeRumah(
            authorization = authorization,
            id = rumah.id,
            request = request
        ) { response, _, errorMessage ->

            retagSedangDiproses = false

            if (_binding == null) return@retagKodeRumah

            if (response?.success == true) {
                dialog.dismiss()

                tampilkanPesan(
                    response.message
                        ?: "Pengajuan retagging berhasil dikirim."
                )

                // Muat ulang dari server. Koordinat aktif tidak diganti
                // secara lokal sebelum server menyatakan berhasil.
                loadKodeRumah()
            } else {
                tombolKirim.isEnabled = true
                tombolKirim.text = "Kirim Pengajuan"

                tampilkanPesan(
                    errorMessage
                        ?: response?.message
                        ?: "Gagal mengirim pengajuan retagging."
                )
            }
        }
    }

    // =====================================================
    // ANIMASI POPUP
    // =====================================================

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
                ObjectAnimator.ofFloat(
                    view,
                    View.TRANSLATION_Y,
                    dp(16).toFloat(),
                    0f
                )
            )
            duration = 240L
            start()
        }
    }

    private fun teks(
        isi: String,
        ukuran: Float,
        warna: Int,
        gaya: Int = Typeface.NORMAL
    ): TextView {
        return TextView(requireContext()).apply {
            text = isi
            textSize = ukuran
            setTextColor(warna)
            typeface = Typeface.create("sans-serif", gaya)
        }
    }

    private fun buatTombol(
        label: String,
        warnaLatar: Int,
        warnaTeks: Int
    ): Button {
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

    private fun buatBackground(
        warna: Int,
        radiusDp: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(warna)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(nilai: Int): Int {
        return (nilai * resources.displayMetrics.density).toInt()
    }

    // =====================================================
    // NAVIGASI GOOGLE MAPS
    // =====================================================

    private fun bukaGoogleMaps(rumah: KodeRumah) {
        val latitude = rumah.latitude
        val longitude = rumah.longitude

        if (!koordinatValid(latitude, longitude)) {
            tampilkanPesan(
                "Rumah ini belum memiliki koordinat yang valid."
            )
            return
        }

        val uriGoogleMaps = Uri.parse(
            "google.navigation:q=$latitude,$longitude&mode=d"
        )

        try {
            val intentGoogleMaps = Intent(
                Intent.ACTION_VIEW,
                uriGoogleMaps
            ).apply {
                setPackage("com.google.android.apps.maps")
            }

            startActivity(intentGoogleMaps)
        } catch (_: Exception) {
            val uriFallback = Uri.parse(
                "https://www.google.com/maps/dir/?api=1" +
                        "&destination=$latitude,$longitude" +
                        "&travelmode=driving"
            )

            try {
                startActivity(Intent(Intent.ACTION_VIEW, uriFallback))
            } catch (_: Exception) {
                tampilkanPesan("Tidak dapat membuka aplikasi peta.")
            }
        }
    }

    // =====================================================
    // FILTER WILAYAH
    // =====================================================

    private fun setupFilters() {
        perbaruiStatusFilter()

        binding.btnFilterKecamatan.setOnClickListener {
            /*
             * Gabungkan cache wilayah awal dengan hasil saat ini.
             * Dengan begitu, opsi Kecamatan tetap lengkap ketika
             * hasilPencarian sudah dibatasi oleh filter Kecamatan.
             */
            val opsiKecamatan = (
                    semuaDataKodeRumah + hasilPencarian
                    )
                .mapNotNull { it.namaKecamatan?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            pilihFilter(
                judul = "Pilih Kecamatan",
                nilaiSaatIni = filterKecamatan,
                opsi = opsiKecamatan
            ) { nilai ->
                if (filterKecamatan != nilai) {
                    filterKecamatan = nilai
                    filterKelurahan = null
                    filterRtRw = null

                    binding.btnFilterKecamatan.text =
                        nilai ?: "Kecamatan"

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

            val sumberKelurahan =
                (semuaDataKodeRumah + hasilPencarian)
                    .distinctBy { it.id }

            val opsiKelurahan = sumberKelurahan
                .filter {
                    it.namaKecamatan.equals(
                        filterKecamatan,
                        ignoreCase = true
                    )
                }
                .mapNotNull { it.namaKelurahan?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            pilihFilter(
                judul = "Pilih Desa/Kelurahan",
                nilaiSaatIni = filterKelurahan,
                opsi = opsiKelurahan
            ) { nilai ->
                if (filterKelurahan != nilai) {
                    filterKelurahan = nilai
                    filterRtRw = null

                    binding.btnFilterDesa.text =
                        nilai ?: "Desa/Kelurahan"

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

            val sumberRtRw =
                (semuaDataKodeRumah + hasilPencarian)
                    .distinctBy { it.id }

            val opsiRtRw = sumberRtRw
                .filter {
                    it.namaKecamatan.equals(
                        filterKecamatan,
                        ignoreCase = true
                    )
                }
                .filter {
                    it.namaKelurahan.equals(
                        filterKelurahan,
                        ignoreCase = true
                    )
                }
                .mapNotNull { it.namaRtRw?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()

            pilihFilter(
                judul = "Pilih RT/RW/SLS",
                nilaiSaatIni = filterRtRw,
                opsi = opsiRtRw
            ) { nilai ->
                if (filterRtRw != nilai) {
                    filterRtRw = nilai

                    binding.btnFilterSls.text =
                        nilai ?: "RT/RW/SLS"

                    perbaruiStatusFilter()
                    loadKodeRumah()
                }
            }
        }
    }

    private fun perbaruiStatusFilter() {
        binding.btnFilterDesa.isEnabled =
            !filterKecamatan.isNullOrBlank()

        binding.btnFilterSls.isEnabled =
            !filterKecamatan.isNullOrBlank() &&
                    !filterKelurahan.isNullOrBlank()
    }

    private fun pilihFilter(
        judul: String,
        nilaiSaatIni: String?,
        opsi: List<String>,
        onSelected: (String?) -> Unit
    ) {
        if (opsi.isEmpty()) {
            tampilkanPesan(
                "Tidak ada pilihan wilayah untuk filter ini."
            )
            return
        }

        val pilihan = listOf("Semua") + opsi

        val indexTerpilih =
            if (nilaiSaatIni == null) {
                0
            } else {
                pilihan.indexOf(nilaiSaatIni).coerceAtLeast(0)
            }

        AlertDialog.Builder(requireContext())
            .setTitle(judul)
            .setSingleChoiceItems(
                pilihan.toTypedArray(),
                indexTerpilih
            ) { dialog, which ->
                val nilai =
                    if (which == 0) null else pilihan[which]

                dialog.dismiss()
                onSelected(nilai)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // =====================================================
    // GPS / LOKASI SAYA
    // =====================================================

    private fun setupLocationButton() {
        locationManager = requireContext().getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        binding.btnLokasiSaya.setOnClickListener {
            periksaIzinLokasi()
        }
    }

    private fun periksaIzinLokasi() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        ambilLokasiSaatIni()
    }

    private fun ambilLokasiSaatIni() {
        val manager = locationManager ?: return
        val context = context ?: return

        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            tampilkanPesan("Izin lokasi belum diberikan.")
            return
        }

        try {
            val providers = manager.getProviders(true).filter {
                it == LocationManager.GPS_PROVIDER ||
                        it == LocationManager.NETWORK_PROVIDER
            }

            if (providers.isEmpty()) {
                binding.tvStatusLokasi.text =
                    "Aktifkan layanan lokasi perangkat"

                tampilkanPesan(
                    "GPS atau layanan lokasi perangkat belum aktif."
                )
                return
            }

            val lastKnown = providers.mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (_: SecurityException) {
                    null
                }
            }.maxByOrNull { it.time }

            if (lastKnown != null) {
                pendingLocation = lastKnown
                perbaruiMarkerLokasiSaya(lastKnown)
                updateCameraToLocation(lastKnown)

                binding.tvStatusLokasi.text =
                    "Memperbarui lokasi GPS…"
            } else {
                binding.tvStatusLokasi.text =
                    "Mencari lokasi GPS…"
            }

            locationListener?.let {
                try {
                    manager.removeUpdates(it)
                } catch (_: SecurityException) {
                }
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

                    binding.tvStatusLokasi.text =
                        "Lokasi saya: %.6f, %.6f (akurasi ±%.0f m)".format(
                            location.latitude,
                            location.longitude,
                            location.accuracy
                        )
                }

                @Deprecated("Deprecated by Android")
                override fun onStatusChanged(
                    provider: String?,
                    status: Int,
                    extras: Bundle?
                ) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                override fun onProviderDisabled(provider: String) = Unit
            }

            locationListener = listener

            providers.forEach { provider ->
                try {
                    manager.requestLocationUpdates(
                        provider,
                        2000L,
                        2f,
                        listener
                    )
                } catch (_: SecurityException) {
                    // Izin lokasi mungkin berubah saat aplikasi berjalan.
                }
            }
        } catch (_: SecurityException) {
            tampilkanPesan("Izin lokasi belum diberikan.")
        } catch (e: Exception) {
            tampilkanPesan(
                e.message ?: "Gagal mendapatkan lokasi perangkat."
            )
        }
    }

    private fun updateCameraToLocation(location: Location) {
        pendingLocation = location
        perbaruiMarkerLokasiSaya(location)

        val map = mapboxMap ?: return

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(location.latitude, location.longitude),
                17.0
            )
        )

        if (_binding != null) {
            binding.tvStatusLokasi.text =
                "Lokasi saya: %.6f, %.6f (akurasi ±%.0f m)".format(
                    location.latitude,
                    location.longitude,
                    location.accuracy
                )
        }
    }

    private fun perbaruiMarkerLokasiSaya(location: Location) {
        val map = mapboxMap ?: return

        if (!mapStyleLoaded || _binding == null) return

        val position = LatLng(
            location.latitude,
            location.longitude
        )

        val existing = markerLokasiSaya

        if (existing != null) {
            existing.position = position
        } else {
            markerLokasiSaya = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title("Lokasi saya")
                    .snippet("Posisi perangkat berdasarkan GPS")
                    .icon(
                        IconFactory.getInstance(requireContext())
                            .defaultMarker()
                    )
            )
        }
    }

    // =====================================================
    // RIWAYAT KODE RUMAH
    // =====================================================

    private fun setupLogButton() {
        binding.btnLihatLog.setOnClickListener {
            val role = authSession.getRole()
                ?.trim()
                ?.lowercase()
                .orEmpty()

            if (role != "pimpinan" && role != "ketua_tim") {
                tampilkanPesan(
                    "Kamu tidak memiliki akses ke menu riwayat."
                )
                return@setOnClickListener
            }

            loadLogKodeRumah()
        }
    }

    private fun loadLogKodeRumah() {
        val authorization = getAuthorization()

        if (authorization == null) {
            tampilkanPesan("Sesi login tidak ditemukan.")
            return
        }

        repository.getLogKodeRumah(
            authorization = authorization,
            perPage = 50
        ) { response, _, errorMessage ->

            if (_binding == null) return@getLogKodeRumah

            if (response?.success == true) {
                tampilkanDaftarLog(response.data)
            } else {
                tampilkanPesan(
                    errorMessage
                        ?: response?.message
                        ?: "Gagal mengambil riwayat kode rumah."
                )
            }
        }
    }

    private fun tampilkanDaftarLog(logs: List<KodeRumahLog>) {
        if (logs.isEmpty()) {
            tampilkanPesan(
                "Belum ada riwayat perubahan kode rumah."
            )
            return
        }

        val labels = logs.map { log ->
            buildString {
                append(log.aksi ?: "Perubahan")
                    .append(" • ")
                    .append(log.idsubsls ?: "Tanpa ID SubSLS")
                    .append(" • ")
                    .append(log.namaPengguna ?: "Pengguna tidak diketahui")

                log.createdAt?.let {
                    append("\n").append(it)
                }
            }
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Riwayat Kode Rumah")
            .setItems(labels) { _, index ->
                tampilkanDetailLog(logs[index])
            }
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun tampilkanDetailLog(log: KodeRumahLog) {
        val pesan = buildString {
            append("Aksi: ").append(log.aksi ?: "-")
            append("\nID SubSLS: ").append(log.idsubsls ?: "-")
            append("\nNomor bangunan: ").append(log.noBangunan ?: "-")
            append("\nPengguna: ").append(log.namaPengguna ?: "-")
            append("\nWaktu: ").append(log.createdAt ?: "-")
            append("\n\nKeterangan:\n").append(log.keterangan ?: "-")
            append("\n\nData sebelum:\n").append(log.dataSebelum ?: "-")
            append("\n\nData sesudah:\n").append(log.dataSesudah ?: "-")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Riwayat")
            .setMessage(pesan)
            .setPositiveButton("Tutup", null)
            .show()
    }

    // =====================================================
    // TOMBOL NAVIGASI
    // =====================================================

    private fun setupNavigationButton() {
        binding.btnMulaiNavigasi.isEnabled = false

        binding.btnMulaiNavigasi.setOnClickListener {
            val rumah = selectedKodeRumah

            if (rumah == null) {
                tampilkanPesan(
                    "Pilih marker rumah terlebih dahulu."
                )
                return@setOnClickListener
            }

            bukaGoogleMaps(rumah)
        }
    }

    // =====================================================
    // UTILITAS
    // =====================================================

    private fun tampilkanPesan(pesan: String) {
        val root = _binding?.root ?: return

        Snackbar.make(
            root,
            pesan,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun aturGesturePeta(mapView: MapView) {
        mapView.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN,
                MotionEvent.ACTION_MOVE -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }

            false
        }
    }

    // =====================================================
    // LIFECYCLE MAPVIEW
    // =====================================================

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
            try {
                locationManager?.removeUpdates(it)
            } catch (_: SecurityException) {
            }
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
            try {
                locationManager?.removeUpdates(it)
            } catch (_: SecurityException) {
            }
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

        // Cache dibersihkan saat tampilan dihancurkan agar tidak
        // membawa data wilayah dari instance fragment sebelumnya.
        semuaDataKodeRumah.clear()
        hasilPencarian.clear()
        markers.clear()

        _binding = null

        super.onDestroyView()
    }
}
