package com.payquick.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionEnvelope(
    val status: String,
    val message: String,
    val pagination: Pagination,
    val data: List<TransactionDto>
)

@Serializable
data class Pagination(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
    @SerialName("total_items") val totalItems: Int,
    @SerialName("items_per_page") val itemsPerPage: Int
)

@Serializable
data class TransactionDto(
    val id: String,
    @SerialName("amount_in_cents") val amountInCents: Int,
    val currency: String,
    val type: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("destination_id") val destinationId: String
)
