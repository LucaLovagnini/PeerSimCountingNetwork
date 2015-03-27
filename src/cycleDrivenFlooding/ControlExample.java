package cycleDrivenFlooding;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;

public class ControlExample implements Control
{
	private final int _cdProtocolPid;

	public ControlExample(final String prefix_)
	{
		_cdProtocolPid = Configuration.getPid(prefix_ + ".cdProtocol");
	}

	@Override
	public boolean execute()
	{
		final boolean isSearchCompleted = isSearchCompleted();

		if(isSearchCompleted)
		{
			System.out.println("Node partecipating: "+ countNodePartecipating());
			reset();
		}

		return isSearchCompleted;
	}

	public int countNodePartecipating()
	{
		int nodePartecipating = 0;

		for(int i = 0 ; (i < Network.size()) ; i ++)
		{
			final CDProtocolExample cdProtocol = ((CDProtocolExample)Network.get(i).getProtocol(_cdProtocolPid));

			if(cdProtocol.isNodePartecipating())
			{
				nodePartecipating ++;
			}
		}

		return nodePartecipating;
	}

	public boolean isSearchCompleted()
	{
		boolean completed = true;

		for(int i = 0 ; (i < Network.size()) && completed ; i ++)
		{
			final CDProtocolExample cdProtocol = ((CDProtocolExample)Network.get(i).getProtocol(_cdProtocolPid));

			completed &= cdProtocol.isSearchCompleted();
		}

		return completed;
	}

	public void reset()
	{
		for(int i = 0 ; (i < Network.size()); i ++)
		{
			final CDProtocolExample cdProtocol = ((CDProtocolExample)Network.get(i).getProtocol(_cdProtocolPid));

			cdProtocol.reset();
		}
	}

}
