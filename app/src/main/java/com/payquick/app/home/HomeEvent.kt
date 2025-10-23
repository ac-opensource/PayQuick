package com.payquick.app.home

import androidx.annotation.StringRes

sealed class HomeEvent {
    data class ShowMessage(
        val message: String? = null,
        @StringRes val messageResId: Int? = null
    ) : HomeEvent()
}
