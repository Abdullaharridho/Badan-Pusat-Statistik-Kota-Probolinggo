package com.example.bpskota.bps.model

data class DataKategoriItem(
    val judul: String,
    val kategori: String,
    val sumber: String,
    val id: String,
    val tahun: Int? = null
)
data class DetailDataItem(
    val label: String,
    val nilai: String,
    val satuan: String? = null
)
data class DetailDataResult(
    val judul: String,
    val kategori: String,
    val sumber: String,
    val tahun: Int? = null,
    val catatan: String? = null,
    val sumberData: String? = null,
    val items: List<DetailDataItem> = emptyList()
)
data class DataItem(
    val judul: String,
    val kategori: String,
    val sumber: String,
    val id: String,
    val tahun: Int? = null,
    val ketersediaanTahun: List<Int> = emptyList(),
    val type: String
)