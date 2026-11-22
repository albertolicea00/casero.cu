package com.casero.cu.persistence

import com.casero.cu.model.PortalDates
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.ByteString.Companion.encodeUtf8
import okio.FileSystem
import okio.Path

/**
 * Disk cache for guest photos (`ObtenerFotoDiie` / `ObtenerFotoSUINPorCi`).
 *
 * Cached by default; `AppSettings.saveImages == false` disables writes, and the
 * existing cache is cleared alongside the guest cache.
 */
@OptIn(ExperimentalUuidApi::class)
class PhotoCache(private val fileSystem: FileSystem, base: Path, private val clock: () -> Instant = PortalDates::now) {

    private val mutex = Mutex()
    private val directory = base / "guest-photos"

    var generation: String = Uuid.random().toString()
        private set

    init {
        fileSystem.createDirectories(directory)
    }

    /** The cached bytes for [key], or `null`. An entry older than [ttl] is deleted. */
    suspend fun data(key: String, ttl: Duration? = null): ByteArray? = mutex.withLock {
        val file = fileFor(key)
        if (ttl != null) {
            val modified = fileSystem.metadataOrNull(file)?.lastModifiedAtMillis?.let(Instant::fromEpochMilliseconds)
            if (modified == null || clock() - modified > ttl) {
                fileSystem.delete(file, mustExist = false)
                return@withLock null
            }
        }
        try {
            fileSystem.read(file) { readByteArray() }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun store(data: ByteArray, key: String, generation: String? = null) = mutex.withLock {
        if (generation != null && generation != this.generation) return@withLock
        try {
            fileSystem.write(fileFor(key)) { write(data) }
        } catch (_: Exception) {
        }
    }

    suspend fun clear() = mutex.withLock {
        generation = Uuid.random().toString()
        fileSystem.deleteRecursively(directory, mustExist = false)
        fileSystem.createDirectories(directory)
    }

    private fun fileFor(key: String): Path = directory / key.encodeUtf8().sha256().hex()
}
