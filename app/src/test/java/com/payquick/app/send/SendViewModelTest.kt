package com.payquick.app.send

import android.content.Context
import com.payquick.R
import com.payquick.app.testing.MainDispatcherRule
import com.payquick.domain.usecase.SubmitMockTransferUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SendViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val context: Context = mockContext()
    private val useCase = SubmitMockTransferUseCase()

    @Test
    fun digitEntryBuildsAmount() = runTest(dispatcherRule.dispatcher) {
        val viewModel = SendViewModel(useCase, context)

        viewModel.onDigitClick('1')
        viewModel.onDigitClick('2')
        viewModel.onDigitClick('3')

        assertEquals("123", viewModel.state.value.amount)
    }

    @Test
    fun submittingWithoutAmountEmitsValidationError() = runTest(dispatcherRule.dispatcher) {
        val viewModel = SendViewModel(useCase, context)

        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(R.string.send_error_amount_required, viewModel.state.value.errorMessageResId)
    }

    @Test
    fun successfulSubmissionEmitsCompletionEvent() = runTest(dispatcherRule.dispatcher) {
        val viewModel = SendViewModel(useCase, context)

        viewModel.onDigitClick('5')
        viewModel.onDigitClick('0')

        viewModel.onSubmit()

        val event = viewModel.events.filterIsInstance<SendEvent.TransferCompleted>().first()
        assertTrue(event is SendEvent.TransferCompleted)
    }

    private fun mockContext(): Context = mockk(relaxed = true) {
        every { getString(R.string.send_recipient_name_katarina) } returns "Katarina"
        every { getString(R.string.send_recipient_name_alex) } returns "Alex Morgan"
        every { getString(R.string.send_recipient_name_jamie) } returns "Jamie Rivera"
        every { getString(R.string.send_joined_date_pattern) } returns "MM.dd.yyyy"
        every { getString(R.string.send_joined_date, any()) } answers { "Joined ${args[1]}" }
        every { getString(R.string.send_transfer_scheduled) } returns "Transfer scheduled"
        every { getString(R.string.send_recipient_avatar_cd) } returns "Recipient avatar"
        every { getString(R.string.send_change_action) } returns "Change"
        every { getString(R.string.send_change_currency_cd) } returns "Change currency"
        every { getString(R.string.send_currency_option, any(), any()) } answers {
            val code = args.getOrNull(1)?.toString() ?: ""
            val symbol = args.getOrNull(2)?.toString() ?: ""
            "$code ($symbol)"
        }
        every { getString(R.string.send_exchange_rate_sample) } returns "1 EUR = 1.22 USD"
        every { getString(R.string.common_zero) } returns "0"
        every { getString(R.string.send_error_amount_required) } returns "Enter an amount"
        every { getString(R.string.send_error_amount_invalid) } returns "Enter a valid amount"
        every { getString(R.string.send_error_generic) } returns "Unable to send money"
    }
}
