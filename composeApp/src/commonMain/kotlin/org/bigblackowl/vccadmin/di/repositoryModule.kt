package org.bigblackowl.vccadmin.di

import org.bigblackowl.vccadmin.data.repository.AiRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.AuthRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.CityRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.CitySearchRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.FileGeneratorRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.LocalRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.ShopRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.SlideRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.UserRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.WorkScheduleRepositoryImpl
import org.bigblackowl.vccadmin.domain.repository.AiRepository
import org.bigblackowl.vccadmin.domain.repository.AuthRepository
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.CitySearchRepository
import org.bigblackowl.vccadmin.domain.repository.FileGeneratorRepository
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.SlideRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.bigblackowl.vccadmin.domain.repository.WorkScheduleRepository
import org.koin.dsl.module

val repositoryModule = module {
    single<LocalRepository>(createdAtStart = true) { LocalRepositoryImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<ShopRepository> { ShopRepositoryImpl(get()) }
    single<CityRepository> { CityRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<SlideRepository> { SlideRepositoryImpl(get()) }
    single<FileGeneratorRepository> { FileGeneratorRepositoryImpl(get()) }
    single<AiRepository> { AiRepositoryImpl(get(), get()) }
    single<CitySearchRepository> { CitySearchRepositoryImpl(json = get()) }
    single<WorkScheduleRepository> { WorkScheduleRepositoryImpl(get(), get()) }
}