package cz.agents.agentpolis.darptestbed.simmodel.agent.logicconstructor;

import com.google.inject.Injector;
import cz.agents.agentpolis.darptestbed.global.Utils;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.dispatching.protocol.DispatchingMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.protocol.DriverCentralizedMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.driver.protocol.DriverMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.passenger.protocol.PassengerMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.protocol.GeneralMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.communication.requestconsumer.protocol.RequestConsumerMessageProtocol;
import cz.agents.agentpolis.darptestbed.siminfrastructure.logger.RequestLogger;
import cz.agents.agentpolis.darptestbed.siminfrastructure.planner.TestbedPlanner;
import cz.agents.agentpolis.darptestbed.simmodel.agent.activity.movement.TestbedPassengerActivity;
import cz.agents.agentpolis.darptestbed.simmodel.agent.dispatching.logic.DispatchingLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.driver.logic.DriverCentralizedLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.driver.logic.DriverDecentralizedLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.driver.logic.DriverDecentralizedLogicExample;
import cz.agents.agentpolis.darptestbed.simmodel.agent.passenger.PassengerProfile;
import cz.agents.agentpolis.darptestbed.simmodel.agent.passenger.logic.PassengerCentralizedLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.passenger.logic.PassengerDecentralizedLogic;
import cz.agents.agentpolis.darptestbed.simmodel.agent.passenger.logic.PassengerDecentralizedLogicExample;
import cz.agents.agentpolis.darptestbed.simmodel.entity.vehicle.TestbedVehicle;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedModel;
import cz.agents.agentpolis.darptestbed.simmodel.environment.model.TestbedVehicleStorage;
import cz.agents.agentpolis.darptestbed.simulator.initializator.LogicConstructor;
import cz.agents.agentpolis.simmodel.agent.activity.TimeSpendingActivity;
import cz.agents.agentpolis.simmodel.agent.activity.movement.DriveVehicleActivity;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.AllNetworkNodes;
import cz.agents.agentpolis.simmodel.environment.model.query.AgentPositionQuery;

public class DecentralizedLogicConstructor implements LogicConstructor {
    @Override
    public PassengerDecentralizedLogic constructPassengerLogic(
            String agentId, RequestConsumerMessageProtocol sender, DriverMessageProtocol driverMessageProtocol,
            GeneralMessageProtocol generalMessageProtocol,
            TestbedModel taxiModel, AgentPositionQuery positionQuery, Utils utils, PassengerProfile passengerProfile,
            TestbedPassengerActivity passengerActivity,
            TimeSpendingActivity timeSpendingActivity, RequestLogger logger) {
        return new PassengerDecentralizedLogicExample(agentId, sender, driverMessageProtocol, generalMessageProtocol,
                taxiModel, positionQuery, utils, passengerProfile, passengerActivity, timeSpendingActivity, logger);
    }

    @Override
    public boolean usesDispatching() {
        return false;
    }

    @Override
    public DriverDecentralizedLogic constructDriverLogic(String agentId, PassengerMessageProtocol sender,
                                                                    GeneralMessageProtocol generalMessageProtocol,
                                                                    TestbedModel taxiModel,
                                                                    AgentPositionQuery positionQuery,
                                                                    AllNetworkNodes allNetworkNodes, Utils utils,
                                                                    TestbedVehicle vehicle,
                                                                    DriveVehicleActivity drivingActivity,
                                                                    Injector injector) {
        return new DriverDecentralizedLogicExample(agentId, sender, generalMessageProtocol,
                taxiModel, positionQuery, allNetworkNodes, utils, vehicle, drivingActivity);
    }

    @Override
    public DispatchingLogic constructDispatchingLogic(
            String dispatching, PassengerMessageProtocol sender,
            DriverCentralizedMessageProtocol driverCentralizedMessageProtocol,
            GeneralMessageProtocol generalMessageProtocol, TestbedModel taxiModel,
            AgentPositionQuery positionQuery, AllNetworkNodes allNetworkNodes, Utils utils, TestbedPlanner pathPlanner,
            TestbedVehicleStorage vehicleStorage) {
        throw new IllegalArgumentException();
    }
}
