package com.example.ble_scanner_test_app;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.ble_scanner_test_app.MessageWithDevice.inputText;
import static com.example.ble_scanner_test_app.MessageWithDevice.listViewAdapterResponse;


/**
 * A simple {@link Fragment} subclass.
 */
public class ScanDevice extends Fragment {


    /**
     * Variable Storage for the App
     */

    private static final String TAG = "Activity";

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private final static int REQUEST_ENABLE_BT = 1;

    private final static int REQUEST_ENABLE_LOCATION = 1;

    // Scanning time for the phone
    private final static int SCAN_PERIOD = 6 * 1000;

    // Ask device to pair with the ble deving named (OTOHTR)
    private String SERVICE_NAME = "OTOHTR";

    // BLE device UUID
    private static UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA TX Characteristic
    private UUID TX_CHARACTERISTIC_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA RX Characteristic
    private static UUID RX_CHARACTERISTIC_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");

    // BLE device characteristic UUID AKA Client Characteristic Configuration Descriptor
    private UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // byte for BLE response
    byte[] messageBytes;

    // String for BLE response text
    String messageString;

    // Declare bluetooth adapter
    BluetoothAdapter bluetoothAdapter;

    // Define a string adapter which will handle the data of the device listview
    ArrayAdapter<String> listViewAdapter;

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
    static BluetoothGatt Gatt;

    // Bluetooth Gatt Characteristic for Asynctask
    BluetoothGattCharacteristic asyncCharacteristic;

    // To notify the characteristic is ready to use
    boolean timeInitialized;

    // To
    boolean echoInitaialized = true;

    // Condition for Gatt connection
    static boolean connected = false;

    // Current Time
    static String currentTime;

    public ScanDevice() {
        // Required empty public constructor
    }

    /**
     * Request and check BLE and Location permissions
     */

    @Override
    public void onStart() {
        super.onStart();

        // Ensure BLE is working on the phone

        // Enable BLE
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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

    /**
     * Check whether phone supports BLE.
     * Instantiate Views
     * Set up Bluetooth Adapter
     *
     * @param savedInstanceState
     */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan_device, container, false);

        // If device does not support BLE then let the User know and exit app
        if (!getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Your Phone Does Not Support BLE")
                    .setPositiveButton("Exit App", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            getActivity().finish();
                        }
                    });
            // Create the AlertDialog object and return it
            AlertDialog alert = builder.create();
            alert.show();
        }

        // List of Array Strings which will serve as list items
        ArrayList<String> listItems = new ArrayList<String>();

        // set up an adapter for listview
        listViewAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listItems);

        // set the data to our listview
        ListView list = (ListView) rootView.findViewById(R.id.list_view_ble_device);
        list.setAdapter(listViewAdapter);

        // make listview items clickable
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(getActivity(), "Swipe left to communicate with your device", Toast.LENGTH_SHORT).show();
            }
        });


        //Set up bluetooth adapter and ble services on phone
        final BluetoothManager bluetoothManager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        /**
         Buttons
         */

        // OnClick for the scan button, which scans for BLE devices
        Button scanButton = rootView.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> startScan());

        return rootView;
    }

    /**
     * Scan operations - Start, Stop, Complete
     */

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
    }

    /**
     * Callback from the BLE scan
     * Connect the device once scan result returns positive
     */


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
        Gatt = device.connectGatt(getActivity(), false, gattCallback);
    }

    /**
     * Connect to GATT in order to receive, read, write characteristics.
     * Use an AsyncTask to update the UI when phone is notified by the BLE device.
     */

    // Know if we are connected or not with he BLE device
    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                Log.d(TAG, "GATT Failed");
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                Log.d(TAG, "GATT Not Success");
                return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "newState Disconnected");
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
                Log.d(TAG, "Characteristic read successfully");
                // Extra string value of data in characteristic
                Log.d(TAG, "read value: " + String.valueOf(characteristic.getValue()));
            } else {
                Log.d(TAG, "Characteristic read unsuccessful, status: " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);


            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic written successfully, status: " + status);
            } else {
                Log.d(TAG, "Characteristic write unsuccessful, status: " + status);
                disconnectGattServer();
            }
        }

        // Triggered when BLE device sends a packet to phone (When notified by the BLE device)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            asyncCharacteristic = characteristic;


            // Record time of this event
            long date = System.currentTimeMillis();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            currentTime = simpleDateFormat.format(date);

            // Execute BleResponseTask to fetch and update Ble response message
            new BleResponseTask().execute();

        }

    }

    // Fetch Ble response message and update to the UI by using an AsyncTask loader
    private class BleResponseTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            messageBytes = asyncCharacteristic.getValue();

            try {
                // Add String and Hex responses to the log
                messageString = new String(messageBytes, "UTF-8");
                Log.d(TAG, currentTime + "\n" + "BLE message: " + messageString);
//                Log.d(TAG, currentTime + "\n" + "BLE hex: " + bytes2HexString(messageBytes));

            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, " Unable to convert message bytes to string");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // Add String and Hex responses to the listview
            listViewAdapterResponse.add(currentTime + "\n" + "BLE message: " + messageString);
//            listViewAdapterResponse.add(currentTime + "\n" + "BLE hex: " + bytes2HexString(messageBytes));
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

    // Method to send message to BLE device
    static void write(Context context) {
        if (!connected) {
            return;
        }
        BluetoothGattService service = Gatt.getService(SERVICE_UUID);
        BluetoothGattCharacteristic writeCharacteristic = service.getCharacteristic(RX_CHARACTERISTIC_UUID);
        String message = inputText.getText().toString();
        Log.d(TAG, "Sending message: " + message);
        // We need to convert our message from String to byte[] in order to send data
        byte[] messageWriteBytes = new byte[0];
        // Let the user know if the CMD they input is incorrect
        try {
            messageWriteBytes = hexStringToByteArray(message);
        } catch (StringIndexOutOfBoundsException exception) {
            Toast.makeText(context, "Please Add A 0 Before Your Number", Toast.LENGTH_SHORT).show();
            inputText.setText("");
            return;
        }
        byte[] builtCMDBytes = buildCMD(messageWriteBytes);
        Log.d(TAG, "Sending CMD bytes message in hex: " + bytes2HexString(builtCMDBytes));

        // Set the value on Characteristic to send our message
        writeCharacteristic.setValue(builtCMDBytes);
        writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean success = Gatt.writeCharacteristic(writeCharacteristic);

        if (success) {
            inputText.setText("");
            // Add operation result to the listview
            listViewAdapterResponse.add(currentTime + "\n" + "BLE message: " + "Characteristic written successfully");
        } else {
            inputText.setText("");
            Log.d(TAG, "Failed to write data");
            // Add operation result to the listview
            listViewAdapterResponse.add(currentTime + "\n" + "BLE message: " + "Failed to write data");
        }
    }

    // Method to close Gatt connection
    public static void close() {
        if (Gatt == null) {
            return;
        }
        Gatt.close();
        Gatt = null;
    }

    /**
     * Functions for conversions
     */

    // Function for converting bytes to hex
    private static final char hexDigits[] =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String bytes2HexString(final byte[] bytes) {
        if (bytes == null) return null;
        int len = bytes.length;
        if (len <= 0) return null;
        char[] ret = new char[len << 1];
        for (int i = 0, j = 0; i < len; i++) {
            ret[j++] = hexDigits[bytes[i] >>> 4 & 0x0f];
            ret[j++] = hexDigits[bytes[i] & 0x0f];
        }
        return new String(ret);
    }

    // Function for converting hex to bytes
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    // Function for converting java int to UNIT8
    public static byte intToUint8Byte(int value) {
        return (byte) value;
    }

    /**
     * Function for building CMD from integer input by the user
     */

    // Function for building CMD
    protected static final int PACKAGE_MAX_LENGTH = 80;
    protected static final int PACKAGE_BASE_LENGTH = 7;
    protected static final byte PACKAGE_START1_VALUE = intToUint8Byte(0xA0);
    protected static final byte PACKAGE_START2_VALUE = intToUint8Byte(0xA1);
    protected static final byte CR = intToUint8Byte(0x0D);
    protected static final byte LF = intToUint8Byte(0x0A);

    static final synchronized byte[] buildCMD(byte[] cmdPayload) {
        int cmdPayloadLength = cmdPayload.length;
        if (cmdPayloadLength > (PACKAGE_MAX_LENGTH - PACKAGE_BASE_LENGTH)) {
            throw new UnsupportedOperationException("only support 73 bytes");
        }

        byte[] pkgData = new byte[cmdPayloadLength + PACKAGE_BASE_LENGTH];
        pkgData[0] = PACKAGE_START1_VALUE;
        pkgData[1] = PACKAGE_START2_VALUE;
        byte[] msb16PayloadLength = msbUint16ToBytes(cmdPayloadLength);
        pkgData[2] = msb16PayloadLength[0];
        pkgData[3] = msb16PayloadLength[1];
        System.arraycopy(cmdPayload, 0, pkgData, 4, cmdPayloadLength);
        pkgData[cmdPayloadLength + 4] = u8CheckSum(cmdPayload);
        pkgData[cmdPayloadLength + 5] = CR;
        pkgData[cmdPayloadLength + 6] = LF;
        return pkgData;
    }

    static final byte u8CheckSum(final byte[] buffer) {
        byte payloadCheckSum = 0;
        for (byte aBuffer : buffer) {
            payloadCheckSum = (byte) (payloadCheckSum ^ aBuffer);
        }
        return payloadCheckSum;
    }

    // Converting msb unit16 to bytes
    public static byte[] msbUint16ToBytes(int value) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) ((value & 0xFF00) >> 8);
        bytes[1] = (byte) (value & 0xFF);
        return bytes;
    }

}
