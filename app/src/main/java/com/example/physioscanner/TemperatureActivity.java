package com.example.physioscanner;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class TemperatureActivity extends AppCompatActivity {

    private static final String TAG = "TemperatureActivity";
    private Socket mSocket;
    private DocumentReference piRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temperature);

        piRef = FirebaseFirestore.getInstance().collection("pi_status").document("status");
        piRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (snapshot != null && snapshot.exists()) {
                    String ip = snapshot.getString("ip");
                    initSocket(ip);
                }
            }
        });
    }

    private void initSocket(String ip) {
        if (ip == null || ip.isEmpty()) {
            Log.w(TAG, "No IP from Firestore, can't connect socket");
            return;
        }
        try {
            if (mSocket != null) {
                mSocket.disconnect();
                mSocket.off("tmp102_data", onTemp);
            }
            mSocket = IO.socket("http://" + ip + ":5000");
            mSocket.connect();
            mSocket.on("tmp102_data", onTemp);
            Log.d(TAG, "Socket connected to " + ip);
        } catch (Exception e) {
            Log.e(TAG, "Socket error", e);
        }
    }

    private final Emitter.Listener onTemp = args -> runOnUiThread(() -> {
        // parse data, update UI
        Log.d(TAG, "Received tmp102_data event");
    });
}
