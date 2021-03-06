package com.marton.edibus.journey.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;

import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.marton.edibus.journey.events.LocationUpdatedEvent;

import de.greenrobot.event.EventBus;


// Frequently publishes events that include the new coordinates of the user
public class LocationProviderService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks {

    private EventBus eventBus = EventBus.getDefault();

    private LocationUpdatedEvent locationUpdateEvent;

    private GoogleApiClient googleApiClient;

    private LocationRequest locationRequest;

    public LocationProviderService() {
        this.locationUpdateEvent = new LocationUpdatedEvent();
    }

    @Override
    public void onCreate(){

        this.googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(3000);
        this.locationRequest.setFastestInterval(3000);
        this.locationRequest.setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        this.googleApiClient.connect();
    }

    @Override
    public void onDestroy()
    {
        this.googleApiClient.disconnect();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.locationUpdateEvent.setLatitude(location.getLatitude());
        this.locationUpdateEvent.setLongitude(location.getLongitude());
        this.eventBus.post(this.locationUpdateEvent);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, this.locationRequest, this);
    }
}
