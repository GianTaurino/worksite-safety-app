package it.unisalento.worksitesafety.view;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import it.unisalento.worksitesafety.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission(Manifest.permission.POST_NOTIFICATIONS);

        Button buttonOperator = findViewById(R.id.but_op);
        buttonOperator.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("BUTTONS", "User tapped operator button");
                        Intent i = new Intent(MainActivity.this, OperatorActivity.class);
                        startActivity(i);
                    }
                }
        );

        Button buttonMachinist = findViewById(R.id.but_mac);
        buttonMachinist.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d("BUTTONS", "User tapped machinist button");
                        Intent i = new Intent(MainActivity.this, MachinistScanActivity.class);
                        startActivity(i);
                    }
                }
        );

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