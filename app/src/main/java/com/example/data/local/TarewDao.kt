package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TarewDao {
    @Query("SELECT * FROM civic_notices ORDER BY publicationDate DESC")
    fun getAllNotices(): Flow<List<CivicNoticeEntity>>

    @Query("SELECT * FROM civic_notices WHERE pincode = :pincode ORDER BY publicationDate DESC")
    fun getNoticesByPincode(pincode: String): Flow<List<CivicNoticeEntity>>

    @Query("SELECT * FROM civic_notices WHERE id = :id")
    suspend fun getNoticeById(id: String): CivicNoticeEntity?

    @Query("SELECT * FROM civic_notices WHERE status = 'OBJECTION_OPEN' OR status = 'ENVIRONMENTAL_HEARING' ORDER BY daysLeftForObjection ASC")
    fun getOpenObjectionNotices(): Flow<List<CivicNoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<CivicNoticeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: CivicNoticeEntity)

    @Query("UPDATE civic_notices SET concernVotesCount = concernVotesCount + 1, hasUserFlagged = 1 WHERE id = :id")
    suspend fun incrementConcernVote(id: String)

    @Query("SELECT * FROM subscriptions ORDER BY createdAt DESC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity): Long

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: Int)

    @Query("SELECT * FROM saved_objections ORDER BY createdAt DESC")
    fun getAllSavedObjections(): Flow<List<SavedObjectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedObjection(objection: SavedObjectionEntity)

    @Query("SELECT COUNT(*) FROM civic_notices")
    suspend fun getNoticesCount(): Int
}
