package exercise2;

import peersim.cdsim.CDState;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class EndSimulationControllerEx2 implements Control {

	protected final int _cdProtocolPid;
	protected final int totalCycle;
	protected final boolean debug;

	public EndSimulationControllerEx2(final String prefix_) 
	{
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
		totalCycle=Configuration.getInt("simulation.cycles");
		debug=Configuration.getBoolean("debug");
	}
	/**
	 * Unlike the static version, the simulation in the dynamic version ends only when the simulator has done a number of cycle equal
	 * to the parameter set in the configuration file, and so this controller has to be re-implemented
	 */
	public boolean execute() {
		int []networkEstimation = new int [Network.size()];
		//since the network graph could be not connected, it is necessary that the network estimation size is !=0 to be considered correct
		for(int i=0;i<Network.size();i++)
			if(((CDProtocolEx2)Network.get(i).getProtocol(_cdProtocolPid)).getNetSize()!=0)
				networkEstimation[i]=((CDProtocolEx2)Network.get(i).getProtocol(_cdProtocolPid)).getNetSize();
		
		if(CDState.getCycle()==totalCycle-1)
		{
			int correctEstimation = 0;
			for(int i=0;i<Network.size();i++)
			{
				CDProtocolEx2 cdProtocol = ((CDProtocolEx2)Network.get(i).getProtocol(_cdProtocolPid));
				System.out.println("Node "+Network.get(i).getID()+" has estimated "+cdProtocol.getNetSize());
				if(networkEstimation[i]==1)
					correctEstimation++;
				else if(networkEstimation[i]!=-1)
				{
					int localEstimation = 1;
					for(int j=i+1;j<Network.size()&&localEstimation<networkEstimation[i];j++)
					{
						CDProtocolEx2 cdProtocoljth = ((CDProtocolEx2)Network.get(j).getProtocol(_cdProtocolPid));
						if(cdProtocoljth.getNetSize()==cdProtocol.getNetSize())
						{
							localEstimation++;
							networkEstimation[j]=-1;
						}
					}
					if(localEstimation==networkEstimation[i])
						correctEstimation+=localEstimation;
					//System.out.println("localEstimation="+localEstimation);
				}
			}
			System.out.println("The number of nodes that estimated correctly the network size are "+correctEstimation+" on "+Network.size());
			return true;
		}
		return false;
	}
}
