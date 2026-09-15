package com.example.bpskota.bpskp.api

import com.example.bpskota.bpskp.model.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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
}


