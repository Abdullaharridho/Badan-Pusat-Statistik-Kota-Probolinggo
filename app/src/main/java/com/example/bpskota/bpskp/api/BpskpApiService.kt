package com.example.bpskota.bpskp.api

import com.example.bpskota.bpskp.model.*
import com.example.bpskota.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface BpskpApiService {

    /** Login menggunakan username dan password. */
    @POST("login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    /** Mengambil data user yang sedang login. */
    @GET("user")
    fun getUser(
        @Header("Authorization") authorization: String
    ): Call<LoginResponse>

    /** Login menggunakan data biometrik. */
    @POST("biometric/login")
    fun biometricLogin(
        @Body request: BiometricLoginRequest
    ): Call<LoginResponse>

    /** Mendaftarkan kredensial biometrik pada akun. */
    @POST("biometric/register")
    fun biometricRegister(
        @Header("Authorization") authorization: String,
        @Body request: BiometricRegisterRequest
    ): Call<BiometricRegisterResponse>

    /** Logout dari akun yang sedang aktif. */
    @POST("logout")
    fun logout(
        @Header("Authorization") authorization: String
    ): Call<LoginResponse>

    /** Menyimpan riwayat aktivitas user. */
    @POST("activity-log")
    fun logActivity(
        @Body request: ActivityLogRequest
    ): Call<ActivityLogResponse>

    /** Mengambil ringkasan statistik aktivitas. */
    @GET("activity-statistics")
    fun getActivityStatistics(): Call<ActivityStatisticsResponse>

    /** Mengambil statistik aktivitas lengkap (Khusus Admin). */
    @GET("activity-statistics/admin")
    fun getAdminActivityStatistics(
        @Header("Authorization") authorization: String
    ): Call<AdminActivityStatisticsResponse>

    // ==========================================
    // PROFILE USER
    // ==========================================

    /** Mengubah profil dasar user (nama, username). */
    @PUT("profile")
    fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileRequest
    ): Call<ProfileResponse>

    /** Mengubah password user. */
    @PUT("profile/password")
    fun updatePassword(
        @Header("Authorization") authorization: String,
        @Body request: PasswordUpdateRequest
    ): Call<ProfileResponse>

    /** Mengambil seluruh daftar data user. */
    @GET("users")
    fun getUsers(
        @Header("Authorization") authorization: String
    ): Call<UserResponse>

    /** Mengambil detail user berdasarkan ID. */
    @GET("users/{id}")
    fun getUserDetail(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<UserDetailResponse>

    /** Membuat akun user baru. */
    @POST("users")
    fun createUser(
        @Header("Authorization") authorization: String,
        @Body request: UserRequest
    ): Call<UserActionResponse>

    /** Mengubah data user (hanya untuk Super Admin). */
    @PUT("users/{id}")
    fun updateUser(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: UserRequest
    ): Call<UserActionResponse>

    /** Menghapus data user (hanya untuk Super Admin). */
    @DELETE("users/{id}")
    fun deleteUser(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<UserActionResponse>


    /** Mengambil daftar surat masuk (mendukung pencarian dan status). */
    @GET("surat-masuk")
    fun getSuratMasuk(
        @Header("Authorization") authorization: String,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Call<SuratMasukListResponse>

    /** Mengambil detail surat masuk. */
    @GET("surat-masuk/{id}")
    fun getSuratMasukDetail(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<SuratMasukResponse>

    /** Menyimpan surat masuk baru beserta dokumen lampiran. */
    @Multipart
    @POST("surat-masuk/simpan")
    fun createSuratMasuk(
        @Header("Authorization") authorization: String,
        @Part("nomor_surat") nomorSurat: RequestBody,
        @Part("tanggal_surat") tanggalSurat: RequestBody,
        @Part("tanggal_diterima") tanggalDiterima: RequestBody,
        @Part("asal_surat") asalSurat: RequestBody,
        @Part("perihal") perihal: RequestBody,
        @Part("isi_ringkas") isiRingkas: RequestBody?,
        @Part("tanggal_acara") tanggalAcara: RequestBody?,
        @Part("waktu_mulai") waktuMulai: RequestBody?,
        @Part("waktu_selesai") waktuSelesai: RequestBody?,
        @Part("lokasi") lokasi: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part fileSurat: MultipartBody.Part?
    ): Call<SuratMasukActionResponse>

    /** Memperbarui data surat masuk yang sudah ada. */
    @Multipart
    @POST("surat-masuk/{id}")
    fun updateSuratMasuk(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Part("nomor_surat") nomorSurat: RequestBody?,
        @Part("tanggal_surat") tanggalSurat: RequestBody?,
        @Part("tanggal_diterima") tanggalDiterima: RequestBody?,
        @Part("asal_surat") asalSurat: RequestBody?,
        @Part("perihal") perihal: RequestBody?,
        @Part("isi_ringkas") isiRingkas: RequestBody?,
        @Part("tanggal_acara") tanggalAcara: RequestBody?,
        @Part("waktu_mulai") waktuMulai: RequestBody?,
        @Part("waktu_selesai") waktuSelesai: RequestBody?,
        @Part("lokasi") lokasi: RequestBody?,
        @Part("status") status: RequestBody?,
        @Part fileSurat: MultipartBody.Part?
    ): Call<SuratMasukActionResponse>

    /** Mengubah status persetujuan/disposisi surat masuk. */
    @PATCH("surat-masuk/{id}/status")
    fun updateSuratMasukStatus(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: SuratMasukStatusRequest
    ): Call<SuratMasukActionResponse>

    /** Menghapus surat masuk beserta data terkait. */
    @DELETE("surat-masuk/{id}")
    fun deleteSuratMasuk(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<SuratMasukActionResponse>


    /** Mengambil daftar tim. */
    @GET("tim")
    fun getTim(
        @Header("Authorization") authorization: String,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Call<TimListResponse>

    /** Mengambil detail tim beserta anggota di dalamnya. */
    @GET("tim/{id}")
    fun getDetailTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<TimDetailResponse>

    /** Membuat tim baru sekaligus menentukan ketua. */
    @POST("tim")
    fun buatTim(
        @Header("Authorization") authorization: String,
        @Body request: TimRequest
    ): Call<TimMutationResponse>

    /** Memperbarui data informasi tim. */
    @PUT("tim/{id}")
    fun updateTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: TimUpdateRequest
    ): Call<TimMutationResponse>

    /** Menghapus tim berdasarkan ID. */
    @DELETE("tim/{id}")
    fun hapusTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<TimMutationResponse>

    /** Menambahkan pegawai ke dalam tim. */
    @POST("tim/{id}/anggota")
    fun tambahAnggotaTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: TambahAnggotaRequest
    ): Call<TimMutationResponse>

    /** Mengeluarkan pegawai dari tim. */
    @DELETE("tim/{id}/anggota/{pegawai_id}")
    fun hapusAnggotaTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Path("pegawai_id") pegawaiId: Int
    ): Call<TimMutationResponse>

    /** Mengambil daftar pegawai yang tersedia untuk dijadikan anggota tim. */
    @GET("tim/pegawai-tersedia")
    fun getPegawaiTersedia(
        @Header("Authorization") authorization: String
    ): Call<PegawaiTersediaResponse>

    /** Mengambil daftar penugasan. */
    @GET("penugasan")
    fun getPenugasan(
        @Header("Authorization") authorization: String,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Call<ApiResponse<List<PenugasanResponse>>>

    /** Membuat penugasan dengan header Authorization. */
    @POST("penugasan")
    fun buatPenugasan(
        @Header("Authorization") authorization: String,
        @Body request: CreatePenugasanRequest
    ): Call<ApiResponse<PenugasanResponse>>
    @GET("penugasan/penerima-bentrok")
    fun getPenerimaBentrok(
        @Header("Authorization") authorization: String,
        @Query("surat_masuk_id") suratMasukId: Long
    ): Call<PenerimaBentrokResponse>
    /** Mengambil detail penugasan berdasarkan ID. */
    @GET("penugasan/{id}")
    fun getDetailPenugasan(
        @Path("id") id: Long
    ): Call<ApiResponse<PenugasanResponse>>

    @GET("penugasan/{id}")
    fun getDetailPenugasan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long
    ): Call<ApiResponse<PenugasanResponse>>

    /** Membuat penugasan tanpa header Authorization (opsional/alternatif). */
    @POST("penugasan")
    fun buatPenugasan(
        @Body request: CreatePenugasanRequest
    ): Call<ApiResponse<PenugasanResponse>>

    /** Merespon penugasan (diterima/ditolak). */
    @PATCH("penugasan/{id}/respon")
    fun responPenugasan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Long,
        @Body request: ResponPenugasanRequest
    ): Call<ApiResponse<PenugasanResponse>>

    /** Mengambil daftar laporan hasil kegiatan. */
    @GET("laporan")
    fun getLaporan(
        @Header("Authorization") authorization: String
    ): Call<LaporanListResponse>

    /** Mengambil detail spesifik dari suatu laporan. */
    @GET("laporan/{id}")
    fun getDetailLaporan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<LaporanDetailResponse>

    /** Mengirimkan laporan baru beserta unggahan lampiran (multipart). */
    @Multipart
    @POST("laporan")
    fun submitLaporan(
        @Header("Authorization") authorization: String,
        @Part("penugasan_id") penugasanId: RequestBody,
        @Part("ringkasan") ringkasan: RequestBody,
        @Part("hasil_kegiatan") hasilKegiatan: RequestBody,
        @Part("catatan") catatan: RequestBody?,
        @Part dokumen: List<MultipartBody.Part>
    ): Call<LaporanActionResponse>

    /** Memperbarui laporan kegiatan (lampiran bersifat opsional di-update). */
    @Multipart
    @POST("laporan/{id}")
    fun updateLaporan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Part("penugasan_id") penugasanId: RequestBody,
        @Part("ringkasan") ringkasan: RequestBody,
        @Part("hasil_kegiatan") hasilKegiatan: RequestBody,
        @Part("catatan") catatan: RequestBody?,
        @Part dokumen: List<MultipartBody.Part>
    ): Call<LaporanActionResponse>

    /** Menghapus laporan kegiatan berdasarkan ID. */
    @DELETE("laporan/{id}")
    fun deleteLaporan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<LaporanActionResponse>



    /** Mengambil daftar kegiatan harian milik pegawai yang login. */
    @GET("kegiatan-harian")
    fun getKegiatanHarian(
        @Header("Authorization") authorization: String,
        @Query("tanggal") tanggal: String? = null
    ): Call<KegiatanHarianListResponse>
    @POST("kegiatan-harian")
    fun tambahKegiatanHarian(
        @Header("Authorization") token: String,
        @Body request: KegiatanHarianRequest
    ): Call<KegiatanHarianMutationResponse>

    @GET("kegiatan-harian/{id}")
    fun getDetailKegiatanHarian(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<KegiatanHarianDetailResponse>

    @PUT("kegiatan-harian/{id}")
    fun updateKegiatanHarian(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: KegiatanHarianRequest
    ): Call<KegiatanHarianMutationResponse>

    @DELETE("kegiatan-harian/{id}")
    fun hapusKegiatanHarian(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Call<KegiatanHarianMutationResponse>

    @GET("rekomendasi")
    fun getRekomendasi(
        @Header("Authorization") authorization: String,
        @Query("status") status: String? = null
    ): Call<RekomendasiListResponse>

    /** Mengajukan rekomendasi pengalihan penugasan. */
    @POST("rekomendasi")
    fun buatRekomendasi(
        @Header("Authorization") authorization: String,
        @Body request: RekomendasiRequest
    ): Call<RekomendasiMutationResponse>

    /** Mengubah rekomendasi yang masih menunggu review. */
    @PUT("rekomendasi/{id}")
    fun updateRekomendasi(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: RekomendasiUpdateRequest
    ): Call<RekomendasiMutationResponse>

    /** Ketua tim menerima atau menolak rekomendasi. */
    @PATCH("rekomendasi/{id}/proses")
    fun prosesRekomendasi(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: RekomendasiProsesRequest
    ): Call<RekomendasiMutationResponse>

    /** Menghapus rekomendasi yang masih menunggu review. */
    @DELETE("rekomendasi/{id}")
    fun hapusRekomendasi(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<RekomendasiMutationResponse>
    @GET("penugasan/{id}/pegawai-pengganti")
    fun getPegawaiPengganti(
        @Header("Authorization") authorization: String,
        @Path("id") idPenugasan: Long
    ): Call<PegawaiTersediaResponse>

    // ==================== RUANGAN ====================

    @GET("ruangan")
    fun getRuangan(
        @Header("Authorization") authorization: String
    ): Call<RuanganResponse>

    @GET("ruangan/{id}")
    fun getDetailRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<RuanganDetailResponse>

    @POST("ruangan")
    fun tambahRuangan(
        @Header("Authorization") authorization: String,
        @Body request: RuanganRequest
    ): Call<RuanganDetailResponse>

    @PUT("ruangan/{id}")
    fun updateRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: RuanganRequest
    ): Call<RuanganDetailResponse>

    @DELETE("ruangan/{id}")
    fun hapusRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<RuanganMutationResponse>

// ==================== PEMESANAN RUANGAN ====================

    @GET("pemesanan-ruangan")
    fun getPemesananRuangan(
        @Header("Authorization") authorization: String,
        @Query("tanggal") tanggal: String? = null,
        @Query("ruangan_id") ruanganId: Int? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null
    ): Call<PemesananRuanganResponse>

    @GET("pemesanan-ruangan/{id}")
    fun getDetailPemesananRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<PemesananRuanganDetailResponse>

    @POST("pemesanan-ruangan")
    fun tambahPemesananRuangan(
        @Header("Authorization") authorization: String,
        @Body request: PemesananRuanganRequest
    ): Call<PemesananRuanganDetailResponse>

    @PUT("pemesanan-ruangan/{id}")
    fun updatePemesananRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: PemesananRuanganRequest
    ): Call<PemesananRuanganDetailResponse>

    @PATCH("pemesanan-ruangan/{id}/batal")
    fun batalPemesananRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<PemesananRuanganMutationResponse>

    @DELETE("pemesanan-ruangan/{id}")
    fun hapusPemesananRuangan(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<PemesananRuanganMutationResponse>

    // ==================== KODE RUMAH ====================

    @GET("kode-rumah")
    fun getKodeRumah(
        @Header("Authorization") authorization: String,
        @Query("q") q: String? = null,
        @Query("idsubsls") idsubsls: String? = null,
        @Query("nama_kecamatan") namaKecamatan: String? = null,
        @Query("nama_kelurahan") namaKelurahan: String? = null,
        @Query("nama_rt_rw") namaRtRw: String? = null,
        @Query("per_page") perPage: Int? = null
    ): Call<KodeRumahListResponse>

    @GET("kode-rumah/{id}")
    fun getDetailKodeRumah(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<KodeRumahDetailResponse>

    @POST("kode-rumah")
    fun tambahKodeRumah(
        @Header("Authorization") authorization: String,
        @Body request: KodeRumahRequest
    ): Call<KodeRumahMutationResponse>

    @PUT("kode-rumah/{id}")
    fun updateKodeRumah(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: KodeRumahRequest
    ): Call<KodeRumahMutationResponse>

    @PATCH("kode-rumah/{id}/retag")
    fun retagKodeRumah(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: KodeRumahRetagRequest
    ): Call<KodeRumahMutationResponse>

    @DELETE("kode-rumah/{id}")
    fun hapusKodeRumah(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<KodeRumahMutationResponse>

// ==================== RIWAYAT KODE RUMAH ====================

    @GET("kode-rumah/log")
    fun getLogKodeRumah(
        @Header("Authorization") authorization: String,
        @Query("kode_rumah_id") kodeRumahId: Int? = null,
        @Query("user_id") userId: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Call<KodeRumahLogListResponse>

    @GET("kode-rumah/log/{id}")
    fun getDetailLogKodeRumah(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<KodeRumahLogDetailResponse>
}