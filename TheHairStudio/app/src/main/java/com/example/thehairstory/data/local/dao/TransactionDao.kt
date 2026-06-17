package com.example.thehairstory.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.thehairstory.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("UPDATE transactions SET transactionCode = :code WHERE id = :txId")
    suspend fun updateTransactionCode(txId: Int, code: String)

    @Query("SELECT * FROM transactions WHERE memberId = :memberId ORDER BY id DESC")
    fun getTransactionsForMember(memberId: Int): Flow<List<TransactionEntity>>
}
