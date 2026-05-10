package com.mypassword.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mypassword.app.data.db.dao.EntryDao
import com.mypassword.app.data.db.entity.Entry
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [Entry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    companion object {
        private const val DB_NAME = "mypassword.db"

        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}
