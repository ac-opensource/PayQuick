package com.payquick.app.receive

data class ReceiveUiState(
    val code: String = "PQ-000000",
    val link: String = "https://payquick.app/pay/pq-000000",
    val refreshedLabel: String? = null
)
