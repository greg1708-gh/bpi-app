package au.bpi.inspect.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name COLLATE NOCASE")
    fun all(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun byId(id: Long): Flow<Contact?>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun get(id: Long): Contact?

    @Upsert
    suspend fun upsert(c: Contact): Long

    @Delete
    suspend fun delete(c: Contact)
}

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY dateEpochDay DESC, startMin ASC")
    fun all(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE id = :id")
    fun byId(id: Long): Flow<Job?>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun get(id: Long): Job?

    @Query("SELECT * FROM jobs WHERE clientId = :cid OR agentId = :cid ORDER BY dateEpochDay DESC")
    fun forContact(cid: Long): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE dateEpochDay = :day AND id != :exclude")
    suspend fun onDay(day: Long, exclude: Long): List<Job>

    @Upsert
    suspend fun upsert(j: Job): Long

    @Delete
    suspend fun delete(j: Job)
}

@Dao
interface ResultDao {
    @Query("SELECT * FROM item_results WHERE jobId = :jobId")
    fun forJob(jobId: Long): Flow<List<ItemResult>>

    @Upsert
    suspend fun upsert(r: ItemResult)

    @Query("DELETE FROM item_results WHERE jobId = :jobId AND area = :area AND room = :room AND item = :item")
    suspend fun clear(jobId: Long, area: String, room: String, item: String)
}

@Dao
interface FindingDao {
    @Query("SELECT * FROM findings WHERE jobId = :jobId ORDER BY createdAt")
    fun forJob(jobId: Long): Flow<List<Finding>>

    @Query("SELECT * FROM findings WHERE id = :id")
    suspend fun get(id: Long): Finding?

    @Upsert
    suspend fun upsert(f: Finding): Long

    @Delete
    suspend fun delete(f: Finding)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE jobId = :jobId ORDER BY takenAt")
    fun forJob(jobId: Long): Flow<List<Photo>>

    @Insert
    suspend fun insert(p: Photo): Long

    @Query("UPDATE photos SET findingId = :findingId WHERE id IN (:ids)")
    suspend fun attach(ids: List<Long>, findingId: Long)

    @Delete
    suspend fun delete(p: Photo)
}

@Dao
interface LibraryDao {
    @Query("SELECT * FROM library ORDER BY sort, id")
    fun all(): Flow<List<LibComment>>

    @Query("SELECT COUNT(*) FROM library")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LibComment>)

    @Upsert
    suspend fun upsert(c: LibComment): Long

    @Delete
    suspend fun delete(c: LibComment)
}
