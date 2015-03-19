package cz.agents.agentpolis.darptestbed.siminfrastructure.logger.item;

import cz.agents.agentpolis.siminfrastructure.logger.LogItem;

public class VehicleMovementLogItem implements LogItem {

	public final String vehicleId;
	public final long currentSimulationTime;
	public final long toByNodeId;

	public VehicleMovementLogItem(String vehicleId, long currentSimulationTime, long toByNodeId) {
		super();
		this.vehicleId = vehicleId;
		this.currentSimulationTime = currentSimulationTime;
		this.toByNodeId = toByNodeId;
	}

}
