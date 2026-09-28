package com.example.bpskota.bpskp.api

import com.example.bpskota.bpskp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface BpskpApiService {

    // =========================
    // LOGIN
    // =========================

    // Login menggunakan username dan password
    @POST("login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    // Mengambil data user yang sedang login
    @GET("user")
    fun getUser(
        @Header("Authorization") authorization: String
    ): Call<LoginResponse>

    @POST("biometric/login")
    fun biometricLogin(
        @Body request: BiometricLoginRequest
    ): Call<LoginResponse>
    @POST("biometric/register")
    fun biometricRegister(
        @Header("Authorization") authorization: String,
        @Body request: BiometricRegisterRequest
    ): Call<BiometricRegisterResponse>

    // Logout dari akun yang sedang login
    @POST("logout")
    fun logout(
        @Header("Authorization") authorization: String
    ): Call<LoginResponse>
    @POST("activity-log")
    fun logActivity(
        @Body request: ActivityLogRequest
    ): Call<ActivityLogResponse>
    @GET("activity-statistics")
    fun getActivityStatistics(): Call<ActivityStatisticsResponse>
    @GET("activity-statistics/admin")
    fun getAdminActivityStatistics(
        @Header("Authorization") authorization: String
    ): Call<AdminActivityStatisticsResponse>

    // =========================
    // PROFILE USER
    // =========================

    // Mengubah nama dan username user yang sedang login
    @PUT("profile")
    fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileRequest
    ): Call<ProfileResponse>

    // Mengubah password user yang sedang login
    @PUT("profile/password")
    fun updatePassword(
        @Header("Authorization") authorization: String,
        @Body request: PasswordUpdateRequest
    ): Call<ProfileResponse>


    // =========================
    // MANAJEMEN USER
    // =========================

    // Mengambil seluruh data user
    @GET("users")
    fun getUsers(
        @Header("Authorization") authorization: String
    ): Call<UserResponse>

    // Mengambil detail user berdasarkan ID
    @GET("users/{id}")
    fun getUserDetail(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<UserDetailResponse>

    // Membuat user baru
    @POST("users")
    fun createUser(
        @Header("Authorization") authorization: String,
        @Body request: UserRequest
    ): Call<UserActionResponse>

    // Mengubah data user melalui Super Admin
    @PUT("users/{id}")
    fun updateUser(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: UserRequest
    ): Call<UserActionResponse>

    // Menghapus user melalui Super Admin
    @DELETE("users/{id}")
    fun deleteUser(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<UserActionResponse>


    @GET("surat-masuk")
    fun getSuratMasuk(
        @Header("Authorization") authorization: String,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Call<SuratMasukListResponse>
    @GET("surat-masuk/{id}")
    fun getSuratMasukDetail(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<SuratMasukResponse>
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
    @PATCH("surat-masuk/{id}/status")
    fun updateSuratMasukStatus(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: SuratMasukStatusRequest
    ): Call<SuratMasukActionResponse>
    @DELETE("surat-masuk/{id}")
    fun deleteSuratMasuk(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<SuratMasukActionResponse>

    // =========================
    // MANAJEMEN TIM
    // =========================

    /**
     * Mengambil daftar tim.
     *
     * GET /tim
     */
    @GET("tim")
    fun getTim(
        @Header("Authorization") authorization: String,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null
    ): Call<TimListResponse>

    /**
     * Mengambil detail tim beserta daftar anggota.
     *
     * GET /tim/{id}
     */
    @GET("tim/{id}")
    fun getDetailTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<TimDetailResponse>

    /**
     * Membuat tim sekaligus menentukan ketua.
     *
     * POST /tim
     */
    @POST("tim")
    fun buatTim(
        @Header("Authorization") authorization: String,
        @Body request: TimRequest
    ): Call<TimMutationResponse>

    /**
     * Memperbarui data tim.
     *
     * PUT /tim/{id}
     */
    @PUT("tim/{id}")
    fun updateTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: TimUpdateRequest
    ): Call<TimMutationResponse>

    /**
     * Menghapus tim.
     *
     * DELETE /tim/{id}
     */
    @DELETE("tim/{id}")
    fun hapusTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int
    ): Call<TimMutationResponse>

    /**
     * Menambahkan anggota ke tim.
     *
     * POST /tim/{id}/anggota
     */
    @POST("tim/{id}/anggota")
    fun tambahAnggotaTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Body request: TambahAnggotaRequest
    ): Call<TimMutationResponse>

    /**
     * Menghapus anggota dari tim.
     *
     * DELETE /tim/{id}/anggota/{pegawai_id}
     */
    @DELETE("tim/{id}/anggota/{pegawai_id}")
    fun hapusAnggotaTim(
        @Header("Authorization") authorization: String,
        @Path("id") id: Int,
        @Path("pegawai_id") pegawaiId: Int
    ): Call<TimMutationResponse>
}

