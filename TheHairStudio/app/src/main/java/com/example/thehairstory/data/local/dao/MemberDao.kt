package com.example.thehairstory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.thehairstory.data.local.entity.MemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity): Long

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("SELECT * FROM members ORDER BY id DESC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE id = :memberId")
    fun getMemberById(memberId: Int): Flow<MemberEntity?>

    @Query("SELECT COUNT(*) FROM members")
    fun getMemberCount(): Flow<Int>

    @Query("UPDATE members SET points = :newPoints WHERE id = :memberId")
    suspend fun updatePoints(memberId: Int, newPoints: Int)

    @Query("UPDATE members SET earnedPoints = :earnedPoints WHERE id = :memberId")
    suspend fun updateEarnedPoints(memberId: Int, earnedPoints: Int)

    @Query("SELECT * FROM members WHERE LOWER(email) = LOWER(:input) OR phone = :input LIMIT 1")
    suspend fun getMemberByEmailOrPhone(input: String): MemberEntity?

    @Query("SELECT * FROM members WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getMemberByEmail(email: String): MemberEntity?

    @Query("SELECT * FROM members WHERE phone = :phone LIMIT 1")
    suspend fun getMemberByPhone(phone: String): MemberEntity?

    @Query("SELECT * FROM members WHERE LOWER(memberCode) = LOWER(:memberCode) LIMIT 1")
    suspend fun getMemberByCode(memberCode: String): MemberEntity?
}

