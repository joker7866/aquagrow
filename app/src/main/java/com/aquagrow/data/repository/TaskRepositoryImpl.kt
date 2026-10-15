package com.aquagrow.data.repository

import com.aquagrow.data.local.dao.TaskDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.model.OccurrenceState
import com.aquagrow.domain.model.TaskOccurrence
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.schedule.OccurrenceResolver
import com.aquagrow.domain.time.TimeProvider
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val time: TimeProvider,
    private val reminders: ReminderScheduler,
) : TaskRepository {

    private val activeTasks: Flow<List<CareTask>> =
        taskDao.observeActive().map { rows -> rows.map { it.toDomain() } }

    override fun observeTasks() = activeTasks
    override fun observeTasksForPlant(plantId: Long) = taskDao.observeForPlant(plantId).map { l -> l.map { it.toDomain() } }
    override fun observeTasksForAquarium(aquariumId: Long) = taskDao.observeForAquarium(aquariumId).map { l -> l.map { it.toDomain() } }

    override fun observeOccurrences(from: LocalDate, to: LocalDate): Flow<List<TaskOccurrence>> = combine(activeTasks, taskDao.observeStates(from, to)) { tasks, states ->
        OccurrenceResolver.resolve(tasks, states.map { it.toDomain() }, from, to)
    }

    override fun observeOverdue(today: LocalDate): Flow<List<TaskOccurrence>> = combine(activeTasks, taskDao.observeStates(today.minusDays(OccurrenceResolver.OVERDUE_LOOKBACK_DAYS), today)) { tasks, states ->
        OccurrenceResolver.overdue(tasks, states.map { it.toDomain() }, today)
    }

    override suspend fun getOccurrences(from: LocalDate, to: LocalDate): List<TaskOccurrence> = OccurrenceResolver.resolve(
        taskDao.getActive().map { it.toDomain() },
        taskDao.getStates(from, to).map { it.toDomain() },
        from,
        to,
    )

    override suspend fun getOverdue(today: LocalDate): List<TaskOccurrence> = OccurrenceResolver.overdue(
        taskDao.getActive().map { it.toDomain() },
        taskDao.getStates(today.minusDays(OccurrenceResolver.OVERDUE_LOOKBACK_DAYS), today).map { it.toDomain() },
        today,
    )

    override suspend fun getTask(id: Long) = taskDao.getWithSubject(id)?.toDomain()
    override suspend fun getTasksForPlant(plantId: Long) = taskDao.getForPlant(plantId).map { it.toDomain() }
    override suspend fun getTasksForAquarium(aquariumId: Long) = taskDao.getForAquarium(aquariumId).map { it.toDomain() }
    override suspend fun getTasksForExport() = taskDao.getActive().map { it.toDomain() }

    override suspend fun saveTask(task: CareTask): Long {
        val now = time.now()
        val createdAt = if (task.id == 0L) now else taskDao.getById(task.id)?.createdAt ?: now
        val id = taskDao.upsert(task.toEntity(createdAt, now)).let { if (it == -1L) task.id else it }
        reminders.requestReschedule()
        return id
    }

    override suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
        reminders.requestReschedule()
    }

    override suspend fun getState(taskId: Long, originalDate: LocalDate) = taskDao.getState(taskId, originalDate)?.toDomain()

    override suspend fun putState(state: OccurrenceState) {
        taskDao.putState(state.toEntity())
        reminders.requestReschedule()
    }

    override suspend fun clearState(taskId: Long, originalDate: LocalDate) {
        taskDao.deleteState(taskId, originalDate)
        reminders.requestReschedule()
    }

    override fun observeHistory(taskId: Long, limit: Int) = taskDao.observeStatesForTask(taskId, limit).map { l -> l.map { it.toDomain() } }
}
