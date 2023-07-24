package it.unisalento.worksitesafety.view;

import it.unisalento.worksitesafety.model.GattAttributes;
import it.unisalento.worksitesafety.service.BluetoothLeService.LocalBinder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.unisalento.worksitesafety.R;
import it.unisalento.worksitesafety.service.BluetoothLeService;

public class MachinistConnectActivity extends AppCompatActivity {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private String deviceName;
    public static final String TAG = "BluetoothLeService";
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private String deviceAddress;
    private TextView nameField;
    private TextView addressField;
    private TextView connectionField;
    private Button button;
    private BluetoothLeService bluetoothService;
    private boolean connected = false;
    private BluetoothGattCharacteristic notifyCharacteristic;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bluetoothService = ((LocalBinder) service).getService();
            if (bluetoothService != null) {
                if (!bluetoothService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth.");
                    finish();
                }
                // perform device connection
                // bluetoothService.connect(deviceAddress);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bluetoothService = null;
        }
    };
    private BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                connectionField.setText("Connected");
                button.setText("Disconnect");
                Log.d(TAG, "Connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                // updateConnectionState(R.string.disconnected);
                connectionField.setText("Not connected");
                button.setText("Connect");
                Log.d(TAG, "Disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface
                displayGattServices(bluetoothService.getSupportedGattServices());
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        Log.d(TAG, "displayGattServices");
        if (gattServices == null) {
            Log.e(TAG,"No GATT Services found");
            return;
        }
        String uuid = null;
        String unknownServiceString = "Unknown service";
        String unknownCharaString = "Unknown characteristic";
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        if (mGattCharacteristics != null) {

            final BluetoothGattCharacteristic characteristic = mGattCharacteristics.get(3).get(0);
            Log.d(TAG, "Characteristic " + characteristic.getUuid());

            final int charaProp = characteristic.getProperties();
            /*if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (notifyCharacteristic != null) {
                    bluetoothService.setCharacteristicNotification(
                            notifyCharacteristic, false);
                    notifyCharacteristic = null;
                }
                bluetoothService.readCharacteristic(characteristic);
            }*/
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                notifyCharacteristic = characteristic;
                bluetoothService.setCharacteristicNotification(
                        characteristic, true);
            }
        } else {
            Log.e(TAG, "No characteristics found");
        }

    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinist_connect);

        // Get extras from intent
        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Set up UI
        nameField = findViewById(R.id.name_field);
        if (deviceName != null) {
            nameField.setText(deviceName);
        } else {
            nameField.setText("Unkown device");
        }
        addressField = findViewById(R.id.address_field);
        addressField.setText(deviceAddress);
        connectionField = findViewById(R.id.connection_state);
        connectionField.setText("Not Connected");
        button = findViewById(R.id.connect_btn);
        button.setText("Connect");

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        gattServiceIntent.putExtra(BluetoothLeService.EXTRAS_DEVICE_NAME, deviceName);
        bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        /*if (bluetoothService != null) {
            final boolean result = bluetoothService.connect(deviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }*/

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bluetoothService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    public void pushButton(View view) {
        if (connected) {
            // perform device disconnection
            if (bluetoothService != null) {
                bluetoothService.disconnect();
            }
        } else {
            // perform device connection
            if (bluetoothService != null) {
                final boolean result = bluetoothService.connect(deviceAddress);
                Log.d(TAG, "Connect request result=" + result);
                if (result != true) {
                    Toast.makeText(bluetoothService, "Unable to connect", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

}