package com.vatsalp.transitlens.di

import android.content.Context
import androidx.room.Room
import com.vatsalp.transitlens.data.db.RouteDao
import com.vatsalp.transitlens.data.db.StopDao
import com.vatsalp.transitlens.data.db.TransitDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TransitDatabase =
        Room.databaseBuilder(context, TransitDatabase::class.java, "transit.db").build()

    @Provides
    fun provideStopDao(db: TransitDatabase): StopDao = db.stopDao()

    @Provides
    fun provideRouteDao(db: TransitDatabase): RouteDao = db.routeDao()
}
