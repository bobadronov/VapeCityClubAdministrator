package org.bigblackowl.vccadmin.di

import org.bigblackowl.vccadmin.data.repository.OtaUpdateRepositoryImpl
import org.bigblackowl.vccadmin.data.utils.OtaDownloader
import org.bigblackowl.vccadmin.domain.repository.OtaUpdateRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val otaModule = module {
    single<OtaUpdateRepository>(createdAtStart = true) { OtaUpdateRepositoryImpl(get()) }
    singleOf(::OtaDownloader)
}