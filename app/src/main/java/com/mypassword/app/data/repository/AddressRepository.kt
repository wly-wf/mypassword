package com.mypassword.app.data.repository

import com.mypassword.app.data.db.AppDatabase
import com.mypassword.app.data.db.entity.Address
import kotlinx.coroutines.flow.Flow

class AddressRepository(private val database: AppDatabase) {

    private val dao = database.addressDao()

    fun getAllAddresses(): Flow<List<Address>> = dao.getAllAddresses()

    suspend fun getAddressById(id: Long): Address? = dao.getAddressById(id)

    suspend fun addAddress(address: Address): Long = dao.insertAddress(address)

    suspend fun updateAddress(address: Address) {
        dao.updateAddress(address.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteAddress(id: Long) = dao.deleteAddress(id)

    suspend fun getAllAddressesList(): List<Address> = dao.getAllAddressesList()
}
