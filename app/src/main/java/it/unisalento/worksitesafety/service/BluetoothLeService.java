package it.unisalento.worksitesafety.service;



import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import it.unisalento.worksitesafety.R;
import it.unisalento.worksitesafety.model.GattAttributes;
import it.unisalento.worksitesafety.view.MachinistConnectActivity;

public class BluetoothLeService extends Service {

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    private String deviceName;
    private Binder binder = new LocalBinder();
    public static final String TAG = "BluetoothLeService";
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private ArrayList<Integer> idsNot = new ArrayList();

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";

    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.action.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.action.EXTRA_DATA";

    public final static UUID UUID_DANGER =
            UUID.fromString(GattAttributes.UUID_DANGER);

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;

    private int connectionState;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain Bluetooth adapter");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.e(TAG, "Bluetooth adapter not initialized or unspecified address.");
            return false;
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            }
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Device not found with provided address.");
            return false;
        }
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        bluetoothGatt.disconnect();
    }

    private void close() {
        if (bluetoothGatt == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT server
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                // Attempts to discover services after successful connection.
                if (ActivityCompat.checkSelfPermission(BluetoothLeService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }
                bluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT server
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered status=" + status);
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            //super.onCharacteristicRead(gatt, characteristic, value, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            //super.onCharacteristicChanged(gatt, characteristic, value);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            showNotification("DANGER", "Danger detected around " + deviceName);
        }
    };

    // Notify the activity when server (dis)connects from GATT server
    private void broadcastUpdate(String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("&02X", byteChar));
            intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
        }
        sendBroadcast(intent);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;
        return bluetoothGatt.getServices();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothGatt not initialized");
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }

        Log.d(TAG, "Enabling notifications");
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (UUID_DANGER.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattAttributes.UUID_DANGER_DESCRIPTOR));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
            Log.d(TAG, "Descriptor value: " + Arrays.toString(descriptor.getValue()));
        }

    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MachinistConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(Notification.PRIORITY_MAX)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(pendingIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Danger", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
        if(idsNot.isEmpty()){
            idsNot.add(0);
        }
        else{
            idsNot.add(idsNot.size());
        }
        notificationManager.notify(idsNot.get(idsNot.size()-1) , notificationBuilder.build());
    }

}