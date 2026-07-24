package com.vatsalp.transitlens.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stops")
data class StopEntity(
    @PrimaryKey val stopId: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val wheelchairBoarding: Int,
)

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val routeId: String,
    val shortName: String,
    val longName: String,
    val type: Int,
)
