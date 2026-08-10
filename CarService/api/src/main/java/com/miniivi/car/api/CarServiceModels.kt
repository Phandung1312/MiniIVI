package com.miniivi.car.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object CarServiceContract {
    const val API_VERSION = 1
    const val SERVICE_PACKAGE = "com.miniivi.car.service"
    const val SERVICE_CLASS = "com.miniivi.car.service.MiniIviCarService"
    const val CONTROL_PERMISSION = "com.miniivi.car.permission.CONTROL"
}

object FeatureStatus {
    const val CONNECTING = 0
    const val READY = 1
    const val UNAVAILABLE = 2
    const val ERROR = 3
}

object CarServiceError {
    const val NONE = 0
    const val INVALID_ARGUMENT = 1
    const val PLATFORM_UNAVAILABLE = 2
    const val PLATFORM_OPERATION_FAILED = 3
    const val INCOMPATIBLE_API = 4
}

object HvacZone {
    const val LEFT = 0
    const val RIGHT = 1
}

@Parcelize
data class BrightnessState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val progress: Float = 0.5f,
    val automatic: Boolean = false,
    val errorCode: Int = 0,
    val diagnosticMessage: String? = null,
) : Parcelable

@Parcelize
data class AudioState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val volume: Int = 0,
    val minimum: Int = 0,
    val maximum: Int = 1,
    val errorCode: Int = 0,
    val diagnosticMessage: String? = null,
) : Parcelable

@Parcelize
data class HvacZoneState(
    val zone: Int,
    val available: Boolean = false,
    val hasTemperature: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val minimumCelsius: Float = 16f,
    val maximumCelsius: Float = 30f,
) : Parcelable

@Parcelize
data class HvacState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val hasCabinTemperature: Boolean = false,
    val cabinTemperatureCelsius: Float = 0f,
    val leftZone: HvacZoneState? = null,
    val rightZone: HvacZoneState? = null,
    val acAvailable: Boolean = false,
    val acOn: Boolean = false,
    val errorCode: Int = 0,
    val diagnosticMessage: String? = null,
) : Parcelable
