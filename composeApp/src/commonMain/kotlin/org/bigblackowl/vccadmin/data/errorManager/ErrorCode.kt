package org.bigblackowl.vccadmin.data.errorManager


object ErrorCode {
    object LoginScreenViewModel {
        const val LOGIN = 1000
    }

    object ShopDetailsScreenViewModel {
        const val LOAD_SHOP = 3000
    }

    object ShopAddEditScreenViewModel {
        const val LOAD_SHOP_DETAILS = 3100
        const val DELETE_SHOP = 3101
        const val SAVE_SHOP = 3102
    }

    object MainScreenViewModel {
        const val LOAD_DATA = 3200
    }

    object SlidesListScreenViewModel {
        const val LOAD_SLIDES = 4000
        const val TOGGLE_SLIDE_VISIBILITY = 4001
    }

    object AddEditSlideViewModel {
        const val OPEN_FILE = 4100
        const val DOWNLOAD_IMAGE = 4101
        const val SELECT_FILE = 4102
        const val LOAD_SLIDE = 4103
        const val DELETE_SLIDE = 4104
        const val SAVE_NEW_SLIDE = 4105
        const val UPDATE_EXISTING_SLIDE = 4106
    }

    object EditSlidesSettingsScreenViewModel {
        const val LOAD_DATA = 4200
        const val REFRESH = 4201
        const val SAVE_DATA = 4202
    }

    object SlideAiGenerationScreenViewModel {
        const val LOAD_DATA = 4300
        const val GENERATE = 4300
    }

    object CitiesListScreenViewModel {
        const val LOAD_CITIES = 5000
    }

    object AddEditCityScreenViewModel {
        const val SAVE_CHANGES = 5100
        const val GET_CITY = 5101
        const val DELETE_CITY = 5102
    }

    object UsersScreenViewModel {
        const val LOAD_USERS = 6000
    }

    object UserDetailScreenViewModel {
        const val LOAD_USER = 6100
    }

    object AddEditUserScreenViewModel {
        const val SAVE_USER = 6200
        const val LOAD_USER = 6201
        const val DELETE_USER = 6202
    }

    object FileGeneratorScreenViewModel {
        const val LOAD_DATA = 7000
        const val OPEN_FILE = 7001
        const val FILE_GENERATION = 7002
    }


}

