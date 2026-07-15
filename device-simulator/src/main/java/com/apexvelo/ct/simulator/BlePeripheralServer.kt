package com.apexvelo.ct.simulator

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.os.ParcelUuid
import com.apexvelo.ct.feature.device.connection.ApexVeloBleContract

class BlePeripheralServer(
    private val context: Context,
    private val onStatus: (String) -> Unit,
    private val onPacket: (SimulatorNavigationPacket) -> Unit
) {
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private var gattServer: BluetoothGattServer? = null

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            onStatus("Advertising — waiting for phone")
        }

        override fun onStartFailure(errorCode: Int) {
            onStatus("Advertising failed ($errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        stop()
        val adapter = manager?.adapter
        val advertiser = adapter?.bluetoothLeAdvertiser
        if (adapter == null || !adapter.isEnabled || advertiser == null) {
            onStatus("Bluetooth LE advertising unavailable")
            return
        }

        gattServer = manager.openGattServer(context, serverCallback).also { server ->
            val service = BluetoothGattService(
                ApexVeloBleContract.NAVIGATION_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            service.addCharacteristic(
                BluetoothGattCharacteristic(
                    ApexVeloBleContract.NAVIGATION_FRAME_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE
                )
            )
            server?.addService(service)
        }

        advertiser.startAdvertising(
            AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build(),
            AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(ApexVeloBleContract.NAVIGATION_SERVICE_UUID))
                .build(),
            advertiseCallback
        )
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        manager?.adapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            onStatus(
                if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    "Phone connected — receiving navigation"
                } else {
                    "Advertising — waiting for phone"
                }
            )
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            var result = BluetoothGatt.GATT_SUCCESS
            if (characteristic.uuid == ApexVeloBleContract.NAVIGATION_FRAME_UUID && offset == 0) {
                try {
                    onPacket(SimulatorPacketCodec.decode(value))
                } catch (_: IllegalArgumentException) {
                    result = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH
                }
            } else {
                result = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED
            }

            if (responseNeeded) {
                @SuppressLint("MissingPermission")
                gattServer?.sendResponse(device, requestId, result, 0, null)
            }
        }
    }
}
