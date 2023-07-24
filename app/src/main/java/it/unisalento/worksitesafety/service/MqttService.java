package it.unisalento.worksitesafety.service;


import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;

import it.unisalento.worksitesafety.R;
import it.unisalento.worksitesafety.utils.ToastRunnable;
import it.unisalento.worksitesafety.view.MainActivity;


@SuppressWarnings("FieldCanBeLocal")
public class MqttService extends Service {
    // private final String IP = "broker.hivemq.com";
    private final String IP = "192.168.1.9";
    private final String PORT = "1883";

    private Handler mHandler;
    private ArrayList<Integer> idsNot = new ArrayList();
    private static final String REQUEST_ACCEPT = "Notification";

    private static final String TAG = "MqttService";
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;
    private String uniqueID;
    private final String topicBase = "worksite/team_";


    /*class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check Internet connection
            Log.d(TAG, "MQTTBroadcastReceiver: onReceive ");
            boolean hasConnectivity;
            boolean hasChanged = false;
            NetworkInfo[] infos = mConnMan.getAllNetworkInfo();
            for (NetworkInfo info : infos) {
                if (info.getTypeName().equalsIgnoreCase("MOBILE")) {
                    if ((info.isConnected() != hasMmobile)) {
                        hasChanged = true;
                        hasMmobile = info.isConnected();
                    }
                    Log.d(TAG, info.getTypeName() + " is " + info.isConnected());
                } else if (info.getTypeName().equalsIgnoreCase("WIFI")) {
                    if ((info.isConnected() != hasWifi)) {
                        hasChanged = true;
                        hasWifi = info.isConnected();
                    }
                    Log.d(TAG, info.getTypeName() + " is " + info.isConnected());
                }
            }
            hasConnectivity = hasMmobile || hasWifi;
            Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - " + (mqttClient == null || !mqttClient.isConnected()));
            if (hasConnectivity && hasChanged && (mqttClient == null || !mqttClient.isConnected())) {
                connectMQTT();
            }
        }
    }*/

    @Override
    public void onCreate() {
        Log.d(TAG, "MqttService: onCreate ");

        mHandler = new Handler();//for toasts
        /*IntentFilter intentf = new IntentFilter();
        setClientID();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);*/
        setClientID();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand()");
        connectMQTT(topicBase + intent.getStringExtra("team code"));
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "MqttService: onConfigurationChanged ");

        Log.d(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        disconnectMQTT();
        super.onDestroy();
    }


    @SuppressLint("HardwareIds")
    private void setClientID() {
        uniqueID = "ANDROID:"+android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        Log.d(TAG, "uniqueID=" + uniqueID);
    }


    private void connectMQTT(String topic) {
        String broker = "tcp://" + IP + ":" + PORT;
        Log.d(TAG, "MQTT: connecting");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setMaxInflight(100);//handle more messages!!so as not to disconnect
        options.setAutomaticReconnect(true);
        try {
            mqttClient = new MqttAsyncClient(broker, uniqueID, new MemoryPersistence());
            token = mqttClient.connect(options);
            token.waitForCompletion(3500);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    try {
                        mqttClient.disconnectForcibly();
                        mqttClient.connect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage msg){
                    Log.i(TAG, "MQTT: message arrived from " + topic);
                    Log.i(TAG, "MQTT: '" + msg.toString() + "'");
                    showNotification("DANGER", msg.toString());
                    /*LocalBroadcastManager broadcaster = LocalBroadcastManager.getInstance(getBaseContext());
                    Intent intent = new Intent(REQUEST_ACCEPT);
                    intent.putExtra("status", msg.toString());
                    broadcaster.sendBroadcast(intent);*/
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    System.out.println("published");
                }
            });

            mqttClient.subscribe(topic, 2);
            Log.i(TAG,"MQTT: subscribing to " + topic);

        } catch (MqttSecurityException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                    mHandler.post(new ToastRunnable("Broker unavailable", 1500, getApplicationContext()));
                    break;
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                    mHandler.post(new ToastRunnable("Client time-out", 1500, getApplicationContext()));
                    break;
                case MqttException.REASON_CODE_CONNECTION_LOST:
                    mHandler.post(new ToastRunnable("Connection lost", 1500, getApplicationContext()));
                    break;
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" + e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b" + e.getMessage());
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage());
            }
        }
        mHandler.post(new ToastRunnable("Connected", 500, getApplicationContext()));

    }

    private void disconnectMQTT() {
        try{
            mqttClient.disconnect();
            Log.d(TAG, "MQTT: disconnected");
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mHandler.post(new ToastRunnable("Disconnected",1500,getApplicationContext()));
    }

    private void showNotification(String title, String message) {
        Intent intent = new Intent(this, MainActivity.class);
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