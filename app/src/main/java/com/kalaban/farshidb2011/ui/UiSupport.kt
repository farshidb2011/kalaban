package com.kalaban.farshidb2011.ui

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kalaban.farshidb2011.KalabanApp
import com.kalaban.farshidb2011.R
import com.kalaban.farshidb2011.domain.Actor
import com.kalaban.farshidb2011.domain.Permission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class UiMessage(@StringRes val res: Int, val args: List<Any> = emptyList(), val error: Boolean = false)

/** App-wide snackbar channel; the root composable collects and shows messages. */
object UiMessages {
    private val flow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 16)
    val messages: SharedFlow<UiMessage> = flow.asSharedFlow()

    fun post(@StringRes res: Int, vararg args: Any, error: Boolean = false) {
        flow.tryEmit(UiMessage(res, args.toList(), error))
    }
}

/**
 * Base for screens that mutate data: permission checks happen here (the business boundary) rather
 * than only by hiding UI, and unexpected failures surface as messages instead of crashing the app.
 */
abstract class GuardedViewModel(application: Application) : AndroidViewModel(application) {
    protected val app: KalabanApp get() = KalabanApp.instance
    protected val session get() = app.session
    protected val actor: Actor get() = session.actor

    fun can(permission: Permission): Boolean = session.has(permission)

    protected fun allowed(permission: Permission): Boolean {
        if (session.has(permission)) return true
        UiMessages.post(R.string.permission_denied, error = true)
        return false
    }

    protected fun launchSafely(block: suspend () -> Unit): Job = viewModelScope.launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiMessages.post(R.string.operation_failed, e.message ?: e.javaClass.simpleName, error = true)
        }
    }
}

inline fun <reified VM : ViewModel> appViewModelFactory(crossinline create: () -> VM): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
    }
