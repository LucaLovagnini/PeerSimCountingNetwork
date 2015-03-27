package exercise2;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import exercise2.CDProtocolEx2;

public class InitializerEx2 implements Control
{
	private final int _cdProtocolPid;

	public InitializerEx2(final String prefix_)
	{
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
	}

	@Override
	public boolean execute()
	{
		/*CDProtocolEx2 nodeCDProtocol = ((CDProtocolEx2)Network.get(7).getProtocol(_cdProtocolPid));
		nodeCDProtocol.startNewSearch(Network.get(7),_cdProtocolPid);*/
		for(int i=0;i<Network.size();i++)
		{
			CDProtocolEx2 nodeCDProtocol = ((CDProtocolEx2)Network.get(i).getProtocol(_cdProtocolPid));
			nodeCDProtocol.startNewSearch(Network.get(i),_cdProtocolPid);
		}
		return false;
	}
}
