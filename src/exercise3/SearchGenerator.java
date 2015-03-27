package exercise3;

import java.util.ArrayList;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Control;
import peersim.core.Node;

public class SearchGenerator implements Control
{
	private final int _cdProtocolPid;
	private final int searchIncrement;
	private final boolean debug;
	private ArrayList<Integer> searchingNodes;

	public SearchGenerator(String prefix_)
	{
		searchingNodes = new ArrayList<Integer>();
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
		debug = Configuration.getBoolean("debug");
		searchIncrement = Configuration.getInt(prefix_+".searchIncrement");
	}
	
	public void removeSearchingNoded(Node node_)
	{
		searchingNodes.remove(node_);
	}
	
	/**
	 * The SearchGenerator controller is executed at each simulation cycle. Unlike InitializersEx1/2 which start
	 * a new search for ALL the nodes in the networks, it makes start at most searchIncrement network estimation per cycle
	 */
	public boolean execute()
	{
		//update the list of nodes that are searching (removing the ones which have finished)
		for(int i=0; i<Network.size() ; i++)
		{
			CDProtocolEx3 nodeCDProtocol = ((CDProtocolEx3)Network.get(i).getProtocol(_cdProtocolPid));
			if(nodeCDProtocol.getSearchFinished()&&searchingNodes.contains(i))
					searchingNodes.remove(new Integer(i));
		}
		
		Random generator = new Random();
		for(int i=0;i<searchIncrement&&searchingNodes.size()<Network.size();i++)
		{
			int newNodeId = generator.nextInt(Network.size());//select a node randomly...
			while(searchingNodes.contains(newNodeId))//...until you choose a node that is not already making an estimation
				newNodeId = generator.nextInt(Network.size());
			searchingNodes.add(newNodeId);//update the list of searching nodes...
			CDProtocolEx3 nodeCDProtocol = ((CDProtocolEx3)Network.get(newNodeId).getProtocol(_cdProtocolPid));
			nodeCDProtocol.startRealNewSearch(Network.get(newNodeId),_cdProtocolPid);//make start a REAL search
			if(debug)System.out.println("Node "+newNodeId+" has started a new research");
		}
		return false;
	}

}
