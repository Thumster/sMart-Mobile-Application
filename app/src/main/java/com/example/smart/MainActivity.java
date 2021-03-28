package com.example.smart;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.smart.ui.home.HomeFragment;
import com.example.smart.ui.home.QRDialogFragment;
import com.example.smart.util.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity
        implements HomeFragment.OnFragmentInteractionListener {

    private static final String TAG = "MAIN_ACTIVITY";

    public List<String> PERMISSIONS_REQUIRED = Arrays.asList(Manifest.permission.CAMERA);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkPermissions();

        setTheme(R.style.Theme_SMart);
        super.onCreate(savedInstanceState);
        if (FirebaseUtil.isSignedIn()) {
            setContentView(R.layout.activity_main);
            FirebaseUtil.initCart();
            BottomNavigationView navView = findViewById(R.id.nav_view);
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_items, R.id.navigation_cart, R.id.navigation_transactions)
                    .build();
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
        } else {
            Intent loginPage = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginPage);
            finish();
        }
    }

    public static final int MULTIPLE_PERMISSIONS = 100;

    // function to check permissions
    private void checkPermissions() {
        List<String> remainingPermissions = getRemainingPermissions();

        if (!remainingPermissions.isEmpty()) {
            requestPermissions(remainingPermissions.toArray(new String[0]), MULTIPLE_PERMISSIONS);
        }
    }

    private List<String> getRemainingPermissions() {
        return PERMISSIONS_REQUIRED.stream()
                .filter(permission -> !isPermissionAllowed(permission))
                .collect(Collectors.toList());
    }

    private boolean isPermissionAllowed(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    // Function to initiate after permissions are given by user
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS:
                if (grantResults.length > 0) {
                    Arrays.asList(permissions)
                            .stream()
                            .filter(permission -> shouldShowRequestPermissionRationale(permission))
                            .forEach(permission -> {
                                new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                                        .setMessage("Press Permissions to Decide Permission Again")
                                        .setPositiveButton("Permissions", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                checkPermissions();
                                            }
                                        })
                                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .create()
                                        .show();
                            });
                }

        }
    }

    @Override
    public void onSelectScanBasket(QRDialogFragment.QRDialogListener listener) {
        Boolean cameraAllowed = isPermissionAllowed(Manifest.permission.CAMERA);
        if (cameraAllowed) {
            Log.w(TAG, "Camera allowed");
            QRDialogFragment qrDialogFragment = new QRDialogFragment(listener);
            qrDialogFragment.show(getSupportFragmentManager(), TAG);
//            Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "Camera not allowed. Requesting permission");
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MULTIPLE_PERMISSIONS);
        }
    }

}