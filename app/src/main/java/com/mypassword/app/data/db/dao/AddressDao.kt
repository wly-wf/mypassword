package com.mypassword.app.data.db.dao

import androidx.room.*
import com.mypassword.app.data.db.entity.Address
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {

    @Query("SELECT * FROM addresses ORDER BY title COLLATE NOCASE ASC")
    fun getAllAddresses(): Flow<List<Address>>

    @Query("SELECT * FROM addresses WHERE id = :id")
    suspend fun getAddressById(id: Long): Address?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: Address): Long

    @Update
    suspend fun updateAddress(address: Address)

    @Query("DELETE FROM addresses WHERE id = :id")
    suspend fun deleteAddress(id: Long)

    @Query("SELECT * FROM addresses ORDER BY title COLLATE NOCASE ASC")
    suspend fun getAllAddressesList(): List<Address>
}
