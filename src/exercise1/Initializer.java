package exercise1;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class Initializer implements Control
{
	private final int _cdProtocolPid;

	public Initializer(final String prefix_)
	{
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
	}

	/**
	 * This method is called only once during the simulation, at its beginning. It makes start a network
	 * estimation for each nodes present inside the network.
	 */
	public boolean execute()
	{
		for(int i=0;i<Network.size();i++)
		{
			CDProtocolEx1 nodeCDProtocol = ((CDProtocolEx1)Network.get(i).getProtocol(_cdProtocolPid));
			nodeCDProtocol.startNewSearch(Network.get(i),_cdProtocolPid);
		}
		return false;
	}
}
