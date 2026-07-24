package com.vatsalp.transitlens.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StopDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<StopEntity>)

    @Query("SELECT * FROM stops")
    suspend fun all(): List<StopEntity>

    @Query("SELECT COUNT(*) FROM stops")
    suspend fun count(): Int
}

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Query("SELECT * FROM routes")
    suspend fun all(): List<RouteEntity>

    @Query("SELECT * FROM routes WHERE shortName = :shortName LIMIT 1")
    suspend fun findByShortName(shortName: String): RouteEntity?
}
