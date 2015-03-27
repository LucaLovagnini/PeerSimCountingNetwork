package exercise2;

import peersim.core.Node;
import exercise1.MessageEx1;

public class MessageEx2 extends MessageEx1{

	private final int version;
	
	//if version=i it means that this is the i-th search computed by the node with identifier=id
	public MessageEx2(Node creator, int id, int version) {
		super(creator, id);
		this.version=version;
	}
	
	public int getVersion()
	{
		return version;
	}

	
}
