package com.example.bpskota.tracking

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.bpskota.bpskp.api.BpskpApiService
import com.example.bpskota.bpskp.model.ActivityLogRequest
import com.example.bpskota.bpskp.model.ActivityLogResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ActivityTracker(
    context: Context,
    private val apiService: BpskpApiService
) {

    companion object {
        private const val TAG = "ActivityTracker"
    }

    private val appContext = context.applicationContext
    private val anonymousIdManager =
        AnonymousIdManager(appContext)

    // ================================================================
    // APP OPEN
    // ================================================================

    fun trackAppOpen(
        metadata: Map<String, Any>? = null
    ) {
        sendActivity(
            event = "app_open",
            screen = null,
            metadata = metadata
        )
    }

    fun trackAppClose(
        metadata: Map<String, Any>? = null
    ) {
        sendActivity(
            event = "app_close",
            screen = null,
            metadata = metadata
        )
    }

    // ================================================================
    // SCREEN VIEW
    // ================================================================


    fun trackScreen(
        screen: String,
        metadata: Map<String, Any>? = null
    ) {
        sendActivity(
            event = "screen_view",
            screen = screen,
            metadata = metadata
        )
    }

    // ================================================================
    // SEND ACTIVITY
    // ================================================================

    private fun sendActivity(
        event: String,
        screen: String?,
        metadata: Map<String, Any>? = null
    ) {
        Log.d(
            TAG,
            "Mengirim aktivitas: event=$event, screen=$screen"
        )

        val request = ActivityLogRequest(
            anonymous_id =
            anonymousIdManager.getAnonymousId(),

            event = event,

            screen = screen,

            device_model = Build.MODEL,

            android_version = Build.VERSION.SDK_INT,

            metadata = metadata
        )

        apiService
            .logActivity(request)
            .enqueue(
                object : Callback<ActivityLogResponse> {

                    override fun onResponse(
                        call: Call<ActivityLogResponse>,
                        response: Response<ActivityLogResponse>
                    ) {

                        if (response.isSuccessful) {

                            Log.d(
                                TAG,
                                "Aktivitas berhasil dicatat: " +
                                        "event=$event, " +
                                        "screen=$screen, " +
                                        "metadata=$metadata"
                            )

                        } else {

                            Log.w(
                                TAG,
                                "Gagal mencatat aktivitas. " +
                                        "HTTP ${response.code()}"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<ActivityLogResponse>,
                        t: Throwable
                    ) {

                        Log.w(
                            TAG,
                            "Tracking gagal: ${t.message}"
                        )
                    }
                }
            )
    }
}