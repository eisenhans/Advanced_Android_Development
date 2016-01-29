package com.example.android.sunshine.wear;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class SunshineListenerService extends WearableListenerService {
    private static final String LOG_TAG = SunshineListenerService.class.getName();

    private GoogleApiClient googleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        googleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i(LOG_TAG, "message received: " + messageEvent);

//        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
//            Intent startIntent = new Intent(this, MainActivity.class);
//            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(startIntent);
//        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(LOG_TAG, "data changed: " + dataEvents);
    }
}
