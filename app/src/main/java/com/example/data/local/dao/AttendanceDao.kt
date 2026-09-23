package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC, id DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId ORDER BY timestamp DESC, id DESC")
    fun getAttendanceByFolder(folderId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId")
    suspend fun getAttendanceByFolderSync(folderId: Long): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE workerId = :workerId ORDER BY timestamp DESC")
    fun getAttendanceForWorker(workerId: Long): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE folderId = :folderId AND date = :date ORDER BY id DESC")
    fun getAttendanceForDateInFolder(folderId: Long, date: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE folderId = :folderId")
    suspend fun deleteAttendanceByFolder(folderId: Long)

    @Query("DELETE FROM attendance")
    suspend fun clearAll()
}
