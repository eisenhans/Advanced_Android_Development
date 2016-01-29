package com.example.android.sunshine.app.sync;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.HashSet;

public class WearableUpdater implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String LOG_TAG = WearableUpdater.class.getName();

    private GoogleApiClient googleApiClient;
    private static final String START_ACTIVITY_PATH = "/start-activity";

    public WearableUpdater(Context context) {
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void updateWearable(int weatherId, String maxTemp, String minTemp) {
        Collection<String> nodes = getNodes();
        Log.i(LOG_TAG, "updating weather for " + nodes.size() + " nodes");

        for (String node : nodes) {
            updateWearable(node, weatherId, maxTemp, minTemp);
        }
    }

    private void updateWearable(String node, int weatherId, String maxTemp, String minTemp) {
        Log.i(LOG_TAG, "updating watch: node=" + node + ", weatherId=" + weatherId + ", maxTemp=" + maxTemp + ", minTemp=" + minTemp);

        PendingResult<MessageApi.SendMessageResult> messageResult =
                Wearable.MessageApi.sendMessage(googleApiClient, node, START_ACTIVITY_PATH, new byte[0]);
        messageResult.setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.i(LOG_TAG, "Received SendMessageResult: " + sendMessageResult +
                                " (status: " + sendMessageResult.getStatus() + ")");
                    }
                }
        );
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOG_TAG, "connected: " + bundle);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOG_TAG, "connection suspended: " + i);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOG_TAG, "connection failed: " + connectionResult);
    }
}
