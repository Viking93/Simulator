package cz.agents.agentpolis.darptestbed.simmodel.agent.dispatching.logic;

import com.google.common.collect.Maps;

import cz.agents.agentpolis.darptestbed.global.Utils;
import cz.agents.agentpolis.darptestbed.global.data.DriverAndDistance;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.message.*;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.protocol.DriverCentralizedMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.message.OrderConfirmation;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.protocol.PassengerMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.protocol.GeneralMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.requestconsumer.message.ProposalAccept;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.requestconsumer.message.ProposalReject;
import cz.agents.agentpolis.darptestbed.siminfrastructure.planner.TestbedPlanner;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.FlexiblePlan;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.Request;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.TripInfo;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.TripPlan;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.generator.PassengersInAndOutPair;
import cz.agents.agentpolis.darptestbed.simmodel.entity.vehicle.TestbedVehicle;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedModel;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedVehicleStorage;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trip;
import cz.agents.agentpolis.siminfrastructure.planner.trip.Trips;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.query.AgentPositionQuery;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class DispatchingLogicExample extends DispatchingLogic {

    private static final Logger LOGGER = Logger.getLogger(DispatchingLogicExample.class);

    public DispatchingLogicExample(String agentId, PassengerMessageProtocol sender,
                                   DriverCentralizedMessageProtocol driverCentralizedMessageProtocol,
                                   GeneralMessageProtocol generalMessageProtocol, TestbedModel taxiModel,
                                   AgentPositionQuery positionQuery, AllNetworkNodes allNetworkNodes, Utils utils, TestbedPlanner pathPlanner,
                                   TestbedVehicleStorage vehicleStorage) {
        super(agentId, sender, driverCentralizedMessageProtocol, generalMessageProtocol, taxiModel, positionQuery,
                allNetworkNodes, utils,
                pathPlanner, vehicleStorage);

    }

    @Override
    public void processNewRequest(Request request)
    {
    	String driverId = null, vehicleId = null;
    	FlexiblePlan flexiblePlan = null;
    	TripPlan tripPlan = null;
    	TestbedVehicle vehicle = null;
    	
    	LOGGER.getRootLogger().setLevel(Level.DEBUG);
    	
    	System.out.println("\n");
    	LOGGER.info("	Request: [" + utils.toHoursAndMinutes(request.getTimeWindow().getEarliestDeparture())
                + "] from " + request.getPassengerId() + ", latest departure: "
                + utils.toHoursAndMinutes(request.getTimeWindow().getLatestDeparture()) + " " + request);
    	
    	DriverAndDistance []driverAndDistance = utils.getDistMapForPassenger(
    			positionQuery.getCurrentPositionByNodeId(request.getPassengerId()),
    			taxiModel.getTaxiDriversFree(), 
    			true);
    	
    	//LOGGER.info("	free drivers : " + taxiModel.getTaxiDriversFree());
    	
    	if(driverAndDistance == null || driverAndDistance.length == 0)
    	{
    		
    		sendRequestReject(request.getPassengerId(), request);
            LOGGER.info("	Reply:   REJECT [suitable taxi not found]");
            
            return;
    	}
    	
    	for(DriverAndDistance drinDis : driverAndDistance)
    	{
	    	driverId = new String(drinDis.getTaxiDriverId());
	    	vehicleId = new String(taxiModel.getVehicleId(driverId));
	    	
	    	taxiModel.addRequestToVehicle(request, vehicleId);
	    	LOGGER.debug("	offBoard Requests : " + utils.getPassengerIdsFromRequests(taxiModel.getRequestsOffBoard(vehicleId)));
	    	LOGGER.debug("	onBoard Requests : " + utils.getPassengerIdsFromRequests(taxiModel.getRequestsOnBoard(vehicleId)));
	    	
	    	vehicle = vehicleStorage.getEntityById(vehicleId);
	    	
	    	if(taxiModel.getRequestsOnBoard(vehicleId) == null)
	    		flexiblePlan = utils.planTrips(
		    			taxiModel.getRequestsOffBoard(vehicleId),
		    			vehicle,
		    			flexiblePlan,
		    			true);
	    	else
	    		flexiblePlan = utils.planTrips(
	        			taxiModel.getRequestsOffBoard(vehicleId),
	        			vehicle,
	        			taxiModel.getRequestsOnBoard(vehicleId),
	        			flexiblePlan,
	        			true);
	    	
	    	if(flexiblePlan == null)
	    	{
	    		taxiModel.removeRequestFromVehicle(request, vehicleId);
	    	}
	    	else
	    		break;
    	}
    	if(flexiblePlan == null)
    	{
    		sendRequestReject(request.getPassengerId(), request);
            LOGGER.info("	Reply:   REJECT [unable to plan a flexibleTrip]");
            
            return;
    	}
    	
    	tripPlan = utils.makeTripPlan(flexiblePlan);
    	//utils.show(flexiblePlan);
    	//LOGGER.debug("	Trip Plan : " + tripPlan);
    	
    	if(tripPlan == null)
    	{
    		sendRequestReject(request.getPassengerId(), request);
            LOGGER.info("	Reply:   REJECT [unable to plan a trip]");
            
            return;
    	}
    	sendMessageDispatcherSendsOutTaxi(driverId, tripPlan);
        sendMessageDispatcherAcceptsRequest(request, new TripInfo(driverId, vehicle.getId()));
        
        taxiModel.setTaxiBusy(vehicle.getId());
        
        LOGGER.info("	Reply:   ACCEPT [sending " + vehicle.getId() + "]");
        LOGGER.info("	Trips:\n\n" + tripPlan);
    }
    
    
    
    //@Override
    public void processNewRequest1(Request request) {
    	LOGGER.getRootLogger().setLevel(Level.DEBUG);
    	
        // print out the request for debugging purposes
        LOGGER.info("	Request: [" + utils.toHoursAndMinutes(request.getTimeWindow().getEarliestDeparture())
                + "] from " + request.getPassengerId() + ", latest departure: "
                + utils.toHoursAndMinutes(request.getTimeWindow().getLatestDeparture()) + " " + request);

        // check if there are any free taxi drivers
        if (taxiModel.getTaxiDriversFree().size() == 0) {
            // if there are no free drivers, dispatcher needs to reject the
            // request
            this.sendRequestReject(request.getPassengerId(), request);
            LOGGER.info("	Reply:   REJECT [no free taxis]");
            return;
        }

        // loop over all free taxi drivers
        for (String taxiDriverId : taxiModel.getTaxiDriversFree()) {

            // get the object representation of this driver's vehicle
            TestbedVehicle taxiVehicle = vehicleStorage.getEntityById(taxiModel.getVehicleId(taxiDriverId));

            // skip those taxi drivers, who have currently full capacity
            if (taxiModel.getNumOfPassenOnBoard(taxiModel.getVehicleId(taxiDriverId)) >= taxiVehicle.getCapacity()) {
                continue;
            }

            // skip those taxis, which don't meet some of the passenger's
            // special requirements (wheel chair support, child seat, etc.)
            if (!taxiVehicle.getVehicleEquipments().containsAll(request.getAdditionalRequirements())) {
                continue;
            }

            // compute the driving time between the passenger and the driver
            double timeToPassenger = utils.computeDrivingTime(taxiDriverId, request.getPassengerId());

            // compute when this driver could pick up the passenger
            long pickUpTime = utils.getCurrentTime() + (long) timeToPassenger;

            // if this driver is able to pick up the passenger within his
            // departure time window ...
            if (pickUpTime <= request.getTimeWindow().getLatestDeparture()) {

                // plan the trips (paths)
                // (1) from driver to passenger's departure node and
                // (2) from origin to arrival node
                Trip toPassenger = utils.planTrip(taxiVehicle.getId(),
                        positionQuery.getCurrentPositionByNodeId(taxiDriverId), request.getFromNode());
                Trip toDestination = utils.planTrip(taxiVehicle.getId(), request.getFromNode(), request.getToNode());

                // concatenate those two trips into a drivePath for the driver
                Trips drivePath = new Trips();
                if (toPassenger != null && toPassenger.numOfCurrentTripItems() > 0)
                    drivePath.addTrip(toPassenger);
                if (toDestination != null && toDestination.numOfCurrentTripItems() > 0)
                    drivePath.addTrip(toDestination);

                // if we didn't successfully find and concatenate two required
                // trips, skip this driver
                if (drivePath.numTrips() != 2)
                    continue;

                // create a pickup map for this path (tells driver where he
                // should pick up which passengers)
                Map<Long, PassengersInAndOutPair> pickUpAndDropOffMap = Maps.newHashMap();
                pickUpAndDropOffMap.put(request.getFromNode(),
                        new PassengersInAndOutPair(new HashSet<>(Arrays.asList(request.getPassengerId())),
                                new HashSet<String>()));
                pickUpAndDropOffMap.put(request.getToNode(),
                        new PassengersInAndOutPair(new HashSet<String>(),
                                new HashSet<>(Arrays.asList(request.getPassengerId()))));

                // send the driver on this path
                TripPlan tripPlan = new TripPlan(drivePath, pickUpAndDropOffMap);

                // confirm the request and tell the passenger which car will
                // pick him/her up
                sendMessageDispatcherSendsOutTaxi(taxiDriverId, tripPlan);
                sendMessageDispatcherAcceptsRequest(request, new TripInfo(taxiDriverId, taxiVehicle.getId()));

                // flag this driver as "busy"
                taxiModel.setTaxiBusy(taxiVehicle.getId());
                	
                

                // print some debug information
                LOGGER.info("	Reply:   ACCEPT [sending " + taxiVehicle.getId() + "]");
                LOGGER.info("	Trips:\n\n" + tripPlan.getTrips().toString());

                // once we've sent the driver out, we can finish the execution
                // of this function
                return;

            }

        }

        sendRequestReject(request.getPassengerId(), request);
        LOGGER.info("	Reply:   REJECT [suitable taxi not found]");

    }

    @Override
    public void processRequests() {
        // TODO Auto-generated method stub

    }

    @Override
    public void confirmOrder(ProposalAccept proposalAccept) {
        sender.sendMessage(proposalAccept.proposal.getPassengerId(), new OrderConfirmation(new TripInfo(
                proposalAccept.proposal.getDriverId(), proposalAccept.proposal.getVehicleId())));
    }

    @Override
    public void processRejectedProposal(ProposalReject proposalReject) {
        taxiModel.setTaxiBusy(proposalReject.proposal.getVehicleId());
    }

    @Override
    public void processPassengerInVehicle(DriverReportsPassengerIsInMessage passengerIsInTaxiMessage) {
    }

    @Override
    public void processPassengerOffVehicle(DriverReportsPassengerHasLeftMessage driverReportsPassengerHasLeftMessage) {
    }

    @Override
    public void processDriverArrivedLateForPassengerPickup(
            DriverReportsLateForPassengerMessage driverArrivedLateForPassenger) {
    }

    @Override
    public void processDriverAcceptsNewPlan(DriverNewPlanAcceptMessage driverNewPlanAcceptMessage) {
        sendFinalPlanConfirmation(driverNewPlanAcceptMessage.tripInfo.getDriverId());
    }

    @Override
    public void processDriverRejectsNewPlan(DriverNewPlanRejectMessage driverNewPlanRejectMessage) {
    	LOGGER.debug("\n\n	Driver rejected new plan :(\n\n");
        sendFinalPlanFailure(driverNewPlanRejectMessage.tripInfo.getDriverId());
    }
}
