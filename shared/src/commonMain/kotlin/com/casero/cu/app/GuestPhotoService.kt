package com.casero.cu.app

import com.casero.cu.model.Guest
import com.casero.cu.network.PortalServing
import com.casero.cu.persistence.AppSettings
import com.casero.cu.persistence.PhotoCache

/** Guest photos from the portal, through the on-disk cache when the host keeps photos. */
class GuestPhotoService(
    private val client: PortalServing,
    private val settings: AppSettings,
    private val cache: PhotoCache,
) {
    /** Encoded image bytes. Throws the portal's error when the photo cannot be fetched. */
    suspend fun image(guest: Guest, isCompanion: Boolean = false, forceRefresh: Boolean = false): ByteArray {
        val key = cacheKey(guest, isCompanion)
        if (!forceRefresh && settings.saveImages.value) {
            cache.data(key, settings.cacheTTL.value.interval)?.let { return it }
        }
        val generation = cache.generation
        // Known issue (AGENTS.md): a Cuban found by passport is asked for through the CI endpoint.
        val data = if (isCompanion || guest.nationality?.uppercase() == "CUBA") {
            client.fetchCompanionPhoto(guest.identificador)
        } else {
            client.fetchForeignGuestPhoto(
                passport = guest.identificador,
                name = guest.fullName,
                nationalityCode = guest.nationalityCode ?: "",
                sex = guest.sex ?: "",
            )
        }
        if (settings.saveImages.value) cache.store(data, key, generation)
        return data
    }

    suspend fun cachedImage(guest: Guest, isCompanion: Boolean = false): ByteArray? {
        if (!settings.saveImages.value) return null
        return cache.data(cacheKey(guest, isCompanion), settings.cacheTTL.value.interval)
    }

    private fun cacheKey(guest: Guest, isCompanion: Boolean): String =
        "casero-photo://guest?id=${guest.identificador}&nationality=${guest.nationalityCode ?: ""}" +
            "&kind=${if (isCompanion) "companion" else "guest"}"
}
