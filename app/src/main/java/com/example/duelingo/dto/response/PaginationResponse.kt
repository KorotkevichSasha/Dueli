package com.example.duelingo.dto.response


 class PaginationResponse<T>(
    val content: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Long
)
