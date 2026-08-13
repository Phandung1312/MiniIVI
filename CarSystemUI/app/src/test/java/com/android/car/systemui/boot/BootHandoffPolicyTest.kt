package com.android.car.systemui.boot

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootHandoffPolicyTest {
    @Test
    fun handoffStartsOnlyForLockedBootOnTheSystemUser() {
        assertTrue(BootUserPolicy.shouldShowHandoff(Intent.ACTION_LOCKED_BOOT_COMPLETED, 1_000))
        assertFalse(BootUserPolicy.shouldShowHandoff(Intent.ACTION_BOOT_COMPLETED, 1_000))
        assertFalse(BootUserPolicy.shouldShowHandoff(Intent.ACTION_LOCKED_BOOT_COMPLETED, 1_001_000))
    }

    @Test
    fun launcherReadyIsRelayedOnlyFromForegroundUsers() {
        assertFalse(BootUserPolicy.shouldRelayLauncherReady(1_000))
        assertTrue(BootUserPolicy.shouldRelayLauncherReady(1_001_000))
    }

    @Test
    fun stateMachineMakesShowAndDismissIdempotent() {
        val stateMachine = BootHandoffStateMachine()

        assertTrue(stateMachine.requestShow())
        assertFalse(stateMachine.requestShow())
        assertEquals(BootHandoffStateMachine.State.Visible, stateMachine.state)
        assertTrue(stateMachine.requestDismiss())
        assertFalse(stateMachine.requestDismiss())
        assertEquals(BootHandoffStateMachine.State.Dismissing, stateMachine.state)
        stateMachine.completeRemoval()
        assertEquals(BootHandoffStateMachine.State.Hidden, stateMachine.state)
        assertTrue(stateMachine.requestShow())
    }

    @Test
    fun timingConstantsMatchTheBootHandoffContract() {
        assertEquals(250L, BootHandoffService.FADE_OUT_DURATION_MILLIS)
        assertEquals(30_000L, BootHandoffService.FAIL_SAFE_TIMEOUT_MILLIS)
    }
}
