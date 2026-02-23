package org.bigblackowl.vccadmin.domain.repository

import org.bigblackowl.vccadmin.data.entity.AdminAppUpdate

interface OtaUpdateRepository {
    suspend fun fetchLatestManifest(): AdminAppUpdate?
}