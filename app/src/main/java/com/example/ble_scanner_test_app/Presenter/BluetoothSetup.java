package com.example.ble_scanner_test_app.Presenter;

import android.bluetooth.BluetoothManager;
import android.content.Context;

import com.example.ble_scanner_test_app.ScanDevice;

public class BluetoothSetup {

    public void setBluetoothManager (Context context) {

        //Set up bluetooth adapter and ble services on phone
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        ScanDevice.bluetoothAdapter = bluetoothManager.getAdapter();
    }
}
