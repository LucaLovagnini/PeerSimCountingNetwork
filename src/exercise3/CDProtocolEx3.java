package exercise3;

import peersim.core.Node;
import exercise2.CDProtocolEx2;

public class CDProtocolEx3  extends CDProtocolEx2{

	private boolean searchFinished;
	
	public CDProtocolEx3(String prefix_)
	{
		super(prefix_);
		searchFinished = true;
	}
	
	public Object clone()
	{
		CDProtocolEx3 ip = null;
		ip = (CDProtocolEx3) super.clone();
		ip.searchFinished = true;
		return ip;
	}	
	
	/**
	 * We want that each node which has finished is network estimation will not start a new one until it is chosen by
	 * the SearchGenerator controller. So, since this method is called only by nextCycle() in CDProtocolEx2, 
	 * when the network estimation is finished we will set the searchFinished flag to true.
	 * So when SearchGenerator will test searchFinished and it will see that is true, it will call startRealNewSearch
	 */
	public void startNewSearch(final Node node_ , final int protocolID)
	{
		if(debug)System.out.println("Node "+node_.getID()+" has finished his research");
		searchFinished = true;
	}
	
	/**
	 * This method is called by execute() inside SearchGenerator. Unless the overridden version of startNewSearch
	 * which actually doesn't start a new search, this method does.
	 * @param node_
	 * @param protocolID
	 */
	
	public void startRealNewSearch(final Node node_ , final int protocolID)
	{
		super.startNewSearch(node_, protocolID);
		searchFinished = false;
	}
	
	public boolean getSearchFinished()
	{
		return searchFinished;
	}
}
