package com.aquagrow.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.aquagrow.data.local.dao.PhotoDao
import com.aquagrow.data.mapper.toDomain
import com.aquagrow.data.mapper.toEntity
import com.aquagrow.data.photo.PhotoStorage
import com.aquagrow.domain.model.Photo
import com.aquagrow.domain.repository.CaptureTarget
import com.aquagrow.domain.repository.PhotoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val dao: PhotoDao,
    private val storage: PhotoStorage,
) : PhotoRepository {
    override fun observeAll() = dao.observeAll().map { l -> l.map { it.toDomain() } }
    override fun observeForPlant(plantId: Long) = dao.observeForPlant(plantId).map { l -> l.map { it.toDomain() } }
    override fun observeForAquarium(aquariumId: Long) = dao.observeForAquarium(aquariumId).map { l -> l.map { it.toDomain() } }
    override fun observeForFish(fishId: Long) = dao.observeForFish(fishId).map { l -> l.map { it.toDomain() } }

    override suspend fun importPhoto(uri: String): String = storage.importFromUri(uri.toUri())

    override fun newCaptureTarget(): CaptureTarget {
        val (file, relative) = storage.newCaptureFile()
        return CaptureTarget(file.absolutePath, relative)
    }

    override suspend fun addPhoto(photo: Photo): Long = dao.insert(photo.toEntity())

    override suspend fun deletePhoto(photo: Photo) {
        dao.delete(photo.toEntity())
        storage.delete(photo.filePath)
    }

    override suspend fun discardFile(relativePath: String) = storage.delete(relativePath)
}
