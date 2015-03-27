package exercise2;

import java.util.ArrayList;
import java.util.HashMap;

import exercise1.CDProtocolEx1;
import exercise1.LinkableEx1;
import exercise1.MessageEx1;
import peersim.core.Network;
import peersim.core.Node;

public class CDProtocolEx2 extends CDProtocolEx1{

	//this table contains a list of pairs <MessageIdentifier, Version>
	//If the value of the key i is v, then this is AT LEAST the i-th network estimation
	//computed by the node with identifier i. This is necessary since a node could receive
	//an already seen message BUT with a newer version (and so it cannot sends a fake BR back).
	//This case is possible with the nodes remotion from the network
	private HashMap<Integer, Integer> messageVersion;
	//used to computes the set of nodes added or removed from the last simulation cycle
	//it contains the set of actual node's neighbors at the end of the previous simulation cycle
	private ArrayList<Long> oldNeighbors;
	//it is initialized to 0, and it counts the number of network estimation processes started
	//by the actual node
	private int version;
	
	public CDProtocolEx2(String prefix_) {
		super(prefix_);
		messageVersion = new HashMap<Integer, Integer>();
		oldNeighbors = new ArrayList<Long>();
		version = 0;
		// TODO Auto-generated constructor stub
	}
	
	public Object clone()
	{
		CDProtocolEx2 ip = null;
		ip = (CDProtocolEx2) super.clone();
		ip.messageVersion = new HashMap<Integer, Integer>();
		ip.oldNeighbors = new ArrayList<Long>();
		ip.version = 0;
		return ip;
	}

	/**
	 * This method computes:
	 * 1. The set of new neighbors of node if getNew=true
	 * 2. The set of dead neighbors otherwise
	 * Notice that in the second case there is a (wanted) side-effect on oldNeighbors 
	 * @param node
	 * @param getNew if true then it computes the set of new neighbors, the dead set of neighbors otherwise
	 * @return the set computed
	 */
	public ArrayList<Long> getNewOrDeadNeighbors (Node node,boolean getNew)
	{
		LinkableEx1 linkableProtocol = ((LinkableEx1)node.getProtocol(_linkableProtocolId));
		ArrayList<Long> neighborsID = new ArrayList<Long>();
		
		for(Node n : linkableProtocol.getNeighboar())
			neighborsID.add(new Long(n.getID()));
		if(getNew)
		{
			neighborsID.removeAll(oldNeighbors);
			return neighborsID;
		}
		else
		{
			oldNeighbors.removeAll(neighborsID);
			return oldNeighbors;
		}
	}
	
	/**
	 * This method override the one defined in CDProtocolEx1, since the actual node has to initialize oldNeighbors (as the set
	 * of node's neighbor) and update messageVersion (version is increased at the end of nextCycle)
	 */
	public void startNewSearch(Node node_, int protocolID) {
		LinkableEx1 linkableProtocol = ((LinkableEx1)node_.getProtocol(_linkableProtocolId));
		ArrayList<Node> nodeList = new ArrayList<Node>();//it will contains the actual node+all its neighbors
		for(int i = 0 ; i < linkableProtocol.degree() ; i++)//for each neighbor
		{
			MessageEx2 neighborMess = new MessageEx2(node_,(int)node_.getID(),version);
			Node neighbor = linkableProtocol.getNeighbor(i);
			oldNeighbors.add(new Long(neighbor.getID()));
			CDProtocolEx2 neighborCdProtocol = ((CDProtocolEx2)neighbor.getProtocol(protocolID));
			neighborCdProtocol.propagateMessage(neighborMess);
			nodeList.add(neighbor);
		}
		messageTable.put((int)node_.getID(), nodeList);
		messageVersion.put((int)node_.getID(), version); 
	}
	
	/**
	 * We need to override this method for a (really) speacial case: we cannot delete a message (calling toRemove)
	 * if it has a newer version the one contained in messageVersion. This special case could happen when a neighbor is removed,
	 * so the actual node checks if it can send a BR message after removing the neighbor, and if it so, it cannot remove the
	 * received message of a newer version!
	 */
	public int computeSizeRemoveMessages(MessageEx1 m)
	{
		int acc=0;
		for(MessageEx1 m1 : inboxMessages)
			if(	m1 instanceof MessageEx2 &&
				messageVersion.containsKey(m1.getId()) &&
				((MessageEx2)m1).getVersion()>messageVersion.get(m1.getId())
			)
			{}
			else if(m1.getId()==m.getId())//now that we can compute the brMessage size, we can remove all the BR messages
				{
					m1.toRemove();
					acc+=m1.size();
				}
		return acc;
	}
	
	/**
	 * Since the network is dynamic, a lot of new cases have to be considered in each simulation cycle of each node.
	 * As first thing, the actual node has to compute the set of new neighbors and dead neighbors from the last cycle.
	 * For each dead neighbor n:
	 * 1. Delete each message inside inboxMessages sent by n
	 * 2. Delete each message with ID n (since the network estimation of this node is interrupted)
	 * 3.1 Update each nodeList that contains n (removing it) for each entry in messageTable 
	 * 3.2 Since nodeList has been modified, the actual node has to check if now it can send a BR message
	 * 
	 * For each new neighbor n and for each entry <MessageIdentifier,nodeList> in messageTable, the actual node node has:
	 * 1. Add n to nodeList
	 * 2. Create a MessageEx2 object with ID=MessageIdentifier (and the latest version) and send it to n
	 * 
	 * For each message already seen by the actual node, but with a NEWER version, the actual node has:
	 * 1. update messageVersion
	 * 2. for each message m1 in inboxMessage with the same ID of m:
	 * 2.1 if m1 is a BR then delete it
	 * 2.2 otherwise if m1 is an older version send a fake BR (otherwise the node which sent m1 would wait forever)
	 * 3. send a new message to all the actual node's neighbor
	 * 
	 * For each message with an older version OR present in messageVersion but not in message table, 
	 * the actual node sends a fake BR (it can happens adding new nodes)
	 * 
	 * nextCycle has to manage each never seen message too, since the previous protocol doesn't manage message versions
	 * 
	 * Then, at this point inside inboxMessages are present only BR messages OR already seen messages, which can be managed by super.nextCycle
	 * 
	 * Finally, if netsize!=0 then the search is finished and the actual node can start a new one (after updating the version field)
	 */
	public void nextCycle(final Node node_, final int protocolID)
	{
		LinkableEx1 linkableProtocol = ((LinkableEx1)node_.getProtocol(_linkableProtocolId));
		//since oldNeighbors is initialized by startNewSearch, and not all nodes execute this method, the actual node need to initialize it manually
		if(oldNeighbors.size()==0)
			for(int i = 0 ; i < linkableProtocol.degree() ; i++)//for each neighbor
			{
				Node neighbor = linkableProtocol.getNeighbor(i);
				oldNeighbors.add(new Long(neighbor.getID()));
			}
		if(debug)System.out.println("CDProtocolEX2Node "+node_.getID()+", this is my messageTable:");
		for(Integer i : messageTable.keySet())
		{
			if(debug)System.out.print("Message ID:"+i+" waiting for nodes: ");
			for(Node n : messageTable.get(i))
			{
				if(debug)System.out.print(n.getID()+" ");
			}
			if(debug)System.out.println("");
				
		}
		if(debug)System.out.println("inboxMessages:");
		for(MessageEx1 m : inboxMessages)
		{
			if(debug)System.out.println("ID="+m.getId()+" Creator="+m.getCreator().getID()+" isBR="+m.isBR());
		}
		//compute the set of new neighbors
		ArrayList<Long> newNeighbors = getNewOrDeadNeighbors(node_, true);
		//compute the set of neighbors that do not exist anymore
		//notice that after this call oldNeighbors will be the set of dead neighbors (beacuase of side effects inside the method)
		getNewOrDeadNeighbors(node_, false);
		//for each neighbor n which doesn't exist anymore:
		for(Long old : oldNeighbors)
		{
			if(debug)System.out.println("Operations made on the dead neighbor "+old+":");
			for(MessageEx1 m : inboxMessages)
				//	1. for each message from n in inboxMessage, delete it
				if(m.getCreator()!=null&&m.getCreator().getID()==old)
				{
					if(debug)System.out.print(" eliminato il messaggio ID="+m.getId()+" inviato da "+m.getCreator().getID()+" ");
					m.toRemove();
				}
				//	2. delete each BR message in inboxMessage for n
				else if(m.getId()==old&&m.isBR())
				{
					if(m.getCreator()!=null)
						if(debug)System.out.print(" eliminato il messaggio di BR destinato a "+old+" inviato da "+m.getCreator().getID()+" ");
					m.toRemove();
				}
			if(debug)System.out.println("");
			removeMessages();
			//since we cannot use a foreach on the set of key (since checkEndOrSendBR could modify this set and so throws an exception)
			ArrayList<Integer> keysModified = new ArrayList<Integer>();
			//	3.1 for each entry in messageTable...
			for(Integer key : messageTable.keySet())
				{
					ArrayList<Node> nodeList = messageTable.get(key);
					//...where n is present inside nodeList...
					for(int i=0;i<nodeList.size();i++)
					{
						if(nodeList.get(i).getID()==old)
						{
							if(debug)System.out.print(" eliminato "+old+" dalla entry con chiave "+key+" ");
							//...delete it
							nodeList.remove(i);
							if(debug)System.out.print(" controllo se posso inviare BR: ");
							keysModified.add(key);
						}
					}
				}
			for(int i=keysModified.size()-1;i>=0;i--)
				checkEndOrSendBR(new MessageEx1(node_, keysModified.get(i)), node_, protocolID);
			if(debug)System.out.println("");
			if(debug)System.out.println("End of the operations made on the dead neighbor "+old);
		}
		
		if(debug)System.out.println("CDProtocolEX2Node "+node_.getID()+", this is my messageTable:");
		for(Integer i : messageTable.keySet())
		{
			if(debug)System.out.print("Message ID:"+i+" waiting for nodes: ");
			for(Node n : messageTable.get(i))
			{
				if(debug)System.out.print(n.getID()+" ");
			}
			if(debug)System.out.println("");
				
		}
		
		if(debug)System.out.println("inboxMessages:");
		for(MessageEx1 m : inboxMessages)
		{
			if(m.getCreator()!=null)
			{	if(debug)System.out.println("ID="+m.getId()+" Creator="+m.getCreator().getID()+" isBR="+m.isBR());}
			else
			{	if(debug)System.out.println("ID="+m.getId()+" Creator="+null+" isBR="+m.isBR());}
		}
		
		//for each new neighbor
		for(Long newNeighborID : newNeighbors)
		{
			if(debug)System.out.print("Forwarded messages to the neigh neighbor "+newNeighborID+":");
			Node newNeighbor=null;
			CDProtocolEx2 neighborCdProtocol=null;
			//find the new neighbor inside the network
			for(int i=0 ; i<Network.size();i++)
				if(newNeighborID==Network.get(i).getID())
				{
					newNeighbor=Network.get(i);
					break;
				}
			if(newNeighbor!=null)
				neighborCdProtocol = ((CDProtocolEx2)newNeighbor.getProtocol(protocolID));
			else
				System.out.println("HELP");
			//for each message inside the message table
			for(Integer key : messageTable.keySet())
			{
				//forward the message to the new neighbor with the latest version
				MessageEx2 newNeighMess = new MessageEx2(node_,key,messageVersion.get(key));
				neighborCdProtocol.propagateMessage(newNeighMess);
				if(debug)System.out.print(" ID="+key+" version="+messageVersion.get(key));
				//modify the list of nodes from which the actual node is waiting an answer
				messageTable.get(key).add(newNeighbor);
			}
			if(debug)System.out.println("");
			
		}
		removeMessages();
		//now the actual node has to check if it has received some already seen message BUT with a newer version. In that case:
		if(debug)System.out.println("Operations made on newer messages: ");
		for(MessageEx1 m : inboxMessages)
		{
			if(m!=null && m instanceof MessageEx2)
			{
				MessageEx2 m2 = (MessageEx2) m;
				if(messageTable.containsKey(m2.getId()))//already seen message
				{
					if(messageVersion.get(m2.getId())<m2.getVersion())//NEWER VERSION!
					{
						if(debug)System.out.println("Received message id="+m2.getId()+" creator="+m2.getCreator().getID()+" version="+m2.getVersion()+" messageVersion("+m2.getId()+")="+messageVersion.get(m2.getId()));
						//1. It must update the relative messageVersion entry
						messageVersion.put(m2.getId(), m2.getVersion());//notice that is a value is already present in an HashMap it will be replaced
						for(MessageEx1 m1 : inboxMessages)
						{
							//2. for each message m1 in inboxMessage with the same ID of m:
							if(m1.getId()==m2.getId())
							{
								//2.1 if m1 is a BR then delete it
								if(m1.isBR())
								{
									if(debug)System.out.println("Removed BR message with ID "+m1.getId()+" sent by "+m1.getCreator().getID());
									m1.toRemove();
								}
								//2.2 otherwise if m1 is an older version send a fake BR (otherwise the node which sent m1 would wait forever)
								else if(m1 instanceof MessageEx2 && ((MessageEx2)m1).getVersion()<m2.getVersion())
								{
									if(m1.getCreator()!=null)
										sendBRMessage(node_, new MessageEx1(node_, m1.getId()), m1.getCreator(), protocolID , false);
									if(debug)System.out.println("Fake BR sent to "+m1.getCreator().getID());
									m1.toRemove();
								}
								
							}
							
						}
						if(debug)System.out.print("Sending updated message to: ");
						ArrayList<Node> nodeList = new ArrayList<Node>();
						//3. send a new message to all the actual node's neighbor
						for(int i = 0 ; i < linkableProtocol.degree() ; i++)
						{
							
							MessageEx2 neighborMess = new MessageEx2(node_,m.getId(),m2.getVersion());
							Node neighbor = linkableProtocol.getNeighbor(i);
							boolean okSend=true;
							//if false it means that the actual node has received the same message from the i-th neighbor in the same cycle 
							//or the neighbor is the message sender
							for(MessageEx1 m1 : inboxMessages)
							{
								if(m1.getCreator()==null)
									continue;
								if((m1.getId()==m.getId()&&m1.getCreator().getID()==neighbor.getID())||neighbor.getID()==m.getCreator().getID())
								{
									okSend=false;
									break;
								}
							}
							if(okSend)
							{
								if(debug)System.out.print(neighbor.getID()+" ");
								CDProtocolEx2 neighborCdProtocol = ((CDProtocolEx2)neighbor.getProtocol(protocolID));
								neighborCdProtocol.propagateMessage(neighborMess);
								nodeList.add(neighbor);
							}
						}
						//this if is true when all the neighbors have sent me the same message in the same cycle
						if(nodeList.size()==0)
						{
							if(debug)System.out.print(" BUT all my neighbors have sent me the same message in the same cycle, so I send back a BR message to ");
							for(int i=0 ; i<linkableProtocol.degree();i++)
							{
								if(debug)System.out.print(linkableProtocol.getNeighboar().get(i).getID());
								//to the first one the actual node sends a normal BR message with size 1
								if(i==0)
								{
									if(debug)System.out.print("(to him with size 1)");
									sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,true);
								}
								else//to the others a "fake" BR message with size 0
									sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,false);
							}
						}
						else
						{
							messageTable.put(m.getId(), nodeList);
							messageSender.put(m.getId(), m.getCreator());
						}
					if(debug)System.out.println("");
					m.toRemove();
					}
				}
				//4.if m is an old version...
				if(messageVersion.containsKey(m2.getId())&&m2.getVersion()<messageVersion.get(m2.getId()))
					if(!m2.isBR())//...of a normal message, then send a fake BR
						sendBRMessage(node_, new MessageEx1(node_, m2.getId()), m2.getCreator(),protocolID, false);
					else//otherwise (so a BR message) ignore it
						m2.toRemove();
				//if m is the actual version BUT it is not present inside messageTable, then the actual node has already seen this message...
				//and so it send a fake BR message (it can happen adding new nodes)
				if(messageVersion.containsKey(m2.getId())&&m2.getVersion()==messageVersion.get(m2.getId())&&!messageTable.containsKey(m2.getId()))
				{
					if(debug)System.out.print (" recieved an actual message from "+m2.getCreator().getID());
					sendBRMessage(node_, new MessageEx1(node_, m2.getId()), m2.getCreator(),protocolID, false);
				}
				//if m is a message with the actual node's version and it was already seen, then it will be managed by super.nextCycle
				//which will send a fake BR
			}
		}
		removeMessages();
		if(debug)System.out.println("Operation mad on never seen messages: ");
		//Unfortunately, the actual node has to manage the case when the message was never seen before
		//because the old method didn't manage message version, and so the actual node has to forward message WITH message version
		for(MessageEx1 m : inboxMessages)
		{
			Node creator = m.getCreator();
			if(creator==null)
				continue;
			if(!messageTable.containsKey(m.getId())&&(!messageVersion.containsKey(m.getId())||messageVersion.get(m.getId())<((MessageEx2)m).getVersion()))
			{
				messageVersion.put(m.getId(), ((MessageEx2)m).getVersion());
				if(debug)System.out.print("ID="+m.getId()+" Creator="+m.getCreator().getID()+" isBR="+m.isBR()+", I have never seen this message");
				//it will contains all the neighbors which the node will send the message + the one which has sent the message to the actual node
				ArrayList<Node> nodeList = new ArrayList<Node>();
				if(linkableProtocol.degree()==1)//if the actual node only neighbor is the one who sent it the new message...
				{
					if(debug)System.out.println(" BUT I have no other neighbor to send, so I send a BR message");
					sendBRMessage(node_ , m , creator ,protocolID,true);//...then it sends back a BR message of size 1
				}
				else
				{
					if(debug)System.out.print(" and I propagated the message to ");
					for(int i = 0 ; i < linkableProtocol.degree() ; i++)
					{
						MessageEx2 neighborMess = new MessageEx2(node_,m.getId(),((MessageEx2) m).getVersion());
						Node neighbor = linkableProtocol.getNeighbor(i);
						boolean okSend=true;
						//if false it means that the actual node has received the same message from the i-th neighbor in the same cycle 
						//or the neighbor is the message sender
						for(MessageEx1 m1 : inboxMessages)
						{
							if((m1.getId()==m.getId()&&m1.getCreator().getID()==neighbor.getID())||neighbor.getID()==m.getCreator().getID())
							{
								okSend=false;
								break;
							}
						}
						if(okSend)
						{
							if(debug)System.out.print(neighbor.getID()+" ");
							CDProtocolEx2 neighborCdProtocol = ((CDProtocolEx2)neighbor.getProtocol(protocolID));
							neighborCdProtocol.propagateMessage(neighborMess);
							nodeList.add(neighbor);
						}
					}
					//this "if" is true when all the neighbors have sent me the same message in the same cycle
					if(nodeList.size()==0)//then the only node in nodeList is the one which has sent me the message
					{
						if(debug)System.out.print(" BUT all my neighbors have sent me the same message in the same cycle, so I send back a BR message to ");
						for(int i=0 ; i<linkableProtocol.degree();i++)
						{
							if(debug)System.out.print(linkableProtocol.getNeighboar().get(i).getID()+" ");
							//to the first one the actual node sends a normal BR message with size 1
							if(i==0)
							{
								if(debug)System.out.print("(to him with size 1) ");
								sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,true);
							}
							else//to the others a "fake" BR message with size 0
								sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,false);
						}
					}
					else
						{
							messageTable.put(m.getId(), nodeList);
							messageSender.put(m.getId(), m.getCreator());
						}
					if(debug)System.out.println("");
				}
				m.toRemove();
			}
			//If a message is not present in the message table BUT it has the same version of messageVersion, then it has to be removed
			//because otherwise super.nextCycle would treat it as a new message (it doesn't look at the version) and forward to the neighbors
			else if(!messageTable.containsKey(m.getId())&&
					messageVersion.containsKey(m.getId())&&
					m instanceof MessageEx2 && 
					((MessageEx2)m).getVersion()==messageVersion.get(m.getId()))
				{
					m.toRemove();
				}
		}
		if(debug)System.out.println("");
		removeMessages();
		if(debug)System.out.println("Operations made on (left) already seen messages and BR messages:");
		super.nextCycle(node_, protocolID);
		//the actual node has to check if the search is finished, and if so start a new one with a new version
		if(netsize!=0)
		{
			version++;
			if(debug)System.out.println("Starting a new search with version "+version);
			startNewSearch(node_, protocolID);
			netsize=0;
		}
		//as last thing, update the oldNeighbors list
		oldNeighbors.clear();
		for(int i=0;i<linkableProtocol.degree();i++)
			oldNeighbors.add(new Long(linkableProtocol.getNeighbor(i).getID()));
		if(debug)System.out.println("");
		if(debug)System.out.println("");

	}

}
