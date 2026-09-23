package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.WorkerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers ORDER BY id DESC")
    fun getAllWorkers(): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE folderId = :folderId ORDER BY id DESC")
    fun getWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE folderId = :folderId")
    suspend fun getWorkersByFolderSync(folderId: Long): List<WorkerEntity>

    @Query("SELECT * FROM workers WHERE id = :id")
    fun getWorkerById(id: Long): Flow<WorkerEntity?>

    @Query("SELECT * FROM workers WHERE folderId = :folderId AND isActive = 1 ORDER BY name ASC")
    fun getActiveWorkersByFolder(folderId: Long): Flow<List<WorkerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity): Long

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("SELECT * FROM workers WHERE dateFolderId = :dateFolderId ORDER BY id DESC")
    fun getWorkersByDateFolder(dateFolderId: Long): Flow<List<WorkerEntity>>

    @Query("DELETE FROM workers WHERE dateFolderId = :dateFolderId")
    suspend fun deleteWorkersByDateFolder(dateFolderId: Long)

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)

    @Query("DELETE FROM workers WHERE folderId = :folderId")
    suspend fun deleteWorkersByFolder(folderId: Long)

    @Query("DELETE FROM workers")
    suspend fun clearAll()
}
