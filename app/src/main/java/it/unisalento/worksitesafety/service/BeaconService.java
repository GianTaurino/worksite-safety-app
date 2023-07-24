package it.unisalento.worksitesafety.service;

import static android.content.Intent.getIntent;

import android.Manifest;
import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;

import it.unisalento.worksitesafety.utils.ToastRunnable;
import it.unisalento.worksitesafety.view.OperatorActivity;

public class BeaconService extends Service {

    public static final String TAG = "BeaconService";
    BeaconTransmitter beaconTransmitter;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        // UUID
        // AltBeacon: 2f234454-cf6d-4a0f-adf2-f4911ba9ffa6
        // iBeacon:   f7826da6-4fa2-4e98-8024-bc5b71e0893e
        //
        // Manufacturer
        // AltBeacon: 0x0118
        // iBeacon:   0x4c
        Beacon beacon = new Beacon.Builder()
                .setId1("f7826da6-4fa2-4e98-8024-bc5b71e0893e")
                .setId2("1")
                .setId3("2")
                .setManufacturer(0x4c) // Radius Networks.  Change this for other beacon layouts
                .setTxPower(-59)
                .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                .build();
        // Change the layout below for other beacon types, is set for AltBeacon
        // AltBeacon: m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25
        // iBeacon:   m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24");
        beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertisement start failed with code: "+errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "Advertisement start succeeded.");
            }
        });
        mHandler.post(new ToastRunnable("Transmitting beacon",500,getApplicationContext()));

    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        beaconTransmitter.stopAdvertising();
        mHandler.post(new ToastRunnable("Transmission stopped",500,getApplicationContext()));
        super.onDestroy();
    }
}