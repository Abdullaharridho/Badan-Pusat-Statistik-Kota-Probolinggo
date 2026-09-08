package com.example.bpskota.bps.model

data class NewsResponse(
    val status: String?,
    val `data-availability`: String?,
    val data: List<Any>?
)

data class NewsPagination(
    val page: Int?,
    val pages: Int?,
    val per_page: Int?,
    val count: Int?,
    val total: Int?
)

data class NewsItem(
    val news_id: Int?,
    val newscat_id: String?,
    val newscat_name: String?,
    val title: String?,
    val news: String?,
    val rl_date: String?,
    val picture: String?
)