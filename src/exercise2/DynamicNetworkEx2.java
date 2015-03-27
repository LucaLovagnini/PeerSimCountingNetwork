package exercise2;

import java.util.ArrayList;
import java.util.Random;

import exercise1.LinkableEx1;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;

public class DynamicNetworkEx2 extends peersim.dynamics.DynamicNetwork {

	protected int _linkableProtocolId;
	private final int _cdProtocolPid;
	private final int _degreeMin;
	private final int _degreeMax;
	
	
	public DynamicNetworkEx2(String prefix)
	{
		super(prefix);
		_linkableProtocolId = Configuration.getPid(prefix + ".linkable");
		_cdProtocolPid = Configuration.getPid(prefix + ".cdProtocol");
		_degreeMin = Configuration.getInt(prefix+".degreeMin", 1);
		_degreeMax = Configuration.getInt(prefix+".degreeMax", 100);

	}
	
	/**
	 * This is an override of the DynamicNetwork's method, since for each removed node we have to update the list of neighbors
	 * For the same reason the add method is overridden too
	 */
	public void remove (int n)
	{
		super.remove(n);
		ArrayList<Node> removedNodes = new ArrayList<Node>();
		for(int i=0 ; i<Network.size() ; i++)
		{
			Node node_ = Network.get(i);
			LinkableEx1 linkableProtocol = ((LinkableEx1)node_.getProtocol(_linkableProtocolId));
			for(int j=linkableProtocol.degree()-1;j>=0;j--)
			{
				Node neighbor = linkableProtocol.getNeighbor(j);
				if(!neighbor.isUp())
				{
					if(!removedNodes.contains(neighbor))
						removedNodes.add(neighbor);
					linkableProtocol.removeNeighbor(j);
				}
			}
		}
		System.out.print("Removed nodes: ");
		for(Node node : removedNodes)
			System.out.print(node.getID()+" ");
		System.out.println("");
	}
	
	public void add (int n)
	{
		final Random randomDegree = new Random(1);
		final Random randomEdge = new Random(10);
		ArrayList<Node> oldNetwork = new ArrayList<Node>();
		for(int i =0; i< Network.size() ; i++)
			oldNetwork.add(Network.get(i));
		super.add(n);
		for(int i=0 ; i < Network.size() ; i++)
			if(!oldNetwork.contains(Network.get(i)))
			{
				Node newNode = Network.get(i);
				LinkableEx1 newNodelinkableProtocol = ((LinkableEx1)newNode.getProtocol(_linkableProtocolId));
				CDProtocolEx2 newNodeCdProtocol = ((CDProtocolEx2)newNode.getProtocol(_cdProtocolPid));
				System.out.print("New node "+newNode.getID()+" has neighbors: ");
				int degree = randomDegree.nextInt((_degreeMax - _degreeMin)) + _degreeMin;
				if(degree>Network.size())
				{
					System.out.print("Degree troppo grande= "+degree+". ");
					degree=Network.size()-1;
				}
				for(int j=0;j<degree;j++)
				{
					Node other;
					do
						other = Network.get(randomEdge.nextInt(Network.size()));
					while(newNodelinkableProtocol.getNeighboar().contains(other)||newNode.equals(other));
					System.out.print(other.getID()+" ");
					LinkableEx1 otherlinkableProtocol = ((LinkableEx1)other.getProtocol(_linkableProtocolId));
					newNodelinkableProtocol.addNeighbor(other);
					otherlinkableProtocol.addNeighbor(newNode);
				}
				newNodeCdProtocol.startNewSearch(newNode, _cdProtocolPid);
				System.out.println("");
			}
	}
	
}
