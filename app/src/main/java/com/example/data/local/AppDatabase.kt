package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.DateFolderDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.WorkerDao
import com.example.data.local.dao.WorkplaceFolderDao
import com.example.data.local.entity.AttendanceEntity
import com.example.data.local.entity.DateFolderEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.WorkerEntity
import com.example.data.local.entity.WorkplaceFolderEntity

@Database(
    entities = [
        WorkplaceFolderEntity::class,
        DateFolderEntity::class,
        WorkerEntity::class,
        AttendanceEntity::class,
        ExpenseEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): WorkplaceFolderDao
    abstract fun dateFolderDao(): DateFolderDao
    abstract fun workerDao(): WorkerDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worker_management_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
