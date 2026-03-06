package com.example.custompicker.di

import com.example.custompicker.media.MediaLoader
import com.example.custompicker.media.MediaLoaderImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaLoaderModule {

    @Binds
    @Singleton
    abstract fun bindMediaLoader(
        mediaLoaderImpl: MediaLoaderImpl,
    ): MediaLoader
}
