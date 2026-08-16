package com.android.car.systemui.data.session

import com.android.car.systemui.domain.repository.CarServiceSession
import com.miniivi.car.client.MiniIviCarClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CarServiceSessionImpl @Inject constructor(
    private val client: MiniIviCarClient,
) : CarServiceSession {
    private var started = false

    @Synchronized
    override fun start() {
        if (started) return
        client.start()
        started = true
    }

    @Synchronized
    override fun stop() {
        if (!started) return
        client.close()
        started = false
    }
}
