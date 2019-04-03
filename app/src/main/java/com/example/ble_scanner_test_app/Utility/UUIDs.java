package com.example.ble_scanner_test_app.Utility;

public class UUIDs {

    // Ask device to pair with the ble deving named (OTOHTR)
    public static final String SERVICE_NAME = "OTOHTR";

    // BLE device UUID
    public static final java.util.UUID SERVICE_UUID = java.util.UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA TX Characteristic
    public  static final java.util.UUID TX_CHARACTERISTIC_UUID = java.util.UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA RX Characteristic
    public static final java.util.UUID RX_CHARACTERISTIC_UUID = java.util.UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA Client Characteristic Configuration Descriptor
    public static final java.util.UUID CCCD_UUID = java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
}
