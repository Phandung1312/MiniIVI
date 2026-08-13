package com.android.car.systemui.boot

internal class BootHandoffStateMachine {
    internal enum class State { Hidden, Visible, Dismissing }

    var state: State = State.Hidden
        private set

    fun requestShow(): Boolean {
        if (state != State.Hidden) return false
        state = State.Visible
        return true
    }

    fun requestDismiss(): Boolean {
        if (state != State.Visible) return false
        state = State.Dismissing
        return true
    }

    fun completeRemoval() {
        state = State.Hidden
    }
}
