package com.vatsalp.transitlens.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Offline cache of GTFS static data (schedules work with no network). */
@Database(entities = [StopEntity::class, RouteEntity::class], version = 1, exportSchema = false)
abstract class TransitDatabase : RoomDatabase() {
    abstract fun stopDao(): StopDao
    abstract fun routeDao(): RouteDao
}
