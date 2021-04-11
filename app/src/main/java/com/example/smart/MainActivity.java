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
import android.util.Log;

import com.example.smart.model.Item;
import com.example.smart.model.RecommendationHistory;
import com.example.smart.model.enums.Enums;
import com.example.smart.ui.home.HomeFragment;
import com.example.smart.ui.home.ProfileDialogFragment;
import com.example.smart.ui.home.QRDialogFragment;
import com.example.smart.util.FirebaseUtil;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity
        implements HomeFragment.OnFragmentInteractionListener {

    private static final String TAG = "MAIN_ACTIVITY";
    private static final String TAG_NOTIFICATION = "MAIN_ACTIVITY_NOTIFICATION";

    public List<String> PERMISSIONS_REQUIRED = Arrays.asList(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.VIBRATE
    );

    public BroadcastReceiver receiver;
    private static final String INTENT_ACTION_DISPLAY_ITEM = "DISPLAY_ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkPermissions();

        setTheme(R.style.Theme_SMart);
        super.onCreate(savedInstanceState);
        if (FirebaseUtil.isSignedIn()) {
            setContentView(R.layout.activity_main);
            FirebaseUtil.initCart();
            initFirebaseMessagingToken();
            BottomNavigationView navView = findViewById(R.id.nav_view);
            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home, R.id.navigation_items, R.id.navigation_cart, R.id.navigation_transactions)
                    .build();
            Toolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case R.id.profile:
                        new ProfileDialogFragment().show(getSupportFragmentManager(), TAG);
                        return true;
                    default:
                        return false;
                }
            });
            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);

            // initialise the channel with channelId: "CHANNEL_ID"
            createNotificationChannel();

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String receivedItemId = intent.getStringExtra("itemId");
                    // STEP 1 - CHECKING IF ITEM EXISTS
                    FirebaseUtil.getItemsRef().document(receivedItemId).get().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            DocumentSnapshot snapshot = task1.getResult();
                            if (snapshot.exists()) {
                                Item item = snapshot.toObject(Item.class);
                                Log.i(TAG_NOTIFICATION, "Successfully retrieved from Firestore \n\titem id: " + receivedItemId + "\n\titem name: " + item.getName());
                                // STEP 2 - RETRIEVING RECOMMENDATION INTERVAL
                                FirebaseUtil.getUserDocRef().get().addOnCompleteListener(task2 -> {
                                    Enums.RECOMMENDATION_ENUM recommendation_interval = Enums.RECOMMENDATION_ENUM.MIN;
                                    if (task2.isSuccessful()) {
                                        DocumentSnapshot snapshot2 = task2.getResult();
                                        Map<String, Object> map = snapshot2.getData();
                                        try {
                                            recommendation_interval = Enums.RECOMMENDATION_ENUM.valueOf((String) map.get(FirebaseUtil.USER_PROFILE_RECOMMENDATION));
                                            Log.i(TAG, "RETRIEVED RESULT RECOMMENDATION: " + recommendation_interval);
                                            if (recommendation_interval == null)
                                                throw new Exception();
                                        } catch (Exception ex) {
                                            recommendation_interval = Enums.RECOMMENDATION_ENUM.MIN;
                                        }
                                    }
                                    if (recommendation_interval == Enums.RECOMMENDATION_ENUM.OFF) {
                                        return;
                                    }
                                    Date dateRecommendationReceived = new Date();
                                    Date recommendationsAfterThisDate = new Date(dateRecommendationReceived.getTime() - recommendation_interval.getMilliseconds());
                                    Log.i(TAG, "\n\tRECOMMENDATION TYPE: " + recommendation_interval +
                                            "\n\tLOOK FOR RECOMMENDATIONS AFTER DATE: " + recommendationsAfterThisDate);
                                    Query query = FirebaseUtil.getUserDocRef().collection(FirebaseUtil.USER_RECOMMENDATION_HISTORY_COLLECTION_NAME)
                                            .whereEqualTo("itemId", receivedItemId)
                                            .orderBy("recommendationDateTime", Query.Direction.DESCENDING)
                                            .whereGreaterThan("recommendationDateTime", recommendationsAfterThisDate)
                                            .limit(1);
                                    query.get().addOnCompleteListener(task3 -> {
                                        if (task3.isSuccessful()) {
                                            List<DocumentSnapshot> results = task3.getResult().getDocuments();
                                            if (results.isEmpty()) {
                                                Log.i(TAG,"NO RECOMMENDATION HISTORY FOUND: performing notification now");
                                                performNotification(item, navController, intent);
                                                RecommendationHistory recommendationHistory = new RecommendationHistory(receivedItemId, dateRecommendationReceived);
                                                FirebaseUtil.getUserDocRef().collection(FirebaseUtil.USER_RECOMMENDATION_HISTORY_COLLECTION_NAME).add(recommendationHistory);
                                            } else {
                                                Log.i(TAG,"RECOMMENDATION HISTORY FOUND WITHIN RANGE: SKIPPING NOTIFICATION");
                                            }
                                        }
                                    });
                                });
                            } else {
                                Log.e(TAG_NOTIFICATION, "Failed to retrieve from Firestore item id: " + receivedItemId);
                            }
                        } else {
                            Log.e(TAG_NOTIFICATION, "Failed to call Firestore to check received item id: " + receivedItemId);
                        }
                    });
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_SEND);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);

            Intent receivedIntent = getIntent();
            if (receivedIntent.getAction() != null && receivedIntent.getAction().equals(INTENT_ACTION_DISPLAY_ITEM)) {
                navController.navigate(R.id.navigation_items, receivedIntent.getExtras());
            }
        } else {
            Intent loginPage = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(loginPage);
            finish();
        }
    }

    private void initFirebaseMessagingToken() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String token = instanceIdResult.getToken();
                String userId = FirebaseUtil.getCurrentUserUid();

                Log.i(TAG, "\nFirebase Messaging Token Successfully Retrieved:\n\tTOKEN IS: " + token + "\n\tUSERID IS: " + userId);

                Map<String, Object> data = new HashMap<>();
                data.put(FirebaseUtil.USER_FMS_TOKEN, token);

                DocumentReference docRef = FirebaseUtil.getUserDocRef();
                docRef.set(data, SetOptions.merge()).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.i(TAG, "Successful write of token to Firestore");
                    } else {
                        Log.e(TAG, "Unsuccessful write. Token not written to Firestore");
                        Log.e(TAG, task.getException().getMessage());
                    }
                });
                // send it to server
            }
        });
    }

    private void performNotification(Item item, NavController navController, Intent intent) {
        Log.i(TAG_NOTIFICATION, "Performing notification creation of item: " + item.getName());
        String header = intent.getStringExtra("header");
        String message = intent.getStringExtra("message");

        Intent notificationIntent = new Intent(MainActivity.this, MainActivity.class);
        // flags to associate what to do with an activity
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationIntent.setAction(INTENT_ACTION_DISPLAY_ITEM);
        notificationIntent.putExtras(intent);


        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setTitle(header)
                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        navController.navigate(R.id.navigation_items, notificationIntent.getExtras());
                    }
                });
        builder.create().show();


        //if the pending intent alr exists, keep it but replace extra data with what is in the new intent
        PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Create a noti builder with the channel id + set params of notification (content intent supplies intent, auto cancel means noti is cancelled once clicked)
        NotificationCompat.Builder notiBuilder = new NotificationCompat.Builder(MainActivity.this, "CHANNEL_ID")
                .setSmallIcon(R.drawable.ic_smart_cart_green_transparent)
                .setContentTitle(header)
                .setContentText(item.getName())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // create a manager to perform tasks
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);

        notificationManager.notify(100, notiBuilder.build());
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

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