package com.mypassword.app.data.db

import androidx.room.TypeConverter
import com.mypassword.app.data.db.entity.EntryType

class Converters {
    @TypeConverter
    fun fromEntryType(value: EntryType): String = value.name

    @TypeConverter
    fun toEntryType(value: String): EntryType = EntryType.valueOf(value)
}
