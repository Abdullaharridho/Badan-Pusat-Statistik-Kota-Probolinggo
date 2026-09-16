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
data class ActivityStatisticsResponse(
    val success: Boolean,
    val period: ActivityStatisticsPeriod?,
    val summary: ActivityStatisticsSummary?,
    val popular_screens: List<ActivityScreenStatistic>?
)

data class ActivityStatisticsPeriod(
    val start_date: String?,
    val end_date: String?
)

data class ActivityStatisticsSummary(
    val users_accessing: Int,
    val total_access: Int,
    val total_duration_seconds: Long
)

data class ActivityScreenStatistic(
    val screen: String?,
    val total: Int
)
data class AdminActivityStatisticsResponse(
    val success: Boolean,
    val period: ActivityStatisticsPeriod?,
    val summary: ActivityStatisticsSummary?,
    val events: List<ActivityEventStatistic>?,
    val screens: List<ActivityScreenStatistic>?,
    val devices: List<ActivityDeviceStatistic>?,
    val android_versions: List<ActivityAndroidStatistic>?
)

data class ActivityEventStatistic(
    val event: String?,
    val total: Int
)

data class ActivityDeviceStatistic(
    val device_model: String?,
    val total_users: Int
)

data class ActivityAndroidStatistic(
    val android_version: Int?,
    val total_users: Int
)