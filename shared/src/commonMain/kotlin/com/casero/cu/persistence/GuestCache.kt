package com.casero.cu.persistence

import com.casero.cu.model.Guest
import com.casero.cu.model.PortalDates
import com.casero.cu.model.PortalJson
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import okio.FileSystem
import okio.Path

/**
 * On-disk cache of the last successful guest fetch, so the "Guests" tab can
 * show something without a live connection. Not a general-purpose database —
 * one JSON snapshot, replaced wholesale on every successful reload.
 */
@OptIn(ExperimentalUuidApi::class)
class GuestCache(private val fileSystem: FileSystem, directory: Path, private val clock: () -> Instant = PortalDates::now) {

    data class Snapshot(
        val fetchedAt: Instant,
        val guests: List<Guest>,
        val rangeStart: Instant? = null,
        val rangeEnd: Instant? = null,
    )

    data class CompanionSnapshot(val fetchedAt: Instant, val companions: List<Guest>)

    private val mutex = Mutex()
    private val file = directory / "guests-cache-v2.json"
    private val companionsFile = directory / "companions-cache-v2.json"

    /** Changes on [clear], so a fetch started before it cannot write stale data back. */
    var generation: String = Uuid.random().toString()
        private set

    init {
        fileSystem.createDirectories(directory)
    }

    suspend fun save(guests: List<Guest>, from: Instant? = null, to: Instant? = null, generation: String? = null) =
        mutex.withLock {
            if (generation != null && generation != this.generation) return@withLock
            val json = buildJsonObject {
                put("fetchedAt", clock().toEpochMilliseconds())
                put("guests", JsonArray(guests.map(Guest::toJson)))
                from?.let { put("rangeStart", it.toEpochMilliseconds()) }
                to?.let { put("rangeEnd", it.toEpochMilliseconds()) }
            }
            write(file, json.toString())
        }

    suspend fun load(): Snapshot? = mutex.withLock {
        val json = read(file) as? JsonObject ?: return@withLock null
        try {
            Snapshot(
                fetchedAt = json.instant("fetchedAt") ?: return@withLock null,
                guests = (json["guests"] as? JsonArray ?: return@withLock null).map(Guest::fromJson),
                rangeStart = json.instant("rangeStart"),
                rangeEnd = json.instant("rangeEnd"),
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveCompanions(companions: List<Guest>, stayId: String) = mutex.withLock {
        if (stayId.isEmpty()) return@withLock
        val snapshots = companionSnapshots().toMutableMap()
        snapshots[stayId] = CompanionSnapshot(clock(), companions)
        writeCompanions(snapshots)
    }

    suspend fun loadCompanions(stayId: String): CompanionSnapshot? = mutex.withLock {
        if (stayId.isEmpty()) null else companionSnapshots()[stayId]
    }

    suspend fun removeCompanions(stayId: String) = mutex.withLock {
        val snapshots = companionSnapshots().toMutableMap()
        snapshots.remove(stayId)
        writeCompanions(snapshots)
    }

    suspend fun clear() = mutex.withLock {
        generation = Uuid.random().toString()
        fileSystem.delete(file, mustExist = false)
        fileSystem.delete(companionsFile, mustExist = false)
    }

    /** Whether a stored snapshot is past [ttl] (`null` never expires). */
    fun isStale(fetchedAt: Instant, ttl: Duration?): Boolean = ttl != null && clock() - fetchedAt > ttl

    private fun companionSnapshots(): Map<String, CompanionSnapshot> {
        val json = read(companionsFile) as? JsonObject ?: return emptyMap()
        return json.mapNotNull { (stayId, value) ->
            val entry = value as? JsonObject ?: return@mapNotNull null
            try {
                val fetchedAt = entry.instant("fetchedAt") ?: return@mapNotNull null
                val companions = (entry["companions"] as? JsonArray ?: return@mapNotNull null).map(Guest::fromJson)
                stayId to CompanionSnapshot(fetchedAt, companions)
            } catch (_: Exception) {
                null
            }
        }.toMap()
    }

    private fun writeCompanions(snapshots: Map<String, CompanionSnapshot>) {
        val json = JsonObject(
            snapshots.mapValues { (_, snapshot) ->
                buildJsonObject {
                    put("fetchedAt", snapshot.fetchedAt.toEpochMilliseconds())
                    put("companions", JsonArray(snapshot.companions.map(Guest::toJson)))
                }
            },
        )
        write(companionsFile, json.toString())
    }

    private fun read(path: Path) =
        try {
            PortalJson.parseToJsonElement(fileSystem.read(path) { readUtf8() })
        } catch (_: Exception) {
            null
        }

    /** Writes to a temporary file and moves it into place, so a crash never leaves half a snapshot. */
    private fun write(path: Path, text: String) {
        val temporary = path.parent!! / "${path.name}.tmp"
        try {
            fileSystem.write(temporary) { writeUtf8(text) }
            fileSystem.atomicMove(temporary, path)
        } catch (_: Exception) {
            fileSystem.delete(temporary, mustExist = false)
        }
    }

    private fun JsonObject.instant(key: String): Instant? =
        (this[key] as? JsonPrimitive)?.longOrNull?.let(Instant::fromEpochMilliseconds)
}
