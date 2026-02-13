package org.bigblackowl.vccadmin.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.People
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.theme.ThemeMode
import org.jetbrains.compose.resources.StringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.cities
import vccadministrator.composeapp.generated.resources.exit
import vccadministrator.composeapp.generated.resources.file_generation
import vccadministrator.composeapp.generated.resources.new_shop
import vccadministrator.composeapp.generated.resources.slides
import vccadministrator.composeapp.generated.resources.slides_ai_generation
import vccadministrator.composeapp.generated.resources.users

@Immutable
data class MenuItem(
    val text: StringResource,
    val icon: ImageVector,
    val isSelected: (Route?) -> Boolean = { false },
    val onClick: () -> Unit,
)
fun buildMenuItems(
    themeMode: ThemeMode,
    setThemeMode: (ThemeMode) -> Unit,
    currentUserRole: UserRole?,
    isLoginScreen: Boolean,
    navigate: (route: Route) -> Unit,
    logout: () -> Unit,
): List<MenuItem> {
    fun applyThemeMode(next: ThemeMode) {
        setThemeMode(next)
    }

    fun nextThemeMode(mode: ThemeMode) = when (mode) {
        ThemeMode.AUTO -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.AUTO
    }

    return buildList {
        add(
            MenuItem(
                text = themeMode.label,
                icon = themeMode.icon,
                onClick = { applyThemeMode(nextThemeMode(themeMode)) }
            )
        )

        if (!isLoginScreen) {
            add(
                MenuItem(
                    text = Res.string.file_generation,
                    icon = Icons.Default.FilePresent,
                    isSelected = { it == Route.FileGenerator },
                    onClick = { navigate(Route.FileGenerator) }
                )
            )

            if (currentUserRole == UserRole.ADMIN) {
                add(
                    MenuItem(
                        text = Res.string.slides,
                        icon = Icons.Default.Image,
                        isSelected = { it == Route.SlidesList || it is Route.AddEditSlide },
                        onClick = { navigate(Route.SlidesList) }
                    )
                )
                add(
                    MenuItem(
                        text = Res.string.slides_ai_generation,
                        icon = Icons.Default.GeneratingTokens,
                        isSelected = { it == Route.SlideAiGeneration },
                        onClick = { navigate(Route.SlideAiGeneration) }
                    )
                )
                add(
                    MenuItem(
                        text = Res.string.users,
                        icon = Icons.Default.People,
                        isSelected = { it == Route.UsersList || it is Route.AddEditUser },
                        onClick = { navigate(Route.UsersList) }
                    )
                )
                add(
                    MenuItem(
                        text = Res.string.new_shop,
                        icon = Icons.Default.AddBusiness,
                        isSelected = { it is Route.AddEditShop },
                        onClick = { navigate(Route.AddEditShop(null)) }
                    )
                )
                add(
                    MenuItem(
                        text = Res.string.cities,
                        icon = Icons.Default.LocationCity,
                        isSelected = { it == Route.CityList || it is Route.AddEditCity },
                        onClick = { navigate(Route.CityList) }
                    )
                )
            }
        }

        add(
            MenuItem(
                text = Res.string.exit,
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = logout

            )
        )
    }
}