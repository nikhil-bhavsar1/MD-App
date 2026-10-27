package com.example.model

data class UnicodeItem(
    val char: String,
    val name: String,
    val shortcode: String,
    val category: String,
    val codePoint: String = ""
)
