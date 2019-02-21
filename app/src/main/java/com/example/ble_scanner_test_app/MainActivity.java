package com.example.ble_scanner_test_app;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
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
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

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

    // UUID for the ble deving we are using (OTOHTR)
    private String SERVICE_ADDRESS = ("D7:67:68:60:71:3F");

    // Declare bluetooth adapter
    BluetoothAdapter bluetoothAdapter;

    // Define a string adapter which will handle the data of the listview
    ArrayAdapter<String> listViewAdapter;

    // Define a ble Scanner object
    BluetoothLeScanner bluetoothLeScanner;

    // Holder for scancallback
    ScanCallback scanCallback;

    // Holder for scan results
    Map<String, BluetoothDevice> scanResults;

    // Determine if the phone is scanning
    Boolean scanning = false;

    // Handler to stop phone from scanning forever
    Handler handler;

    // Gatt profile
    BluetoothGatt Gatt;


    // SetMap for duplicated device scan
    Set<String> unduplicatedDeviceMacAddress;
    // Condition for the scan button
    boolean isScanning = false;
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


        // /set up an adapter for listview
        listViewAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);

        // set the data to our listview
        ListView list = (ListView) findViewById(R.id.list_view_ble_devlice);
        list.setAdapter(listViewAdapter);

        //Set up bluetooth adapter and ble services on phone
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();


    }


    private void startScan() {

        // Add a filter for GATT server's Service UUID
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(SERVICE_ADDRESS).build();
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
        handler.postDelayed(this::stopScan, SCAN_PERIOD);


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
        // Read information from scanResults
        for (String information : scanResults.keySet()) {
            Log.d(TAG, "Found device: " + information);
            listViewAdapter.add(information);
        }

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

            // Display a listview full of scanned devices
            String deviceAddress = bluetoothDevice.getAddress();
            String deviceName = bluetoothDevice.getName();
            String deviceUuid = String.valueOf(bluetoothDevice.getUuids());
            // Add the name and address to a ListView
            int checkBondState = bluetoothDevice.getBondState();
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
            String information = deviceName + "\n" + bondState + "\n" + deviceAddress + "\n" + deviceUuid;
            // Store scanned information in scanResults
            scanResults.put(information, bluetoothDevice);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        Log.d(TAG, "Connecting to " + device.getAddress());
        GattCallback gattCallback = new GattCallback();
        Gatt = device.connectGatt(this, false, gattCallback);
    }

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
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }

        }
    }

    // Disconnect Gatt Server
    public void disconnectGattServer() {
        connected = false;
        if (Gatt != null) {
            Gatt.disconnect();
            Gatt.close();
        }
    }

    public void scanBleDevices(View view) {
        listViewAdapter.clear();
startScan();
//
//        // Setup BluetoothLeScanner and scanCallback
//        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
//        ScanCallback scanCallback = null;
//
//        Button scanButton = findViewById(R.id.button);
//
//        // Scanning for BLE devices
//        if (!isScanning) {
//            scanCallback = new ScanCallback() {
//                @Override
//                public void onScanResult(int callbackType, ScanResult result) {
//                    super.onScanResult(callbackType, result);
//
//                    BluetoothDevice device = result.getDevice();
//                    // Add the name and address to a ListView
//                    int checkBondState = device.getBondState();
//                    String bondState;
//                    switch (checkBondState) {
//                        case (12):
//                            bondState = "Paired";
//                            break;
//                        case (11):
//                            bondState = "Paring";
//                            break;
//                        case (10):
//                            bondState = "Unpaired";
//                            break;
//                        default:
//                            bondState = "Error";
//                            break;
//                    }
//
//                    // Double check if the device is already listed, check for duplication
//                    unduplicatedDeviceMacAddress = new HashSet<>();
//
//                    if (!unduplicatedDeviceMacAddress.contains(result.getDevice().getAddress())) {
//                        listViewAdapter.add(device.getName() + "\n" + device.getAddress() + "\n" + bondState
//                                + "\n" + device.getUuids());
//                        unduplicatedDeviceMacAddress.add(device.getAddress());
//                    }
//
//                }
//
//                @Override
//                public void onBatchScanResults(List<ScanResult> results) {
//                    super.onBatchScanResults(results);
//                }
//
//                @Override
//                public void onScanFailed(int errorCode) {
//                    super.onScanFailed(errorCode);
//                    Log.d(TAG, "Scanning Failed " + errorCode);
//                }
//            };
//
//            // Scanning
//            bluetoothLeScanner.startScan(scanCallback);
//
//            scanButton.setText("Stop Scanning");
//        }
//
//        // Stop scanning to save battery
//        if (isScanning) {
//
//        }
//        isScanning = !isScanning;
    }


}
