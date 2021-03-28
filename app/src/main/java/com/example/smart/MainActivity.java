package com.example.smart;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.example.smart.ui.home.HomeFragment;
import com.example.smart.ui.home.QRDialogFragment;
import com.example.smart.util.FirebaseUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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

    public BroadcastReceiver receiver;

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

            //Testing notis
            // initialise the channel with channelId: "CHANNEL_ID"
            createNotificationChannel();



            //listen on sth and use lambda expression to create a new view
//            buttonShowNotification.setOnClickListener(view -> {
//                notificationManager.notify(100, builder.build());
//            });

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String message = intent.getStringExtra("message");

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                            .setMessage(message)
                            .setTitle("Alert!")
                            .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            });
                    builder.create().show();

                    Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
                    // flags to associate what to do with an activity
                    notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    //if the pending intent alr exists, keep it but replace extra data with what is in the new intent
                    PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    // Create a noti builder with the channel id + set params of notification (content intent supplies intent, auto cancel means noti is cancelled once clicked)
                    NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(MainActivity.this, "CHANNEL_ID")
                            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
                            .setContentTitle("sMart")
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);

                    // create a manager to perform tasks
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

                    notificationManager.notify(100, notiBuilder.build());

                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
        } else {
            Intent loginPage = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginPage);
            finish();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

//    @Override
//    public void onResume() {
//        super.onResume();
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
//        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
//    }

//    @Override
//    public void onPause() {
//        super.onPause();
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
//    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "myChannel";
            String description = "Channel for notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("CHANNEL_ID", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
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