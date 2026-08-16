package com.android.car.launcher.feature.media.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class MediaRefreshCoordinator(
    scope: CoroutineScope,
    private val debounceMillis: Long,
    private val refresh: suspend () -> Unit,
) {
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val job = scope.launch {
        for (request in requests) {
            delay(debounceMillis)
            refresh()
        }
    }

    fun requestRefresh() {
        requests.trySend(Unit)
    }

    fun close() {
        requests.close()
        job.cancel()
    }
}
