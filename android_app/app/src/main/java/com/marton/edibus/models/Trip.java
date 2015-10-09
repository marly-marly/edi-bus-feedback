package com.marton.edibus.models;


import java.util.Date;

public class Trip {

    /**
     * Description
     * @param id
     * @param startTime
     * @param endTime
     * @param journey
     * @param startStop
     * @param endStop
     * @param service
     * @param waitDuration
     * @param travelDuration
     * @param seat
     * @param rating
     * @param createdAt
     * @param updatedAt
     */
    public Trip(int id, Date startTime, Date endTime, Journey journey, Stop startStop, Stop endStop,
            Service service, int waitDuration, int travelDuration, boolean seat, float rating,
            Date createdAt, Date updatedAt){

        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.journey = journey;
        this.startStop = startStop;
        this.endStop = endStop;
        this.service = service;
        this.waitDuration = waitDuration;
        this.travelDuration = travelDuration;
        this.seat = seat;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

    }

    private int id;

    private Date startTime;

    private Date endTime;

    private Journey journey;

    private Stop startStop;

    private Stop endStop;

    private Service service;

    private int waitDuration;

    private int travelDuration;

    private boolean seat;

    private float rating;

    private Date createdAt;

    private Date updatedAt;

    public int getId(){
        return id;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public Journey getJourney() {
        return journey;
    }

    public Stop getStartStop() {
        return startStop;
    }

    public Stop getEndStop() {
        return endStop;
    }

    public Service getService() {
        return service;
    }

    public int getWaitDuration() {
        return waitDuration;
    }

    public int getTravelDuration() {
        return travelDuration;
    }

    public boolean isSeat() {
        return seat;
    }

    public float getRating() {
        return rating;
    }

    public Date getCreatedAt() {
        return createdAt;
    }
}