package com.example.muplay.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Tidak perlu @Provides karena semua repository sudah menggunakan @Inject constructor
}