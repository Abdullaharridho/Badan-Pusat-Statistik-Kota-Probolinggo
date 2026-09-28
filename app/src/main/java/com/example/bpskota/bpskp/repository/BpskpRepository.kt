package com.example.bpskota.bpskp.repository

import android.util.Log
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Repository untuk mengelola semua pemanggilan API BPS Kota Probolinggo.
 * Menangani parsing response, error handling, dan pemetaan data.
 */
class BpskpRepository {

    companion object {
        private const val TAG = "BpskpRepository"
    }

    private fun String.toTextRequestBody(): RequestBody {
        return this.toRequestBody("text/plain".toMediaType())
    }

    /**
     * Extension function generic untuk menyederhanakan pemanggilan Retrofit enqueue.
     * Mencegah duplikasi kode onResponse dan onFailure di setiap fungsi API.
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
                    onResult(null, code, getErrorMessage(code))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                Log.e(TAG, "API Request Failure: ${t.message}", t)
                onResult(null, null, getConnectionErrorMessage(t))
            }
        })
    }

    /** Helper untuk mendapatkan Bearer Token */
    private fun getAuthorizationHeader(session: BpskpAuthSession): String? {
        val token = session.getToken()
        return if (!token.isNullOrBlank()) "Bearer $token" else null
    }

    // ==========================================
    // AUTHENTICATION & PROFILE
    // ==========================================

    fun login(username: String, password: String, callback: (LoginResponse?, String?) -> Unit) {
        val request = LoginRequest(username, password)
        BpskpRetrofitClient.api.login(request).enqueueRequest { body, code, errorMsg ->
            if (body != null && body.success && !body.token.isNullOrBlank() && body.user != null) {
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

    fun biometricLogin(credentialId: String, callback: (LoginResponse?, String?, Int?) -> Unit) {
        if (credentialId.isBlank()) {
            return callback(null, "Credential biometrik tidak ditemukan.", null)
        }
        val request = BiometricLoginRequest(credential_id = credentialId)
        BpskpRetrofitClient.api.biometricLogin(request).enqueueRequest { body, code, errorMsg ->
            if (body != null && body.success && !body.token.isNullOrBlank() && body.user != null) {
                callback(body, null, code)
            } else {
                val customError = when (code) {
                    401 -> "Credential biometrik tidak valid atau sudah tidak aktif."
                    else -> errorMsg ?: body?.message ?: "Response login biometrik tidak valid."
                }
                callback(null, customError, code)
            }
        }
    }

    fun logout(session: BpskpAuthSession, callback: (LoginResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session)
        if (auth == null) {
            session.clearSession()
            return callback(null, "Token tidak ditemukan.")
        }
        BpskpRetrofitClient.api.logout(auth).enqueueRequest { body, _, errorMsg ->
            session.clearSession()
            callback(body, errorMsg)
        }
    }

    fun biometricRegister(
        session: BpskpAuthSession,
        credentialId: String,
        deviceName: String?,
        callback: (BiometricRegisterResponse?, String?, Int?) -> Unit
    ) {
        if (credentialId.isBlank()) return callback(null, "Credential biometrik tidak valid.", null)
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.", null)

        val request = BiometricRegisterRequest(credential_id = credentialId, device_name = deviceName)
        BpskpRetrofitClient.api.biometricRegister(auth, request).enqueueRequest { body, code, errorMsg ->
            if (body != null && body.success && body.credential != null) {
                callback(body, null, code)
            } else {
                val customError = if (code == 422) "Credential sudah terdaftar atau tidak valid." else (errorMsg ?: body?.message)
                callback(null, customError, code)
            }
        }
    }

    fun getUser(session: BpskpAuthSession, callback: (LoginResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        BpskpRetrofitClient.api.getUser(auth).enqueueRequest { body, _, errorMsg ->
            if (body != null && body.success && body.user != null) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Response data user tidak valid.")
        }
    }

    fun updateProfile(
        session: BpskpAuthSession,
        name: String,
        username: String,
        callback: (ProfileResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = ProfileRequest(name = name, username = username)

        BpskpRetrofitClient.api.updateProfile(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal update profil.")
        }
    }

    fun updatePassword(
        session: BpskpAuthSession,
        currentPassword: String,
        newPassword: String,
        callback: (ProfileResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = PasswordUpdateRequest(currentPassword, newPassword, newPassword)

        BpskpRetrofitClient.api.updatePassword(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal update password.")
        }
    }

    // ==========================================
    // USER MANAGEMENT
    // ==========================================

    fun getUsers(session: BpskpAuthSession, callback: (UserResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        BpskpRetrofitClient.api.getUsers(auth).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Response tidak valid.")
        }
    }

    fun getUserDetail(session: BpskpAuthSession, id: Int, callback: (UserDetailResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        BpskpRetrofitClient.api.getUserDetail(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal mengambil detail user.")
        }
    }

    fun createUser(
        session: BpskpAuthSession, name: String, username: String, password: String, role: String,
        callback: (UserActionResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = UserRequest(name, username, password, role)

        BpskpRetrofitClient.api.createUser(auth, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal membuat user.")
        }
    }

    fun updateUser(
        session: BpskpAuthSession, id: Int, name: String, username: String, password: String?, role: String,
        callback: (UserActionResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        val request = UserRequest(name, username, password, role)

        BpskpRetrofitClient.api.updateUser(auth, id, request).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal update user.")
        }
    }

    fun deleteUser(session: BpskpAuthSession, id: Int, callback: (UserActionResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        BpskpRetrofitClient.api.deleteUser(auth, id).enqueueRequest { body, _, errorMsg ->
            if (body?.success == true) callback(body, null)
            else callback(null, errorMsg ?: body?.message ?: "Gagal menghapus user.")
        }
    }

    // ==========================================
    // STATISTICS
    // ==========================================

    fun getActivityStatistics(callback: (ActivityStatisticsResponse?, String?) -> Unit) {
        BpskpRetrofitClient.api.getActivityStatistics().enqueueRequest { body, _, errorMsg ->
            if (body != null && body.success && body.summary != null) callback(body, null)
            else callback(null, errorMsg ?: "Data statistik tidak valid.")
        }
    }

    fun getAdminActivityStatistics(session: BpskpAuthSession, callback: (AdminActivityStatisticsResponse?, String?) -> Unit) {
        val auth = getAuthorizationHeader(session) ?: return callback(null, "Sesi login tidak ditemukan.")
        BpskpRetrofitClient.api.getAdminActivityStatistics(auth).enqueueRequest { body, _, errorMsg ->
            if (body != null && body.success && body.summary != null) callback(body, null)
            else callback(null, errorMsg ?: "Gagal mengambil statistik admin.")
        }
    }

    // ==========================================
    // SURAT MASUK
    // ==========================================

    fun getSuratMasuk(
        authorization: String, search: String? = null, status: String? = null,
        callback: (List<SuratMasuk>?, String?) -> Unit
    ) {
        BpskpRetrofitClient.api.getSuratMasuk(authorization, search, status).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal mengambil data surat.")
        }
    }

    fun getSuratMasukDetail(authorization: String, id: Int, callback: (SuratMasuk?, String?) -> Unit) {
        BpskpRetrofitClient.api.getSuratMasukDetail(authorization, id).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal mengambil detail surat.")
        }
    }

    fun deleteSuratMasuk(authorization: String, id: Int, callback: (Boolean, String?) -> Unit) {
        BpskpRetrofitClient.api.deleteSuratMasuk(authorization, id).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(true, null) else callback(false, errorMsg ?: "Gagal menghapus surat.")
        }
    }

    fun updateSuratMasukStatus(authorization: String, id: Int, status: String, callback: (Boolean, String?) -> Unit) {
        val request = SuratMasukStatusRequest(status)
        BpskpRetrofitClient.api.updateSuratMasukStatus(authorization, id, request).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(true, null) else callback(false, errorMsg ?: "Gagal mengubah status surat.")
        }
    }

    fun createSuratMasuk(
        authorization: String, nomorSurat: String, tanggalSurat: String, tanggalDiterima: String,
        asalSurat: String, perihal: String, isiRingkas: String?, tanggalAcara: String?,
        waktuMulai: String?, waktuSelesai: String?, lokasi: String?, fileSurat: MultipartBody.Part?,
        callback: (SuratMasuk?, String?) -> Unit
    ) {
        BpskpRetrofitClient.api.createSuratMasuk(
            authorization = authorization,
            nomorSurat = nomorSurat.toTextRequestBody(),
            tanggalSurat = tanggalSurat.toTextRequestBody(),
            tanggalDiterima = tanggalDiterima.toTextRequestBody(),
            asalSurat = asalSurat.toTextRequestBody(),
            perihal = perihal.toTextRequestBody(),
            isiRingkas = isiRingkas?.toTextRequestBody(),
            tanggalAcara = tanggalAcara?.toTextRequestBody(),
            waktuMulai = waktuMulai?.toTextRequestBody(),
            waktuSelesai = waktuSelesai?.toTextRequestBody(),
            lokasi = lokasi?.toTextRequestBody(),
            status = null,
            fileSurat = fileSurat
        ).enqueueRequest { body, _, errorMsg ->
            if (body != null) callback(body.data, null) else callback(null, errorMsg ?: "Gagal menambahkan surat.")
        }
    }

    // ==========================================
    // ERROR HANDLERS
    // ==========================================

    private fun getErrorMessage(code: Int?): String {
        return when (code) {
            400 -> "Permintaan tidak valid."
            401 -> "Sesi login tidak valid atau Anda tidak berhak mengakses (401)."
            403 -> "Anda tidak memiliki akses (403)."
            404 -> "Data tidak ditemukan (404)."
            422 -> "Data yang dikirim tidak valid (422)."
            500 -> "Terjadi kesalahan pada server (500)."
            null -> "Tidak dapat terhubung."
            else -> "Permintaan gagal. Kode: $code"
        }
    }

    private fun getConnectionErrorMessage(throwable: Throwable): String {
        return when {
            throwable.message?.contains("Unable to resolve host", true) == true -> "Tidak dapat terhubung ke jaringan."
            throwable.message?.contains("CLEARTEXT", true) == true -> "Koneksi HTTP diblokir oleh Android."
            else -> "Terjadi kesalahan koneksi jaringan."
        }
    }
    fun getTim(
        session: BpskpAuthSession,
        search: String? = null,
        status: String? = null,
        callback: (List<Tim>?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(null, "Sesi login tidak ditemukan.")

        BpskpRetrofitClient.api.getTim(
            authorization = auth,
            search = search,
            status = status
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(body.data, null)
            } else {
                callback(
                    null,
                    errorMsg ?: body?.message ?: "Gagal mengambil daftar tim."
                )
            }
        }
    }

    /**
     * Mengambil detail tim beserta anggotanya.
     *
     * GET /api/tim/{id}
     */
    fun getDetailTim(
        session: BpskpAuthSession,
        id: Int,
        callback: (Tim?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(null, "Sesi login tidak ditemukan.")

        BpskpRetrofitClient.api.getDetailTim(
            authorization = auth,
            id = id
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true && body.data != null) {
                callback(body.data, null)
            } else {
                callback(
                    null,
                    errorMsg ?: body?.message ?: "Gagal mengambil detail tim."
                )
            }
        }
    }

    /**
     * Membuat tim sekaligus menentukan ketua.
     *
     * POST /api/tim
     */
    fun buatTim(
        session: BpskpAuthSession,
        namaTim: String,
        ketuaId: Int,
        keterangan: String? = null,
        status: String = "aktif",
        callback: (TimMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(null, "Sesi login tidak ditemukan.")

        if (namaTim.isBlank()) {
            return callback(null, "Nama tim wajib diisi.")
        }

        if (ketuaId <= 0) {
            return callback(null, "Ketua tim belum dipilih.")
        }

        val request = TimRequest(
            namaTim = namaTim.trim(),
            ketuaId = ketuaId,
            keterangan = keterangan?.trim()?.takeIf { it.isNotEmpty() },
            status = status
        )

        BpskpRetrofitClient.api.buatTim(
            authorization = auth,
            request = request
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(body, null)
            } else {
                callback(
                    null,
                    errorMsg ?: body?.message ?: "Gagal membuat tim."
                )
            }
        }
    }

    /**
     * Memperbarui data tim.
     *
     * PUT /api/tim/{id}
     *
     * Parameter nullable dapat dibiarkan null jika
     * field tersebut tidak ingin diubah.
     */
    fun updateTim(
        session: BpskpAuthSession,
        id: Int,
        namaTim: String? = null,
        ketuaId: Int? = null,
        keterangan: String? = null,
        status: String? = null,
        callback: (TimMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(null, "Sesi login tidak ditemukan.")

        if (id <= 0) {
            return callback(null, "ID tim tidak valid.")
        }

        if (namaTim != null && namaTim.isBlank()) {
            return callback(null, "Nama tim tidak boleh kosong.")
        }

        if (ketuaId != null && ketuaId <= 0) {
            return callback(null, "ID ketua tim tidak valid.")
        }

        if (status != null && status !in listOf("aktif", "nonaktif")) {
            return callback(null, "Status tim tidak valid.")
        }

        val request = TimUpdateRequest(
            namaTim = namaTim?.trim(),
            ketuaId = ketuaId,
            keterangan = keterangan,
            status = status
        )

        BpskpRetrofitClient.api.updateTim(
            authorization = auth,
            id = id,
            request = request
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(body, null)
            } else {
                callback(
                    null,
                    errorMsg ?: body?.message ?: "Gagal memperbarui tim."
                )
            }
        }
    }

    /**
     * Menghapus tim.
     *
     * DELETE /api/tim/{id}
     */
    fun hapusTim(
        session: BpskpAuthSession,
        id: Int,
        callback: (Boolean, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(false, "Sesi login tidak ditemukan.")

        if (id <= 0) {
            return callback(false, "ID tim tidak valid.")
        }

        BpskpRetrofitClient.api.hapusTim(
            authorization = auth,
            id = id
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(true, null)
            } else {
                callback(
                    false,
                    errorMsg ?: body?.message ?: "Gagal menghapus tim."
                )
            }
        }
    }

    /**
     * Menambahkan anggota ke tim.
     *
     * POST /api/tim/{id}/anggota
     */
    fun tambahAnggotaTim(
        session: BpskpAuthSession,
        timId: Int,
        pegawaiId: Int,
        callback: (TimMutationResponse?, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(null, "Sesi login tidak ditemukan.")

        if (timId <= 0) {
            return callback(null, "ID tim tidak valid.")
        }

        if (pegawaiId <= 0) {
            return callback(null, "Pegawai belum dipilih.")
        }

        val request = TambahAnggotaRequest(
            pegawaiId = pegawaiId
        )

        BpskpRetrofitClient.api.tambahAnggotaTim(
            authorization = auth,
            id = timId,
            request = request
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(body, null)
            } else {
                callback(
                    null,
                    errorMsg ?: body?.message ?: "Gagal menambahkan anggota."
                )
            }
        }
    }

    /**
     * Menghapus anggota dari tim.
     *
     * DELETE /api/tim/{id}/anggota/{pegawai_id}
     */
    fun hapusAnggotaTim(
        session: BpskpAuthSession,
        timId: Int,
        pegawaiId: Int,
        callback: (Boolean, String?) -> Unit
    ) {
        val auth = getAuthorizationHeader(session)
            ?: return callback(false, "Sesi login tidak ditemukan.")

        if (timId <= 0 || pegawaiId <= 0) {
            return callback(false, "ID tim atau pegawai tidak valid.")
        }

        BpskpRetrofitClient.api.hapusAnggotaTim(
            authorization = auth,
            id = timId,
            pegawaiId = pegawaiId
        ).enqueueRequest { body, _, errorMsg ->

            if (body?.success == true) {
                callback(true, null)
            } else {
                callback(
                    false,
                    errorMsg ?: body?.message ?: "Gagal menghapus anggota."
                )
            }
        }
    }
}