package com.example.thehairstory.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.thehairstory.data.local.dao.MemberDao
import com.example.thehairstory.data.local.dao.RewardDao
import com.example.thehairstory.data.local.dao.ServiceDao
import com.example.thehairstory.data.local.dao.TransactionDao
import com.example.thehairstory.data.local.entity.MemberEntity
import com.example.thehairstory.data.local.entity.RewardEntity
import com.example.thehairstory.data.local.entity.ServiceEntity
import com.example.thehairstory.data.local.entity.TransactionEntity

@Database(
    entities = [MemberEntity::class, TransactionEntity::class, ServiceEntity::class, RewardEntity::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun memberDao(): MemberDao
    abstract fun transactionDao(): TransactionDao
    abstract fun serviceDao(): ServiceDao
    abstract fun rewardDao(): RewardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thehairstory_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
