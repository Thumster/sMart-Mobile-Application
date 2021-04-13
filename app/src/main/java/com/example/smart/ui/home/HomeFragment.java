package com.example.smart.ui.home;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
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
import com.example.smart.MainActivity;
import com.example.smart.R;
import com.example.smart.model.CartItem;
import com.example.smart.model.response.CoordsVO;
import com.example.smart.model.response.InitNavigateResponseVO;
import com.example.smart.model.response.PathResponseVO;
import com.example.smart.ui.cart.CheckoutDialogFragment;
import com.example.smart.util.ApiUtilService;
import com.example.smart.util.FirebaseUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class HomeFragment extends Fragment
        implements FirebaseUtil.OnCartFound, QRDialogFragment.QRDialogListener {

    public interface OnFragmentInteractionListener {
        void onSelectScanBasket(QRDialogFragment.QRDialogListener listener);
    }

    ListenerRegistration cartsListenerRegistration;
    Query cartsQuery = FirebaseUtil.getCartsRef();

    ListenerRegistration itemsListenerRegistration;
    Query itemsQuery = FirebaseUtil.getUserCartItemsRef().orderBy("sortIdx", Query.Direction.ASCENDING);
    ArrayList<DocumentSnapshot> mSnapshots = new ArrayList<>();
    List<CartItem> validCartItems = new ArrayList<>();
    CartItem currentCartItem;
    Integer currentItemIdx = 0;

    OnFragmentInteractionListener listener;

    private static final String TAG = "HOME_FRAGMENT";
    private final NumberFormat DECIMAL_FORMATTER = new DecimalFormat("#0.00");

    ApiUtilService apiService;

    private final int TABLE_LENGTH = 3;
    private final int TABLE_WIDTH = 12;
    private final int DISTANCE_FROM_MAIN_DOOR = 8;
    private final int DISTANCE_BETWEEN_TABLE_IN_LENGTH = 4;
    private final int DISTANCE_BETWEEN_TABLE_IN_WIDTH = 28 - 2 * TABLE_WIDTH;

    private double pixelsPerUnitWidth;
    private double pixelsPerUnitLength;

    View shoppingLayout;
    View notShoppingLayout;

    TextView welcomeTextView;
    TextView nameTextView;
    Button buttonQrCode;

    ConstraintLayout layoutItemView;
    ConstraintLayout layoutEmptyItemView;
    Button buttonRedirect;
    Button buttonCheckout;
    Button buttonItemPrev;
    Button buttonItemNext;
    TextView textViewEmpty;
    TextView textViewItemNumberLabel;
    ImageView imageViewItem;
    TextView textViewItemName;
    TextView textViewItemCategory;
    TextView textViewItemPrice;
    TextView textViewItemQuantity;

    Location location; // The actual location, i.e., Lab
    ScanningIndoorLocationManager indoorLocationManager;
    LocationPosition currentPosition; // User's current position

    Paint wallPaint = new Paint(); // Stores how to draw, e.g., color, style, line thickness, text size, etc
    Paint tablePaint = new Paint();
    Paint personPaint = new Paint();
    Paint pathPaint = new Paint();
    Paint destPaint = new Paint();

    Rect wallRectangle = new Rect(); // Rectangle
    Rect tableRectangle = new Rect(); // Rectangle

    Canvas canvasIndoorMap; // Stores information on what to draw onto its associated bitmap, e.g., lines, circles, text, custom paths, etc
    ImageView imageViewIndoorMap; // Container for bitmap
    Bitmap bitmapIndoorMap; // Represents the pixels that are shown on the display

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiUtilService.BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiUtilService.class);

//        Integer originX = 28;
//        Integer originY = 0;
//        callApiInitNavigate(originX, originY);

        notShoppingLayout = root.findViewById(R.id.layout_not_shopping);
        shoppingLayout = root.findViewById(R.id.layout_shopping);

        nameTextView = root.findViewById(R.id.text_name);
        buttonQrCode = root.findViewById(R.id.fab_qr_code);
        welcomeTextView = root.findViewById(R.id.text_welcome);

        layoutItemView = root.findViewById(R.id.item_view);
        layoutEmptyItemView = root.findViewById(R.id.empty_item_view);
        buttonRedirect = root.findViewById(R.id.button_redirect_items);
        buttonCheckout = root.findViewById(R.id.button_checkout);
        buttonItemPrev = root.findViewById(R.id.button_previous_item);
        buttonItemNext = root.findViewById(R.id.button_next_item);
        textViewEmpty = root.findViewById(R.id.empty_text);
        textViewItemNumberLabel = root.findViewById(R.id.item_number_label);
        imageViewItem = root.findViewById(R.id.item_image);
        textViewItemName = root.findViewById(R.id.item_name);
        textViewItemCategory = root.findViewById(R.id.item_category);
        textViewItemPrice = root.findViewById(R.id.item_price);
        textViewItemQuantity = root.findViewById(R.id.in_shop_quantity);

        imageViewIndoorMap = root.findViewById(R.id.image_view_indoor_map);

        wallPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.black, null));
        tablePaint.setColor(ResourcesCompat.getColor(getResources(), R.color.dark_green_variant, null));
        personPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.light_blue, null));
        pathPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.orange, null));
        destPaint.setColor(ResourcesCompat.getColor(getResources(), R.color.dark_red, null));
        destPaint.setStrokeWidth(10);
        destPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        nameTextView.setText(FirebaseUtil.getCurrentUser().getDisplayName());
        FirebaseUtil.startListening(this);
        onCartFound(FirebaseUtil.getCurrentUserCartId() != null);
        buttonQrCode.setOnClickListener(v -> {
            listener.onSelectScanBasket(this);
        });
        initCartsRegistrationListener();
        initItemsRegistrationListener();
        buttonRedirect.setOnClickListener(v -> {
            Navigation.findNavController(root).navigate(R.id.navigation_items);
        });
        buttonItemPrev.setOnClickListener(v -> {
            currentItemIdx = Math.max(0, currentItemIdx - 1);
            onDataChanged();
        });
        buttonItemNext.setOnClickListener(v -> {
            currentItemIdx = Math.min(validCartItems.size() - 1, currentItemIdx + 1);
            onDataChanged();
        });

        Handler handler = new Handler();
        Runnable r = () -> {
            Log.i("onCreateView() Width", Double.toString(imageViewIndoorMap.getWidth()));
            Log.i("onCreateView() Height", Double.toString(imageViewIndoorMap.getHeight()));

            if (shoppingLayout.getVisibility() == View.VISIBLE && !validCartItems.isEmpty()) {
                setupIndoorLayout();
                updateNavigationPath();
            }
        };
        handler.postDelayed(r, 3000);

        buttonCheckout.setOnClickListener(v -> {
            if (!mSnapshots.isEmpty()) {
                Query query = FirebaseUtil.getUserCartItemsRef().whereGreaterThan("quantityInCart", 0);
                CheckoutDialogFragment checkoutDialog = new CheckoutDialogFragment(new CheckoutDialogFragment.CheckoutDialogListener() {
                    @Override
                    public void onCheckout(List<CartItem> purchasedCartItems) {
                        itemsQuery.get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUtil.setCurrentUserCartId(null);
                                FirebaseUtil.setIsCurrentlyShopping(false);
                                List<DocumentSnapshot> snapshots = task.getResult().getDocuments();
                                for (DocumentSnapshot snapshot : snapshots) {
                                    CartItem cartItem = snapshot.toObject(CartItem.class);
                                    purchasedCartItems.stream()
                                            .filter(item -> item.getId().equals(cartItem.getId()))
                                            .findFirst()
                                            .ifPresent(foundItem -> {
                                                Integer newQuantity = cartItem.getQuantity() - foundItem.getQuantityInCart();
                                                if (newQuantity > 0) {
                                                    cartItem.setQuantity(newQuantity);
                                                    cartItem.setQuantityInCart(0);
                                                    snapshot.getReference().set(cartItem);
                                                } else {
                                                    snapshot.getReference().delete();
                                                }
                                            });
                                    onDataChanged();
                                }
                            }
                        });
                    }
                }, query);
                checkoutDialog.show(getFragmentManager(), TAG);
            } else {
                Toast.makeText(getContext(), "No items detected in physical cart!", Toast.LENGTH_LONG).show();
            }
        });

        return root;
    }

    private void initCartsRegistrationListener() {
        cartsListenerRegistration = cartsQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "listen:error", error);
                return;
            }
            for (DocumentChange change : snapshots.getDocumentChanges()) {
                // Snapshot of the changed document
                DocumentSnapshot snapshot = change.getDocument();
                String changeCartId = snapshot.getId();
                String currentUserCartId = FirebaseUtil.getCurrentUserCartId();


                Object changeUserIdObject = snapshot.get(FirebaseUtil.CART_USER_DOC_NAME);
                String changeUserId = null;
                if (changeUserIdObject != null) {
                    changeUserId = (String) changeUserIdObject;
                }
                String currentUserId = FirebaseUtil.getCurrentUserUid();

                Log.i(TAG, "CART CHANGE DETECTED!" +
                        "\n\tCHANGE CART ID: " + changeCartId +
                        "\n\tCURRENT CART ID: " +  currentUserCartId +
                        "\n\tCHANGE USER ID: " + changeUserId +
                        "\n\tCURRENT USER ID: " + currentUserId);

                switch (change.getType()) {
                    case ADDED:
                    case REMOVED:
                        if (currentUserId.equals(changeUserId)) {
                            updateCartToRegistered(changeCartId);
                        }
                        break;
                    case MODIFIED:
                        if (changeCartId.equals(currentUserCartId)) {
                            if (changeUserId == null || !currentUserId.equals(changeUserId)) {
                                updateCartToRegistered(null);
                            }
                        } else if (currentUserId.equals(changeUserId)) {
                            updateCartToRegistered(changeCartId);
                        }
                        break;
                }
                Log.i(TAG, "CART UPDATES" +
                        "\n\tUPDATED CART ID: " + FirebaseUtil.getCurrentUserCartId());
            }
        });
    }

    private void initItemsRegistrationListener() {
        itemsListenerRegistration = itemsQuery.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.w(TAG, "listen:error", error);
                return;
            }

            for (DocumentChange change : snapshots.getDocumentChanges()) {
                // Snapshot of the changed document
                DocumentSnapshot snapshot = change.getDocument();

                switch (change.getType()) {
                    case ADDED:
                        onDocumentAdded(change); // Add this line
                        break;
                    case MODIFIED:
                        onDocumentModified(change); // Add this line
                        break;
                    case REMOVED:
                        onDocumentRemoved(change); // Add this line
                        break;
                }
            }

            onDataChanged();
        });
    }

    private void onDataChanged() {
        validCartItems = mSnapshots.stream()
                .map(documentSnapshot -> documentSnapshot.toObject(CartItem.class))
                .filter(cartItem -> cartItem.getQuantity() > cartItem.getQuantityInCart())
                .collect(Collectors.toList());
        if (mSnapshots.isEmpty()) {
            textViewEmpty.setText("No Items Found in Your Shopping List");
            buttonRedirect.setText("Shop Now");
            buttonCheckout.setVisibility(View.GONE);
        } else {
            textViewEmpty.setText("Shopping List Complete");
            buttonRedirect.setText("Shop More");
            buttonCheckout.setVisibility(View.VISIBLE);
        }
        if (validCartItems.isEmpty()) {
            layoutEmptyItemView.setVisibility(View.VISIBLE);
            layoutItemView.setVisibility(View.INVISIBLE);
            return;
        } else {
            layoutEmptyItemView.setVisibility(View.INVISIBLE);
            layoutItemView.setVisibility(View.VISIBLE);
        }
        if (currentItemIdx >= validCartItems.size()) {
            currentItemIdx--;
        }
        if (validCartItems.contains(currentCartItem)) {
            currentItemIdx = validCartItems.indexOf(validCartItems);
        }
        currentCartItem = validCartItems.get(currentItemIdx);

        if (currentCartItem == null) {
            return;
        }

        // Load image
        Glide.with(imageViewItem.getContext())
                .load(currentCartItem.getPhoto())
                .into(imageViewItem);
        textViewItemNumberLabel.setText(String.format("%d / %d", currentItemIdx + 1, validCartItems.size()));
        textViewItemName.setText(currentCartItem.getName());
        textViewItemCategory.setText(currentCartItem.getCategory());
        textViewItemPrice.setText(String.format("$%.2f", currentCartItem.getPrice()));
        textViewItemQuantity.setText(String.format("%d / %d", currentCartItem.getQuantityInCart(), currentCartItem.getQuantity()));

        if (!validCartItems.isEmpty() && shoppingLayout.getVisibility() == View.VISIBLE && imageViewIndoorMap.getWidth() != 0 && imageViewIndoorMap.getHeight() != 0) {
            Log.i("onDataChanged()", "Entering...");
            Log.i("onDataChanged() Width", Double.toString(imageViewIndoorMap.getWidth()));
            Log.i("onDataChanged() Height", Double.toString(imageViewIndoorMap.getHeight()));

            setupIndoorLayout();
            updateNavigationPath();
        }
    }

    private void setupIndoorLayout() {
        // A bitmap configuration describes how pixels are stored.
        bitmapIndoorMap = Bitmap.createBitmap(imageViewIndoorMap.getWidth(), imageViewIndoorMap.getHeight(), Bitmap.Config.ARGB_8888);
        imageViewIndoorMap.setImageBitmap(bitmapIndoorMap);
        canvasIndoorMap = new Canvas(bitmapIndoorMap); // Drawing on the canvas draws on the bitmap.

        pixelsPerUnitWidth = imageViewIndoorMap.getWidth() / 28.00;
        pixelsPerUnitLength = imageViewIndoorMap.getHeight() / 37.00;

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
                    imageViewIndoorMap.getWidth(),
                    (int) ((DISTANCE_FROM_MAIN_DOOR + ((i + 1) * TABLE_LENGTH + i * DISTANCE_BETWEEN_TABLE_IN_LENGTH)) * pixelsPerUnitLength)
            );
            canvasIndoorMap.drawRect(tableRectangle, tablePaint);
        }
    }

    private void updateNavigationPath() {
        if (currentPosition != null) {
            int scaledCurrentX = (int) (currentPosition.getX() / 0.25);
            int scaledCurrentY =  37 - (int) (currentPosition.getY() / 0.25);

            Log.i("Raw Current Position", "(" + currentPosition.getX() + ", " + currentPosition.getY() + ")");
            Log.i("Modified Current Position", "(" + scaledCurrentX + ", " + scaledCurrentY + ")");

            callApiRefreshPath(scaledCurrentX, scaledCurrentY, currentCartItem.getId().getId());
        }
    }

    private void onDocumentAdded(DocumentChange change) {
        mSnapshots.add(change.getNewIndex(), change.getDocument());
    }

    private void onDocumentModified(DocumentChange change) {
        if (change.getOldIndex() == change.getNewIndex()) {
            mSnapshots.set(change.getOldIndex(), change.getDocument());
        } else {
            mSnapshots.remove(change.getOldIndex());
            mSnapshots.add(change.getNewIndex(), change.getDocument());
        }
    }

    private void onDocumentRemoved(DocumentChange change) {
        mSnapshots.remove(change.getOldIndex());
    }

    // Init to update the sortIdxes on Firestore
    private void callApiInitNavigate(int posX, int posY) {
        Call<InitNavigateResponseVO> call = apiService.initNavigate(posX, posY, FirebaseUtil.getCurrentUserUid());
        call.enqueue(new Callback<InitNavigateResponseVO>() {
            @Override
            public void onResponse(Call<InitNavigateResponseVO> call, Response<InitNavigateResponseVO> response) {
                if (response.isSuccessful()) {
                    InitNavigateResponseVO initNavigateResponseVO = response.body();
                    if (initNavigateResponseVO.getStatus() > 0) {
                        Log.i("callApiInitNavigate() RECEIVED RESULT", initNavigateResponseVO.getStatus().toString());
                    } else {
                        Log.e("Failed to init navigation from API. RECEIVED FAILED RESULT", initNavigateResponseVO.getStatus().toString());
                    }
                }
            }

            @Override
            public void onFailure(Call<InitNavigateResponseVO> call, Throwable t) {
                Log.e(TAG, "Failed to init navigation from API: " + t.toString());
            }
        });
    }

    private void callApiRefreshPath(int posX, int posY, String itemId) {
        Call<PathResponseVO> call = apiService.path(posX, posY, itemId, FirebaseUtil.getCurrentUserUid());
        call.enqueue(new Callback<PathResponseVO>() {
            @Override
            public void onResponse(Call<PathResponseVO> call, Response<PathResponseVO> response) {
                if (response.isSuccessful()) {
                    PathResponseVO pathResponseVO = response.body();
                    List<CoordsVO> coords = pathResponseVO.getPath();

                    Log.i("callApiRefreshPath", pathResponseVO.getItem());

                    // Formula: position * pixels per unit dimension
                    float currentX = (float) (coords.get(0).getPosX() * pixelsPerUnitWidth);
                    float currentY = (float) (coords.get(0).getPosY() * pixelsPerUnitLength);
                    canvasIndoorMap.drawCircle(currentX, currentY, (float) (imageViewIndoorMap.getWidth() / 45), personPaint);

                    for (int i = 1; i < coords.size() - 1; i++) {
                        CoordsVO coord = coords.get(i);
                        float pathX = (float) (coord.getPosX() * pixelsPerUnitWidth);
                        float pathY = (float) (coord.getPosY() * pixelsPerUnitLength);

                        canvasIndoorMap.drawCircle(pathX, pathY, (float) (imageViewIndoorMap.getWidth() / 60), pathPaint);

                        Log.i("Coordinate", "(" + pathX + ", " + pathY + ")");
                    }

                    float destX = (float) (coords.get(coords.size() - 1).getPosX() * pixelsPerUnitWidth);
                    float destY = (float) (coords.get(coords.size() - 1).getPosY() * pixelsPerUnitLength);

                    Path path = new Path();
                    path.moveTo((float) (destX - pixelsPerUnitWidth / 2), (float) (destY - pixelsPerUnitLength / 2));
                    path.lineTo((float) (destX + pixelsPerUnitWidth / 2), (float) (destY + pixelsPerUnitLength / 2));
                    path.close();
                    canvasIndoorMap.drawPath(path, destPaint);

                    path = new Path();
                    path.moveTo((float) (destX + pixelsPerUnitWidth / 2), (float) (destY - pixelsPerUnitLength / 2));
                    path.lineTo((float) (destX - pixelsPerUnitWidth / 2), (float) (destY + pixelsPerUnitLength / 2));
                    path.close();
                    canvasIndoorMap.drawPath(path, destPaint);

                    imageViewIndoorMap.invalidate();
                }
            }

            @Override
            public void onFailure(Call<PathResponseVO> call, Throwable t) {
                Log.e(TAG, "Failed to retrieve path from API: " + t.toString());
            }
        });
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
                    }

                    @Override
                    public void onPositionOutsideLocation() {
                        Log.i("onPositionOutsideLocation", "Out of BLE Beacon Range...");
                    }
                });

                indoorLocationManager.startPositioning();

                Handler handler = new Handler();
                Runnable r = () -> {
                    int scaledOriginX = (int) (currentPosition.getX() / 0.25);
                    int scaledOriginY =  37 - (int) (currentPosition.getY() / 0.25);

                    Log.i("initNav", "(" + scaledOriginX + ", " + scaledOriginY + ")");

                    callApiInitNavigate(scaledOriginX, scaledOriginY);
                };
                handler.postDelayed(r, 1500);
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
            buttonQrCode.setVisibility(View.GONE);

            instantiateIndoorPositioning();
        } else {
            notShoppingLayout.setVisibility(View.VISIBLE);
            shoppingLayout.setVisibility(View.GONE);
            buttonQrCode.setVisibility(View.VISIBLE);

            if (indoorLocationManager != null) {
                indoorLocationManager.stopPositioning();
            }
        }
    }

    @Override
    public void onScanResultListener(String cartId) {
        updateCartToRegistered(cartId);
    }

    private void updateCartToRegistered(String cartId) {
        Boolean register = cartId != null;
        Log.e(TAG, "\n\tUpdating cart registration: " + register);
        Log.e(TAG, "\n\tUpdating cart registration for cart ID: " + cartId);
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

        if (itemsListenerRegistration != null) {
            itemsListenerRegistration.remove();
        }

        if (cartsListenerRegistration != null) {
            cartsListenerRegistration.remove();
        }
    }
}

