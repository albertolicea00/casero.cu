package com.casero.cu.platform

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Minimal Keychain wrapper for the user's own portal credentials. Same service
 * and accounts as the native iOS app, so saved credentials carry over.
 *
 * Items are readable only while the device is unlocked, and never restored to
 * another device from a backup. See SECURITY.md, "Saved credentials".
 */
internal object KeychainStore : SecureStore {
    private const val SERVICE = "com.casero.cu.portal"

    override fun set(key: String, value: String) {
        remove(key)
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { CFDataCreate(null, it.addressOf(0).reinterpret(), bytes.size.toLong()) }
        query(key) {
            add(kSecValueData, data)
            add(kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        }.use { SecItemAdd(it, null) }
        data?.let(::CFRelease)
    }

    /**
     * Re-protects items saved by earlier builds with `AfterFirstUnlock`, which
     * stayed readable while locked and travelled in device backups.
     */
    fun migrateAccessibility() {
        for (key in listOf(SecureStore.USERNAME, SecureStore.PASSWORD)) {
            val update = dictionary { add(kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly) }
            query(key).use { SecItemUpdate(it, update) }
            CFRelease(update)
        }
    }

    override fun get(key: String): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = query(key) {
            add(kSecReturnData, kCFBooleanTrue)
            add(kSecMatchLimit, kSecMatchLimitOne)
        }.use { SecItemCopyMatching(it, result.ptr) }
        val data: CFDataRef = result.value?.reinterpret() ?: return@memScoped null
        try {
            if (status != errSecSuccess) return@memScoped null
            val length = CFDataGetLength(data).toInt()
            if (length == 0) "" else CFDataGetBytePtr(data)?.readBytes(length)?.decodeToString()
        } finally {
            CFRelease(data)
        }
    }

    override fun remove(key: String) {
        query(key).use { SecItemDelete(it) }
    }

    private fun query(key: String, extra: CFMutableDictionaryRef.() -> Unit = {}): CFMutableDictionaryRef =
        dictionary {
            add(kSecClass, kSecClassGenericPassword)
            addString(kSecAttrService, SERVICE)
            addString(kSecAttrAccount, key)
            extra()
        }

    private fun dictionary(build: CFMutableDictionaryRef.() -> Unit): CFMutableDictionaryRef {
        val dictionary = CFDictionaryCreateMutable(
            null,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )!!
        dictionary.build()
        return dictionary
    }

    private fun CFMutableDictionaryRef.add(key: COpaquePointer?, value: COpaquePointer?) =
        CFDictionaryAddValue(this, key, value)

    private fun CFMutableDictionaryRef.addString(key: COpaquePointer?, value: String) {
        val string = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)
        CFDictionaryAddValue(this, key, string)
        string?.let(::CFRelease)
    }

    private inline fun <T> CFMutableDictionaryRef.use(block: (CFMutableDictionaryRef) -> T): T =
        try {
            block(this)
        } finally {
            CFRelease(this)
        }
}
