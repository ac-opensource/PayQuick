package com.payquick.domain.model

data class TransactionPage(
    val transactions: List<Transaction>,
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val itemsPerPage: Int
) {
    val hasNextPage: Boolean get() = currentPage < totalPages
    val hasPreviousPage: Boolean get() = currentPage > 1
}
