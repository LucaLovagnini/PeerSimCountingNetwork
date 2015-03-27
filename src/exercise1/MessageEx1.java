package exercise1;

import peersim.core.Node;

public class MessageEx1 {
	protected final int id;//the identifier of the node which has started the search
	protected Node creator;//the node which has created the message
	protected boolean BR;//Backward Routing flag
	protected int size;//the partial network estimation
	public MessageEx1(Node creator , int id)
	{
		this.id=id;
		this.creator=creator;
		this.BR=false;
		this.size=0;
	}

	public int getId()
	{
		return id;
	}
	
	public Node getCreator()
	{
		return creator;
	}
	
	public boolean isBR()
	{
		return BR;
	}
	
	public void setBR()
	{
		BR=true;
	}
	public void increaseSize(int s)
	{
		size+=s;
	}
	public int size()
	{
		return size;
	}
	public void toRemove()
	{
		this.creator=null;
	}
}
