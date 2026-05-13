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

    companion object {
        private const val DB_NAME = "mypassword.db"

        fun create(context: Context, passphrase: ByteArray): AppDatabase {
            val factory = SupportOpenHelperFactory(passphrase.copyOf())
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .build()
        }

        /**
         * 绕过 Room，直接用 SQLCipher 原生 API 修改数据库加密密钥。
         * 调用前必须先关闭 Room 数据库，完成后用新密钥重新 create()。
         */
        fun rekeyDatabase(context: Context, oldKey: ByteArray, newKey: ByteArray) {
            val dbFile = context.getDatabasePath(DB_NAME)

            val db = net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                dbFile.absolutePath,
                oldKey,
                null,
                null
            )
            try {
                // 将 WAL 完整合并到主文件，然后截断 WAL
                db.rawExecSQL("PRAGMA wal_checkpoint(TRUNCATE);")
                // 修改加密密钥
                db.rawExecSQL("PRAGMA rekey = \"x'${newKey.toHex()}\";")
            } finally {
                db.close()
            }
        }

        private fun ByteArray.toHex(): String =
            joinToString(separator = "") { "%02x".format(it) }
    }
}
