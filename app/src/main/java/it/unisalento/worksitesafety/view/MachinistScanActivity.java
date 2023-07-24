package it.unisalento.worksitesafety.view;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import it.unisalento.worksitesafety.R;


// Scanning and displaying available Bluetooth LE devices
public class MachinistScanActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> enableBtLauncher;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler;
    private LeDeviceListAdapter leDeviceListAdapter;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private Button scanButton;
    private ListView list;
    private static final String TAG = "BleScan";
    // Defining Permission codes.
    private static final int BLUETOOTH_CONNECT_CODE = 100;
    private static final int BLUETOOTH_SCAN_CODE = 101;
    private static final int BACKGROUND_LOCATION_CODE = 102;
    private static final int FINE_LOCATION_CODE = 103;
    private static final int COARSE_LOCATION_CODE = 104;
    TextView deviceName;
    TextView deviceAddress;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinist_scan);

        scanButton = findViewById(R.id.but_scan);
        scanButton.setText("Scan");

        handler = new Handler();
        scanning = false;

        checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);
        checkPermission(Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_CODE);
        //checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, BACKGROUND_LOCATION_CODE);
        //checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, FINE_LOCATION_CODE);
        //checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION, COARSE_LOCATION_CODE);

        if (ActivityCompat.checkSelfPermission(MachinistScanActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

        }
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            //finish();
        }

        // Get the Bluetooth adapter
        final BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            // Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            //finish();
            return;
        }

        // Check and enable Bluetooth
        enableBtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        // User chose not to enable Bluetooth.
                        if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            Toast.makeText(MachinistScanActivity.this, "Task needs Bluetooth", Toast.LENGTH_SHORT).show();
                            //finish();
                        }
                    }
                });

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        leDeviceListAdapter = new LeDeviceListAdapter(getApplicationContext());
        leDeviceListAdapter.clear();
        list = findViewById(R.id.list_view);
        list.setAdapter(leDeviceListAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = leDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(MachinistScanActivity.this, MachinistConnectActivity.class);
                if (ActivityCompat.checkSelfPermission(MachinistScanActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    //checkPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);
                }
                intent.putExtra(MachinistConnectActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(MachinistConnectActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (scanning) {
                    bluetoothLeScanner.stopScan(leScanCallback);
                    scanning = false;
                }
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        /*// Check Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtLauncher.launch(enableBtIntent);
        }

        // Initialize list view
        leDeviceListAdapter = new LeDeviceListAdapter();
        leDeviceListAdapter.clear();
        list.setAdapter(leDeviceListAdapter);*/

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void scanLeDevice(View view) {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanButton.setText("Scan");
                    Log.i(TAG, "Stopped for scanning BLE devices");
                    scanning = false;
                    if (ActivityCompat.checkSelfPermission(MachinistScanActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        //checkPermission(Manifest.permission.BLUETOOTH_SCAN, BLUETOOTH_SCAN_CODE);
                        return;
                    }
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            leDeviceListAdapter.clear();
            Log.i(TAG, "Scanning for BLE devices");
            scanButton.setText("Stop");
            scanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanButton.setText("Scan");
            Log.i(TAG, "Stopped for scanning BLE devices");
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // Adapter for holding devices found through scanning
    private class LeDeviceListAdapter extends BaseAdapter {
        ArrayList<BluetoothDevice> leDevices;
        LayoutInflater inflater;
        Context context;


        public LeDeviceListAdapter(Context context) {
            super();
            this.context = context;
            leDevices = new ArrayList<BluetoothDevice>();
            inflater = MachinistScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!leDevices.contains(device)) {
                leDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return leDevices.get(position);
        }

        public void clear() {
            leDevices.clear();
        }

        @Override
        public int getCount() {
            return leDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return leDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflater.inflate(R.layout.activity_custom_list_view, null);
            deviceName = view.findViewById(R.id.deviceName);
            deviceAddress = view.findViewById(R.id.deviceAddress);

            BluetoothDevice device = leDevices.get(i);
            if (ActivityCompat.checkSelfPermission(MachinistScanActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            }
            if (device != null) {
                final String name = device.getName();
                if (name != null && name.length() > 0) {
                    deviceName.setText(name);
                } else {
                    deviceName.setText("Unkown device");
                }
                final String address = device.getAddress();
                if (address != null && address.length() > 0) {
                    deviceAddress.setText("Address: " + device.getAddress());
                } else {
                    deviceAddress.setText("Address: unkown");
                }
            }
            return view;
        }
    }

    // Device callback
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    leDeviceListAdapter.addDevice(result.getDevice());
                    leDeviceListAdapter.notifyDataSetChanged();
                }
            };

    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            requestPermissionLauncher.launch(permission);
        }
        else {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // feature requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                    onStop();
                }
            });

}