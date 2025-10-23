package com.payquick.data.di

import com.payquick.data.auth.DefaultAuthRepository
import com.payquick.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AuthModule {

    @Binds
    @Singleton
    fun bindAuthRepository(impl: DefaultAuthRepository): AuthRepository
}
