package com.miniivi.car.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

object CarServiceContract {
    const val API_VERSION = 4
    const val MIN_COMPATIBLE_API_VERSION = 3
    const val SERVICE_PACKAGE = "com.miniivi.car.service"
    const val SERVICE_CLASS = "com.miniivi.car.service.MiniIviCarService"
    const val CONTROL_PERMISSION = "com.miniivi.car.permission.CONTROL"
}

object CarFeature {
    const val BRIGHTNESS = 1 shl 0
    const val AUDIO = 1 shl 1
    const val HVAC = 1 shl 2
    const val VEHICLE_STATUS = 1 shl 3
    const val QUICK_CONTROLS = 1 shl 4
    const val BLUETOOTH = 1 shl 5
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

object ClimateFanDirection {
    const val FACE = 1
    const val FEET = 2
    const val FACE_AND_FEET = 3
    const val DEFROST = 4
}

object ClimateWindow {
    const val FRONT = 0
    const val REAR = 1
}

object TemperatureUnit {
    const val CELSIUS = 0
    const val FAHRENHEIT = 1
}

object ClimateCapability {
    const val POWER = 1L shl 0
    const val AC = 1L shl 1
    const val AUTO = 1L shl 2
    const val SYNC = 1L shl 3
    const val RECIRCULATION = 1L shl 4
    const val FAN_SPEED = 1L shl 5
    const val FAN_DIRECTION = 1L shl 6
    const val FRONT_DEFROST = 1L shl 7
    const val REAR_DEFROST = 1L shl 8
    const val SEAT_HEATING = 1L shl 9
    const val SEAT_VENTILATION = 1L shl 10
    const val MAX_AC = 1L shl 11
    const val MAX_DEFROST = 1L shl 12
    const val AUTO_RECIRCULATION = 1L shl 13
    const val STEERING_WHEEL_HEAT = 1L shl 14
    const val TEMPERATURE_UNIT = 1L shl 15
}

object QuickControl {
    const val WIFI = 0
    const val BLUETOOTH = 1
    const val HOTSPOT = 2
    const val VALET_MODE = 3

    const val WIFI_CAPABILITY = 1L shl WIFI
    const val BLUETOOTH_CAPABILITY = 1L shl BLUETOOTH
    const val HOTSPOT_CAPABILITY = 1L shl HOTSPOT
    const val SCREEN_OFF_CAPABILITY = 1L shl 4
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

@Parcelize
data class ClimateZoneControlState(
    val zone: Int,
    val temperatureCelsius: Float = 22f,
    val minimumCelsius: Float = 16f,
    val maximumCelsius: Float = 30f,
    val fanSpeed: Int = 4,
    val minimumFanSpeed: Int = 0,
    val maximumFanSpeed: Int = 7,
    val fanDirection: Int = ClimateFanDirection.FACE,
    val availableFanDirections: IntArray = intArrayOf(
        ClimateFanDirection.FACE,
        ClimateFanDirection.FEET,
        ClimateFanDirection.FACE_AND_FEET,
    ),
    val seatHeatingLevel: Int = 0,
    val maximumSeatHeatingLevel: Int = 3,
    val seatVentilationLevel: Int = 0,
    val maximumSeatVentilationLevel: Int = 3,
) : Parcelable

@Parcelize
data class ClimateControlState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val powerOn: Boolean = true,
    val acOn: Boolean = true,
    val autoOn: Boolean = true,
    val syncOn: Boolean = false,
    val recirculationOn: Boolean = false,
    val hasCabinTemperature: Boolean = false,
    val cabinTemperatureCelsius: Float = 25f,
    val driverZone: ClimateZoneControlState = ClimateZoneControlState(HvacZone.LEFT),
    val passengerZone: ClimateZoneControlState = ClimateZoneControlState(
        zone = HvacZone.RIGHT,
        temperatureCelsius = 22.5f,
    ),
    val frontDefrostOn: Boolean = false,
    val rearDefrostOn: Boolean = false,
    val maxAcOn: Boolean = false,
    val maxDefrostOn: Boolean = false,
    val autoRecirculationOn: Boolean = false,
    val steeringWheelHeatLevel: Int = 0,
    val maximumSteeringWheelHeatLevel: Int = 3,
    val temperatureUnit: Int = TemperatureUnit.CELSIUS,
    val realCapabilities: Long = 0L,
    val errorCode: Int = CarServiceError.NONE,
    val diagnosticMessage: String? = null,
) : Parcelable

@Parcelize
data class QuickControlsState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val wifiEnabled: Boolean = false,
    val bluetoothEnabled: Boolean = false,
    val hotspotEnabled: Boolean = false,
    val valetModeEnabled: Boolean = false,
    val realCapabilities: Long = 0L,
    val errorCode: Int = CarServiceError.NONE,
    val diagnosticMessage: String? = null,
) : Parcelable

@Parcelize
data class VehicleStatusState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val hasBatteryPercentage: Boolean = false,
    val batteryPercentage: Float = 0f,
    val hasOutsideTemperature: Boolean = false,
    val outsideTemperatureCelsius: Float = 0f,
    val hasRange: Boolean = false,
    val rangeKilometers: Float = 0f,
    val hasTirePressure: Boolean = false,
    val minimumTirePressureKpa: Float = 0f,
    val tiresHealthy: Boolean = true,
    val errorCode: Int = CarServiceError.NONE,
    val diagnosticMessage: String? = null,
) : Parcelable

@Parcelize
data class BluetoothDeviceInfo(
    val address: String,
    val name: String? = null,
    val bonded: Boolean = false,
) : Parcelable

@Parcelize
data class BluetoothFeatureState(
    val status: Int = FeatureStatus.CONNECTING,
    val available: Boolean = false,
    val supported: Boolean = false,
    val enabled: Boolean = false,
    val discovering: Boolean = false,
    val localName: String? = null,
    val localAddress: String? = null,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val nearbyDevices: List<BluetoothDeviceInfo> = emptyList(),
    val connectedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val errorCode: Int = CarServiceError.NONE,
    val diagnosticMessage: String? = null,
) : Parcelable
