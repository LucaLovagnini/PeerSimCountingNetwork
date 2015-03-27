package exercise1;

import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class EndSimulationController implements Control {

	protected final int _cdProtocolPid;
	protected final int totalCycle;
	protected final boolean debug;

	public EndSimulationController(final String prefix_) 
	{
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
		totalCycle=Configuration.getInt("simulation.cycles");
		debug=Configuration.getBoolean("debug");
	}
	/**
	 * This method is executed at the end of every simulation cycle, and it is used only in the static version. It check that every node
	 * has finished its size estimation. If so, for each node, it counts the number of nodes in the network which has computed the same
	 * network estimation. If this number is equal to the node's estimation itself, then it means that the estimation was correct.
	 * This mechanism is necessary since the the network graph could be not connected, and so if a node makes an estimation smaller than
	 * the network size, it is not necessarily wrong. 
	 * The peersim simulation ends if all nodes have finished their network search, or if the max simulation cycle number is reached
	 */
	public boolean execute() {
		int finished=0;
		int []networkEstimation = new int [Network.size()];
		for(int i=0;i<Network.size();i++)
			if(((CDProtocolEx1)Network.get(i).getProtocol(_cdProtocolPid)).getNetSize()!=0)
			{
				finished++;
				networkEstimation[i]=((CDProtocolEx1)Network.get(i).getProtocol(_cdProtocolPid)).getNetSize();
			}	
		
		if(CDState.getCycle()==totalCycle-1||finished==Network.size())
		{
			int correctEstimation = 0;
			for(int i=0;i<Network.size();i++)
			{
				CDProtocolEx1 cdProtocol = ((CDProtocolEx1)Network.get(i).getProtocol(_cdProtocolPid));
				System.out.println("Node "+Network.get(i).getID()+" has estimated "+cdProtocol.getNetSize());
				if(networkEstimation[i]==1)
					correctEstimation++;
				else if(networkEstimation[i]!=-1)
				{
					int localEstimation = 1;
					for(int j=i+1;j<Network.size()&&localEstimation<networkEstimation[i];j++)
					{
						CDProtocolEx1 cdProtocoljth = ((CDProtocolEx1)Network.get(j).getProtocol(_cdProtocolPid));
						if(cdProtocoljth.getNetSize()==cdProtocol.getNetSize())
						{
							localEstimation++;
							networkEstimation[j]=-1;
						}
					}
					if(localEstimation==networkEstimation[i])
					{
						correctEstimation+=localEstimation;
					}
				}
			}
			System.out.println("The number of nodes that estimated correctly the network size are "+correctEstimation+" on "+Network.size());
			return true;
		}
		return false;
	}
}
