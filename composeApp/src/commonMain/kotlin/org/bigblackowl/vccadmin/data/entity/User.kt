package org.bigblackowl.vccadmin.data.entity

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.theme.locals.userRoleColors
import org.jetbrains.compose.resources.StringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.admin
import vccadministrator.composeapp.generated.resources.user

@Serializable
data class User(
    val id: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    @SerialName("schedule_color") val scheduleColor: Long = 4280307852,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("last_modified") val lastModified: Long,
    @SerialName("last_modified_user_id") val lastModifiedUserId: String,
){
    val fullName: String
        get() = "$firstName $lastName"
}

@Serializable
enum class UserRole {
    @SerialName("admin")
    ADMIN,

    @SerialName("user")
    USER;

    val getName: StringResource
        get() = when (this) {
            ADMIN -> Res.string.admin
            USER -> Res.string.user
        }

    val color: Color
        @Composable
        get() = with(MaterialTheme.userRoleColors) {
            return when (this@UserRole) {
                ADMIN -> admin
                USER -> user
            }
        }

}

@Immutable
data class UserRoleColors(
    val admin: Color,
    val user: Color,
)