package com.payquick.data.di

import com.payquick.data.transactions.DefaultTransactionRepository
import com.payquick.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TransactionModule {

    @Binds
    @Singleton
    fun bindTransactionRepository(
        impl: DefaultTransactionRepository
    ): TransactionRepository
}
