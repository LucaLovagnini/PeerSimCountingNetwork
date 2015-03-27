package exercise2;

import java.util.ArrayList;
import java.util.HashMap;

import exercise1.CDProtocolEx1;
import exercise1.LinkableEx1;
import exercise1.MessageEx1;
import peersim.chord.MessageEx2;
import peersim.core.Network;
import peersim.core.Node;

public class CDProtocolEx2 extends CDProtocolEx1{

	protected HashMap<Integer, Integer> messageVersion;
	protected ArrayList<Long> oldNeighbors;
	protected int version;
	
	/**
	 * Constructor
	 * @param prefix_
	 */
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
	 * @param getNew
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
	
	@Override
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
	
	public void checkEndOrSendBR(MessageEx1 m , Node node_,final int protocolID)
	{
		super.checkEndOrSendBR(m, node_, protocolID);
		if(netsize!=0)//if the search is ended...
		{
			version++;//update the message version
			netsize=0;
			startNewSearch(node_, protocolID);
		}
	}
	
	public void oldCheckEndOrSendBR(MessageEx1 m , Node node_,final int protocolID)
	{
		super.checkEndOrSendBR(m, node_, protocolID);
	}
	
	
	@Override
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
		if(debug)System.out.println("Qui nodo "+node_.getID());
		//compute the set of new neighbors
		ArrayList<Long> newNeighbors = getNewOrDeadNeighbors(node_, true);
		//compute the set of neighbors that do not exist anymore
		//notice that after this call oldNeighbors will be the set of dead neighbors (beacuase of side effects inside the method)
		getNewOrDeadNeighbors(node_, false);
		//for each neighbor n which doesn't exist anymore:
		for(Long old : oldNeighbors)
		{
			if(debug)System.out.print("Operazioni eseguite sul vicino morto "+old+":");
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
			if(debug)System.out.println("Fine operazioni sul vicino morto "+old);
		}
		
		//for each new neighbor
		for(Long newNeighborID : newNeighbors)
		{
			if(debug)System.out.println("NODE "+node_.getID()+", this is my messageTable:");
			for(Integer i : messageTable.keySet())
			{
				if(debug)System.out.print("Message ID:"+i+" waiting for nodes: ");
				for(Node n : messageTable.get(i))
				{
					if(debug)System.out.print(n.getID()+" ");
				}
				if(debug)System.out.println("");
					
			}
			if(debug)System.out.print("Operazioni eseguite sul nuovo vicino "+newNeighborID+", inoltrati i messaggi:");
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
		
		//now the actual node has to check if it has received some already seen message BUT with a newer version. In that case:
		if(debug)System.out.println("Operazioni su messaggi già visti ma più nuovi: ");
		for(MessageEx1 m : inboxMessages)
		{
			if(m!=null && m instanceof MessageEx2)
			{
				MessageEx2 m2 = (MessageEx2) m;
				if(messageTable.containsKey(m2.getId()))//already seen message
				{
					if(debug)System.out.println("NODE "+node_.getID()+", this is my messageTable:");
					for(Integer i : messageTable.keySet())
					{
						if(debug)System.out.print("Message ID:"+i+" waiting for nodes: ");
						for(Node n : messageTable.get(i))
						{
							if(debug)System.out.print(n.getID()+" ");
						}
						if(debug)System.out.println("");
							
					}
					if(messageVersion.get(m2.getId())<m2.getVersion())//NEWER VERSION!
					{
						if(debug)System.out.println("Trovato messaggio di versione nuova: id="+m2.getId()+" creator="+m2.getCreator().getID()+" version="+m2.getVersion());
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
									if(debug)System.out.println("Ho rimosso il messaggio di BR id="+m1.getId()+" creator="+m1.getCreator().getID());
									m1.toRemove();
								}
								//2.2 otherwise if m1 is an older version send a fake BR (otherwise the node which sent m1 would wait forever)
								else if(m1 instanceof MessageEx2 && ((MessageEx2)m1).getVersion()<m2.getVersion())
								{
									if(m1.getCreator()!=null)
										sendBRMessage(node_, new MessageEx1(node_, m1.getId()), m1.getCreator(), protocolID , false);
									if(debug)System.out.println("Ho inviato un fake BR a "+m1.getCreator().getID());
									m1.toRemove();
								}
								
							}
							
						}
						if(debug)System.out.print("Invio del messaggio aggiornato ai miei vicini, inviato a :");
						ArrayList<Node> nodeList = new ArrayList<Node>();
						//3. send a new message to all the actual node's neighbor (except for the newNeighbors, see below)
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
						if(debug)System.out.println("");
						//this if is true when all the neighbors have sent me the same message in the same cycle
						if(nodeList.size()==0)
						{
							if(debug)System.out.print(" BUT all my neighbors have sent me the same message in the same cycle, so I send back a BR message to ");
							for(int i=0 ; i<linkableProtocol.degree();i++)
							{
								if(debug)System.out.print(linkableProtocol.getNeighboar().get(i).getID());
								//to the first one the actual node sends a normal BR message with size 1
								if(i==0)
									sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,true);
								else//to the others a "fake" BR message with size 0
									sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,false);
							}
						}
						else
						{
							messageTable.put(m.getId(), nodeList);
							messageSender.put(m.getId(), m.getCreator());
						}
					m.toRemove();
					}
				}
				//4.if m is an old version...
				if(messageVersion.containsKey(m2.getId())&&m2.getVersion()<messageVersion.get(m2.getId()))
					if(!m2.isBR())//...of a normal message, then send a fake BR
						sendBRMessage(node_, new MessageEx1(node_, m2.getId()), m2.getCreator(),protocolID, false);
					else//otherwise (so a BR message) ignore it
						m2.toRemove();
				//if m is the actual version BUT it is not present inside messageTable, then the actual node has managed...
				//and so it send a fake BR message
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
		if(debug)System.out.print("Qui nodo "+node_.getID()+" gestione dei messaggi mai visti ");
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
				if(debug)System.out.print(", I have never seen this message with ID "+m.getId()+" from "+m.getCreator().getID());
				//it will contains all the neighbors which the node will send the message + the one which has sent the message to the actual node
				ArrayList<Node> nodeList = new ArrayList<Node>();
				if(linkableProtocol.degree()==1)//if the actual node only neighbor is the one who sent it the new message...
				{
					if(debug)System.out.println(" BUT I have no other neighbor, so I send a BR message");
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
							if(debug)System.out.print(linkableProtocol.getNeighboar().get(i).getID());
							//to the first one the actual node sends a normal BR message with size 1
							if(i==0)
								sendBRMessage(node_,m,linkableProtocol.getNeighboar().get(i),protocolID,true);
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
		for(MessageEx1 m : inboxMessages)
		{
			if(debug)System.out.println("Message from "+m.getCreator().getID()+" with ID "+m.getId()+" isBr="+m.isBR());
		}
		if(debug)System.out.print("Ci sono ancora non BR messaggi di tipo 1:");
		boolean exist = false;
		for(MessageEx1 m : inboxMessages)
		{
			if(!m.isBR()&&(m instanceof MessageEx1))
			{
				exist=true;
				break;
			}
		}
		if(debug)System.out.println(exist);
		super.nextCycle(node_, protocolID);
		//as last thing, update the oldNeighbors list
		oldNeighbors.clear();
		for(int i=0;i<linkableProtocol.degree();i++)
		{
			oldNeighbors.add(new Long(linkableProtocol.getNeighbor(i).getID()));
		}
		
		if(debug)System.out.println("");
		if(debug)System.out.println("");

	}

}
