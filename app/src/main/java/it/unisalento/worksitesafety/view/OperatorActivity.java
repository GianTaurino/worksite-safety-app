package it.unisalento.worksitesafety.view;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import it.unisalento.worksitesafety.R;
import it.unisalento.worksitesafety.service.BeaconService;
import it.unisalento.worksitesafety.service.MqttService;

public class OperatorActivity extends AppCompatActivity {

    private Intent mqttIntent;
    private Intent beaconIntent;
    private TextView textView;
    private EditText inputText;
    private Button connectButton;
    private Button disconnectButton;
    private static final String TAG = "OperatorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator);

        textView = findViewById(R.id.op_text_view);
        textView.setText("Insert your team number below");
        inputText = findViewById(R.id.teamNumberBox);
        connectButton = findViewById(R.id.but_op_conn);
        disconnectButton = findViewById(R.id.but_op_dis);
        disconnectButton.setVisibility(View.GONE);

        checkPermission(Manifest.permission.BLUETOOTH_ADVERTISE);
        // set intent for beacon
        beaconIntent = new Intent(this, BeaconService.class);
        startService(beaconIntent);
    }

    @Override
    protected void onStop() {
        stopService(mqttIntent);
        stopService(beaconIntent);
        super.onStop();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        stopService(mqttIntent);
        stopService(beaconIntent);
        super.onBackPressed();
    }

    public void pushConnect(View view) {
        if (!inputText.getText().toString().isEmpty()) {
            // get team code from input
            String teamCode = String.valueOf(inputText.getText());
            Log.d(TAG, "User selected team " + teamCode);
            // set UI
            inputText.setVisibility(View.GONE);
            connectButton.setVisibility(View.GONE);
            disconnectButton.setVisibility(View.VISIBLE);
            textView.setText("You selected Team " + teamCode + "\n\n You will receive notifications\nin case of danger");
            // set intent for MQTT with extra
            mqttIntent = new Intent(this, MqttService.class)
                    .putExtra("team code", teamCode);
            startService(mqttIntent);
        }

    }


    public void pushDisconnect(View view) {
        stopService(mqttIntent);
        textView.setText("Insert your team number below");
        inputText.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.GONE);
    }

    public void checkPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission);
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
                    this.onBackPressed();
                }
            });


}