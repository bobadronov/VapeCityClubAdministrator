package org.bigblackowl.vccadmin.data.repository

import org.bigblackowl.vccadmin.data.entity.AdminAppUpdate
import org.bigblackowl.vccadmin.data.entity.AssetInfo
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.DeviceType
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.entity.ShopStatus
import org.bigblackowl.vccadmin.data.entity.Slide
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.ui.city.addEdit.CitySuggestion
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.Clock

object FakeBackend {
    private val nowMs: Long = Clock.System.now().toEpochMilliseconds()
    private val rnd = Random(42)

    // Якщо хочеш — винеси це в окремий файл FakeData.kt
    private val kyivStreets = listOf("Хрещатик", "Саксаганського", "Велика Васильківська", "Антоновича", "Лесі Українки")
    private val lvivStreets = listOf("Городоцька", "Зелена", "Кульпарківська", "Шевченка", "Личаківська")
    private val odesaStreets = listOf("Дерибасівська", "Пушкінська", "Рішельєвська", "Катерининська", "Французький бульвар")
    private val kharkivStreets = listOf("Сумська", "Пушкінська", "Полтавський шлях", "Науки", "Гагаріна")

    private val providers = listOf("Kyivstar", "Volia", "Lanet", "Datagroup", "Triolan", "Ukrtelecom")
    private val malls = listOf("ТРЦ Gulliver", "ТРЦ Lavina", "ТРЦ Ocean Plaza", "ТРЦ Forum", "ТРЦ Nikolsky", "ТРЦ City Center")
    private val addressComments = listOf("1 поверх", "2 поверх, біля ескалатора", "вхід з парковки", "окремий вхід", "біля фудкорту", null)

    private val deviceTypesRotation = listOf(DeviceType.TV, DeviceType.TV, DeviceType.TV /*, DeviceType.PROJECTOR ... */)

    // Фейкові файли слайдів
    private val slideNames = listOf(
        "Promo_Sale", "NewCollection", "WeekendDeal", "Cashback", "SuperPrice", "Winter", "Spring", "NightOffer"
    )
    private val imageExt = listOf("png", "jpg", "webp")

    // -------------------------
    // Users
    // -------------------------
    val users: List<User> = buildUsers()

    val admin: User = users.first()

    // -------------------------
    // Cities
    // -------------------------
    val cities: List<City> = buildCities()

    // -------------------------
    // Shops (зв’язані з містами)
    // -------------------------
    val shops: List<Shop> = buildShops(
        cities = cities,
        shopsPerCityRange = 3..9
    )

    // -------------------------
    // Slides (зв’язані з shops)
    // -------------------------
    val slides: List<Slide> = buildSlides(
        shops = shops,
        slidesPerShopRange = 2..8
    )

    // -------------------------
    // “Single” зразки (якщо треба)
    // -------------------------
    val singleCity: City = cities.first()
    val singleShop: Shop = shops.first()
    val singleSlide: Slide = slides.first()
    val singleUser: User = users.first()
    // --------------------------
    // Preview
    // -------------------------
    // 2026-02-15T12:34:56Z -> epoch millis
    private const val PUBLISHED_AT_MS: Long = 1771158896000L

    private val manifest = AdminAppUpdate(
        id = "00000000-0000-0000-0000-000000000000",
        version = "1.2.303",
        publishedAt = PUBLISHED_AT_MS,
        releaseNotes = """
            - Додали OTA-оновлення для Desktop
            - Виправили SHA256 перевірку
            - Покращили прогрес завантаження
            - Дрібні UI/UX правки
        """.trimIndent(),
        windows = AssetInfo(
            name = "VCC-Admin-Setup-1.2.303.msi",
            url = "https://example.com/VCC-Admin-Setup-1.2.303.msi",
            size = 120L * 1024 * 1024,
            sha256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        ),
        macos = AssetInfo(
            name = "VCC-Admin-1.2.303.dmg",
            url = "https://example.com/VCC-Admin-1.2.303.dmg",
            size = 140L * 1024 * 1024,
            sha256 = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        ),
        linux = AssetInfo(
            name = "vcc-admin_1.2.303_amd64.deb",
            url = "https://example.com/vcc-admin_1.2.303_amd64.deb",
            size = 110L * 1024 * 1024,
            sha256 = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"
        ),
        android = AssetInfo(
            name = "vcc-admin-1.2.303-release.apk",
            url = "https://example.com/vcc-admin-1.2.303-release.apk",
            size = 45L * 1024 * 1024,
            sha256 = "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"
        ),
    )

    // ---- UpdateInfo під кожну ОС ----

    val updateInfoWin = UpdateInfo(
        manifest = manifest,
        asset = requireNotNull(manifest.windows) { "Fake windows asset is null" }
    )

    val updateInfoMac = UpdateInfo(
        manifest = manifest,
        asset = requireNotNull(manifest.macos) { "Fake macos asset is null" }
    )

    val updateInfoLinux = UpdateInfo(
        manifest = manifest,
        asset = requireNotNull(manifest.linux) { "Fake linux asset is null" }
    )

    val updateInfoAndroid = UpdateInfo(
        manifest = manifest,
        asset = requireNotNull(manifest.android) { "Fake android asset is null" }
    )
    private val previewSuggestions = listOf(
        CitySuggestion(CityDto(name = "Київ", oblast = "Київ"), exists = true),
        CitySuggestion(CityDto(name = "Київська", oblast = "Київська область"), true),
        CitySuggestion(CityDto(name = "Ірпінь", oblast = "Київська область"), false),
        CitySuggestion(CityDto(name = "Біла Церква", oblast = "Київська область"), false),
        CitySuggestion(CityDto(name = "Київ", oblast = "Київ"), exists = true),
        CitySuggestion(CityDto(name = "Київська", oblast = "Київська область"), true),
        CitySuggestion(CityDto(name = "Київ", oblast = "Київ"), exists = true),
        CitySuggestion(CityDto(name = "Київська", oblast = "Київська область"), true),
    )

    val previewSuggestionsList = previewSuggestions

    // ========================================================================
    // Builders
    // ========================================================================
    private val generatedFilesList = listOf(
        GeneratedFile(name = "Документи_Київ_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Львів_02_2026.pdf", content = null, error = "Some error"),
        GeneratedFile(name = "Документи_Харків_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Одеса_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Дніпро_02_2026.pdf", content = null, error = "Network timeout"),
        GeneratedFile(name = "Звіт_Запоріжжя_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Вінниця_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Чернігів_02_2026.pdf", content = null, error = "File generation failed"),
        GeneratedFile(name = "Документи_Полтава_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Черкаси_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Суми_02_2026.pdf", content = null, error = "No data"),
        GeneratedFile(name = "Звіт_Житомир_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Рівне_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Івано-Франківськ_02_2026.pdf", content = null, error = "Some error"),
        GeneratedFile(name = "Документи_Тернопіль_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Луцьк_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Ужгород_02_2026.pdf", content = null, error = "Permission denied"),
        GeneratedFile(name = "Звіт_Хмельницький_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Документи_Кропивницький_02_2026.pdf", content = ByteArray(1)),
        GeneratedFile(name = "Звіт_Миколаїв_02_2026.pdf", content = null, error = "Some error"),
    )
    val generatedFiles = generatedFilesList
    private fun buildUsers(): List<User> {
        val adminId = "f336cdb0-3c9a-406f-be2a-2d50c696c702"
        val baseCreatedAt = 1704067200000L // 2024-01-01

        val admin = User(
            id = adminId,
            firstName = "Admin",
            lastName = "User",
            email = "admin@bigblackowl.com",
            phone = "+380670000000",
            role = UserRole.ADMIN,
            createdAt = baseCreatedAt,
            lastModified = nowMs - 10_000L,
            lastModifiedUserId = adminId
        )

        // Декілька додаткових користувачів
        val staff = listOf(
            "Olena" to "Shevchenko",
            "Ihor" to "Koval",
            "Yulia" to "Bondar",
            "Taras" to "Hnatyuk",
            "Denys" to "Savchenko"
        ).mapIndexed { idx, (fn, ln) ->
            User(
                id = "user-${idx + 1}",
                firstName = fn,
                lastName = ln,
                email = "${fn.lowercase()}.${ln.lowercase()}@bigblackowl.com",
                phone = "+38067${rnd.nextInt(1000000, 9999999)}",
                role = if (idx % 2 == 0) UserRole.ADMIN else UserRole.USER,
                createdAt = baseCreatedAt + (idx + 1) * 7L * 24 * 60 * 60 * 1000,
                lastModified = nowMs - (idx + 1) * 3_600_000L,
                lastModifiedUserId = adminId
            )
        }

        return listOf(admin) + staff
    }

    private fun buildCities(): List<City> {
        val cityDefs = listOf(
            1 to "Київ",
            2 to "Львів",
            3 to "Одеса",
            4 to "Харків",
            5 to "Дніпро",
        )

        return cityDefs.mapIndexed { idx, (id, name) ->
            City(
                id = id,
                name = name,
                lastModified = nowMs - idx * 60_000L,
                lastModifiedUserId = admin.id,
                // Якщо хочеш реальні лого з Supabase — підставляй свій baseUrl
                logoUrl = "https://picsum.photos/seed/city_$id/256/256"
            )
        }
    }

    private fun buildShops(
        cities: List<City>,
        shopsPerCityRange: IntRange
    ): List<Shop> {
        val list = mutableListOf<Shop>()
        var globalIndex = 1

        val allStatuses = ShopStatus.entries.toList()
        val pendingStatuses = allStatuses.shuffled(rnd).toMutableList() // які ще треба “покрити”

        fun nextStatusGuaranteed(): ShopStatus {
            // Поки є непокриті — віддаємо їх по одному.
            // Далі — твоя вагова рандомізація.
            return if (pendingStatuses.isNotEmpty()) {
                pendingStatuses.removeAt(pendingStatuses.lastIndex)
            } else {
                ShopStatus.entries.randomWeighted(rnd) { s ->
                    when (s) {
                        ShopStatus.ACTIVE -> 4
                        ShopStatus.RELOCATING -> 3
                        ShopStatus.CLOSED -> 2
                        ShopStatus.UNDER_REPAIR -> 1
                        ShopStatus.INACTIVE -> 0
                    }
                }
            }
        }

        cities.forEach { city ->
            var count = rnd.nextInt(shopsPerCityRange.first, shopsPerCityRange.last + 1)

            // ВАЖЛИВО: щоб ми змогли “втиснути” всі статуси, якщо ще залишились непокриті
            // (інакше може не вистачити загальної кількості магазинів)
            if (pendingStatuses.isNotEmpty()) {
                val minNeed = pendingStatuses.size
                if (count < minNeed) count = minNeed
            }

            repeat(count) { localIndex ->
                val shopId = "shop-${city.id}-$localIndex"
                val code = "${city.name.take(4).uppercase()}-${globalIndex.toString().padStart(3, '0')}"
                val street = pickStreet(city.name)
                val house = rnd.nextInt(1, 220).toString() + (if (rnd.nextBoolean()) "" else listOf("A", "B", "C").random(rnd))
                val provider = providers.random(rnd)

                val status = nextStatusGuaranteed()

                val personalAccountsCount = rnd.nextInt(1, 4)
                val personalAccounts = List(personalAccountsCount) { "PA-${rnd.nextInt(100000, 999999)}" }

                val camerasCount = rnd.nextInt(0, 5)
                val cameraCodes = List(camerasCount) { "CAM-${rnd.nextInt(1, 99).toString().padStart(2, '0')}" }

                val replenishmentDay = rnd.nextInt(1, 29)
                val replenishmentAmount = listOf("250", "300", "350", "400", "500").random(rnd)

                val lastModUser = users.random(rnd)
                val lastModifiedMs = nowMs - rnd.nextInt(0, 14) * 24L * 60 * 60 * 1000 - rnd.nextInt(0, 86_400_000)

                list += Shop(
                    id = shopId,
                    code = code,
                    cityId = city.id,
                    cityName = city.name,
                    logoUrl = city.logoUrl ?: "",
                    street = street,
                    houseNumber = house,
                    addressComment = addressComments.random(rnd) ?: malls.random(rnd),
                    phoneNumber = "+380 67 ${rnd.nextInt(100, 999)} ${rnd.nextInt(10, 99)} ${rnd.nextInt(10, 99)}",
                    status = status,
                    statusComment = buildStatusComment(status),
                    internetProvider = provider,
                    internetProviderPersonalAccount = personalAccounts,
                    internetReplenishmentDay = replenishmentDay,
                    internetReplenishmentAmount = replenishmentAmount,
                    remoteNumber = "REM-${rnd.nextInt(1000, 9999)}",
                    cameraCodes = cameraCodes,
                    lastModified = formatIsoLike(lastModifiedMs),
                    lastModifiedUser = "${lastModUser.firstName} ${lastModUser.lastName}",
                    deviceType = deviceTypesRotation.random(rnd)
                )

                globalIndex++
            }
        }

        return list
    }

    private fun buildSlides(
        shops: List<Shop>,
        slidesPerShopRange: IntRange
    ): List<Slide> {
        val list = mutableListOf<Slide>()

        shops.forEach { shop ->
            val count = rnd.nextInt(slidesPerShopRange.first, slidesPerShopRange.last + 1)
            val basePosition = rnd.nextInt(1, 5)

            repeat(count) { idx ->
                val id = "slide-${shop.id}-$idx"
                val createdMs = nowMs - rnd.nextInt(1, 60) * 24L * 60 * 60 * 1000
                val modifiedMs = nowMs - rnd.nextInt(0, 14) * 24L * 60 * 60 * 1000

                val name = slideNames.random(rnd)
                val ext = imageExt.random(rnd)
                val fileName = "${name}_${shop.code}_${idx + 1}.$ext"

                val modUser = users.random(rnd)
                val isActive = rnd.nextInt(0, 100) >= 15 // ~85% активних

                list += Slide(
                    id = id,
                    fileName = fileName,
                    publicUrl = "https://picsum.photos/seed/${id.hashCode().absoluteValue}/1024/768",
                    shopCodes = listOf(shop.code), // краще один код (або зроби мульти)
                    position = basePosition + idx,
                    isActive = isActive,
                    lastModified = formatUaLike(modifiedMs),
                    lastModifiedUserName = "${modUser.firstName} ${modUser.lastName}",
                    createdAt = formatUaLike(createdMs)
                )
            }
        }

        return list
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun pickStreet(cityName: String): String = when {
        cityName.contains("Київ", ignoreCase = true) -> kyivStreets.random(rnd)
        cityName.contains("Львів", ignoreCase = true) -> lvivStreets.random(rnd)
        cityName.contains("Одеса", ignoreCase = true) -> odesaStreets.random(rnd)
        cityName.contains("Харків", ignoreCase = true) -> kharkivStreets.random(rnd)
        else -> listOf("Центральна", "Шкільна", "Незалежності", "Соборна", "Миру").random(rnd)
    }

    private fun buildStatusComment(status: ShopStatus): String = when (status) {
        ShopStatus.ACTIVE -> listOf("Працює стабільно", "Все ок", "Без інцидентів").random(rnd)
        ShopStatus.RELOCATING -> listOf("Переїзд, можливі перебої", "Монтаж на новій локації").random(rnd)
        ShopStatus.CLOSED -> listOf("Тимчасово зачинено", "Ремонт", "Очікуємо рішення").random(rnd)
        else -> listOf("Статус уточнюється", "Є нюанси", "Потрібна перевірка").random(rnd)
    }

    private fun formatIsoLike(ms: Long): String {
        // легкий “ISO-like” без залежностей: "YYYY-MM-DD HH:mm"
        // (якщо в тебе вже є форматер — краще використовуй його)
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        // Це фейк-формат, але стабільний. Якщо хочеш реальні дати — підключи kotlinx-datetime.
        val day = (days % 28 + 1).toInt().toString().padStart(2, '0')
        val month = ((days / 28) % 12 + 1).toInt().toString().padStart(2, '0')
        val year = (2026).toString()
        val hh = (hours % 24).toInt().toString().padStart(2, '0')
        val mm = (minutes % 60).toInt().toString().padStart(2, '0')
        return "$year-$month-$day $hh:$mm"
    }

    private fun formatUaLike(ms: Long): String {
        // "18:40 01 лютого 2026" — умовно
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        val day = (days % 28 + 1).toInt()
        val monthIdx = ((days / 28) % 12).toInt()
        val year = 2026
        val hh = (hours % 24).toInt().toString().padStart(2, '0')
        val mm = (minutes % 60).toInt().toString().padStart(2, '0')

        val months = listOf(
            "січня", "лютого", "березня", "квітня", "травня", "червня",
            "липня", "серпня", "вересня", "жовтня", "листопада", "грудня"
        )
        return "$hh:$mm ${day.toString().padStart(2, '0')} ${months[monthIdx]} $year"
    }

    private fun <T> List<T>.randomWeighted(
        rnd: Random,
        weight: (T) -> Int
    ): T {
        val weights = this.map { weight(it).coerceAtLeast(0) }
        val total = weights.sum().coerceAtLeast(1)
        var roll = rnd.nextInt(total)
        for (i in indices) {
            roll -= weights[i]
            if (roll < 0) return this[i]
        }
        return first()
    }
}
