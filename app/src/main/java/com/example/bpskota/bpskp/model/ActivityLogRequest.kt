package com.example.bpskota.bpskp.model

data class ActivityLogRequest(
    val anonymous_id: String,
    val event: String,
    val screen: String? = null,
    val device_model: String? = null,
    val android_version: Int? = null,
    val metadata: Map<String, Any>? = null
)
data class ActivityLogResponse(
    val success: Boolean,
    val message: String,
    val data: ActivityLogData?
)

data class ActivityLogData(
    val id: Int
)