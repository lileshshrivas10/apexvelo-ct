package com.apexvelo.ct.feature.device.connection

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid

class ApexVeloBleClient(
    private val context: Context
) {
    var onStateChanged: (BleConnectionState) -> Unit = {}

    private val bluetoothManager =
        context.getSystemService(BluetoothManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var navigationCharacteristic: BluetoothGattCharacteristic? = null
    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun connect() {
        disconnect()

        val adapter = bluetoothManager?.adapter
        val scanner = adapter?.bluetoothLeScanner
        if (adapter == null || !adapter.isEnabled || scanner == null) {
            publish(BleConnectionState.Error("Bluetooth is unavailable or turned off"))
            return
        }

        publish(BleConnectionState.Scanning)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val advertisedName = result.scanRecord?.deviceName
                val deviceName = advertisedName ?: device.name ?: "ApexVelo device"

                scanner.stopScan(this)
                scanCallback = null
                publish(BleConnectionState.Connecting(deviceName))
                gatt = device.connectGatt(context, false, gattCallback)
            }

            override fun onScanFailed(errorCode: Int) {
                scanCallback = null
                publish(BleConnectionState.Error("BLE scan failed ($errorCode)"))
            }
        }

        scanCallback = callback
        scanner.startScan(
            listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(ApexVeloBleContract.NAVIGATION_SERVICE_UUID))
                    .build()
            ),
            ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build(),
            callback
        )

        mainHandler.postDelayed({
            if (scanCallback === callback) {
                scanner.stopScan(callback)
                scanCallback = null
                publish(BleConnectionState.Error("No ApexVelo device found"))
            }
        }, SCAN_TIMEOUT_MILLIS)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        val scanner = bluetoothManager?.adapter?.bluetoothLeScanner
        scanCallback?.let { scanner?.stopScan(it) }
        scanCallback = null
        navigationCharacteristic = null
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        publish(BleConnectionState.Disconnected)
    }

    @SuppressLint("MissingPermission")
    fun sendNavigationFrame(bytes: ByteArray): Boolean {
        val activeGatt = gatt ?: return false
        val characteristic = navigationCharacteristic ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activeGatt.writeCharacteristic(
                characteristic,
                bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            ) == BluetoothGatt.GATT_SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = bytes
            characteristic.writeType =
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            @Suppress("DEPRECATION")
            activeGatt.writeCharacteristic(characteristic)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(
            bluetoothGatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED
            ) {
                if (!bluetoothGatt.requestMtu(REQUIRED_MTU)) {
                    bluetoothGatt.discoverServices()
                }
                return
            }

            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                navigationCharacteristic = null
                bluetoothGatt.close()
                if (gatt === bluetoothGatt) gatt = null
                publish(BleConnectionState.Disconnected)
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                publish(BleConnectionState.Error("BLE connection failed ($status)"))
                bluetoothGatt.close()
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(
            bluetoothGatt: BluetoothGatt,
            mtu: Int,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS || mtu < REQUIRED_MTU) {
                publish(BleConnectionState.Error("Device does not support 34-byte navigation frames"))
                bluetoothGatt.disconnect()
                return
            }

            bluetoothGatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(
            bluetoothGatt: BluetoothGatt,
            status: Int
        ) {
            val service: BluetoothGattService? = bluetoothGatt.getService(
                ApexVeloBleContract.NAVIGATION_SERVICE_UUID
            )
            val characteristic = service?.getCharacteristic(
                ApexVeloBleContract.NAVIGATION_FRAME_UUID
            )

            if (status != BluetoothGatt.GATT_SUCCESS || characteristic == null) {
                publish(BleConnectionState.Error("Navigation service is unavailable"))
                bluetoothGatt.disconnect()
                return
            }

            navigationCharacteristic = characteristic
            val deviceName = bluetoothGatt.device.name ?: "ApexVelo device"
            publish(BleConnectionState.Connected(deviceName))
        }
    }

    private fun publish(state: BleConnectionState) {
        mainHandler.post { onStateChanged(state) }
    }

    private companion object {
        const val SCAN_TIMEOUT_MILLIS = 10_000L
        // ATT reserves three bytes of the negotiated MTU for its write header.
        const val REQUIRED_MTU = 37
    }
}
