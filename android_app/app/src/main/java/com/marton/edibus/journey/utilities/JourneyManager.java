package com.marton.edibus.journey.utilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marton.edibus.App;
import com.marton.edibus.shared.network.WebCallBack;
import com.marton.edibus.journey.enums.RideStateEnum;
import com.marton.edibus.journey.enums.JourneyStateEnum;
import com.marton.edibus.journey.enums.RideActionEnum;
import com.marton.edibus.journey.events.RideActionFiredEvent;
import com.marton.edibus.shared.models.Log;
import com.marton.edibus.shared.models.Stop;
import com.marton.edibus.shared.models.Ride;
import com.marton.edibus.shared.network.BusClient;
import com.marton.edibus.shared.utilities.SharedPreferencesManager;
import com.marton.edibus.shared.utilities.StatisticsManager;

import java.util.Date;

import de.greenrobot.event.EventBus;

@Singleton
public class JourneyManager {

    @Inject
    private BusClient busWebService;

    private EventBus eventBus = EventBus.getDefault();

    // The current ride of the user
    private Ride ride;

    // The stop that is currently under review by the user
    private Stop reviewStop;

    private JourneyStateEnum journeyStateEnum;

    private RideStateEnum rideStateEnum;

    private RideActionFiredEvent rideActionFiredEvent;

    // The flag indicating whether the ride should be automated
    private boolean automaticFlow;

    public JourneyManager(){
        this.setDefaults();
    }

    // Set the defaults for the Journey
    public void setDefaults(){
        this.ride = new Ride();
        this.reviewStop = null;
        this.journeyStateEnum = JourneyStateEnum.SETUP_INCOMPLETE;
        this.rideStateEnum = RideStateEnum.PREPARING;
        this.rideActionFiredEvent = new RideActionFiredEvent();
        this.automaticFlow = false;
    }

    public JourneyStateEnum getJourneyStateEnum() {
        return journeyStateEnum;
    }

    public RideStateEnum getRideStateEnum() {
        return rideStateEnum;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public Stop getReviewStop() {
        return reviewStop;
    }

    public void setReviewStop(Stop reviewStop) {
        this.reviewStop = reviewStop;
    }

    public boolean getAutomaticFlow() {
        return automaticFlow;
    }

    public void setAutomaticFlow(boolean automaticFlow) {
        this.automaticFlow = automaticFlow;
    }

    public void startWaiting(){

        // Fire event
        this.rideActionFiredEvent.setRideActionEnum(RideActionEnum.WAITING_STARTED);
        this.eventBus.post(rideActionFiredEvent);

        this.journeyStateEnum = JourneyStateEnum.STARTED;
        this.rideStateEnum = RideStateEnum.WAITING;

        this.ride.setStartTime(new Date());
    }

    public void startTravelling(){

        // Fire event
        this.rideActionFiredEvent.setRideActionEnum(RideActionEnum.TRAVELLING_STARTED);
        this.eventBus.post(rideActionFiredEvent);

        this.journeyStateEnum = JourneyStateEnum.STARTED;
        this.rideStateEnum = RideStateEnum.TRAVELLING;

        this.ride.setStartTime(new Date());
    }

    public void finishRide(){

        this.journeyStateEnum = JourneyStateEnum.FINISHED;
        this.ride.setEndTime(new Date());
    }

    // Returns a flag indicating if a ride has been set up
    public boolean rideSetupComplete(){
        return this.ride.getStartStopId() != 0 && this.ride.getEndStopId() != 0 && this.ride.getServiceId() != 0;
    }

    public void saveRide(WebCallBack<Integer> callback){

        // Read current statistics
        int journeys = StatisticsManager.readJourneysFromSharedPreferences();
        int totalWaitingTime = StatisticsManager.readTotalWaitingTimeFromSharedPreferences();
        int totalTravellingTime = StatisticsManager.readTotalTravellingTimeFromSharedPreferences();
        double totalTravellingDistance = StatisticsManager.readTotalTravellingDistanceFromSharedPreferences();

        // Upload as a new journey, or as an existing one
        if (this.ride.getJourneyId() == 0){
            this.busWebService.uploadNewTrip(this.ride, callback);
            journeys++;
        }else{
            this.busWebService.uploadNewTrip(this.ride.getJourneyId(), this.ride, callback);
        }

        // Store general user statistics locally
        totalWaitingTime += this.ride.getWaitDuration();
        totalTravellingTime += this.ride.getTravelDuration();
        totalTravellingDistance += this.ride.getDistance();

        SharedPreferencesManager.writeString(App.getAppContext(), StatisticsManager.JOURNEYS_KEY, String.valueOf(journeys));
        SharedPreferencesManager.writeString(App.getAppContext(), StatisticsManager.TOTAL_WAITING_TIME_KEY, String.valueOf(totalWaitingTime));
        SharedPreferencesManager.writeString(App.getAppContext(), StatisticsManager.TOTAL_TRAVELLING_TIME_KEY, String.valueOf(totalTravellingTime));
        SharedPreferencesManager.writeString(App.getAppContext(), StatisticsManager.TOTAL_TRAVELLING_DISTANCE_KEY, String.valueOf(totalTravellingDistance));

        // Store diary log locally
        Log log = new Log(ride);
        log.save();
    }
}
