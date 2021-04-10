package com.example.smart.ui.home;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.smart.MainActivity;
import com.example.smart.R;
import com.example.smart.util.FirebaseUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileDialogFragment extends DialogFragment {

    private static final String TAG = "PROFILE_DIALOG_FRAGMENT";

    public ProfileDialogFragment() {
    }

    public enum SHOPPING_HABITS_ENUM {
        SAVER,
        MODERATE,
        QUALITY;
    }

    public enum RECOMMENDATION_ENUM {
        MIN,
        HOUR,
        DAY;
    }

    Button buttonLogout;
    RadioGroup rgShoppingHabits;
    RadioGroup rgRecommendations;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.dialog_profile, null);
        rgShoppingHabits = view.findViewById(R.id.radioGroup_shopping_habits);
        rgRecommendations = view.findViewById(R.id.radioGroup_recommendation);
        buttonLogout = view.findViewById(R.id.button_logout);
        buttonLogout.setOnClickListener(v -> {
                    AuthUI.getInstance()
                            .signOut(v.getContext())
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                public void onComplete(Task<Void> task) {
                                    ((MainActivity) v.getContext()).recreate();
                                }
                            });
                }
        );

        DocumentReference userDocRef = FirebaseUtil.getUserDocRef();
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                Map<String, Object> map = snapshot.getData();
                SHOPPING_HABITS_ENUM habit;
                try {
                    habit = SHOPPING_HABITS_ENUM.valueOf((String)map.get(FirebaseUtil.USER_PROFILE_HABIT));
                    Log.e(TAG, "RETRIEVED RESULT HABIT: " + habit);
                    if (habit == null) throw new Exception();
                } catch (Exception ex) {
                    habit = SHOPPING_HABITS_ENUM.MODERATE;
                }
                RECOMMENDATION_ENUM recommendation_interval;
                try {
                    recommendation_interval = RECOMMENDATION_ENUM.valueOf((String)map.get(FirebaseUtil.USER_PROFILE_RECOMMENDATION));
                    Log.e(TAG, "RETRIEVED RESULT RECOMMENDATION: " + recommendation_interval);
                    if (recommendation_interval == null) throw new Exception();
                } catch (Exception ex) {
                    recommendation_interval = RECOMMENDATION_ENUM.MIN;
                }

                switch (habit) {
                    case SAVER:
                        rgShoppingHabits.check(R.id.radio_shopping_0);
                        break;
                    case MODERATE:
                        rgShoppingHabits.check(R.id.radio_shopping_1);
                        break;
                    case QUALITY:
                        rgShoppingHabits.check(R.id.radio_shopping_2);
                        break;
                    default:
                        rgShoppingHabits.check(R.id.radio_shopping_1);
                        break;
                }

                switch (recommendation_interval) {
                    case MIN:
                        rgRecommendations.check(R.id.radio_recommendation_0);
                        break;
                    case HOUR:
                        rgRecommendations.check(R.id.radio_recommendation_1);
                        break;
                    case DAY:
                        rgRecommendations.check(R.id.radio_recommendation_2);
                        break;
                    default:
                        rgRecommendations.check(R.id.radio_recommendation_0);
                        break;
                }
            } else {
                Log.e(TAG, "Failed to retrieve from Firestore...");
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        Dialog dialog = builder
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int shoppingRadioId = rgShoppingHabits.getCheckedRadioButtonId();
                        int recommendationRadioId = rgRecommendations.getCheckedRadioButtonId();

                        SHOPPING_HABITS_ENUM selectedShoppingHabit;
                        RECOMMENDATION_ENUM selectedRecommendation;
                        switch (shoppingRadioId) {
                            case R.id.radio_shopping_0:
                                selectedShoppingHabit = SHOPPING_HABITS_ENUM.SAVER;
                                break;
                            case R.id.radio_shopping_1:
                                selectedShoppingHabit = SHOPPING_HABITS_ENUM.MODERATE;
                                break;
                            case R.id.radio_shopping_2:
                                selectedShoppingHabit = SHOPPING_HABITS_ENUM.QUALITY;
                                break;
                            default:
                                selectedShoppingHabit = SHOPPING_HABITS_ENUM.MODERATE;
                        }
                        switch (recommendationRadioId) {
                            case R.id.radio_recommendation_0:
                                selectedRecommendation = RECOMMENDATION_ENUM.MIN;
                                break;
                            case R.id.radio_recommendation_1:
                                selectedRecommendation = RECOMMENDATION_ENUM.HOUR;
                                break;
                            case R.id.radio_recommendation_2:
                                selectedRecommendation = RECOMMENDATION_ENUM.DAY;
                                break;
                            default:
                                selectedRecommendation = RECOMMENDATION_ENUM.MIN;
                        }
                        Map<String, Object> data = new HashMap<>();
                        data.put(FirebaseUtil.USER_PROFILE_HABIT, selectedShoppingHabit.toString());
                        data.put(FirebaseUtil.USER_PROFILE_RECOMMENDATION, selectedRecommendation.toString());

                        userDocRef.set(data, SetOptions.merge()).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.i(TAG, "Successful write of profile to Firestore");
                            } else {
                                Log.e(TAG, "Unsuccessful write. Profile not written to Firestore");
                                Log.e(TAG, task.getException().getMessage());
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        return dialog;
    }

    private void toastMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
        });
    }
}