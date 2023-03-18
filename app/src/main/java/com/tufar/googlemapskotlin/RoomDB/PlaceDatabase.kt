package com.tufar.googlemapskotlin.RoomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tufar.googlemapskotlin.Model.Place

@Database(entities = arrayOf(Place::class), version = 1)
abstract class PlaceDatabase : RoomDatabase() {
    abstract fun placeDao() : PlaceDao
}