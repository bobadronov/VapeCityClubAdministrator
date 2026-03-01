package org.bigblackowl.vccadmin.di

import org.bigblackowl.vccadmin.ui.workSchedule.view.WorkScheduleViewScreenViewModel
import org.bigblackowl.vccadmin.ui.addEditShop.ShopAddEditScreenViewModel
import org.bigblackowl.vccadmin.ui.addEditSlideScreen.AddEditSlideViewModel
import org.bigblackowl.vccadmin.ui.city.addEdit.AddEditCityScreenViewModel
import org.bigblackowl.vccadmin.ui.city.list.CitiesListScreenViewModel
import org.bigblackowl.vccadmin.ui.editSlidesSettings.EditSlidesSettingsScreenViewModel
import org.bigblackowl.vccadmin.ui.fileGenerator.FileGeneratorScreenViewModel
import org.bigblackowl.vccadmin.ui.login.LoginScreenViewModel
import org.bigblackowl.vccadmin.ui.main.MainScreenViewModel
import org.bigblackowl.vccadmin.ui.settings.SettingsScreenViewModel
import org.bigblackowl.vccadmin.ui.shopDetail.ShopDetailsScreenViewModel
import org.bigblackowl.vccadmin.ui.slideAiGeneration.SlideAiGenerationScreenViewModel
import org.bigblackowl.vccadmin.ui.slidesList.SlidesListScreenViewModel
import org.bigblackowl.vccadmin.ui.users.addEdit.AddEditUserScreenViewModel
import org.bigblackowl.vccadmin.ui.users.detail.UserDetailScreenViewModel
import org.bigblackowl.vccadmin.ui.users.list.UsersScreenViewModel
import org.bigblackowl.vccadmin.ui.workSchedule.create.WorkScheduleCreateScreenViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val screensViewModelModule = module {
    singleOf(::LoginScreenViewModel)
    singleOf(::MainScreenViewModel)
    singleOf(::ShopDetailsScreenViewModel)
    singleOf(::SlidesListScreenViewModel)
    singleOf(::AddEditSlideViewModel)
    singleOf(::ShopAddEditScreenViewModel)
    singleOf(::CitiesListScreenViewModel)
    singleOf(::AddEditCityScreenViewModel)
    singleOf(::UsersScreenViewModel)
    singleOf(::UserDetailScreenViewModel)
    singleOf(::AddEditUserScreenViewModel)
    singleOf(::FileGeneratorScreenViewModel)
    singleOf(::EditSlidesSettingsScreenViewModel)
    singleOf(::SlideAiGenerationScreenViewModel)
    singleOf(::SettingsScreenViewModel)
    singleOf(::WorkScheduleViewScreenViewModel)
    singleOf(::WorkScheduleCreateScreenViewModel) // di

}