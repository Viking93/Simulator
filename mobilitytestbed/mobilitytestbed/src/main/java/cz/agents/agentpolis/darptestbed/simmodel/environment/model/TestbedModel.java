package cz.agents.agentpolis.darptestbed.simmodel.environment.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.sun.istack.logging.Logger;

import cz.agents.agentpolis.darptestbed.simmodel.agent.data.Request;
import cz.agents.agentpolis.darptestbed.simmodel.agent.timer.Timer;
import cz.agents.agentpolis.darptestbed.simmodel.entity.vehicle.TestbedVehicle;
import cz.agents.agentpolis.simmodel.agent.Agent;
import cz.agents.agentpolis.simmodel.environment.model.query.AgentPositionQuery;
import groovy.util.logging.Log;

import java.util.*;

/**
 * A storage that contains current information about taxis and their drivers,
 * e.g. free taxis list etc. This storage also enables to change this
 * information, e.q. when a taxi is taken.
 *
 * 
 */
@Singleton
public class TestbedModel {

    /**
     * Returns an agent's current position.
     */
    protected final AgentPositionQuery positionQuery;
    /**
     * Storage for all vehicles (you can get them by ids)
     */
    protected final TestbedVehicleStorage vehicleStorage;
    /**
     * The dispatching that serves as the central communication hub between
     * passengers and taxi drivers (if using centralized communication).
     */
    protected Agent dispatching;
    /**
     * The time when the simulation started
     */
    protected long simulationStartTime;
    /**
     * The time when the simulation finished
     */
    protected long simulationEndTime;
    /**
     * Timers are used to call timer callback methods on all agents in a regular
     * manner. It enables agents to process requests and proposals only once in
     * a while.
     */
    private Timer dispatchingTimer;
    private Timer taxiDriversTimer;
    private Timer passengersTimer;
   

    // TODO: Refactor to set
    /**
     * Following properties saves the current state of the environment:
     * taxisFree - a list of ids of taxis, which have currently no task assigned
     * taxiDriversFree - a list of ids of free taxis' drivers (the same order as previous list) 
     * taxisAtWork - a list of ids of taxis, which have been assigned a task 
     * passengers - a list of ids of all passengers
     */
    private List<String> taxisFree;
    private List<String> taxiDriversFree;
    private List<String> taxisBusy;
    private List<String> taxiDriversBusy;
    private List<String> passengers; 
    private List<Request> passengerReq; 
    /**
     * Map of taxi ids along with their current passengers on board
     */
    protected Map<String, List<String>> taxiWithPassengersOnBoard;
    protected Map<String, List<Request>> passengerRequests; //* added for mapping taxi id with Requests
    /**
     * Busy taxi driver save their future positions at the end of their trips
     * into this map (<Taxi ID, The number of node = position at the end>)
     */
    protected Map<String, Long> taxiWithEndOfTripPositions;

    @Inject
    public TestbedModel(AgentPositionQuery positionQuery, TestbedVehicleStorage vehicleStorage) {

        this.positionQuery = positionQuery;
        this.vehicleStorage = vehicleStorage;

        this.taxisFree = new ArrayList<String>();
        this.taxiDriversFree = new ArrayList<String>();
        this.taxisBusy = new ArrayList<String>();
        this.taxiDriversBusy = new ArrayList<String>();
        this.passengers = new ArrayList<String>();
        this.passengerReq = new ArrayList<Request>();
        this.taxiWithPassengersOnBoard = new HashMap<String, List<String>>();
        this.passengerRequests = new HashMap<String, List<Request>>();
        this.taxiWithEndOfTripPositions = new HashMap<String, Long>();
    }

    public void setDispatching(Agent dispatching) {
        this.dispatching = dispatching;
    }

    /*public void addPassengerRequest(Request request){
    	this.passengerRequests.add(request);
    }
    
    
    public List<Request> getPassengerRequests(){
    	return this.passengerRequests;
    }
    */
    public void setTimers(Timer dispatchingTimer, Timer taxiDriversTimer, Timer passengersTimer) {

        this.dispatchingTimer = dispatchingTimer;
        this.taxiDriversTimer = taxiDriversTimer;
        this.passengersTimer = passengersTimer;
    }

    /**
     * Starts all timers, that've been set, and also remembers the time when the
     * simulation started (now)
     */
    public void startTimers() {

        if (this.dispatchingTimer != null) {
            this.dispatchingTimer.start();
        }
        if (this.taxiDriversTimer != null) {
            this.taxiDriversTimer.start();
        }
        if (this.passengersTimer != null) {
            this.passengersTimer.start();
        }

        this.simulationStartTime = System.currentTimeMillis();
    }

    /**
     * Adds a free vehicle into the list, along with its driver
     *
     * @param vehicleId
     * @param taxiDriverId
     */
    public void addFreeTaxi(String vehicleId, String taxiDriverId) {
        this.taxisFree.add(vehicleId);
        this.taxiDriversFree.add(taxiDriverId);
        // in the case it was busy before
        this.taxisBusy.remove(vehicleId);
        this.taxiDriversBusy.remove(vehicleId);
    }

    /**
     * Adds a passenger into the list
     *
     * @param passengerId
     */
    public void addPassenger(String passengerId) {
        this.passengers.add(passengerId);
    }

    public List<String> getTaxisFree() {
        return taxisFree;
    }

    public List<String> getTaxiDriversFree() {
        return taxiDriversFree;
    }

    public List<String> getTaxiDriversBusy() {
    	return taxiDriversBusy;
    }

    public List<String> getAllTaxiDrivers() {
        List<String> allDrivers = new ArrayList<String>(this.taxiDriversFree);
        allDrivers.addAll(this.taxiDriversBusy);
        return allDrivers;
    }

    /**
     * Moves the taxi given as parameter from list of free taxis into the list
     * "busy". And also does the same with its driver.
     *
     * @param taxiIndex the index of the taxi in the list
     */
    public void setTaxiBusy(int taxiIndex) {
    	//String a = null;
    	//a.charAt(8);
    	System.out.println("\nSetting taxi busy : ");
        this.taxisBusy.add(this.taxisFree.remove(taxiIndex));
        this.taxiDriversBusy.add(this.taxiDriversFree.remove(taxiIndex));
    }

    /**
     * Moves the taxi given as parameter from list of free taxis into the list
     * "busy". And also does the same with its driver.
     *
     * @param taxiId the id of the taxi in the list
     */
    public void setTaxiBusy(String taxiId) {
        if (this.taxisFree.contains(taxiId)) {
        	
        	if(getNumOfPassenOnBoard(taxiId) >= vehicleStorage.getEntityById(taxiId).getCapacity())
        	{
        		int taxiIndex = this.taxisFree.lastIndexOf(taxiId);
            	setTaxiBusy(taxiIndex);
        	}
        }
    }

    /**
     * Moves the taxi given as parameter from list of busy taxis into the list
     * "free". And also does the same with its driver.
     *
     * @param taxiId the id of the taxi in the list
     */
    public void setTaxiFree(String taxiId) {
    	System.out.println("\n\n\n\n\n taxi is free : " + taxiId );
    	System.out.println("\nsetting taxi free : " + taxiId );
    	vehicleStorage.getEntityById(taxiId).setTaxiFree();
    	
        if (this.taxisBusy.contains(taxiId)) {
            int taxiIndex = this.taxisBusy.lastIndexOf(taxiId);
            this.taxisFree.add(this.taxisBusy.remove(taxiIndex));
            this.taxiDriversFree.add(this.taxiDriversBusy.remove(taxiIndex));
        }
        System.out.println("\n\n\n\n\n");
    }

    /**
     * Get the id of the vehicle driven by the given driver
     *
     * @param taxiDriverId taxi driver id
     * @return taxi id, or null, if there's no such id
     */
    public String getVehicleId(String taxiDriverId) {
        for (int i = 0; i < this.taxiDriversFree.size(); i++) {
            if (this.taxiDriversFree.get(i).equals(taxiDriverId)) {
                return this.taxisFree.get(i);
            }
        }
        for (int i = 0; i < this.taxiDriversBusy.size(); i++) {
            if (this.taxiDriversBusy.get(i).equals(taxiDriverId)) {
                return this.taxisBusy.get(i);
            }
        }
        return null;
    }

    /**
     * Get the id of the taxi driver who drives the given vehicle
     *
     * @param vehicleId vehicle id
     * @return taxi driver id, or null, if there's no such id
     */
    public String getTaxiDriverId(String vehicleId) {
        for (int i = 0; i < this.taxisFree.size(); i++) {
            if (this.taxisFree.get(i).equals(vehicleId)) {
                return this.taxiDriversFree.get(i);
            }
        }
        for (int i = 0; i < this.taxisBusy.size(); i++) {
            if (this.taxisBusy.get(i).equals(vehicleId)) {
                return this.taxiDriversBusy.get(i);
            }
        }
        return null;
    }

    /**
     * Add a passenger on board of a taxi (just to remember it)
     *
     * @param passenId  passenger getting in a taxi
     * @param vehicleId taxi id
     */
    public void addPassengerOnBoard(String passenId, String vehicleId) {
        List<String> passenIds = this.taxiWithPassengersOnBoard.get(vehicleId);
        if (passenIds == null) {
            passenIds = new ArrayList<String>();
        }
        passenIds.add(passenId);
        this.taxiWithPassengersOnBoard.put(vehicleId, passenIds);
    }

    /**
     * Remove a passenger from a taxi (just to remember its current state)
     *
     * @param passenId  passenger getting off a taxi
     * @param vehicleId taxi id
     * @return true, if the passenger was found in the taxi
     */
    public boolean removePassengerOnBoard(String passenId, String vehicleId) {
       
    	System.out.println("\n======>>> Vehicle : " + vehicleId + "    remainging Passen : " + this.taxiWithPassengersOnBoard + "\n");
    	
    	/* 	made to remove old reqs*/
    	List<Request> passenReq = this.passengerRequests.get(vehicleId);
    	Request req = this.getRequestWithPassengerId(passenId);
    	
    	//System.out.println("\n==========>   Removing passenger : " + passenId + "    req : " + req.getPassengerId() + "\n");
	    passenReq.remove(req);
	    this.passengerRequests.put(vehicleId, passenReq); // not needed! working with references
    	/*for(Request r : this.passengerRequests.get(vehicleId))
    	{
    		System.out.println("\n==========>  vehicle : " + vehicleId + " passenger : " + r.getPassengerId() + "\n");
    	}*/
    	
    	List<String> passenIds = this.taxiWithPassengersOnBoard.get(vehicleId);
        if (passenIds == null) {
            return false;
        }
        boolean ret = passenIds.remove(passenId);
        this.taxiWithPassengersOnBoard.put(vehicleId, passenIds); // not needed! working with references
        
        System.out.println("\n======>>> Vehicle : " + vehicleId + "    Removing : " + passenId + "\n");
        System.out.println("\n======>>> Vehicle : " + vehicleId + "    remainging Passen : " + this.taxiWithPassengersOnBoard + "\n");
        
        return ret;
    }

    /**
     * Get number of passengers currently on board of the taxi
     *
     * @param vehicleId the taxi we're asking for
     * @return the number of passengers on board
     */
    public int getNumOfPassenOnBoard(String vehicleId) {
        List<String> passenOnBoard = this.taxiWithPassengersOnBoard.get(vehicleId);
        if (passenOnBoard == null) {
            return 0;
        }
        return passenOnBoard.size();
    }
    
    public List<String> getPassengerOnBoard(String vehicleId){
    	
    	return this.taxiWithPassengersOnBoard.get(vehicleId);
    }

    public List<Request> getPassengerRequests(String vehicleId) {
    	return this.passengerRequests.get(vehicleId);
    	
    }
    
   /* public void addPassengerRequest(Request request,String vehicleId) {
        List<Request> passenReq = this.passengerRequests.get(vehicleId);
        if (passenReq == null) {
            passenReq = new ArrayList<Request>();
        }
        passenReq.add(request);
        this.passengerRequests.put(vehicleId, passenReq);
    }*/
    
    /**
     * Save the position at the end of my trip (usually used by busy taxi
     * drivers, that are currently on the way)
     *
     * @param vehicleId    the id of the taxi
     * @param endOfTripPos the number of node - tposition at the end of taxi's trip
     */
    public void setEndOfTripPosition(String vehicleId, Long endOfTripPos) {
        this.taxiWithEndOfTripPositions.put(vehicleId, endOfTripPos);
    }

    /**
     * Get the future position of the taxi after finishing its trip
     *
     * @param taxiId id of the taxi
     * @return number of node - position at the end of the trip
     */
    public Long getEndOfTripPosition(String taxiId) {
        Long position = this.taxiWithEndOfTripPositions.get(taxiId);
        if (position == null) {
            return positionQuery.getCurrentPositionByNodeId(taxiId);
        }
        return position;
    }

    public Timer getDispatchingTimer() {
        return dispatchingTimer;
    }

    public Timer getTaxiDriversTimer() {
        return taxiDriversTimer;
    }

    public Timer getPassengersTimer() {
        return passengersTimer;
    }

    public Agent getDispatching() {
        return dispatching;
    }

    public List<String> getPassengers() {
        return passengers;
    }

    public List<String> getPassengers(String vehicleId) {
        List<String> retrieved = this.taxiWithPassengersOnBoard.get(vehicleId);

        if (retrieved != null)
            return Collections.unmodifiableList(retrieved);
        else
            return null;
    }

    public long getSimulationStartTime() {
        return simulationStartTime;
    }

    public void setSimulationEndTime(long simulationEndTime) {
        this.simulationEndTime = simulationEndTime;
    }

    /**
     * Returns how much time the simulation took
     *
     * @return simulation runtime (in milliseconds)
     */
    public long getSimulationRuntime() {
        long time;
        if (simulationEndTime == 0) {
            time = System.currentTimeMillis() - simulationStartTime;
        } else {
            time = simulationEndTime - simulationStartTime;
        }
        return time;
    }

    public boolean checkAdditionalRequirementsWithIncomingVehilce(String vehicleId,
                                                                  Set<String> passengerAdditionalRequirements) {
        TestbedVehicle vehicle = vehicleStorage.getEntityById(vehicleId);
        return vehicle.getVehicleEquipments().containsAll(passengerAdditionalRequirements);
    }

    public boolean isFree(String taxiDriverId) {
        return taxiDriversFree.contains(taxiDriverId) && !taxiDriversBusy.contains(taxiDriverId);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("TestbedModel - Taxis with passengers: ");
        sb.append(taxiWithPassengersOnBoard);
        sb.append("End locations: ");
        sb.append(taxiWithEndOfTripPositions);

        return sb.toString();
    }
    
    ///////////////
	public void addPassengerRequest(Request request, String vehicleId) {
		// TODO Auto-generated method stub
		
		List<Request> passenReq = this.passengerRequests.get(vehicleId);
        if (passenReq == null) {
            passenReq = new ArrayList<Request>();
        }
        passenReq.add(request);
        this.passengerRequests.put(vehicleId, passenReq);
        this.passengerReq.add(request);
	}

	
    public Request getRequestWithPassengerId(String passengerId)
    {
    	for(Request req : passengerReq)
    	{
    		if(passengerId == req.getPassengerId())
    			return req;
    	}
    	
		return null;
    }
}
