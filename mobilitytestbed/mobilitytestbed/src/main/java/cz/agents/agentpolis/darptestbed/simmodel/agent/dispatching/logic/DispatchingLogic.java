package cz.agents.agentpolis.darptestbed.simmodel.agent.dispatching.logic;

import cz.agents.agentpolis.darptestbed.global.Utils;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.dispatching.message.DispatcherSendsOutTaxiMessage;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.dispatching.message.FinalPlanConfirmationMessage;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.dispatching.message.FinalPlanFailureMessage;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.message.*;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.protocol.DriverCentralizedMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.message.Proposal;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.message.RequestReject;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.protocol.PassengerMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.protocol.GeneralMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.requestconsumer.message.ProposalAccept;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.requestconsumer.message.ProposalReject;
import cz.agents.agentpolis.darptestbed.siminfrastructure.planner.TestbedPlanner;
import cz.agents.agentpolis.darptestbed.simmodel.agent.AgentLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.Request;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.TripInfo;
import cz.agents.agentpolis.darptestbed.simmodel.agent.data.TripPlan;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedModel;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedVehicleStorage;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.query.AgentPositionQuery;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The basic features of a DispatchingAgent, especially his communication
 * protocol that enables him to contact other agents.
 * <p/>
 * The dispatching is used only in the case of centralized communication.
 *
 * @author Lukas Canda
 */
public abstract class DispatchingLogic extends AgentLogic<PassengerMessageProtocol> {

    private static final Logger LOGGER = Logger.getLogger(DispatchingLogic.class);

    /**
     * A planner, which computes the shortest path between two nodes
     */
    protected final TestbedPlanner pathPlanner;

    /**
     * A list of requests, that haven't been assigned to a taxi driver yet
     */
    protected List<Request> queueOfRequests;

    protected final DriverCentralizedMessageProtocol driverCentralizedMessageProtocol;

    /**
     * Storage for all vehicles (you can get them by ids)
     */
    protected final TestbedVehicleStorage vehicleStorage;

    public DispatchingLogic(String agentId, PassengerMessageProtocol sender,
                            DriverCentralizedMessageProtocol driverCentralizedMessageProtocol,
                            GeneralMessageProtocol generalMessageProtocol, TestbedModel taxiModel,
                            AgentPositionQuery positionQuery, AllNetworkNodes allNetworkNodes, Utils utils,
                            TestbedPlanner pathPlanner,
                            TestbedVehicleStorage vehicleStorage) {

        super(agentId, sender, generalMessageProtocol, taxiModel, positionQuery, utils);
        this.queueOfRequests = new ArrayList<Request>();
        this.pathPlanner = pathPlanner;
        this.driverCentralizedMessageProtocol = driverCentralizedMessageProtocol;
        this.vehicleStorage = vehicleStorage;
    }

    /**
     * Process a request, that's been just received from a passenger.
     *
     * @param request the new request
     */
    public abstract void processNewRequest(Request request);

    /**
     * Process the queue of requests (e.g. by a planning algorithm). This method
     * is usually indirectly called by a timer at regular intervals.
     */
    public abstract void processRequests();

    public abstract void confirmOrder(ProposalAccept proposalAccept);

    public abstract void processRejectedProposal(ProposalReject proposalReject);

    public abstract void processPassengerInVehicle(DriverReportsPassengerIsInMessage passengerIsInTaxiMessage);

    protected void sendMessageDispatcherAcceptsRequest(Request passengersRequest, TripInfo confirmation) {
        sendMessageDispatcherAcceptsRequest(Arrays.asList(passengersRequest), confirmation);
    }

    /**
     * When we send a taxi to passengers, we should notify them of it using this
     * message.
     *
     * @param passengerRequests all passengers that will be driven during the planned trip
     * @param confirmation      information about the trip (vehicle id etc.)
     */
    protected void sendMessageDispatcherAcceptsRequest(List<Request> passengerRequests, TripInfo confirmation) {

        for (Request request : passengerRequests) {
            sender.sendMessage(request.getPassengerId(),
                    new Proposal(request, confirmation.getDriverId(), confirmation.getVehicleId()));
        }

        // print out
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(confirmation.getDriverId() + " has been send to work for ");
        for (Request request : passengerRequests) {
            stringBuilder.append(request.getPassengerId() + " ");
        }

        LOGGER.debug(stringBuilder.toString());
    }

    protected void sendRequestReject(String passengersId, Request originalRequest) {
        sender.sendMessage(passengersId, new RequestReject(originalRequest, "dispatcher"));
    }

    /**
     * Order a vehicle to go for a specific trip.
     *
     * @param driverId driver id
     * @param tripPlan the exact way the driver should take
     */
    protected void sendMessageDispatcherSendsOutTaxi(String driverId, TripPlan tripPlan) {
        DispatcherSendsOutTaxiMessage dispatcherSendsOutTaxiMessage = new DispatcherSendsOutTaxiMessage(tripPlan);
        LOGGER.debug("send: " + driverId + " " + dispatcherSendsOutTaxiMessage);
        driverCentralizedMessageProtocol.sendMessage(driverId, dispatcherSendsOutTaxiMessage);
    }

    public void sendFinalPlanConfirmation(String driver) {
        List<String> wrapper = new ArrayList<>();
        wrapper.add(driver);
        sendFinalPlanConfirmation(wrapper);
    }

    public void sendFinalPlanConfirmation(List<String> drivers) {
        driverCentralizedMessageProtocol.sendMessage(drivers, new FinalPlanConfirmationMessage());
    }

    public void sendFinalPlanFailure(String driver) {
        List<String> wrapper = new ArrayList<>();
        wrapper.add(driver);
        sendFinalPlanFailure(wrapper);
    }


    public void sendFinalPlanFailure(List<String> drivers) {
        driverCentralizedMessageProtocol.sendMessage(drivers, new FinalPlanFailureMessage());
    }

    public abstract void processPassengerOffVehicle(
            DriverReportsPassengerHasLeftMessage driverReportsPassengerHasLeftMessage);

    public abstract void processDriverArrivedLateForPassengerPickup(
            DriverReportsLateForPassengerMessage driverArrivedLateForPassenger);

    public abstract void processDriverAcceptsNewPlan(DriverNewPlanAcceptMessage driverNewPlanAcceptMessage);

    public abstract void processDriverRejectsNewPlan(DriverNewPlanRejectMessage driverNewPlanRejectMessage);

}
