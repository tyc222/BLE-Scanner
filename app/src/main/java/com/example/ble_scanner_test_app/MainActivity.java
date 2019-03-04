package com.example.ble_scanner_test_app;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.MacAddress;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.datatype.Duration;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Activity";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private final static int REQUEST_ENABLE_BT = 1;

    private final static int REQUEST_ENABLE_LOCATION = 1;

    // Scanning time for the phone
    private final static int SCAN_PERIOD = 5 * 1000;

    // Ask device to pair with the ble deving named (OTOHTR)
    private String SERVICE_NAME = "OTOHTR";

    // BLE device UUID
    private UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA TX Characteristic
    private UUID TX_CHARACTERISTIC_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA RX Characteristic
    private UUID RX_CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA Client Characteristic Configuration Descriptor
    private UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    // Declare bluetooth adapter
    BluetoothAdapter bluetoothAdapter;

    // Define a string adapter which will handle the data of the device listview
    ArrayAdapter<String> listViewAdapter;

    // Define a string adapter which will handle the data of the response listview
    ArrayAdapter<String> listViewAdapterResponse;

    // Define a ble Scanner object
    BluetoothLeScanner bluetoothLeScanner;

    // Holder for scancallback
    ScanCallback scanCallback;

    // Holder for scan results
    Map<String, BluetoothDevice> scanResults;

    // Determine if the phone is scanning
    boolean scanning = false;

    // Handler to stop phone from scanning forever
    Handler handler;

    // Gatt profile
    BluetoothGatt Gatt;

    // To notify the characteristic is ready to use
    boolean timeInitialized;

    // To
    boolean echoInitaialized;

    // Edit text view from the user
    EditText inputText;

    // Condition for Gatt connection
    boolean connected = false;


    @Override
    protected void onStart() {
        super.onStart();
        // Ensure BLE is working on the phone

        // Enable BLE
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs bluetooth access");
            builder.setMessage("Please grant bluetooth access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            });
            builder.show();
        }

        // Enable location permission for sdk 23+
        if (Build.VERSION.SDK_INT >= 23) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
        // Enable location
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location acess");
            builder.setMessage("Please turn on location so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Intent enableLocationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(enableLocationIntent, REQUEST_ENABLE_LOCATION);
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // If device does not support BLE then let the User know and exit app
        if (!this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Your Phone Does Not Support BLE")
                    .setPositiveButton("Exit App", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
            // Create the AlertDialog object and return it
            AlertDialog alert = builder.create();
            alert.show();
        }

        // List of Array Strings which will serve as list items
        ArrayList<String> listItems = new ArrayList<String>();


        // set up an adapter for listview
        listViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);

        // set the data to our listview
        ListView list = (ListView) findViewById(R.id.list_view_ble_device);
        list.setAdapter(listViewAdapter);

        // List of Array Strings which will serve as response list items
        ArrayList<String> listItemResponses = new ArrayList<String>();

        // set up an adapter for response listview
        listViewAdapterResponse = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItemResponses);

        // set the data to our response listview
        ListView responseList = (ListView) findViewById(R.id.list_view_ble_device_response);
        responseList.setAdapter(listViewAdapterResponse);

        // Find the edit text box for inputText
        inputText = (EditText) findViewById(R.id.inputEditText);

        //Set up bluetooth adapter and ble services on phone
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


    }


    private void startScan() {

        // Add a filter (Service Name) to GATT
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName(SERVICE_NAME).build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_BALANCED).build();
        scanResults = new HashMap<>();
        scanCallback = new BtleScanCallback(scanResults);

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(filters, settings, scanCallback);
        scanning = true;
        // Scan ble devices for a set amount of time
        handler = new Handler();
        handler.postDelayed(() -> {
            stopScan();
        }, SCAN_PERIOD);


    }

    // Use the same ScanCallback to avoid unnecessary calls
    private void stopScan() {
        if (scanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
            scanComplete();


        }
        scanCallback = null;
        scanning = false;
        handler = null;
    }

    //Perform action using results
    private void scanComplete() {
        if (scanResults.isEmpty()) {
            return;
        }
//        // Read information from scanResults
//        for (String information : scanResults.keySet()) {
//            Log.d(TAG, "Found device: " + information);
//            listViewAdapter.add(information);
//        }

    }




    private class BtleScanCallback extends ScanCallback {


        private BtleScanCallback(Map<String, BluetoothDevice> scanResultMap) {
            scanResults = scanResultMap;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addScanResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                addScanResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE Scan Failed with code" + errorCode);
        }

        private void addScanResult(ScanResult result) {
            stopScan();
            BluetoothDevice bluetoothDevice = result.getDevice();
            connectDevice(bluetoothDevice);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        // Display a listview full of scanned devices
        String deviceAddress = device.getAddress();
        String deviceName = device.getName();
        // Add the name and address to a ListView
        int checkBondState = device.getBondState();
        String bondState;
        switch (checkBondState) {
            case (12):
                bondState = "Paired";
                break;
            case (11):
                bondState = "Paring";
                break;
            case (10):
                bondState = "Unpaired";
                break;
            default:
                bondState = "Error";
                break;
        }
        listViewAdapter.clear();
        Log.d(TAG, "Connecting to " + deviceName + "\n" + bondState + "\n" + deviceAddress + "\n");
        listViewAdapter.add(deviceName + "\n" + bondState + "\n" + deviceAddress + "\n");
        GattCallback gattCallback = new GattCallback();
        // Connecting phone to BLE device
        Gatt = device.connectGatt(this, false, gattCallback);
    }

    // Know if we are connected or not with he BLE device
    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }

        }

        // New services discovered
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            BluetoothGattService service = gatt.getService(SERVICE_UUID);
            Log.d(TAG, "Acquiring service");
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(TX_CHARACTERISTIC_UUID);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            timeInitialized = gatt.setCharacteristicNotification(characteristic, true);
            Log.d(TAG, "Initializing: setting write type and enabling notification");

            // Ask the BLE device to send packet to phone when something changes
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
            Log.d(TAG, "Initialized: enabled notification");
        }

        // Result of a characteristic read operation
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG,"Characteristic read successfully");
                // Extra string value of data in characteristic
                Log.d(TAG, "read value: " + String.valueOf(characteristic.getValue()));
            }else {
                Log.d(TAG, "Characteristic read unsuccessful, status: " + status);}
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG,"Characteristic written successfully");
            }else {
                Log.d(TAG, "Characteristic write unsuccessful, status: " + status);
                disconnectGattServer();
            }
        }

        // Triggered when BLE device sends a packet to phone
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte [] messageBytes = characteristic.getValue();
            String messageString = null;
            try{
                messageString = new String(messageBytes, "UTF-8");
                Log.d(TAG, "BLE message: " + messageString);
                // Clear the listview first then add response
                listViewAdapterResponse.add("BLE message: " + messageString);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, " Unable to convert message bytes to string");
            }
            Log.d(TAG, messageString);
        }

    }

    // Disconnect Gatt Server
    public void disconnectGattServer() {
        connected = false;
        echoInitaialized = false;
        if (Gatt != null) {
            Gatt.disconnect();
            Gatt.close();
        }
    }

    // Sending messaging to BLE device
    private void sendMessage () {
        if (!connected || !echoInitaialized) {
            return;
        }
        BluetoothGattService service = Gatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID);
        String message = inputText.getText().toString();
        // We need to convert our message from String to byte[] in order to send data
        byte [] messageBytes = new byte[0];
        try {
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Failed to convert message string to byte array");
        }
        // Set the value on Characteristic to send our message
        characteristic.setValue(messageBytes);
        boolean success = Gatt.writeCharacteristic(characteristic);
        if (success) {
            Log.d(TAG, "Successful");
        } else {
            Log.d(TAG,"Failed to write data");
        }
    }

    public void close() {
        if (Gatt == null) {
            return;
        }
        Gatt.close();
        Gatt = null;
    }

    public void scanBleDevices(View view) {

        startScan();
    }

    public void sendInput(View view) {
        sendMessage();
    }

}
