package com.aquagrow.data.seed

import com.aquagrow.data.local.dao.NotificationSettingsDao
import com.aquagrow.data.local.dao.SowingGuideDao
import com.aquagrow.data.local.entity.NotificationSettingsEntity
import com.aquagrow.data.local.entity.SowingGuideEntity
import com.aquagrow.domain.catalog.PlantCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Inserts reference data (not user data) on first launch: the editable seasonal sowing
 * guide and the default notification settings row. Safe to run on every start.
 */
@Singleton
class ReferenceDataSeeder @Inject constructor(
    private val sowingGuideDao: SowingGuideDao,
    private val settingsDao: NotificationSettingsDao,
) {
    suspend fun seedIfNeeded() {
        if (sowingGuideDao.count() == 0) {
            sowingGuideDao.insertAll(
                PlantCatalog.all.flatMap { plant ->
                    plant.seasons.map { w ->
                        SowingGuideEntity(
                            catalogKey = plant.key,
                            plantName = plant.name,
                            season = w.season,
                            startMonth = w.startMonth,
                            endMonth = w.endMonth,
                            notes = null,
                        )
                    }
                },
            )
        }
        if (settingsDao.get() == null) settingsDao.upsert(NotificationSettingsEntity())
    }
}
