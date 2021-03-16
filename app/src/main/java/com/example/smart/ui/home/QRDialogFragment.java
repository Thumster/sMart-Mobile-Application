package com.example.smart.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart.R;
import com.example.smart.util.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.IOException;
import java.util.Map;

public class QRDialogFragment extends DialogFragment {

    private static final String TAG = "QR_DIALOG_FRAGMENT";

    public interface QRDialogListener {
        public void onScanResultListener(String cartId);
    }

    private QRDialogListener qrDialogListener;

    private SurfaceView cameraView;

    private String detectedText = "";

    public QRDialogFragment(QRDialogListener qrDialogListener) {
        this.qrDialogListener = qrDialogListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_qr, null);
        cameraView = view.findViewById(R.id.qrcode_surfaceView);

        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        CameraSource cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                try {
                    cameraSource.start(cameraView.getHolder());
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    String value = String.valueOf((barcodes.valueAt(0).displayValue));
                    if (value != null && !detectedText.equals(value)) {
                        vibratePhone();
                        detectedText = value;
                        scanCart(detectedText);
                    }
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_SMart_FullScreenDialog);
        builder.setView(view);
        Dialog dialog = builder.create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }

    private void scanCart(String scannedStr) {
        Log.i(TAG, "ATTEMPTING TO SCAN CART ID: " + scannedStr);
        if (scannedStr.equals(null) || scannedStr.equals("")) {
            Log.i(TAG, "Unable to query empty/null string");
            return;
        } else if (FirebaseUtil.getIsCurrentlyShopping()) {
            Log.i(TAG, "User is already tagged to a shopping cart: "
                    + FirebaseUtil.getCurrentUserCartId());
            toastMessage("You are already registered to a shopping cart: "
                    + FirebaseUtil.getCurrentUserCartId());

            return;
        }
        DocumentReference cartDocRef = FirebaseUtil.getCartsRef().document(scannedStr);
        Task<DocumentSnapshot> documentSnapshotTask = cartDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Object userIdCheck = document.get(FirebaseUtil.CART_USER_DOC_NAME);
                    if (userIdCheck != null) {
                        Log.i(TAG, "Cart is currently in use");
                        toastMessage("The cart is currently in use");
                        return;
                    }
                    Log.i(TAG, "Cart is available");
                    Map<String, Object> map = document.getData();
                    map.put(FirebaseUtil.CART_USER_DOC_NAME, FirebaseUtil.getCurrentUserUid());
                    cartDocRef.set(map).addOnCompleteListener(task2 -> {
                        if (task2.isSuccessful()) {
                            qrDialogListener.onScanResultListener(scannedStr);
                            Log.i(TAG, "Successfully registered cart");
                            toastMessage("Successfully registered cart!");
                            dismiss();
                        } else {
                            Log.i(TAG, "Failed to set cart");
                            return;
                        }
                    });
                } else {
                    Log.i(TAG, "Cart Id: " + scannedStr + " does not exist");
                    toastMessage("Please scan a valid cart");
                    return;
                }
            } else {
                Log.e(TAG, "firestore get failed with ", task.getException());
            }
        });
    }

    private void toastMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void vibratePhone() {
        Vibrator vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            vibrator.vibrate(500);
        }
    }
}