package cycleDrivenFlooding;

import java.util.ArrayList;
import java.util.List;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

public class LinkableExample implements Linkable, Protocol
{
	protected List<Node> _neighbors;

	public LinkableExample(final String prefix_)
	{
		_neighbors = new ArrayList<Node>();
	}

	public List<Node> getNeighboar()
	{
		return _neighbors;
	}

	@Override
	public Object clone()
	{
		LinkableExample ip = null;
		try
		{
			ip = (LinkableExample) super.clone();
		} catch (final CloneNotSupportedException e)
		{
			System.out.println("HELP");
		}
		ip._neighbors = new ArrayList<Node>();
		return ip;
	}

	@Override
	public boolean contains(final Node n)
	{
		return _neighbors.contains(n);
	}

	@Override
	public boolean addNeighbor(final Node n)
	{
		if ((n != null) && (false == _neighbors.contains(n)))
		{
			_neighbors.add(n);
			return true;
		} else
		{
			return false;
		}
	}

	@Override
	public Node getNeighbor(final int i)
	{
		return _neighbors.get(i);
	}

	public Node lookup(final long id_)
	{
		for(final Node n : _neighbors)
		{
			if(n.getID() == id_)
			{
				return n;
			}
		}

		return null;
	}

	@Override
	public int degree()
	{
		return _neighbors.size();
	}

	@Override
	public void pack()
	{

	}

	@Override
	public String toString()
	{
		return "";
	}

	@Override
	public void onKill()
	{
		_neighbors = null;
	}

}