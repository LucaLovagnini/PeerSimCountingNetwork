package cycleDrivenFlooding;

import java.util.ArrayList;
import java.util.List;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Node;

public class CDProtocolExample implements CDProtocol
{
	private int _linkableProtocolId;

	private boolean _nodeFounded;
	private boolean _searchCompleted;
	private long _nodeToFindId;
	private List<CDProtocolExample> _nodeToNotify;
	private int _propagationStep;

	public CDProtocolExample(final String prefix_)
	{
		_linkableProtocolId = Configuration.getPid(prefix_ + ".linkable");

		_nodeFounded = false;
		_nodeToFindId = -1;
		_nodeToNotify = new ArrayList<>();
		_propagationStep = -1;
	}

	@Override
	public Object clone()
	{
		CDProtocolExample ip = null;
		try
		{
			ip = (CDProtocolExample) super.clone();
		} catch (final CloneNotSupportedException e)
		{
			System.out.println("HELP");
		}
		ip._linkableProtocolId = _linkableProtocolId;
		ip._nodeFounded = false;
		ip._searchCompleted = false;
		ip._nodeToFindId = -1;
		ip._nodeToNotify = new ArrayList<>();
		ip._propagationStep = -1;

		return ip;
	}

	public boolean isSearchCompleted()
	{
		return _nodeFounded || (_nodeToFindId < 0) || _searchCompleted;
	}

	public boolean isNodePartecipating()
	{
		return _nodeFounded || (_nodeToFindId >= 0);
	}

	public boolean isNodeToFind()
	{
		return !_nodeFounded && (_nodeToFindId >= 0);
	}

	public boolean isToNotify()
	{
		return (_nodeFounded || _searchCompleted) && !_nodeToNotify.isEmpty();
	}

	public void nodeFounded(final boolean success_)
	{
		if(success_)
		{
			_nodeFounded = true;
			_nodeToFindId = -1;
		}
		_searchCompleted = true;
	}

	public void notifyNodes(final boolean success_)
	{
		for(final CDProtocolExample n : _nodeToNotify)
		{
			n.nodeFounded(success_);
		}

		_nodeToNotify = new ArrayList<>();
	}

	public void propagateSearch(final CDProtocolExample sender_, final long nodeIdToFind_)
	{
		_nodeToNotify.add(sender_);
		_nodeToFindId = nodeIdToFind_;
	}

	public void propagateSearch(final CDProtocolExample sender_, final long nodeIdToFind_, final int propagatioStep_)
	{
		_nodeToNotify.add(sender_);
		_nodeToFindId = nodeIdToFind_;
		_propagationStep = propagatioStep_;
	}

	@Override
	public void nextCycle(final Node node_, final int protocolID)
	{
		final CDProtocolExample cdProtocol = ((CDProtocolExample)node_.getProtocol(protocolID));
		final LinkableExample linkableProtocol = ((LinkableExample)node_.getProtocol(_linkableProtocolId));

		if(cdProtocol.isNodeToFind())
		{
			final Node nodeFounded = linkableProtocol.lookup(_nodeToFindId);

			if(nodeFounded != null)
			{
				cdProtocol.nodeFounded(true);
			} else if(_propagationStep > 0)
			{
				for(int i = 0 ; i < linkableProtocol.degree() ; i ++)
				{
					final Node neighbour = linkableProtocol.getNeighbor(i);
					final CDProtocolExample neighbourCdProtocol = ((CDProtocolExample)neighbour.getProtocol(protocolID));

					neighbourCdProtocol.propagateSearch(cdProtocol, _nodeToFindId, _propagationStep - 1);
				}
			} else
			{
				cdProtocol.nodeFounded(false);
			}
		}

		if(cdProtocol.isToNotify())
		{
			cdProtocol.notifyNodes(_nodeFounded);
		}
	}

	public void reset()
	{
		_nodeFounded = false;
		_nodeToFindId = -1;
		_nodeToNotify = new ArrayList<>();
	}
}