package com.mypassword.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mypassword.app.data.db.dao.EntryDao
import com.mypassword.app.data.db.entity.Entry
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [Entry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao

    /**
     * 修改数据库加密密钥（PRAGMA rekey），完成后需 close() 并重新 create()
     */
    fun rekey(newPassphrase: ByteArray) {
        val hexKey = newPassphrase.joinToString(separator = "") { "%02x".format(it) }
        openHelper.writableDatabase.execSQL("PRAGMA rekey = \"x'$hexKey'\";")
    }

    companion object {
        private const val DB_NAME = "mypassword.db"

        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportOpenHelperFactory(passphrase)

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
