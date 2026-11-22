package com.casero.cu.feature.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.casero.cu.app.AppState
import com.casero.cu.platform.SecureStore
import com.casero.cu.resources.Res
import com.casero.cu.resources.use_8100_characters_with_uppercase_lowercase_a
import com.casero.cu.ui.UiText
import com.casero.cu.ui.toUiText
import kotlin.coroutines.cancellation.CancellationException

class LoginViewModel(private val app: AppState) {
    var user by mutableStateOf("")
    var password by mutableStateOf("")
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<UiText?>(null)
        private set

    init {
        if (app.settings.rememberCredentials.value) {
            app.secureStore.get(SecureStore.USERNAME)?.let { user = it }
        }
    }

    val validationMessage: UiText?
        get() {
            if (password.isEmpty()) return null
            if (password.length !in 8..100 || !PASSWORD_RULE.matches(password)) {
                return UiText.of(Res.string.use_8100_characters_with_uppercase_lowercase_a)
            }
            return null
        }

    val canSubmit: Boolean
        get() = user.isNotBlank() && password.length in 8..100 && validationMessage == null && !isLoading

    /** Returns `true` on a successful sign-in. */
    suspend fun submit(): Boolean {
        if (!canSubmit) return false
        isLoading = true
        errorMessage = null
        val trimmedUser = user.trim()
        return try {
            app.client.login(trimmedUser, password)
            app.guestCache.clear()
            app.photoCache.clear()
            if (app.settings.rememberCredentials.value) {
                app.secureStore.set(SecureStore.USERNAME, trimmedUser)
                app.secureStore.set(SecureStore.PASSWORD, password)
            }
            true
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.toUiText()
            false
        } finally {
            isLoading = false
        }
    }

    private companion object {
        val PASSWORD_RULE = Regex("""^(?=.*[A-Z])(?=.*\W)(?=.*[0-9])(?=.*[a-z]).*$""")
    }
}
