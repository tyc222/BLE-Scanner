package com.example.ble_scanner_test_app;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.Duration;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;

    // Declare bluetooth adapter
    BluetoothAdapter bluetoothAdapter;

    // Define a string adapter which will handle the data of the listview
    ArrayAdapter<String> listViewAdapter;

    // SetMap for duplicated device scan
    Set<String> unduplicatedDeviceMacAddress;
    // Condition for the scan button
    boolean isScanning = false;

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

        // Set up bluetooth adapter and ble services on phone
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensure BLE is enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


    }

    public void scanBleDevices(View view) {

        // Setup BluetoothLeScanner and scanCallback
        BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanCallback scanCallback = null;

        Button scanButton = findViewById(R.id.button);

        // Scanning for BLE devices
        if (!isScanning) {
            scanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    BluetoothDevice device = result.getDevice();
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

                    // Double check if the device is already listed, check for duplication
                    unduplicatedDeviceMacAddress = new HashSet<>();

                    if (!unduplicatedDeviceMacAddress.contains(result.getDevice().getAddress())) {
                        listViewAdapter.add(device.getName() + "\n" + device.getAddress() + "\n" + bondState
                                + "\n" + device.getUuids());
                        unduplicatedDeviceMacAddress.add(device.getAddress());
                    }

                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    super.onBatchScanResults(results);
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.d("TAG", "Scanning Failed " + errorCode);
                }
            };

            // Scanning
            bluetoothLeScanner.startScan(scanCallback);

            scanButton.setText("Stop Scanning");
        }

        // Stop scanning to save battery
        if (isScanning) {

            bluetoothLeScanner.stopScan(scanCallback);

            scanButton.setText("Start Scanning");
        }
        isScanning = !isScanning;
    }


}
