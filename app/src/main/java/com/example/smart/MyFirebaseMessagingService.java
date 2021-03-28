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

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        Log.d("TAG", "The token refreshed: " + token);

        sendRegistrationToServer("1", token);
    }

    private void sendRegistrationToServer(String userId, String token) {
//        URL url = null;
//        try {
//            url = new URL("http://localhost:5000/register?userId=" + userId + "&fcmToken=" + token);
//            Log.d("TAG", "URL is:" + url);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//        URLConnection con = null;
//        try {
//            con = url.openConnection();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        HttpURLConnection http = (HttpURLConnection)con;
//        try {
//            http.setRequestMethod("GET"); // PUT is another valid option
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        }
//        http.setDoOutput(true);

        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://localhost:5000/register?userId=" + userId + "&fcmToken=" + token;
        Log.d("TAG", "URL is:" + url);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("TAG","Response is: "+ response.substring(0,500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("TAG","That didn't work!");
            }
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String messageReceived = remoteMessage.getData().get("message");
        Log.d("TAG", "Received message: " + messageReceived);

        passMessageToActivity(messageReceived);
    }

    private void passMessageToActivity(String message) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra("message", message);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
