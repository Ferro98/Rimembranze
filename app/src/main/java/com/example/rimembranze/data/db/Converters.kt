package com.example.rimembranze.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun itemTypeToString(value: ItemType): String = value.name

    @TypeConverter
    fun stringToItemType(value: String): ItemType = ItemType.valueOf(value)
}