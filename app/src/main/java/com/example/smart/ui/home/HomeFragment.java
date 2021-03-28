package com.example.smart.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.smart.MainActivity;
import com.example.smart.R;
import com.example.smart.util.FirebaseUtil;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment
        implements FirebaseUtil.OnCartFound, QRDialogFragment.QRDialogListener {

    public interface OnFragmentInteractionListener {
        void onSelectScanBasket(QRDialogFragment.QRDialogListener listener);
    }

    OnFragmentInteractionListener listener;

    private static final String TAG = "HOME_FRAGMENT";

    View shoppingLayout;
    View notShoppingLayout;

    TextView welcomeTextView;
    TextView nameTextView;
    TextView userCartTextView;
    Button buttonLogout;
    Button buttonUnregisterCart;
    Button buttonQrCode;



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


        nameTextView.setText(FirebaseUtil.getCurrentUser().getDisplayName());
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

        return root;
    }

    @Override
    public void onCartFound(Boolean found) {
        Log.e("SHOPPING", found.toString());
        if (found) {
            notShoppingLayout.setVisibility(View.GONE);
            shoppingLayout.setVisibility(View.VISIBLE);
            userCartTextView.setText(FirebaseUtil.getCurrentUserCartId());

            buttonQrCode.setVisibility(View.GONE);
//            buttonUnregisterCart.setVisibility(View.VISIBLE);
//            userCartTextView.setVisibility(View.VISIBLE);
        } else {
            notShoppingLayout.setVisibility(View.VISIBLE);
            shoppingLayout.setVisibility(View.GONE);

            buttonQrCode.setVisibility(View.VISIBLE);
//            userCartTextView.setVisibility(View.GONE);
//            buttonUnregisterCart.setVisibility(View.GONE);
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
            Toast.makeText(getContext(), "You are not currently shopping", Toast.LENGTH_LONG).show();
            return;
        }
        DocumentReference cartDocRef = FirebaseUtil.getCartsRef().document(FirebaseUtil.getCurrentUserCartId());
        Map<String, Object> updates = new HashMap<>();
        updates.put(FirebaseUtil.CART_USER_DOC_NAME, FieldValue.delete());
        cartDocRef.update(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.i(TAG, "Successfully unregistered cart");
                updateCartToRegistered(null);
                Toast.makeText(getContext(), "Successfully unregistered cart!", Toast.LENGTH_LONG).show();
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

}

