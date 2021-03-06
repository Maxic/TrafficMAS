package nl.uu.trafficmas.tests.agents.action;

import static org.junit.Assert.assertEquals;
import it.polito.appeal.traci.SumoTraciConnection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

import nl.uu.trafficmas.agent.Agent;
import nl.uu.trafficmas.agent.actions.AgentAction;
import nl.uu.trafficmas.controller.TrafficMASController;
import nl.uu.trafficmas.datamodel.DataModel;
import nl.uu.trafficmas.datamodel.DataModelXML;
import nl.uu.trafficmas.datamodel.MASData;
import nl.uu.trafficmas.roadnetwork.RoadNetwork;
import nl.uu.trafficmas.roadnetwork.Route;
import nl.uu.trafficmas.simulationmodel.SimulationModelTraaS;
import nl.uu.trafficmas.simulationmodel.StateData;

import org.junit.Test;
import org.xml.sax.SAXException;

public class ChangeRouteTest {

	@Test
	public void changeRoute() throws SAXException, IOException, ParserConfigurationException {
		Random random 	= new Random(1337);
		String dir 		= System.getProperty("user.dir")+"/tests/AgentActions/ChangeRoute/";
		String sumocfg 	= System.getProperty("user.dir")+"/tests/AgentActions/ChangeRoute/ConfigTest.xml";
		String masXML 	= "MASTest.xml";
		
		DataModel dataModel = new DataModelXML(dir,masXML);
		MASData masData		= dataModel.getMASData();
		
		HashMap<String, String> options = new LinkedHashMap<String, String>();
		options.put("e", Integer.toString(masData.simulationLength));
		options.put("start", "1");
		options.put("quit-on-end", "1");
		
		SumoTraciConnection conn = SimulationModelTraaS.initializeWithOptions(options,"sumo", sumocfg);				
		RoadNetwork rn = dataModel.instantiateRoadNetwork();
		ArrayList<Route> routes = dataModel.getRoutes(rn);
		
		HashMap<Agent,Integer> agentPairList 	= TrafficMASController.instantiateAgents(masData, random, routes, rn);
		HashMap<String, Agent> completeAgentMap = SimulationModelTraaS.addAgents(agentPairList, random, -1, conn);
		HashMap<String, Agent> currentAgentMap 	= SimulationModelTraaS.updateCurrentAgentMap(completeAgentMap, new LinkedHashMap<String, Agent>(), conn);
	
		try {
			int i = 0;
			ArrayList<Agent> newRouteAgents = new ArrayList<Agent>();
			ArrayList<String> newRoute = new ArrayList<String>();
			newRoute.add("edge0");
			newRoute.add("edge1");
			newRoute.add("edge2");
			while (i++ < masData.simulationLength) {
				currentAgentMap = SimulationModelTraaS.updateCurrentAgentMap(completeAgentMap, currentAgentMap, conn);
				StateData stateData 		= SimulationModelTraaS.getStateData(conn, true);
				currentAgentMap = TrafficMASController.updateAgents(completeAgentMap, rn, stateData);
				rn = TrafficMASController.updateRoadNetwork(rn, stateData);
				// Change the route of the first two agents
				if(i==10){
					HashMap<Agent, AgentAction> actions = new HashMap<Agent, AgentAction>();
					for(Entry<String, Agent> entry : currentAgentMap.entrySet()){
						Agent agent = entry.getValue();
						actions.put(agent, AgentAction.ChangeRoute);
						newRouteAgents.add(agent);
						agent.setPossibleNewRoute(newRoute);
					}
					SimulationModelTraaS.simulateAgentActions(actions, conn);
				}

				// Check if both agents have taken the alternate route.
				if(i==48){
					for(Agent agent : newRouteAgents){
						assertEquals("edge1", agent.getRoad().id);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
