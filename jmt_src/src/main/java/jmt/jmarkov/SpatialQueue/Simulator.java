/**
 * Based on the standard Jmarkov Simulator
 */

package jmt.jmarkov.SpatialQueue;


import jmt.jmarkov.Graphics.Notifier;
import jmt.jmarkov.Job;
import jmt.jmarkov.Queues.Arrivals;
import jmt.jmarkov.SpatialQueue.Map.MapConfig;

import jmt.jmarkov.SpatialQueue.Sender;
import jmt.jmarkov.SpatialQueue.Location;


import java.util.Date;
import java.util.LinkedList;
import java.util.Random;

public class Simulator implements Runnable {

    // Receiver is the server that deals with requests.
    // All logic related to dealing with requests is delegated to it
    private Receiver receiver;

    private ClientRegion[] regions;

    private Notifier[] notifier;

    //current simulation time
    private double currentTime;// in milliseconds

    //if lambda is zero this value is set to true
    //(if lambda set to zero new jobs will not be created
    private boolean lambdaZero = false;

    //it saves the data if the simulator is paused or not
    private boolean paused = false;

    //multiplier is used for multiplier for timer.(comes from simulation time slide bar)
    private double timeMultiplier = 1.0;

    private boolean running = false;

    private boolean started = false;

    private int currentRequestID;

    public Simulator(double timeMultiplier,
                     Notifier[] notifier,
                     Receiver receiver,
                     MapConfig mapConfig) {
        super();

        currentTime = 0;
        setTimeMultiplier(timeMultiplier);
        this.notifier = notifier;
        this.receiver = receiver;
        this.regions = mapConfig.getClientRegions();
        this.currentRequestID = 0;
    }

    private Sender generateNewSenderWithinArea(ClientRegion clientRegion) {
        Location senderLocation = clientRegion.generatePoint();
        return new Sender(senderLocation);    }

    public void run() {
        running = true;
        started = true;
        // this is the simulation time till run command is called
        double currentTimeMultiplied;
        //when calling run getting the current real time
        long realTimeStart;
        //this is the time after return the thread.sleep
        long realTimeCurrent;
        currentTimeMultiplied = 0;
        realTimeStart = new Date().getTime();

        //this is the first job which is created
        //if job list is not empty this means run is called from the paused situation
        if (this.receiver.getQueue().isEmpty()) {
            this.receiver.handleRequest(this.createRequest());
        }

        //if there is still some job is waiting for processing it is running recursive
        //if paused the running will stop.
        while (this.receiver.getQueue().size() > 0 && !paused) {
            //this is calculating how long system will sleep
            currentTimeMultiplied += (peekJob().getNextEventTime() - currentTime) / timeMultiplier;
            //this is calculating how long system will sleep
            realTimeCurrent = new Date().getTime() - realTimeStart;

            //this is for calculating if the system will pause or not
            if ((long) currentTimeMultiplied > realTimeCurrent) {
                try {
                    Thread.sleep((long) currentTimeMultiplied - realTimeCurrent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                realTimeCurrent = new Date().getTime() - realTimeStart;
            }

            Request job = dequeueJob();
            currentTime = job.getNextEventTime();
        }
        running = false;
    }

    private int getNextRequestID() {
        int r = this.currentRequestID;
        this.currentRequestID ++;
        return r;
    }

    public Request createRequest() {
        //Current implementation: create a new sender then generate a request from them
        Sender sender = this.generateNewSenderWithinArea(this.regions[0]);
        return new Request(getNextRequestID(), this.currentTime, sender);
    }

    public void enqueueJob(Request newRequest) {
        this.receiver.handleRequest(newRequest);
    }

    public Request dequeueJob() {
        return this.receiver.getNextRequest();
    }

    public Request peekJob() {
        return this.receiver.getQueue().element();
    }

    public boolean isLambdaZero() {
        return lambdaZero;
    }

    public void setLambdaZero(boolean lambdaZero) {
        //This doesn't seem to actually set lambda to zero?
        this.lambdaZero = lambdaZero;
    }

    public void pause() {
        if (paused) {
            paused = false;
            start();
        } else {
            paused = true;
        }
    }

    public void start() {
        Thread simt = new Thread(this);
        simt.setDaemon(true);
        simt.start();
    }

    public void stop() {
        this.paused = true;
        this.started = false;
    }

    public double getTimeMultiplier() {
        return this.timeMultiplier;
    }

    public void setTimeMultiplier(double timeMultiplier) {
        this.timeMultiplier = timeMultiplier;
    }

    public boolean isRunning() {
        return this.running;
    }

    public boolean isStarted() {
        return this.started;
    }
}
