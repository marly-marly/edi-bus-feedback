package com.marton.edibus.utilities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.marton.edibus.WebCallBack;
import com.marton.edibus.enums.TripControlEnum;
import com.marton.edibus.events.JourneyControlEvent;
import com.marton.edibus.events.TripControlEvent;
import com.marton.edibus.models.Trip;
import com.marton.edibus.network.BusWebService;

import de.greenrobot.event.EventBus;

@Singleton
public class JourneyManager {

    @Inject
    BusWebService busWebService;

    private EventBus eventBus = EventBus.getDefault();

    // The current trip of the user
    private Trip trip;

    // The flag indicating whether the journey has been paused
    private boolean paused = true;

    // The flag indicating whether the journey has been finished
    private boolean finished = false;

    // The flag indicating whether the trip should be uploaded automatically
    private boolean automaticFinish = false;

    public JourneyManager(){
        trip = new Trip();
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public boolean getPaused() {
        return paused;
    }

    public boolean getFinished() {
        return finished;
    }

    public boolean getAutomaticFinish() {
        return automaticFinish;
    }

    public void setAutomaticFinish(boolean automaticFinish) {
        this.automaticFinish = automaticFinish;
    }

    public void pauseTrip(){
        this.paused = true;
        this.eventBus.post(new TripControlEvent(TripControlEnum.PAUSE));
    }

    public void continueTrip(){
        this.paused = false;
        this.eventBus.post(new TripControlEvent(TripControlEnum.CONTINUE));
    }

    public void startTrip(){
        this.paused = false;
        this.eventBus.post(new TripControlEvent(TripControlEnum.START));
    }

    public void finishTrip(){
        this.finished = true;
        this.eventBus.post(new TripControlEvent(TripControlEnum.FINISH));
    }

    public void uploadTrip(WebCallBack callback){

        WebCallBack<Integer> serviceCallback = new WebCallBack<Integer>() {
            @Override
            public void onSuccess(Integer data) {
                trip = new Trip();
                trip.setJourneyId(data);
                eventBus.post(new TripControlEvent(TripControlEnum.UPLOAD));
            }
        };

        if (this.trip.getJourneyId() == 0){
            busWebService.uploadNewTrip(this.trip, serviceCallback);
        }else{
            busWebService.uploadNewTrip(this.trip.getJourneyId(), this.trip, serviceCallback);
        }
    }
}
