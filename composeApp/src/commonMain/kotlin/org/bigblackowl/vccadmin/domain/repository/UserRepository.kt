package org.bigblackowl.vccadmin.domain.repository

import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole

interface UserRepository {
    suspend fun getUsers(): List<User>

    suspend fun getCurrentUser(): User?

    suspend fun getUserById(userId: String): User?

    suspend fun getUserNameById(userId: String): String

    suspend fun registerUser(email: String, phone: String?, password: String, firstName: String, lastName: String, role: UserRole): Boolean
    suspend fun updateUser(id: String, phone: String?, firstName: String, lastName: String, role: UserRole): Boolean
    suspend fun deleteUser(userId: String): Boolean
}