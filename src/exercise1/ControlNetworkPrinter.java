package exercise1;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class ControlNetworkPrinter implements Control
{
	protected final int _linkableProtocolId;
	protected final boolean debug;

	public ControlNetworkPrinter(final String prefix_)
	{
		_linkableProtocolId = Configuration.getPid(prefix_ + ".linkable");
		debug=Configuration.getBoolean("debug");
	}

	/**
	 * This controller simply print the network graph at the actual cycle of the simulator
	 */
	public boolean execute()
	{
		if(debug) System.out.println("");
		if(debug) System.out.println("Network Print:");
		for(int i=0;i<Network.size();i++)
		{
			LinkableEx1 linkableProtocol = ((LinkableEx1)Network.get(i).getProtocol(_linkableProtocolId));
			for(int j=0;j<linkableProtocol.degree();j++)
			{
				if(linkableProtocol.getNeighbor(j).getID()>Network.get(i).getID())
					if(debug)System.out.println("("+Network.get(i).getID()+","+linkableProtocol.getNeighbor(j).getID()+")");
			}
			if(linkableProtocol.degree()==0)
				if(debug)System.out.println("("+Network.get(i).getID()+")<---NODE WITHOUT NEIGHBORS!");
		}
		return false;
	}
}
