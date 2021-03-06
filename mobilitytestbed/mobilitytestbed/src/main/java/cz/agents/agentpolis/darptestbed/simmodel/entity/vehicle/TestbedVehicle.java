package cz.agents.agentpolis.darptestbed.simmodel.entity.vehicle;

import java.util.Set;

import cz.agents.agentpolis.darptestbed.simmodel.agent.data.TripPlan;
import cz.agents.agentpolis.simmodel.entity.EntityType;
import cz.agents.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.GraphType;


/**
 * One such vehicle is generated for every taxi driver. 
 * 
 */
public class TestbedVehicle extends Vehicle {

	private final Set<String> vehicleEquipments;
	private TripPlan vehicleTripPlan;

	public TestbedVehicle(String vehicleId, EntityType type, double lengthInMeters, int vehiclePassengerCapacity,
			GraphType usingGraphTypeForMoving, Set<String> vehicleEquipments) {

		super(vehicleId, type, lengthInMeters, vehiclePassengerCapacity, usingGraphTypeForMoving);

		this.vehicleEquipments = vehicleEquipments;
		this.vehicleTripPlan = null;
	}

	public Set<String> getVehicleEquipments() {
		return vehicleEquipments;
	}

	public TripPlan getVehicleTrip() {
		return vehicleTripPlan;
	}
	
	public void setTripPlan(TripPlan vehicleTripPlan)
	{
		this.vehicleTripPlan = vehicleTripPlan;
	}
	
	public void setTaxiFree()
	{
		this.vehicleTripPlan = null;
		System.out.println("\n New Trip plan : " + vehicleTripPlan);
	}
}
