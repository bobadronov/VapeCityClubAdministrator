package org.bigblackowl.vccadmin.di

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.dsl.module

val logger = module {
    Napier.base(DebugAntilog())
}