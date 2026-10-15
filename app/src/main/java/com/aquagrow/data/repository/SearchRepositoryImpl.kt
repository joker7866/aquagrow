package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.AquariumDao
import com.aquagrow.data.local.dao.FishDao
import com.aquagrow.data.local.dao.GardenLogDao
import com.aquagrow.data.local.dao.PlantDao
import com.aquagrow.data.local.dao.TaskDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.domain.repository.SearchRepository
import com.aquagrow.domain.repository.SearchResults
import javax.inject.Inject

/** LIKE-based search across the main tables. Wildcards in the query are escaped by trimming them. */
class SearchRepositoryImpl @Inject constructor(
    private val plants: PlantDao,
    private val fish: FishDao,
    private val tanks: AquariumDao,
    private val tasks: TaskDao,
    private val logs: GardenLogDao,
) : SearchRepository {
    override suspend fun search(query: String): SearchResults {
        val q = query.trim().replace("%", "").replace("_", " ")
        if (q.length < 2) return SearchResults()
        return SearchResults(
            plants = plants.search(q).map { it.toDomain() },
            fish = fish.search(q).map { it.toDomain() },
            tanks = tanks.search(q).map { it.toDomain() },
            tasks = tasks.search(q).map { it.toDomain() },
            notes = logs.searchGrowthNotes(q).map { it.toDomain() },
        )
    }
}
