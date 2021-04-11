package com.example.smart;

import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FIREBASE_MESSAGING_SERVICE";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "Received remote message from Firebase Messaging Service");
        passMessageToActivity(remoteMessage.getData());
    }

    private void passMessageToActivity(Map<String, String> dataMap) {

        String itemId = dataMap.get("itemId");
        String header = dataMap.get("header");
        String message = dataMap.get("message");
        Log.i(TAG, "Processing remote message:" +
                "\n\titemId:\t" + itemId +
                "\n\theader:\t" + header +
                "\n\tmessage:\t" + message);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra("itemId", itemId);
        intent.putExtra("header", header);
        intent.putExtra("message", message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
