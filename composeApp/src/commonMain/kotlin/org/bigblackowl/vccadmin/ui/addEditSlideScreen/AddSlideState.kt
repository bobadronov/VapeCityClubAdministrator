package org.bigblackowl.vccadmin.ui.addEditSlideScreen

import io.github.vinceglb.filekit.PlatformFile
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.entity.Shop

data class AddSlideState(
    val isLoading: Boolean = false,
    val slideId: String? = null,
    val fileName: String = "",
    val fileNameError: String? = null,
    val fileNameHint: String? = null,

    val selectedShopCodes: Set<String> = emptySet(),
    val allShopList: List<Shop> = emptyList(),

    // derived selections
    val allCodes: Set<String> = emptySet(),
    val tabletCodes: Set<String> = emptySet(),
    val tvCodes: Set<String> = emptySet(),
    val isAllSelected: Boolean = false,
    val isAllTabletSelected: Boolean = false,
    val isAllTvSelected: Boolean = false,

    val isActive: Boolean = true,
    val selectedFile: PlatformFile? = null,
    val currentImageUrl: String? = null,
    val isFileDownloaded: Boolean? = null,
    val cities: List<City> = emptyList(),
)
