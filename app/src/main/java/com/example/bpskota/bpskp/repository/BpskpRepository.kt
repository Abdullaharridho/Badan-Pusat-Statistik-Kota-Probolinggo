package com.example.bpskota.bpskp.repository

import android.content.Context
import android.provider.OpenableColumns
import android.util.Log
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.*
import com.example.bpskota.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Repository untuk mengelola semua pemanggilan API BPS Kota Probolinggo.
 * Menangani parsing response, error handling tersentralisasi, dan pemetaan data.
 */
class BpskpRepository {

    companion object {
        private const val TAG = "BpskpRepository"
        private val api = BpskpRetrofitClient.api
    }

    /** Helper mengubah String menjadi RequestBody (Plain Text) untuk Multipart */
    private fun String.toTextRequestBody(): RequestBody {
        return this.toRequestBody("text/plain".toMediaType())
    }

    /**
     * Extension function generic untuk menyederhanakan pemanggilan Retrofit enqueue.
     * Mencegah duplikasi kode onResponse dan onFailure, sekaligus menangani
     * ekstraksi pesan error dari respons Laravel.
     */
    private fun <T> Call<T>.enqueueRequest(
        onResult: (body: T?, code: Int?, errorMsg: String?) -> Unit
    ) {
        this.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val code = response.code()
                if (response.isSuccessful) {
                    onResult(response.body(), code, null)
                } else {
                    val errorString = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    val parsedError = parseErrorBodyMessage(errorString)
                    onResult(null, code, parsedError ?: getErrorMessage(code))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e(TAG, "API Request Failure: ${t.message}", t)
                onResult(null, null, getConnectionErrorMessage(t))
            }
        })
    }

    /** Helper untuk mendapatkan Bearer Token dari Sesi */
    private fun getAuthorizationHeader(session: BpskpAuthSession): String? {
        val token = session.getToken()
        return if (!token.isNullOrBlank()) "Bearer $token" else null
    }

    // ==========================================
    // AUTHENTICATION & PROFILE
    // ==========================================

    /** Login menggunakan username dan password standar */
    fun login(username: String, password: String, callback: (LoginResponse?, String?) -> Unit) {
        val request = LoginRequest(username, password)
        api.login(request).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && !body.token.isNullOrBlank() && body.user != null) {
                callback(body, null)
            } else {
                val customError = when (code) {
                    401 -> "Username atau password salah."
                    422 -> "Data login tidak valid."
                    else -> errorMsg ?: body?.message ?: "Response login tidak valid."
                }
                callback(null, customError)
            }
        }
    }

    /** Login menggunakan autentikasi biometrik */
    fun biometricLogin(credentialId: String, callback: (LoginResponse?, String?, Int?) -> Unit) {
        if (credentialId.isBlank()) return callback(null, "Credential biometrik tidak ditemukan.", null)
        val request = BiometricLoginRequest(credential_id = credentialId)
        api.biometricLogin(request).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && !body.token.isNullOrBlank() && body.user != null) {
                callback(body, null, code)
            } else {
                val customError = if (code == 401) "Credential biometrik tidak valid atau kadaluarsa." else (errorMsg ?: body?.message)
                callback(null, customError, code)
            }
        }
    }

    /** Logout dari sistem dan menghapus token */
    fun logout(session: BpskpAuthSession, callback: (LoginResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session)
        if (auth == null) {
            session.clearSession()
            return callback(null, "Token tidak ditemukan.")
        }
        api.logout(auth).enqueueRequest { body, _, errorMsg ->
            session.clearSession()
            callback(body, errorMsg)
        }
    }

    /** Mendaftarkan kredensial biometrik ke akun pengguna yang sedang aktif */
    fun biometricRegister(session: BpskpAuthSession, credentialId: String, deviceName: String?, callback: (BiometricRegisterResponse?, String?, Int?) -> Unit) {
        if (credentialId.isBlank()) return callback(null, "Credential biometrik tidak valid.", null)
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.", null)
        val request = BiometricRegisterRequest(credential_id = credentialId, device_name = deviceName)
        api.biometricRegister(auth, request).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && body.credential != null) callback(body, null, code)
            else callback(null, if (code == 422) "Credential sudah terdaftar." else (errorMsg ?: body?.message), code)
        }
    }

    /** Mengambil profil pengguna yang sedang login */
    fun getUser(session: BpskpAuthSession, callback: (LoginResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getUser(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.user != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Response data user tidak valid.")
        }
    }

    /** Memperbarui profil (Nama, Username) pengguna */
    fun updateProfile(session: BpskpAuthSession, name: String, username: String, callback: (ProfileResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = ProfileRequest(name = name, username = username)
        api.updateProfile(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal update profil.")
        }
    }

    /** Memperbarui password pengguna */
    fun updatePassword(session: BpskpAuthSession, currentPassword: String, newPassword: String, callback: (ProfileResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = PasswordUpdateRequest(currentPassword, newPassword, newPassword)
        api.updatePassword(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal update password.")
        }
    }

    // ==========================================
    // USER MANAGEMENT
    // ==========================================

    /** Mengambil seluruh daftar pengguna dari server */
    fun getUsers(session: BpskpAuthSession, callback: (UserResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getUsers(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Response tidak valid.")
        }
    }

    /** Mengambil detail spesifik dari satu pengguna */
    fun getUserDetail(session: BpskpAuthSession, id: Int, callback: (UserDetailResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getUserDetail(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil detail user.")
        }
    }

    /** Membuat pengguna baru (Admin role) */
    fun createUser(session: BpskpAuthSession, name: String, username: String, password: String, role: String, callback: (UserActionResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = UserRequest(name, username, password, role)
        api.createUser(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal membuat user.")
        }
    }

    /** Memperbarui data pengguna yang sudah ada */
    fun updateUser(session: BpskpAuthSession, id: Int, name: String, username: String, password: String?, role: String, callback: (UserActionResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = UserRequest(name, username, password, role)
        api.updateUser(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal update user.")
        }
    }

    /** Menghapus pengguna secara spesifik berdasarkan ID */
    fun deleteUser(session: BpskpAuthSession, id: Int, callback: (UserActionResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.deleteUser(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal menghapus user.")
        }
    }

    // ==========================================
    // STATISTICS
    // ==========================================

    /** Mendapatkan statistik aktivitas pengguna secara umum */
    fun getActivityStatistics(callback: (ActivityStatisticsResponse?, String?) -> Unit) {
        api.getActivityStatistics().enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.summary != null) callback(body, null)
            else callback(null, errorMsg ?: "Data statistik tidak valid.")
        }
    }

    /** Mendapatkan statistik seluruh aktivitas untuk tampilan Admin */
    fun getAdminActivityStatistics(session: BpskpAuthSession, callback: (AdminActivityStatisticsResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getAdminActivityStatistics(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.summary != null) callback(body, null)
            else callback(null, errorMsg ?: "Gagal mengambil statistik admin.")
        }
    }

    // ==========================================
    // SURAT MASUK
    // ==========================================

    /** Mengambil seluruh daftar surat masuk */
    fun getSuratMasuk(authorization: String, search: String? = null, status: String? = null, callback: (List<SuratMasuk>?, String?) -> Unit) {
        api.getSuratMasuk(authorization, search, status).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal mengambil data surat.")
        }
    }

    /** Mengambil detail lengkap sebuah surat masuk */
    fun getSuratMasukDetail(authorization: String, id: Int, callback: (SuratMasuk?, String?) -> Unit) {
        api.getSuratMasukDetail(authorization, id).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal mengambil detail surat.")
        }
    }

    /** Menghapus sebuah data surat masuk */
    fun deleteSuratMasuk(authorization: String, id: Int, callback: (Boolean, String?) -> Unit) {
        api.deleteSuratMasuk(authorization, id).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(true, null) else callback(false, errorMsg ?: "Gagal menghapus surat.")
        }
    }

    /** Memperbarui status dari sebuah surat masuk */
    fun updateSuratMasukStatus(authorization: String, id: Int, status: String, callback: (Boolean, String?) -> Unit) {
        val request = SuratMasukStatusRequest(status)
        api.updateSuratMasukStatus(authorization, id, request).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(true, null) else callback(false, errorMsg ?: "Gagal mengubah status surat.")
        }
    }

    /** Membuat entri baru untuk surat masuk, mendukung lampiran file */
    fun createSuratMasuk(
        authorization: String, nomorSurat: String, tanggalSurat: String, tanggalDiterima: String,
        asalSurat: String, perihal: String, isiRingkas: String?, tanggalAcara: String?,
        waktuMulai: String?, waktuSelesai: String?, lokasi: String?, fileSurat: MultipartBody.Part?,
        callback: (SuratMasuk?, String?) -> Unit
    ) {
        api.createSuratMasuk(
            authorization, nomorSurat.toTextRequestBody(), tanggalSurat.toTextRequestBody(),
            tanggalDiterima.toTextRequestBody(), asalSurat.toTextRequestBody(), perihal.toTextRequestBody(),
            isiRingkas?.toTextRequestBody(), tanggalAcara?.toTextRequestBody(), waktuMulai?.toTextRequestBody(),
            waktuSelesai?.toTextRequestBody(), lokasi?.toTextRequestBody(), null, fileSurat
        ).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal menambahkan surat.")
        }
    }

    // ==========================================
    // TIM & PEGAWAI
    // ==========================================

    /** Mengambil daftar tim beserta anggotanya */
    fun getTim(session: BpskpAuthSession, search: String? = null, status: String? = null, callback: (List<Tim>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getTim(auth, search, status).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil daftar tim.")
        }
    }

    /** Mengambil detail sebuah tim (termasuk list anggotanya) */
    fun getDetailTim(session: BpskpAuthSession, id: Int, callback: (Tim?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getDetailTim(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil detail tim.")
        }
    }

    /** Membuat tim kerja baru */
    fun buatTim(session: BpskpAuthSession, namaTim: String, ketuaId: Int, keterangan: String? = null, status: String = "aktif", callback: (TimMutationResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (namaTim.isBlank()) return callback(null, "Nama tim wajib diisi.")
        if (ketuaId <= 0) return callback(null, "Ketua tim belum dipilih.")

        val request = TimRequest(namaTim.trim(), ketuaId, keterangan?.trim()?.takeIf { it.isNotEmpty() }, status)
        api.buatTim(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal membuat tim.")
        }
    }

    /** Memperbarui profil tim (Ketua, status, keterangan) */
    fun updateTim(session: BpskpAuthSession, id: Int, namaTim: String? = null, ketuaId: Int? = null, keterangan: String? = null, status: String? = null, callback: (TimMutationResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID tim tidak valid.")
        if (namaTim != null && namaTim.isBlank()) return callback(null, "Nama tim tidak boleh kosong.")
        if (status != null && status !in listOf("aktif", "nonaktif")) return callback(null, "Status tim tidak valid.")

        val request = TimUpdateRequest(namaTim?.trim(), ketuaId, keterangan, status)
        api.updateTim(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal memperbarui tim.")
        }
    }

    /** Menghapus sebuah tim dari database */
    fun hapusTim(session: BpskpAuthSession, id: Int, callback: (Boolean, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(false, "Sesi login tidak ditemukan.")
        api.hapusTim(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(true, null) else callback(false, errorMsg ?: body?.message ?: "Gagal menghapus tim.")
        }
    }

    /** Menambah pegawai ke dalam sebuah tim */
    fun tambahAnggotaTim(session: BpskpAuthSession, timId: Int, pegawaiId: Int, callback: (TimMutationResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (timId <= 0 || pegawaiId <= 0) return callback(null, "ID tim atau pegawai tidak valid.")

        val request = TambahAnggotaRequest(pegawaiId)
        api.tambahAnggotaTim(auth, timId, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal menambahkan anggota.")
        }
    }

    /** Menghapus anggota/pegawai dari sebuah tim */
    fun hapusAnggotaTim(session: BpskpAuthSession, timId: Int, pegawaiId: Int, callback: (Boolean, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(false, "Sesi login tidak ditemukan.")
        api.hapusAnggotaTim(auth, timId, pegawaiId).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(true, null) else callback(false, errorMsg ?: body?.message ?: "Gagal menghapus anggota.")
        }
    }

    /** Mendapatkan daftar seluruh pegawai yang tersedia (untuk ditugaskan) */
    fun getPegawaiTersedia(session: BpskpAuthSession, callback: (List<PegawaiTersedia>?, String?) -> Unit) {
        val tag = "REKOMENDASI_PEGAWAI"
        Log.d(tag, "Memulai permintaan daftar pegawai tersedia.")
        val auth = getAuthorizationHeader(session)
        if (auth == null) {
            Log.e(tag, "Gagal: sesi login tidak ditemukan.")
            return callback(null, "Sesi login tidak ditemukan.")
        }

        api.getPegawaiTersedia(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) {
                val daftarPegawai = body.data
                Log.d(tag, "Jumlah pegawai dari API: ${daftarPegawai?.size ?: 0}")
                daftarPegawai.orEmpty().forEachIndexed { index, pegawai ->
                    Log.d(tag, "Pegawai ke-${index + 1}: ID=${pegawai.id}, Nama=${pegawai.nama}, Role=${pegawai.role}")
                }
                callback(daftarPegawai, null)
            } else {
                val pesanError = errorMsg ?: body?.message ?: "Gagal mengambil pegawai tersedia."
                Log.e(tag, "Permintaan gagal: $pesanError")
                callback(null, pesanError)
            }
        }
    }

    // ==========================================
    // PENUGASAN
    // ==========================================

    /** Mendapatkan daftar semua penugasan (dengan opsional pencarian & filter status) */
    fun getPenugasan(session: BpskpAuthSession, search: String? = null, status: String? = null, callback: (ApiResponse<List<PenugasanResponse>>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getPenugasan(auth, search, status).enqueueRequest { body, _, errorMsg ->
            if (body?.data != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Data penugasan tidak tersedia.")
        }
    }

    /** Mendapatkan detail penuh dari sebuah penugasan */
    fun getDetailPenugasan(session: BpskpAuthSession, id: Long, callback: (ApiResponse<PenugasanResponse>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0L) return callback(null, "ID penugasan tidak valid.")

        api.getDetailPenugasan(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Detail penugasan tidak tersedia.")
        }
    }

    /** Menerbitkan penugasan baru kepada pegawai */
    fun buatPenugasan(session: BpskpAuthSession, request: CreatePenugasanRequest, callback: (ApiResponse<PenugasanResponse>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.buatPenugasan(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.status == "success" && body.data != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Penugasan gagal disimpan.")
        }
    }

    /** Memberikan respon (terima/tolak) terhadap penugasan */
    fun responPenugasan(session: BpskpAuthSession, id: Long, status: String, catatanRespon: String? = null, callback: (ApiResponse<PenugasanResponse>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0L) return callback(null, "ID penugasan tidak valid.")
        if (status !in listOf("diterima", "ditolak")) return callback(null, "Status respons penugasan tidak valid.")

        val request = ResponPenugasanRequest(status, catatanRespon)
        api.responPenugasan(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.data != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Respons penugasan gagal disimpan.")
        }
    }

    // ==========================================
    // LAPORAN & DOKUMEN
    // ==========================================

    /** Mendapatkan seluruh laporan pelaksanaan tugas yang telah diupload */
    fun getLaporan(session: BpskpAuthSession, callback: (List<LaporanData>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getLaporan(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil daftar laporan.")
        }
    }

    /** Melihat detail laporan yang merujuk pada penugasan tertentu */
    fun getDetailLaporan(session: BpskpAuthSession, id: Int, callback: (LaporanData?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID laporan tidak valid.")

        api.getDetailLaporan(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil detail laporan.")
        }
    }

    /**
     * Membangun daftar Part dokumen menggunakan pendekatan functional programming (flatMapIndexed).
     * Lebih efisien, terhindar dari mutable states, dan error handling yang clean.
     */
    private fun buildLaporanDocumentParts(context: Context, dokumen: List<LaporanDokumenUpload>): List<MultipartBody.Part> {
        require(dokumen.size <= 5) { "Maksimal 5 dokumen dalam satu kali upload." }
        val jenisValid = setOf("materi", "dokumentasi", "notulen", "lainnya")
        val resolver = context.contentResolver

        return dokumen.flatMapIndexed { index, item ->
            require(item.jenisDokumen in jenisValid) { "Jenis dokumen tidak valid: ${item.jenisDokumen}" }
            val namaFile = item.namaFile ?: resolver.query(item.uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
            } ?: "dokumen_${index + 1}"

            val bytes = resolver.openInputStream(item.uri)?.use { it.readBytes() }
                ?: throw IllegalArgumentException("File $namaFile tidak dapat dibaca.")

            require(bytes.size <= 5 * 1024 * 1024) { "File $namaFile melebihi batas 5 MB." }

            val mimeType = resolver.getType(item.uri) ?: "application/octet-stream"
            val fileBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

            listOf(
                MultipartBody.Part.createFormData("dokumen[$index][file]", namaFile, fileBody),
                MultipartBody.Part.createFormData("dokumen[$index][jenis_dokumen]", item.jenisDokumen)
            )
        }
    }

    /** Menyimpan dan meng-upload dokumen laporan baru */
    fun submitLaporan(
        context: Context, session: BpskpAuthSession, penugasanId: Int, ringkasan: String,
        hasilKegiatan: String, catatan: String?, dokumen: List<LaporanDokumenUpload>,
        callback: (LaporanActionResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (penugasanId <= 0) return callback(null, "ID penugasan tidak valid.")
        if (ringkasan.isBlank() || hasilKegiatan.isBlank()) return callback(null, "Ringkasan dan hasil kegiatan wajib diisi.")
        if (dokumen.isEmpty() || dokumen.size > 5) return callback(null, "Laporan harus memiliki 1 hingga 5 dokumen.")

        try {
            val parts = buildLaporanDocumentParts(context, dokumen)
            api.submitLaporan(
                auth, penugasanId.toString().toTextRequestBody(), ringkasan.trim().toTextRequestBody(),
                hasilKegiatan.trim().toTextRequestBody(), catatan.orEmpty().toTextRequestBody(), parts
            ).enqueueRequest { body, _, errorMsg ->
                if (body?.success == true) callback(body, null) else callback(null, errorMsg ?: body?.message ?: "Gagal mengirim laporan.")
            }
        } catch (e: Exception) {
            callback(null, e.message ?: "Gagal menyiapkan dokumen laporan.")
        }
    }

    /** Memperbarui laporan yang sudah diupload (mendukung hapus & upload dokumen baru) */
    fun updateLaporan(
        context: Context, session: BpskpAuthSession, id: Int, penugasanId: Int, ringkasan: String,
        hasilKegiatan: String, catatan: String?, dokumenBaru: List<LaporanDokumenUpload> = emptyList(),
        dokumenHapus: List<Int> = emptyList(), callback: (LaporanActionResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0 || penugasanId <= 0) return callback(null, "ID laporan/penugasan tidak valid.")
        if (ringkasan.isBlank() || hasilKegiatan.isBlank()) return callback(null, "Ringkasan dan hasil kegiatan wajib diisi.")
        if (dokumenHapus.any { it <= 0 }) return callback(null, "ID dokumen yang dihapus tidak valid.")
        if (dokumenHapus.distinct().size != dokumenHapus.size) return callback(null, "Daftar dokumen yang dihapus mengandung ID duplikat.")
        if (dokumenBaru.size > 5) return callback(null, "Maksimal 5 dokumen baru.")

        try {
            val partsDokumenBaru = buildLaporanDocumentParts(context = context, dokumen = dokumenBaru)
            val partsDokumenHapus = dokumenHapus.map { dokumenId ->
                MultipartBody.Part.createFormData("dokumen_hapus[]", dokumenId.toString())
            }
            val semuaPartsDokumen = partsDokumenBaru + partsDokumenHapus

            api.updateLaporan(
                auth, id, penugasanId.toString().toTextRequestBody(), ringkasan.trim().toTextRequestBody(),
                hasilKegiatan.trim().toTextRequestBody(), catatan.orEmpty().toTextRequestBody(), semuaPartsDokumen
            ).enqueueRequest { body, _, errorMsg ->
                if (body?.success == true) callback(body, null)
                else callback(null, errorMsg ?: body?.message ?: "Gagal memperbarui laporan.")
            }
        } catch (e: Exception) {
            callback(null, e.message ?: "Gagal menyiapkan dokumen laporan.")
        }
    }

    /** Menghapus laporan beserta dokumennya */
    fun deleteLaporan(session: BpskpAuthSession, id: Int, callback: (Boolean, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(false, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(false, "ID laporan tidak valid.")
        api.deleteLaporan(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(true, null) else callback(false, errorMsg ?: body?.message ?: "Gagal menghapus laporan.")
        }
    }

    // ==========================================
    // ERROR HANDLERS
    // ==========================================

    /** Ekstrak error string "message" dari JSON response Laravel */
    private fun parseErrorBodyMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            JSONObject(errorBody).optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    /** Helper mengubah HTTP Code menjadi teks pesan standard */
    private fun getErrorMessage(code: Int?): String {
        return when (code) {
            400 -> "Permintaan tidak valid."
            401 -> "Sesi login tidak valid atau kadaluarsa (401)."
            403 -> "Anda tidak memiliki akses (403)."
            404 -> "Data tidak ditemukan (404)."
            422 -> "Data yang dikirim tidak valid (422)."
            500 -> "Terjadi kesalahan internal server (500)."
            null -> "Tidak dapat terhubung ke server."
            else -> "Permintaan gagal. Kode: $code"
        }
    }

    /** Helper menerjemahkan exception Throwable ke dalam teks logis */
    private fun getConnectionErrorMessage(throwable: Throwable): String {
        return when {
            throwable.message?.contains("Unable to resolve host", true) == true -> "Tidak dapat terhubung ke jaringan internet."
            throwable.message?.contains("CLEARTEXT", true) == true -> "Koneksi HTTP tidak aman diblokir oleh Android."
            else -> "Terjadi kesalahan koneksi jaringan."
        }
    }

    // ==========================================
    // KEGIATAN HARIAN
    // ==========================================

    /** Mengambil daftar kegiatan harian user, opsional difilter berdasarkan tanggal */
    fun getKegiatanHarian(session: BpskpAuthSession, tanggal: String? = null, callback: (List<KegiatanHarian>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getKegiatanHarian(authorization = auth, tanggal = tanggal?.trim()?.takeIf { it.isNotEmpty() }).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil daftar kegiatan harian.")
        }
    }

    /** Mengambil detail satu kegiatan harian */
    fun getDetailKegiatanHarian(session: BpskpAuthSession, id: Int, callback: (KegiatanHarian?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID kegiatan harian tidak valid.")

        api.getDetailKegiatanHarian(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil detail kegiatan harian.")
        }
    }

    /**
     * Menambahkan kegiatan harian.
     * Tanggal ditentukan oleh Laravel, bukan oleh Android.
     */
    fun tambahKegiatanHarian(
        session: BpskpAuthSession, namaKegiatan: String, jamMulai: String, jamSelesai: String,
        tanggal: String, callback: (KegiatanHarianMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val nama = namaKegiatan.trim()
        val mulai = jamMulai.trim()
        val selesai = jamSelesai.trim()
        val tanggalInput = tanggal.trim()

        if (nama.isBlank()) return callback(null, "Nama kegiatan wajib diisi.")
        if (nama.length > 255) return callback(null, "Nama kegiatan maksimal 255 karakter.")
        if (mulai.isBlank() || selesai.isBlank()) return callback(null, "Jam mulai dan jam selesai wajib diisi.")
        if (tanggalInput.isBlank()) return callback(null, "Tanggal kegiatan wajib diisi.")

        val request = KegiatanHarianRequest(namaKegiatan = nama, jamMulai = mulai, jamSelesai = selesai, tanggal = tanggalInput)
        api.tambahKegiatanHarian(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal menambahkan kegiatan harian.")
        }
    }

    /** Memperbarui kegiatan harian yang sudah ada */
    fun updateKegiatanHarian(
        session: BpskpAuthSession, id: Int, namaKegiatan: String, jamMulai: String, jamSelesai: String,
        tanggal: String, callback: (KegiatanHarianMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID kegiatan harian tidak valid.")

        val nama = namaKegiatan.trim()
        val mulai = jamMulai.trim()
        val selesai = jamSelesai.trim()
        val tanggalInput = tanggal.trim()

        if (nama.isBlank()) return callback(null, "Nama kegiatan wajib diisi.")
        if (nama.length > 255) return callback(null, "Nama kegiatan maksimal 255 karakter.")
        if (mulai.isBlank() || selesai.isBlank()) return callback(null, "Jam mulai dan jam selesai wajib diisi.")
        if (tanggalInput.isBlank()) return callback(null, "Tanggal kegiatan wajib diisi.")

        val request = KegiatanHarianRequest(namaKegiatan = nama, jamMulai = mulai, jamSelesai = selesai, tanggal = tanggalInput)
        api.updateKegiatanHarian(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal memperbarui kegiatan harian.")
        }
    }

    /** Menghapus kegiatan harian berdasarkan ID */
    fun hapusKegiatanHarian(session: BpskpAuthSession, id: Int, callback: (Boolean, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(false, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(false, "ID kegiatan harian tidak valid.")

        api.hapusKegiatanHarian(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(true, null)
            else callback(false, errorMsg ?: body?.message ?: "Gagal menghapus kegiatan harian.")
        }
    }

    // ==========================================
    // CEK JADWAL BENTROK
    // ==========================================

    /** Mengembalikan list User ID yang memiliki jadwal bentrok terhadap surat masuk tertentu */
    fun getPenerimaBentrok(session: BpskpAuthSession, suratMasukId: Long, callback: (List<Int>?, String?) -> Unit) {
        val token = session.getToken()
        if (token.isNullOrBlank()) {
            callback(null, "Sesi login tidak ditemukan.")
            return
        }

        api.getPenerimaBentrok(authorization = "Bearer $token", suratMasukId = suratMasukId)
            .enqueue(object : retrofit2.Callback<PenerimaBentrokResponse> {
                override fun onResponse(call: retrofit2.Call<PenerimaBentrokResponse>, response: retrofit2.Response<PenerimaBentrokResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) callback(body.data?.userIds ?: emptyList(), null)
                        else callback(null, body?.message ?: "Gagal mengambil jadwal bentrok.")
                    } else {
                        callback(null, "Gagal mengambil jadwal bentrok. Kode HTTP: ${response.code()}")
                    }
                }
                override fun onFailure(call: retrofit2.Call<PenerimaBentrokResponse>, t: Throwable) {
                    callback(null, t.message ?: "Terjadi kesalahan koneksi.")
                }
            })
    }

    // ==========================================
    // REKOMENDASI PENUGASAN
    // ==========================================

    /**
     * Mengambil daftar rekomendasi. status: menunggu_review, diterima, ditolak.
     * Jika status null, backend mengembalikan seluruh data yang diizinkan untuk user.
     */
    fun getRekomendasi(session: BpskpAuthSession, status: String? = null, callback: (List<RekomendasiData>?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        api.getRekomendasi(authorization = auth, status = status?.trim()?.takeIf { it.isNotEmpty() }).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true && body.data != null) callback(body.data, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil daftar rekomendasi penugasan.")
        }
    }

    /** Mengajukan usulan rekomendasi pegawai pengganti pada suatu penugasan */
    fun buatRekomendasi(
        session: BpskpAuthSession, penugasanId: Long, pegawaiRekomendasiId: Int, alasan: String,
        callback: (RekomendasiMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val alasanBersih = alasan.trim()

        if (penugasanId <= 0L) return callback(null, "ID penugasan tidak valid.")
        if (pegawaiRekomendasiId <= 0) return callback(null, "Pegawai pengganti belum dipilih.")
        if (alasanBersih.length < 5) return callback(null, "Alasan rekomendasi minimal 5 karakter.")
        if (alasanBersih.length > 2000) return callback(null, "Alasan rekomendasi maksimal 2.000 karakter.")

        val request = RekomendasiRequest(penugasanId = penugasanId, pegawaiRekomendasiId = pegawaiRekomendasiId, alasan = alasanBersih)
        api.buatRekomendasi(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengajukan rekomendasi penugasan.")
        }
    }

    /**
     * Memperbarui rekomendasi yang masih menunggu review.
     * Parameter dapat dikosongkan jika hanya sebagian field yang ingin diperbarui.
     */
    fun updateRekomendasi(
        session: BpskpAuthSession, id: Int, pegawaiRekomendasiId: Int? = null, alasan: String? = null,
        callback: (RekomendasiMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID rekomendasi tidak valid.")
        if (pegawaiRekomendasiId != null && pegawaiRekomendasiId <= 0) return callback(null, "Pegawai pengganti tidak valid.")

        val alasanBersih = alasan?.trim()
        if (alasanBersih != null && alasanBersih.length < 5) return callback(null, "Alasan rekomendasi minimal 5 karakter.")
        if (alasanBersih != null && alasanBersih.length > 2000) return callback(null, "Alasan rekomendasi maksimal 2.000 karakter.")
        if (pegawaiRekomendasiId == null && alasanBersih == null) return callback(null, "Tidak ada perubahan yang dikirim.")

        val request = RekomendasiUpdateRequest(pegawaiRekomendasiId = pegawaiRekomendasiId, alasan = alasanBersih)
        api.updateRekomendasi(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal memperbarui rekomendasi.")
        }
    }

    /** Ketua tim memproses usulan rekomendasi ("diterima" atau "ditolak") */
    fun prosesRekomendasi(session: BpskpAuthSession, id: Int, status: String, callback: (RekomendasiMutationResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(null, "ID rekomendasi tidak valid.")

        val statusBersih = status.trim().lowercase()
        if (statusBersih !in listOf("diterima", "ditolak")) return callback(null, "Status rekomendasi harus diterima atau ditolak.")

        val request = RekomendasiProsesRequest(status = statusBersih)
        api.prosesRekomendasi(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal memproses rekomendasi penugasan.")
        }
    }

    /** Menghapus rekomendasi yang masih berstatus menunggu review */
    fun hapusRekomendasi(session: BpskpAuthSession, id: Int, callback: (Boolean, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(false, "Sesi login tidak ditemukan.")
        if (id <= 0) return callback(false, "ID rekomendasi tidak valid.")

        api.hapusRekomendasi(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(true, null)
            else callback(false, errorMsg ?: body?.message ?: "Gagal menghapus rekomendasi.")
        }
    }

    /** Mendapatkan list pegawai untuk diajukan sebagai pengganti di sebuah penugasan */
    fun getPegawaiPengganti(session: BpskpAuthSession, idPenugasan: Long, callback: (List<PegawaiTersedia>?, String?) -> Unit) {
        val authorization = getAuthorizationHeader(session)
        if (authorization == null) {
            callback(null, "Sesi login tidak ditemukan.")
            return
        }

        api.getPegawaiPengganti(authorization = authorization, idPenugasan = idPenugasan)
            .enqueue(object : retrofit2.Callback<PegawaiTersediaResponse> {
                override fun onResponse(call: retrofit2.Call<PegawaiTersediaResponse>, response: retrofit2.Response<PegawaiTersediaResponse>) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.success == true) {
                            callback(body.data.orEmpty(), null)
                        } else {
                            val pesan = body?.message ?: "Respons API tidak berhasil."
                            Log.e("REKOMENDASI_PEGAWAI", "Respons API: $pesan")
                            callback(null, pesan)
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("REKOMENDASI_PEGAWAI", "HTTP ${response.code()}: $errorBody")
                        callback(null, "HTTP ${response.code()}: ${errorBody ?: "Respons error kosong"}")
                    }
                }
                override fun onFailure(call: retrofit2.Call<PegawaiTersediaResponse>, t: Throwable) {
                    Log.e("REKOMENDASI_PEGAWAI", "Request gagal", t)
                    callback(null, t.localizedMessage ?: "Request gagal.")
                }
            })
    }

    // ==================== RUANGAN ====================

    fun getRuangan(
        authorization: String,
        onResult: (RuanganResponse?, Int?, String?) -> Unit
    ) {
        api.getRuangan(authorization).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true) {
                onResult(body, code, null)
            } else {
                onResult(null, code, errorMsg ?: body?.message ?: "Gagal mengambil daftar ruangan.")
            }
        }
    }

    fun getDetailRuangan(
        authorization: String,
        id: Int,
        onResult: (RuanganDetailResponse?, Int?, String?) -> Unit
    ) {
        api.getDetailRuangan(authorization, id).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && body.data != null) {
                onResult(body, code, null)
            } else {
                onResult(null, code, errorMsg ?: body?.message ?: "Gagal mengambil detail ruangan.")
            }
        }
    }

    fun tambahRuangan(
        authorization: String,
        request: RuanganRequest,
        onResult: (RuanganDetailResponse?, Int?, String?) -> Unit
    ) {
        api.tambahRuangan(authorization, request).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && body.data != null) {
                onResult(body, code, null)
            } else {
                onResult(null, code, errorMsg ?: body?.message ?: "Gagal menambahkan ruangan.")
            }
        }
    }

    fun updateRuangan(
        authorization: String,
        id: Int,
        request: RuanganRequest,
        onResult: (RuanganDetailResponse?, Int?, String?) -> Unit
    ) {
        api.updateRuangan(authorization, id, request).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true && body.data != null) {
                onResult(body, code, null)
            } else {
                onResult(null, code, errorMsg ?: body?.message ?: "Gagal memperbarui ruangan.")
            }
        }
    }

    fun hapusRuangan(
        authorization: String,
        id: Int,
        onResult: (RuanganMutationResponse?, Int?, String?) -> Unit
    ) {
        api.hapusRuangan(authorization, id).enqueueRequest { body, code, errorMsg ->
            if (body?.success == true) {
                onResult(body, code, null)
            } else {
                onResult(null, code, errorMsg ?: body?.message ?: "Gagal menghapus ruangan.")
            }
        }
    }

    // ==================== PEMESANAN RUANGAN ====================

    /** Mendapatkan list pesanan pemakaian ruangan (mendukung filter opsional) */
    fun getPemesananRuangan(
        authorization: String, tanggal: String? = null, ruanganId: Int? = null, status: String? = null,
        search: String? = null, onResult: (PemesananRuanganResponse?, Int?, String?) -> Unit
    ) {
        api.getPemesananRuangan(authorization, tanggal, ruanganId, status, search).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    /** Mengambil rincian informasi dan status pesanan ruangan spesifik */
    fun getDetailPemesananRuangan(authorization: String, id: Int, onResult: (PemesananRuanganDetailResponse?, Int?, String?) -> Unit) {
        api.getDetailPemesananRuangan(authorization, id).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    /** Mengajukan jadwal pemesanan/booking ruangan baru */
    fun tambahPemesananRuangan(authorization: String, request: PemesananRuanganRequest, onResult: (PemesananRuanganDetailResponse?, Int?, String?) -> Unit) {
        api.tambahPemesananRuangan(authorization, request).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    /** Mengubah data pengajuan pesanan ruangan yang telah di-submit */
    fun updatePemesananRuangan(authorization: String, id: Int, request: PemesananRuanganRequest, onResult: (PemesananRuanganDetailResponse?, Int?, String?) -> Unit) {
        api.updatePemesananRuangan(authorization, id, request).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    /** Membatalkan pesanan ruangan yang sudah diterima/dijadwalkan */
    fun batalPemesananRuangan(authorization: String, id: Int, onResult: (PemesananRuanganMutationResponse?, Int?, String?) -> Unit) {
        api.batalPemesananRuangan(authorization, id).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    /** Menghapus history catatan pemesanan ruangan */
    fun hapusPemesananRuangan(authorization: String, id: Int, onResult: (PemesananRuanganMutationResponse?, Int?, String?) -> Unit) {
        api.hapusPemesananRuangan(authorization, id).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

    // =====================================================
// KODE RUMAH - DAFTAR DAN PENCARIAN
// =====================================================

    fun getKodeRumah(
        authorization: String,
        q: String? = null,
        idsubsls: String? = null,
        namaKecamatan: String? = null,
        namaKelurahan: String? = null,
        namaRtRw: String? = null,
        perPage: Int? = 20,
        onResult: (
            KodeRumahListResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.getKodeRumah(
            authorization = authorization,
            q = q,
            idsubsls = idsubsls,
            namaKecamatan = namaKecamatan,
            namaKelurahan = namaKelurahan,
            namaRtRw = namaRtRw,
            perPage = perPage
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - DETAIL
// =====================================================

    fun getDetailKodeRumah(
        authorization: String,
        id: Int,
        onResult: (
            KodeRumahDetailResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.getDetailKodeRumah(
            authorization,
            id
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - TAMBAH
// =====================================================

    fun tambahKodeRumah(
        authorization: String,
        request: KodeRumahRequest,
        onResult: (
            KodeRumahMutationResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.tambahKodeRumah(
            authorization,
            request
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - UBAH DATA
// =====================================================

    fun updateKodeRumah(
        authorization: String,
        id: Int,
        request: KodeRumahRequest,
        onResult: (
            KodeRumahMutationResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.updateKodeRumah(
            authorization,
            id,
            request
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - RETAG KOORDINAT
// =====================================================

    fun retagKodeRumah(
        authorization: String,
        id: Int,
        request: KodeRumahRetagRequest,
        onResult: (
            KodeRumahMutationResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.retagKodeRumah(
            authorization,
            id,
            request
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - HAPUS
// =====================================================

    fun hapusKodeRumah(
        authorization: String,
        id: Int,
        onResult: (
            KodeRumahMutationResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.hapusKodeRumah(
            authorization,
            id
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - DAFTAR RIWAYAT
// =====================================================

    fun getLogKodeRumah(
        authorization: String,
        kodeRumahId: Int? = null,
        userId: Int? = null,
        perPage: Int? = 20,
        onResult: (
            KodeRumahLogListResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.getLogKodeRumah(
            authorization = authorization,
            kodeRumahId = kodeRumahId,
            userId = userId,
            perPage = perPage
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }

// =====================================================
// KODE RUMAH - DETAIL RIWAYAT
// =====================================================

    fun getDetailLogKodeRumah(
        authorization: String,
        id: Int,
        onResult: (
            KodeRumahLogDetailResponse?,
            Int?,
            String?
        ) -> Unit
    ) {
        BpskpRetrofitClient.api.getDetailLogKodeRumah(
            authorization,
            id
        ).enqueueRequest { body, code, errorMsg ->
            onResult(body, code, errorMsg)
        }
    }
}