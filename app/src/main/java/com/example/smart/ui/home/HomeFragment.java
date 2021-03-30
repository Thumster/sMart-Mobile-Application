package com.example.smart.ui.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.estimote.indoorsdk.EstimoteCloudCredentials;
import com.estimote.indoorsdk.IndoorLocationManagerBuilder;
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener;
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager;
import com.estimote.indoorsdk_module.cloud.CloudCallback;
import com.estimote.indoorsdk_module.cloud.EstimoteCloudException;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManager;
import com.estimote.indoorsdk_module.cloud.IndoorCloudManagerFactory;
import com.estimote.indoorsdk_module.cloud.Location;
import com.estimote.indoorsdk_module.cloud.LocationPosition;
import com.estimote.indoorsdk_module.view.IndoorLocationView;
import com.example.smart.MainActivity;
import com.example.smart.R;
import com.example.smart.util.FirebaseUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment
        implements FirebaseUtil.OnCartFound, QRDialogFragment.QRDialogListener {

    public interface OnFragmentInteractionListener {
        void onSelectScanBasket(QRDialogFragment.QRDialogListener listener);
    }

    OnFragmentInteractionListener listener;

    private static final String TAG = "HOME_FRAGMENT";
    private final NumberFormat DECIMAL_FORMATTER = new DecimalFormat("#0.00");

    private final int TABLE_LENGTH = 3;
    private final int TABLE_WIDTH = 10;
    private final int DISTANCE_FROM_MAIN_DOOR = 8;
    private final int DISTANCE_BETWEEN_TABLE_IN_LENGTH = 4;
    private final int DISTANCE_BETWEEN_TABLE_IN_WIDTH = 28 - 2 * TABLE_WIDTH;

    private double pixelsPerUnitWidth;
    private double pixelsPerUnitLength;

    View shoppingLayout;
    View notShoppingLayout;

    TextView welcomeTextView;
    TextView nameTextView;
    TextView userCartTextView;
    Button buttonLogout;
    Button buttonUnregisterCart;
    Button buttonQrCode;

    Location location;
    // TextView coordinateTextView;
    // IndoorLocationView indoorLocationView;
    ScanningIndoorLocationManager indoorLocationManager;
    LocationPosition currentPosition;

    Paint wallPaint = new Paint(); // Stores how to draw, e.g., color, style, line thickness, text size, etc
    Paint tablePaint = new Paint();
    Paint personPaint = new Paint();

    Rect wallRectangle = new Rect(); // Rectangle
    Rect tableRectangle = new Rect(); // Rectangle

    Canvas canvasIndoorMap; // Stores information on what to draw onto its associated bitmap, e.g., lines, circles, text, custom paths, etc
    ImageView imageViewIndoorMap; // Container for bitmap
    Bitmap bitmapIndoorMap; // Represents the pixels that are shown on the display

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        notShoppingLayout = root.findViewById(R.id.layout_not_shopping);
        shoppingLayout = root.findViewById(R.id.layout_shopping);

        nameTextView = root.findViewById(R.id.text_name);
        buttonLogout = root.findViewById(R.id.button_logout);
        buttonUnregisterCart = root.findViewById(R.id.button_unregister_cart);
        userCartTextView = root.findViewById(R.id.text_user_cart);
        buttonQrCode = root.findViewById(R.id.fab_qr_code);
        welcomeTextView = root.findViewById(R.id.text_welcome);

        imageViewIndoorMap = root.findViewById(R.id.image_view_indoor_map);
        // indoorLocationView = (IndoorLocationView) root.findViewById(R.id.indoorLocationView);
        // coordinateTextView = (TextView) root.findViewById(R.id.coordinateTextView);

        nameTextView.setText(FirebaseUtil.getCurrentUser()
                                         .getDisplayName());
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
        FirebaseUtil.startListening(this);
        onCartFound(FirebaseUtil.getCurrentUserCartId() != null);
        buttonUnregisterCart.setOnClickListener(v -> {
            unregisterCart();
        });
        buttonQrCode.setOnClickListener(v -> {
            listener.onSelectScanBasket(this);
        });

        imageViewIndoorMap.setOnClickListener(view -> {
            // A bitmap configuration describes how pixels are stored.
            bitmapIndoorMap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
            imageViewIndoorMap.setImageBitmap(bitmapIndoorMap);
            canvasIndoorMap = new Canvas(bitmapIndoorMap); // Drawing on the canvas draws on the bitmap.

            pixelsPerUnitWidth = view.getWidth() / 28.00;
            pixelsPerUnitLength = view.getHeight() / 37.00;

            // Log.i("Width", Integer.toString(view.getWidth()));
            // Log.i("Pixels Per Unit Width", Double.toString(pixelsPerUnitWidth));
            // Log.i("Height", Integer.toString(view.getHeight()));
            // Log.i("Pixels Per Unit Length", Double.toString(pixelsPerUnitLength));

            wallPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
            tablePaint.setColor(ResourcesCompat.getColor(getResources(), R.color.dark_green_variant, null));
            personPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.light_blue, null));

            wallRectangle.set(0, 0, (int) (TABLE_WIDTH * pixelsPerUnitWidth), (int) (DISTANCE_FROM_MAIN_DOOR * pixelsPerUnitLength));
            canvasIndoorMap.drawRect(wallRectangle, wallPaint);

            for (int i = 0; i < 4; i++) {
                tableRectangle.set(
                        0,
                        (int) ((DISTANCE_FROM_MAIN_DOOR + (DISTANCE_BETWEEN_TABLE_IN_LENGTH + TABLE_LENGTH) * i) * pixelsPerUnitLength),
                        (int) (TABLE_WIDTH * pixelsPerUnitWidth),
                        (int) ((DISTANCE_FROM_MAIN_DOOR + ((i + 1) * TABLE_LENGTH + i * DISTANCE_BETWEEN_TABLE_IN_LENGTH)) * pixelsPerUnitLength)
                );
                canvasIndoorMap.drawRect(tableRectangle, tablePaint);
            }

            for (int i = 0; i < 4; i++) {
                tableRectangle.set(
                        (int) ((TABLE_WIDTH + DISTANCE_BETWEEN_TABLE_IN_WIDTH) * pixelsPerUnitWidth),
                        (int) ((DISTANCE_FROM_MAIN_DOOR + (DISTANCE_BETWEEN_TABLE_IN_LENGTH + TABLE_LENGTH) * i) * pixelsPerUnitLength),
                        view.getWidth(),
                        (int) ((DISTANCE_FROM_MAIN_DOOR + ((i + 1) * TABLE_LENGTH + i * DISTANCE_BETWEEN_TABLE_IN_LENGTH)) * pixelsPerUnitLength)
                );
                canvasIndoorMap.drawRect(tableRectangle, tablePaint);
            }

            if (currentPosition != null) {
                float currentX = (float) (currentPosition.getX() * pixelsPerUnitWidth);
                float currentY = (float) ((9.28 - currentPosition.getY()) * pixelsPerUnitLength);
                canvasIndoorMap.drawCircle(currentX / 0.25f, currentY / 0.25f, (float) (view.getWidth() / 30), personPaint);

                // Log.i("Current X", Float.toString(currentX / 0.25f));
                // Log.i("Current Y", Float.toString(currentY / 0.25f));
            }

            view.invalidate();
        });

        return root;
    }

    private void instantiateIndoorPositioning() {
        IndoorCloudManager cloudManager = new IndoorCloudManagerFactory().create(
                getContext(),
                new EstimoteCloudCredentials("smart-testing-gel", "f0bbad4d22be585f7880f34dad91a7e4")
        );

        cloudManager.getLocation("smart-test-2", new CloudCallback<Location>() {
            @Override
            public void success(Location location) {
                HomeFragment.this.location = location;

                // indoorLocationView.setLocation(location);
                indoorLocationManager = new IndoorLocationManagerBuilder(
                        getContext(),
                        location,
                        new EstimoteCloudCredentials("smart-testing-gel", "f0bbad4d22be585f7880f34dad91a7e4")
                ).withDefaultScanner()
                 .build();

                indoorLocationManager.setOnPositionUpdateListener(new OnPositionUpdateListener() {
                    @Override
                    public void onPositionUpdate(@NotNull LocationPosition locationPosition) {
                        currentPosition = locationPosition;
                        // indoorLocationView.updatePosition(locationPosition);

                        // LocationPosition lp = new LocationPosition(7.05, 9.28, 0.00);
                        // indoorLocationView.updatePosition(lp);

                        // String position = "X: " + DECIMAL_FORMATTER.format(locationPosition.getX())
                        //        + "; Y: " + DECIMAL_FORMATTER.format(locationPosition.getY())
                        //        + "; O: " + locationPosition.getOrientation();

                        // Log.i("onPositionUpdate", position);
                        // coordinateTextView.setText(position);
                    }

                    @Override
                    public void onPositionOutsideLocation() {
                        // indoorLocationView.hidePosition();
                        // Log.i("onPositionOutsideLocation", "Outside...");
                        // coordinateTextView.setText("Out of range");
                    }
                });

                indoorLocationManager.startPositioning();
            }

            @Override
            public void failure(@NotNull EstimoteCloudException e) {
                Log.e("Estimote Location Error", e.toString());
            }
        });
    }

    @Override
    public void onCartFound(Boolean found) {
        if (found) {
            notShoppingLayout.setVisibility(View.GONE);
            shoppingLayout.setVisibility(View.VISIBLE);
            userCartTextView.setText(FirebaseUtil.getCurrentUserCartId());

            buttonQrCode.setVisibility(View.GONE);
//            buttonUnregisterCart.setVisibility(View.VISIBLE);
//            userCartTextView.setVisibility(View.VISIBLE);

            instantiateIndoorPositioning();
        } else {
            notShoppingLayout.setVisibility(View.VISIBLE);
            shoppingLayout.setVisibility(View.GONE);

            buttonQrCode.setVisibility(View.VISIBLE);
//            userCartTextView.setVisibility(View.GONE);
//            buttonUnregisterCart.setVisibility(View.GONE);

            if (indoorLocationManager != null) {
                indoorLocationManager.stopPositioning();
            }
        }
    }

    @Override
    public void onScanResultListener(String cartId) {
        updateCartToRegistered(cartId);
    }

    private void unregisterCart() {
        Log.i(TAG, "ATTEMPTING TO UNREGISTER CART ID: " + FirebaseUtil.getCurrentUserCartId());
        if (!FirebaseUtil.getIsCurrentlyShopping()) {
            Log.i(TAG, "User is not registered to a shopping cart");
            Toast.makeText(getContext(), "You are not currently shopping", Toast.LENGTH_LONG)
                 .show();
            return;
        }
        DocumentReference cartDocRef = FirebaseUtil.getCartsRef()
                                                   .document(FirebaseUtil.getCurrentUserCartId());
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseUtil.CART_USER_DOC_NAME, FieldValue.delete());
        cartDocRef.update(updates)
                  .addOnCompleteListener(task -> {
                      if (task.isSuccessful()) {
                          Log.i(TAG, "Successfully unregistered cart");
                          updateCartToRegistered(null);
                          Toast.makeText(getContext(), "Successfully unregistered cart!", Toast.LENGTH_LONG)
                               .show();
                      } else {
                          Log.e(TAG, "firestore update failed with ", task.getException());
                      }
                  });
    }

    private void updateCartToRegistered(String cartId) {
        Boolean register = cartId != null;
        FirebaseUtil.setCurrentUserCartId(cartId);
        FirebaseUtil.setIsCurrentlyShopping(register);
        onCartFound(register);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (indoorLocationManager != null) {
            indoorLocationManager.stopPositioning();
        }
    }
}

