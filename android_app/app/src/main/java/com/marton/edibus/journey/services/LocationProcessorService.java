package com.marton.edibus.journey.services;

import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.google.inject.Inject;
import com.marton.edibus.journey.enums.RideStateEnum;
import com.marton.edibus.journey.enums.JourneyStateEnum;
import com.marton.edibus.journey.events.JourneyUploadRequestedEvent;
import com.marton.edibus.journey.events.LocationUpdatedEvent;
import com.marton.edibus.journey.events.TimerUpdatedEvent;
import com.marton.edibus.shared.models.Stop;
import com.marton.edibus.journey.events.TrackingUpdatedEvent;
import com.marton.edibus.shared.models.Ride;
import com.marton.edibus.journey.utilities.GpsCalculator;
import com.marton.edibus.journey.utilities.JourneyManager;

import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import roboguice.service.RoboService;


public class LocationProcessorService extends RoboService {

    @Inject
    private JourneyManager journeyManager;
    
    private static final double START_STOP_DISTANCE_THRESHOLD = 35;

    private static final double END_STOP_DISTANCE_THRESHOLD = 35;

    private EventBus eventBus = EventBus.getDefault();

    private TrackingUpdatedEvent previousTrackingUpdatedEvent;

    private TrackingUpdatedEvent trackingUpdatedEvent;

    private long latestUpdateTime;

    private TimerUpdatedEvent timerUpdatedEvent;

    private Timer timer;

    private int waitingSeconds = 0;

    private int travellingSeconds = 0;

    public LocationProcessorService() {
        this.eventBus.register(this);
        this.trackingUpdatedEvent = new TrackingUpdatedEvent();
        this.previousTrackingUpdatedEvent = new TrackingUpdatedEvent();
        this.timerUpdatedEvent = new TimerUpdatedEvent();
    }

    @Override
    public void onCreate(){
        super.onCreate();

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {

                if (!journeyManager.getJourneyStateEnum().equals(JourneyStateEnum.STARTED)){
                    return;
                }

                switch(journeyManager.getRideStateEnum()){
                    case WAITING:
                        waitingSeconds++;
                        break;

                    case TRAVELLING:
                        travellingSeconds++;
                        break;
                }

                timerUpdatedEvent.setWaitingMilliseconds(waitingSeconds * 1000);
                timerUpdatedEvent.setTravellingMilliseconds(travellingSeconds * 1000);
                eventBus.post(timerUpdatedEvent);
            }
        }, 0, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){

        // Don't forget to stop the timer
        this.timer.cancel();
        this.waitingSeconds = 0;
        this.travellingSeconds = 0;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Handles the location update event
    public void onEvent(LocationUpdatedEvent locationUpdatedEvent){

        this.processLocationUpdate(locationUpdatedEvent.getLatitude(), locationUpdatedEvent.getLongitude());
    }

    private void processLocationUpdate(double latitude, double longitude){

        long currentUpdateTime = System.currentTimeMillis();

        this.trackingUpdatedEvent.setLatitude(latitude);
        this.trackingUpdatedEvent.setLongitude(longitude);

        // Calculate the remaining distance
        Stop endStop = this.journeyManager.getRide().getEndStop();
        double remainingDistance = GpsCalculator.getDistanceBetweenPoints(
                latitude, longitude, endStop.getLatitude(), endStop.getLongitude()
        );

        // Calculate the passed distance (euclidean)
        Stop startStop = this.journeyManager.getRide().getStartStop();
        double passedDistance = GpsCalculator.getDistanceBetweenPoints(
                latitude, longitude, startStop.getLatitude(), startStop.getLongitude()
        );

        switch (this.journeyManager.getJourneyStateEnum()){
            case STARTED:
                this.trackingUpdatedEvent.setDistanceFromGoal(remainingDistance);
                this.trackingUpdatedEvent.setWaitingTime(this.waitingSeconds * 1000);
                this.trackingUpdatedEvent.setTravellingTime(this.travellingSeconds * 1000);

                // Update the waiting times on the ride
                Ride ride = this.journeyManager.getRide();
                ride.setWaitDuration(this.waitingSeconds * 1000);
                ride.setTravelDuration(this.travellingSeconds * 1000);

                // Only count distances and speed if the user is travelling
                if (this.journeyManager.getRideStateEnum() == RideStateEnum.TRAVELLING){

                    // Calculate travelled distance in metres
                    double pastDistanceDelta;
                    double pastDistance;
                    double previousDistanceFromStart = this.trackingUpdatedEvent.getDistanceFromStart();
                    pastDistanceDelta = GpsCalculator.getDistanceBetweenPoints(
                            this.trackingUpdatedEvent.getLatitude(),
                            this.trackingUpdatedEvent.getLongitude(),
                            this.previousTrackingUpdatedEvent.getLatitude(),
                            this.previousTrackingUpdatedEvent.getLongitude());
                    pastDistance = previousDistanceFromStart + pastDistanceDelta;
                    this.trackingUpdatedEvent.setDistanceFromStart(pastDistance);

                    ride.setDistance(pastDistance);

                    // Calculate the current speed in metre per second
                    if (this.latestUpdateTime != 0.0 && pastDistanceDelta != 0.0){
                        long timeDelta = (currentUpdateTime - this.latestUpdateTime)/1000;
                        double speed = pastDistanceDelta/timeDelta;
                        this.trackingUpdatedEvent.setCurrentSpeed(speed);
                    }

                    // Calculate the maximum speed in metre per second
                    double currentSpeed = this.trackingUpdatedEvent.getCurrentSpeed();
                    if (currentSpeed > this.trackingUpdatedEvent.getMaximumSpeed()){
                        this.trackingUpdatedEvent.setMaximumSpeed(currentSpeed);
                    }

                    // Calculate the average speed in metre per second
                    double averageSpeed = this.trackingUpdatedEvent.getDistanceFromStart()/(this.trackingUpdatedEvent.getTravellingTime()/1000);
                    this.trackingUpdatedEvent.setAverageSpeed(averageSpeed);
                }

                // Automatically change the user's activity, if automation is enabled
                if (this.journeyManager.getAutomaticFlow()){

                    this.triggerNewActivity(passedDistance, remainingDistance);
                }
        }

        // Store the current state for later
        if (!this.journeyManager.getRideStateEnum().equals(RideStateEnum.PREPARING)){
            this.previousTrackingUpdatedEvent.copyValuesFrom(this.trackingUpdatedEvent);
            this.latestUpdateTime = currentUpdateTime;
        }

        // Fire the processed tracker information event
        this.eventBus.post(this.trackingUpdatedEvent);
    }

    private void triggerNewActivity(double passedDistance, double remainingDistance){

        // Calculate the new activity enum
        switch (this.journeyManager.getRideStateEnum()){
            case PREPARING:
                if (passedDistance < START_STOP_DISTANCE_THRESHOLD){
                    this.journeyManager.startWaiting();
                }
                break;
            case WAITING:
                if (passedDistance >= START_STOP_DISTANCE_THRESHOLD){
                    this.journeyManager.startTravelling();
                }
                break;
            case TRAVELLING:
                if (remainingDistance < END_STOP_DISTANCE_THRESHOLD){
                    this.journeyManager.finishRide();
                    this.eventBus.post(new JourneyUploadRequestedEvent());
                }
                break;
        }
    }
}
