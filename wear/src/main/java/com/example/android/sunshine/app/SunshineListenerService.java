package com.example.android.sunshine.app;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.Charset;

public class SunshineListenerService extends WearableListenerService {
    private static final String LOG_TAG = "SunshineListenerServ";

    private static final String UPDATE_WEATHER_PATH = "/update-weather";
    private static final Charset CHARSET = Charset.forName("UTF-8");
//    public static final String WEATHER_CHANGED_INTENT = "weather_changed_intent";
//    public static final String WEATHER_CHANGED_KEY = "weather_changed_key";

    private GoogleApiClient googleApiClient;
//    private LocalBroadcastManager broadcastManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "connecting to googleApiClient");

        googleApiClient = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        googleApiClient.connect();

        Log.i(LOG_TAG, "connecting: " + googleApiClient.isConnecting() + ", connected: " + googleApiClient.isConnected());

//        broadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        Log.i(LOG_TAG, "message received: " + event);
        if (event.getPath().equals(UPDATE_WEATHER_PATH)) {
            byte[] data = event.getData();
            String messageContent = new String(data, CHARSET);
            Log.i(LOG_TAG, "message content: " + messageContent);

//            Intent weatherIntent = new Intent(WEATHER_CHANGED_INTENT);
//            weatherIntent.putExtra(WEATHER_CHANGED_KEY, messageContent);
//            broadcastManager.sendBroadcast(weatherIntent);
//            Log.i(LOG_TAG, "sent weather broadcast intent: intent=" + weatherIntent + ", message=" + messageContent);
        }

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
