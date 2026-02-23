package org.bigblackowl.vccadmin.data.repository

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.email_already_exists
import vccadministrator.composeapp.generated.resources.user_not_found

class UserRepositoryImpl(private val supabase: SupabaseClient) : UserRepository {

    companion object {
        private const val USER_ADMINISTRATION_FUNCTION = "user_administration"
        private const val USER_TABLE = "users"
        private const val COLUMN_EMAIL = "email"
        @Serializable
        private data class UserAdminRequest(
            @SerialName("id") val id: String? = null,
            @SerialName("action") val action: String,
            @SerialName("email") val email: String? = null,
            @SerialName("phone") val phone: String? = null,
            @SerialName("password") val password: String? = null,
            @SerialName("first_name") val firstName: String? = null,
            @SerialName("last_name") val lastName: String? = null,
            @SerialName("role") val role: String? = null,
            @SerialName("last_modified_user_id") val lastModifiedUserId: String? = null,
        )
    }

    private val userTable = supabase.postgrest.from(USER_TABLE)
    private val function = supabase.functions

    override suspend fun getUsers(): List<User> {
        return userTable.select().decodeList<User>()
    }

    override suspend fun getCurrentUser(): User? {
        val users = getUsers()
        val id = supabase.auth.currentUserOrNull()?.id
        return users.find { it.id == id }
    }

    override suspend fun getUserById(userId: String): User? {
        val users = getUsers()
        return users.find { it.id == userId }
    }

    override suspend fun getUserNameById(userId: String): String {
        val users = getUsers()
        val user = users.find { it.id == userId }
        if (user == null) return getString(Res.string.user_not_found)
        return "${user.firstName} ${user.lastName}"
    }

    @OptIn(InternalAPI::class)
    override suspend fun registerUser(
        email: String,
        phone: String?,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole
    ): Boolean {
        if (userTable.select { filter { eq(COLUMN_EMAIL, email) } }.decodeSingleOrNull<User>() != null) {
            throw IllegalStateException(getString(Res.string.email_already_exists))
        }
        val adminId = supabase.auth.currentUserOrNull()?.id
        val req = UserAdminRequest(
            action = "create",
            email = email,
            phone = phone,
            password = password,
            firstName = firstName,
            lastName = lastName,
            role = role.name.lowercase(),
            lastModifiedUserId = adminId
        )
        val request = function.invoke(USER_ADMINISTRATION_FUNCTION) {
            this.body = Json.encodeToString(req)
            contentType(ContentType.Application.Json)
        }

        Napier.d(tag = "CREATE USER") { "request: $request" }
        return request.status == HttpStatusCode.OK
    }

    @OptIn(InternalAPI::class)
    override suspend fun updateUser(
        id: String,
        phone: String?,
        firstName: String,
        lastName: String,
        role: UserRole
    ): Boolean {
        val adminId = supabase.auth.currentUserOrNull()?.id
        val req = UserAdminRequest(
            action = "update", id = id, phone = phone, firstName = firstName, lastName = lastName, role = role.name.lowercase(), lastModifiedUserId = adminId
        )
        val request = function.invoke(USER_ADMINISTRATION_FUNCTION) {
            this.body = Json.encodeToString(req)
            contentType(ContentType.Application.Json)
        }
        Napier.d(tag = "UPDATE USER") { "request: $request" }
        return request.status == HttpStatusCode.OK
    }

    @OptIn(InternalAPI::class)
    override suspend fun deleteUser(userId: String): Boolean {
        val req = UserAdminRequest(action = "delete", id = userId)
        val request = function.invoke(USER_ADMINISTRATION_FUNCTION) {
            this.body = Json.encodeToString(req)
            contentType(ContentType.Application.Json)
        }
        Napier.d(tag = "DELETE USER") { "request: $request" }
        return request.status == HttpStatusCode.OK
    }
}