package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ListingEntity::class,
        EscrowTransactionEntity::class,
        UserAccountEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GideonDatabase : RoomDatabase() {
    abstract fun listingDao(): ListingDao
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao

    companion object {
        @Volatile
        private var INSTANCE: GideonDatabase? = null

        fun getInstance(context: Context): GideonDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GideonDatabase::class.java,
                    "gideon_global_marketplace.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
