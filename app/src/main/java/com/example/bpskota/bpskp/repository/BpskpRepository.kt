package com.example.bpskota.bpskp.repository

import android.util.Log
import com.example.bpskota.bpskp.api.BpskpRetrofitClient
import com.example.bpskota.bpskp.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BpskpRepository {

    companion object {
        private const val TAG = "BpskpRepository"
    }

    fun login(
        username: String,
        password: String,
        callback: (LoginResponse?, String?) -> Unit
    ) {
        val request = LoginRequest(
            username = username,
            password = password
        )

        Log.d(TAG, "LOGIN request username=$username")

        BpskpRetrofitClient.api
            .login(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    Log.d(TAG, "LOGIN HTTP CODE = ${response.code()}")
                    Log.d(TAG, "LOGIN HTTP MESSAGE = ${response.message()}")

                    if (response.isSuccessful) {
                        val body = response.body()

                        Log.d(TAG, "LOGIN success = ${body?.success}")
                        Log.d(
                            TAG,
                            "LOGIN token exists = ${!body?.token.isNullOrBlank()}"
                        )
                        Log.d(
                            TAG,
                            "LOGIN user exists = ${body?.user != null}"
                        )

                        if (
                            body != null &&
                            body.success &&
                            !body.token.isNullOrBlank() &&
                            body.user != null
                        ) {
                            Log.d(TAG, "LOGIN berhasil")

                            callback(
                                body,
                                null
                            )
                        } else {
                            Log.e(
                                TAG,
                                "LOGIN response tidak valid"
                            )

                            callback(
                                null,
                                body?.message
                                    ?: "Response login tidak valid."
                            )
                        }
                    } else {
                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (_: Exception) {
                            null
                        }

                        Log.e(TAG, "LOGIN gagal")
                        Log.e(
                            TAG,
                            "LOGIN HTTP CODE = ${response.code()}"
                        )
                        Log.e(
                            TAG,
                            "LOGIN ERROR BODY = $errorBody"
                        )

                        val message = when (response.code()) {
                            401 ->
                                "Username atau password salah."

                            422 ->
                                "Data login tidak valid."

                            500 ->
                                "Terjadi kesalahan pada server."

                            else ->
                                "Login gagal. Kode: ${response.code()}"
                        }

                        callback(
                            null,
                            message
                        )
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "LOGIN onFailure"
                    )
                    Log.e(
                        TAG,
                        "LOGIN exception = ${t.javaClass.simpleName}"
                    )
                    Log.e(
                        TAG,
                        "LOGIN message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun biometricLogin(
        credentialId: String,
        callback: (
            LoginResponse?,
            String?,
            Int?
        ) -> Unit
    ) {
        if (credentialId.isBlank()) {
            Log.e(
                TAG,
                "BIOMETRIC LOGIN gagal: credential_id kosong"
            )

            callback(
                null,
                "Credential biometrik tidak ditemukan.",
                null
            )

            return
        }

        val request =
            BiometricLoginRequest(
                credential_id = credentialId
            )

        Log.d(
            TAG,
            "BIOMETRIC LOGIN request dimulai"
        )

        Log.d(
            TAG,
            "BIOMETRIC LOGIN credential tersedia=true"
        )

        BpskpRetrofitClient.api
            .biometricLogin(request)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    val httpCode =
                        response.code()

                    Log.d(
                        TAG,
                        "BIOMETRIC LOGIN HTTP CODE = $httpCode"
                    )

                    Log.d(
                        TAG,
                        "BIOMETRIC LOGIN HTTP MESSAGE = ${response.message()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "BIOMETRIC LOGIN success = ${body?.success}"
                        )

                        Log.d(
                            TAG,
                            "BIOMETRIC LOGIN token exists = ${!body?.token.isNullOrBlank()}"
                        )

                        Log.d(
                            TAG,
                            "BIOMETRIC LOGIN user exists = ${body?.user != null}"
                        )

                        if (
                            body != null &&
                            body.success &&
                            !body.token.isNullOrBlank() &&
                            body.user != null
                        ) {
                            Log.d(
                                TAG,
                                "BIOMETRIC LOGIN berhasil"
                            )

                            callback(
                                body,
                                null,
                                httpCode
                            )
                        } else {
                            Log.e(
                                TAG,
                                "BIOMETRIC LOGIN response tidak valid"
                            )

                            callback(
                                null,
                                body?.message
                                    ?: "Response login biometrik tidak valid.",
                                httpCode
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "BIOMETRIC LOGIN gagal"
                        )

                        Log.e(
                            TAG,
                            "BIOMETRIC LOGIN HTTP CODE = $httpCode"
                        )

                        Log.e(
                            TAG,
                            "BIOMETRIC LOGIN ERROR BODY = $errorBody"
                        )

                        val message =
                            when (httpCode) {

                                401 ->
                                    "Credential biometrik tidak valid atau sudah tidak aktif."

                                422 ->
                                    "Credential biometrik tidak valid."

                                500 ->
                                    "Terjadi kesalahan pada server."

                                else ->
                                    "Login biometrik gagal. Kode: $httpCode"
                            }

                        callback(
                            null,
                            message,
                            httpCode
                        )
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "BIOMETRIC LOGIN onFailure"
                    )

                    Log.e(
                        TAG,
                        "BIOMETRIC LOGIN exception = ${t.javaClass.simpleName}"
                    )

                    Log.e(
                        TAG,
                        "BIOMETRIC LOGIN message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t),
                        null
                    )
                }
            })
    }

    fun logout(
        session: BpskpAuthSession,
        callback: (LoginResponse?, String?) -> Unit
    ) {
        val token =
            session.getToken()

        if (token.isNullOrBlank()) {
            Log.e(
                TAG,
                "LOGOUT gagal: token tidak ditemukan"
            )

            session.clearSession()

            callback(
                null,
                "Token tidak ditemukan."
            )

            return
        }

        Log.d(
            TAG,
            "LOGOUT request"
        )

        BpskpRetrofitClient.api
            .logout(
                "Bearer $token"
            )
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    Log.d(
                        TAG,
                        "LOGOUT HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        Log.d(
                            TAG,
                            "LOGOUT berhasil"
                        )

                        session.clearSession()

                        callback(
                            response.body(),
                            null
                        )
                    } else {

                        Log.e(
                            TAG,
                            "LOGOUT gagal HTTP ${response.code()}"
                        )

                        session.clearSession()

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "LOGOUT onFailure"
                    )

                    Log.e(
                        TAG,
                        "LOGOUT message = ${t.message}",
                        t
                    )

                    session.clearSession()

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    private fun getAuthorizationHeader(
        session: BpskpAuthSession
    ): String? {

        val token =
            session.getToken()

        return if (!token.isNullOrBlank()) {
            "Bearer $token"
        } else {
            null
        }
    }

    fun getUser(
        session: BpskpAuthSession,
        callback: (LoginResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            Log.e(
                TAG,
                "GET /user gagal: authorization tidak ditemukan"
            )

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        Log.d(
            TAG,
            "GET /user request dimulai"
        )

        Log.d(
            TAG,
            "GET /user authorization tersedia=true"
        )

        BpskpRetrofitClient.api
            .getUser(authorization)
            .enqueue(object : Callback<LoginResponse> {

                override fun onResponse(
                    call: Call<LoginResponse>,
                    response: Response<LoginResponse>
                ) {
                    Log.d(
                        TAG,
                        "GET /user HTTP CODE = ${response.code()}"
                    )

                    Log.d(
                        TAG,
                        "GET /user HTTP MESSAGE = ${response.message()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "GET /user success = ${body?.success}"
                        )

                        Log.d(
                            TAG,
                            "GET /user user exists = ${body?.user != null}"
                        )

                        if (
                            body != null &&
                            body.success &&
                            body.user != null
                        ) {
                            Log.d(
                                TAG,
                                "GET /user berhasil"
                            )

                            callback(
                                body,
                                null
                            )
                        } else {

                            Log.e(
                                TAG,
                                "GET /user response tidak valid"
                            )

                            callback(
                                null,
                                body?.message
                                    ?: "Response data user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "GET /user GAGAL"
                        )

                        Log.e(
                            TAG,
                            "GET /user HTTP CODE = ${response.code()}"
                        )

                        Log.e(
                            TAG,
                            "GET /user HTTP MESSAGE = ${response.message()}"
                        )

                        Log.e(
                            TAG,
                            "GET /user ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<LoginResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "GET /user onFailure"
                    )

                    Log.e(
                        TAG,
                        "GET /user exception = ${t.javaClass.simpleName}"
                    )

                    Log.e(
                        TAG,
                        "GET /user message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun updateProfile(
        session: BpskpAuthSession,
        name: String,
        username: String,
        callback: (ProfileResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        val request =
            ProfileRequest(
                name = name,
                username = username
            )

        Log.d(
            TAG,
            "UPDATE PROFILE request username=$username"
        )

        BpskpRetrofitClient.api
            .updateProfile(
                authorization = authorization,
                request = request
            )
            .enqueue(object : Callback<ProfileResponse> {

                override fun onResponse(
                    call: Call<ProfileResponse>,
                    response: Response<ProfileResponse>
                ) {
                    Log.d(
                        TAG,
                        "UPDATE PROFILE HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "UPDATE PROFILE success = ${body?.success}"
                        )

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response update profil tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "UPDATE PROFILE gagal"
                        )

                        Log.e(
                            TAG,
                            "UPDATE PROFILE ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<ProfileResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "UPDATE PROFILE onFailure"
                    )

                    Log.e(
                        TAG,
                        "UPDATE PROFILE message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun updatePassword(
        session: BpskpAuthSession,
        currentPassword: String,
        newPassword: String,
        callback: (ProfileResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        val request =
            PasswordUpdateRequest(
                current_password = currentPassword,
                password = newPassword,
                password_confirmation = newPassword
            )

        Log.d(
            TAG,
            "UPDATE PASSWORD request"
        )

        BpskpRetrofitClient.api
            .updatePassword(
                authorization = authorization,
                request = request
            )
            .enqueue(object : Callback<ProfileResponse> {

                override fun onResponse(
                    call: Call<ProfileResponse>,
                    response: Response<ProfileResponse>
                ) {
                    Log.d(
                        TAG,
                        "UPDATE PASSWORD HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "UPDATE PASSWORD success = ${body?.success}"
                        )

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response update password tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "UPDATE PASSWORD gagal"
                        )

                        Log.e(
                            TAG,
                            "UPDATE PASSWORD ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<ProfileResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "UPDATE PASSWORD onFailure"
                    )

                    Log.e(
                        TAG,
                        "UPDATE PASSWORD message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun getUsers(
        session: BpskpAuthSession,
        callback: (UserResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        Log.d(
            TAG,
            "GET /users request"
        )

        BpskpRetrofitClient.api
            .getUsers(authorization)
            .enqueue(object : Callback<UserResponse> {

                override fun onResponse(
                    call: Call<UserResponse>,
                    response: Response<UserResponse>
                ) {
                    Log.d(
                        TAG,
                        "GET /users HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "GET /users success = ${body?.success}"
                        )

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response data user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "GET /users gagal"
                        )

                        Log.e(
                            TAG,
                            "GET /users ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<UserResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "GET /users onFailure"
                    )

                    Log.e(
                        TAG,
                        "GET /users message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun getUserDetail(
        session: BpskpAuthSession,
        id: Int,
        callback: (UserDetailResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        Log.d(
            TAG,
            "GET /users/$id request"
        )

        BpskpRetrofitClient.api
            .getUserDetail(
                authorization = authorization,
                id = id
            )
            .enqueue(object : Callback<UserDetailResponse> {

                override fun onResponse(
                    call: Call<UserDetailResponse>,
                    response: Response<UserDetailResponse>
                ) {
                    Log.d(
                        TAG,
                        "GET /users/$id HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response detail user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "GET /users/$id gagal"
                        )

                        Log.e(
                            TAG,
                            "GET /users/$id ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<UserDetailResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "GET /users/$id onFailure"
                    )

                    Log.e(
                        TAG,
                        "GET /users/$id message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun createUser(
        session: BpskpAuthSession,
        name: String,
        username: String,
        password: String,
        role: String,
        callback: (UserActionResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        val request =
            UserRequest(
                name = name,
                username = username,
                password = password,
                role = role
            )

        Log.d(
            TAG,
            "CREATE USER username=$username role=$role"
        )

        BpskpRetrofitClient.api
            .createUser(
                authorization = authorization,
                request = request
            )
            .enqueue(object : Callback<UserActionResponse> {

                override fun onResponse(
                    call: Call<UserActionResponse>,
                    response: Response<UserActionResponse>
                ) {
                    Log.d(
                        TAG,
                        "CREATE USER HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response pembuatan user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "CREATE USER gagal"
                        )

                        Log.e(
                            TAG,
                            "CREATE USER ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<UserActionResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "CREATE USER onFailure"
                    )

                    Log.e(
                        TAG,
                        "CREATE USER message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun updateUser(
        session: BpskpAuthSession,
        id: Int,
        name: String,
        username: String,
        password: String?,
        role: String,
        callback: (UserActionResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        val request =
            UserRequest(
                name = name,
                username = username,
                password = password,
                role = role
            )

        Log.d(
            TAG,
            "UPDATE USER id=$id username=$username role=$role"
        )

        BpskpRetrofitClient.api
            .updateUser(
                authorization = authorization,
                id = id,
                request = request
            )
            .enqueue(object : Callback<UserActionResponse> {

                override fun onResponse(
                    call: Call<UserActionResponse>,
                    response: Response<UserActionResponse>
                ) {
                    Log.d(
                        TAG,
                        "UPDATE USER HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response update user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "UPDATE USER gagal"
                        )

                        Log.e(
                            TAG,
                            "UPDATE USER ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<UserActionResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "UPDATE USER onFailure"
                    )

                    Log.e(
                        TAG,
                        "UPDATE USER message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    fun deleteUser(
        session: BpskpAuthSession,
        id: Int,
        callback: (UserActionResponse?, String?) -> Unit
    ) {
        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            callback(
                null,
                "Sesi login tidak ditemukan."
            )

            return
        }

        Log.d(
            TAG,
            "DELETE USER id=$id"
        )

        BpskpRetrofitClient.api
            .deleteUser(
                authorization = authorization,
                id = id
            )
            .enqueue(object : Callback<UserActionResponse> {

                override fun onResponse(
                    call: Call<UserActionResponse>,
                    response: Response<UserActionResponse>
                ) {
                    Log.d(
                        TAG,
                        "DELETE USER HTTP CODE = ${response.code()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        if (
                            body != null &&
                            body.success == true
                        ) {
                            callback(
                                body,
                                null
                            )
                        } else {
                            callback(
                                null,
                                body?.message
                                    ?: "Response hapus user tidak valid."
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "DELETE USER gagal"
                        )

                        Log.e(
                            TAG,
                            "DELETE USER ERROR BODY = $errorBody"
                        )

                        callback(
                            null,
                            getErrorMessage(
                                response.code()
                            )
                        )
                    }
                }

                override fun onFailure(
                    call: Call<UserActionResponse>,
                    t: Throwable
                ) {
                    Log.e(
                        TAG,
                        "DELETE USER onFailure"
                    )

                    Log.e(
                        TAG,
                        "DELETE USER message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t)
                    )
                }
            })
    }

    private fun getErrorMessage(
        code: Int
    ): String {
        return when (code) {

            401 ->
                "Sesi login tidak valid atau sudah berakhir."

            403 ->
                "Anda tidak memiliki akses sebagai Super Admin."

            404 ->
                "Data user tidak ditemukan."

            422 ->
                "Data yang dikirim tidak valid."

            500 ->
                "Terjadi kesalahan pada server."

            else ->
                "Permintaan gagal. Kode: $code"
        }
    }

    private fun getConnectionErrorMessage(
        throwable: Throwable
    ): String {
        return when {

            throwable.message?.contains(
                "Unable to resolve host",
                ignoreCase = true
            ) == true -> {
                "Tidak dapat terhubung ke server."
            }

            throwable.message?.contains(
                "CLEARTEXT",
                ignoreCase = true
            ) == true -> {
                "Koneksi HTTP diblokir Android."
            }

            else -> {
                "Terjadi kesalahan koneksi."
            }
        }
    }
    fun biometricRegister(
        session: BpskpAuthSession,
        credentialId: String,
        deviceName: String?,
        callback: (
            BiometricRegisterResponse?,
            String?,
            Int?
        ) -> Unit
    ) {
        if (credentialId.isBlank()) {

            Log.e(
                TAG,
                "BIOMETRIC REGISTER gagal: credential_id kosong"
            )

            callback(
                null,
                "Credential biometrik tidak valid.",
                null
            )

            return
        }

        val authorization =
            getAuthorizationHeader(session)

        if (authorization == null) {

            Log.e(
                TAG,
                "BIOMETRIC REGISTER gagal: authorization tidak ditemukan"
            )

            callback(
                null,
                "Sesi login tidak ditemukan.",
                null
            )

            return
        }

        val request =
            BiometricRegisterRequest(
                credential_id = credentialId,
                device_name = deviceName
            )

        Log.d(
            TAG,
            "BIOMETRIC REGISTER request dimulai"
        )

        Log.d(
            TAG,
            "BIOMETRIC REGISTER credential tersedia=true"
        )

        Log.d(
            TAG,
            "BIOMETRIC REGISTER device_name=$deviceName"
        )

        BpskpRetrofitClient.api
            .biometricRegister(
                authorization = authorization,
                request = request
            )
            .enqueue(object : Callback<BiometricRegisterResponse> {

                override fun onResponse(
                    call: Call<BiometricRegisterResponse>,
                    response: Response<BiometricRegisterResponse>
                ) {
                    val httpCode =
                        response.code()

                    Log.d(
                        TAG,
                        "BIOMETRIC REGISTER HTTP CODE = $httpCode"
                    )

                    Log.d(
                        TAG,
                        "BIOMETRIC REGISTER HTTP MESSAGE = ${response.message()}"
                    )

                    if (response.isSuccessful) {

                        val body =
                            response.body()

                        Log.d(
                            TAG,
                            "BIOMETRIC REGISTER success = ${body?.success}"
                        )

                        Log.d(
                            TAG,
                            "BIOMETRIC REGISTER credential exists = ${body?.credential != null}"
                        )

                        if (
                            body != null &&
                            body.success &&
                            body.credential != null
                        ) {

                            Log.d(
                                TAG,
                                "BIOMETRIC REGISTER berhasil"
                            )

                            callback(
                                body,
                                null,
                                httpCode
                            )

                        } else {

                            Log.e(
                                TAG,
                                "BIOMETRIC REGISTER response tidak valid"
                            )

                            callback(
                                null,
                                body?.message
                                    ?: "Response registrasi biometrik tidak valid.",
                                httpCode
                            )
                        }

                    } else {

                        val errorBody =
                            try {
                                response
                                    .errorBody()
                                    ?.string()
                            } catch (_: Exception) {
                                null
                            }

                        Log.e(
                            TAG,
                            "BIOMETRIC REGISTER gagal"
                        )

                        Log.e(
                            TAG,
                            "BIOMETRIC REGISTER HTTP CODE = $httpCode"
                        )

                        Log.e(
                            TAG,
                            "BIOMETRIC REGISTER ERROR BODY = $errorBody"
                        )

                        val message =
                            when (httpCode) {

                                401 ->
                                    "Sesi login tidak valid atau sudah berakhir."

                                422 ->
                                    "Credential biometrik sudah terdaftar atau data tidak valid."

                                500 ->
                                    "Terjadi kesalahan pada server."

                                else ->
                                    "Registrasi biometrik gagal. Kode: $httpCode"
                            }

                        callback(
                            null,
                            message,
                            httpCode
                        )
                    }
                }

                override fun onFailure(
                    call: Call<BiometricRegisterResponse>,
                    t: Throwable
                ) {

                    Log.e(
                        TAG,
                        "BIOMETRIC REGISTER onFailure"
                    )

                    Log.e(
                        TAG,
                        "BIOMETRIC REGISTER exception = ${t.javaClass.simpleName}"
                    )

                    Log.e(
                        TAG,
                        "BIOMETRIC REGISTER message = ${t.message}",
                        t
                    )

                    callback(
                        null,
                        getConnectionErrorMessage(t),
                        null
                    )
                }
            })
    }
}