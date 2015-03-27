package exercise4;

import java.util.ArrayList;

import peersim.config.Configuration;
import peersim.core.Node;
import exercise1.MessageEx1;
import exercise2.CDProtocolEx2;

public class CDProtocolEx4 extends CDProtocolEx2 {
	
	private int maxsize;
	private ArrayList<MessageEx1> realInbox ;

	public CDProtocolEx4(String prefix_)
	{
		super(prefix_);
		maxsize = Configuration.getInt(prefix_+".maxsize");
		realInbox = new ArrayList<MessageEx1> ();
	}
	
	public Object clone()
	{
		CDProtocolEx4 ip = null;
		ip = (CDProtocolEx4) super.clone();
		ip.maxsize = maxsize;
		ip.realInbox = new ArrayList<MessageEx1>();
		return ip;
	}
	
	@Override
	public void propagateMessage(MessageEx1 m)
	{
		realInbox.add(m);
		System.out.println("realInbox received message ID="+m.getId()+" isBR="+m.isBR());
	}
	
	public void nextCycle(final Node node_, final int protocolID)
	{
		//addedKeys will contains the set of keys that are still not present inside messageTable, but that are inside realInbox
		ArrayList<Integer> addedKeys = new ArrayList<Integer>();
		System.out.println("Here Node "+node_.getID());
		for(int i=0;i<realInbox.size();i++)
		{
			MessageEx1 m = realInbox.get(i);
			System.out.println("messageTable.size+addedKeys.size="+(messageTable.size()+addedKeys.size()));
			//The actual node can add a message to inboxMessage (calling super.propagateMessage) if:
			//1. messageTable size + the set of keys that will be added to messageTable is less than maxsize
			//2. otherwise (and so if new entries in messageTable cannot be added) if the messageTable already contains the incoming message ID
			//	 (and so the messageTable size will not increase)
			if(	messageTable.size()+addedKeys.size()<=maxsize||messageTable.containsKey(m.getId())
			)
			{	
				//if the message ID is a new key...
				if(!addedKeys.contains(m.getId())&&!messageTable.containsKey(m.getId()))
				{
					System.out.println("Added "+m.getId()+" to addedKeys");
					addedKeys.add(m.getId());
				}
				System.out.println("Propagated message ID="+m.getId()+" isBR="+m.isBR());
				super.propagateMessage(m);
				realInbox.remove(i);
			}
			else
				System.out.println("Cannot add message ID="+m.getId()+" isBR="+m.isBR());
		}
		if(realInbox.size()!=0)
			System.out.println("Too many messages in realInbox! messageTable.size+addedKeys.size="+(messageTable.size()+addedKeys.size()));
		else
			System.out.println("realbox emptied!");
		super.nextCycle(node_, protocolID);
	}
	
}
