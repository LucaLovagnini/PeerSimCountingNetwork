package exercise1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.Node;

public class CDProtocolEx1 implements CDProtocol
{
	protected int _linkableProtocolId;	
	protected HashMap<Integer,ArrayList<Node>> messageTable;
	protected HashMap<Integer, Node> messageSender;
	protected List<MessageEx1> inboxMessages;
	protected int netsize,lastnetsize;
	protected final boolean debug;
	
	public CDProtocolEx1(final String prefix_)
	{
		_linkableProtocolId = Configuration.getPid(prefix_ + ".linkable");
		//This table shows which messages this nodes has forwarded
		//It contains the pair <MessageIdentifier,NodesList>
		//MessageIdentifier is the node's ID that started the research
		//NodeList is the list of neighbors to which the actual node sent the message. Notice that this set of nodes is
		//not necessary the whole neighbor set (for example two nodes can send the same message at the same time to the actual node)
		//When a node will receive all the needed answer for its neighbors, it will delete the relative table entry
		messageTable = new HashMap<Integer,ArrayList<Node>>();
		//This second table is used to store a reference to the node which sent a message to the actual node
		//It contains the pair <MessageIdentifier,Sender>
		//MessageIdentifier is the node's ID that started the research
		//Sender is the node which has sent the message to the actual node
		//This table is used in order to decide to which node the actual node has to send the BR message
		messageSender = new HashMap<Integer, Node>();
		//The actual network size estimation: it will be updated only when this nodes will receive all the
		//Backward Routing (BR) messages from its neighbors at the end of the research
		//In the static search, this value will be never reset, but in the dynamic search if netsize!=0 then it means that
		//the search is finished, and so that the node can start a new one (and reset this field to 0)
		netsize = 0;
		//The last estimated network size: in the static search this parameter can be considered useless, since netsize
		//and this parameter are equals. BUT in the dynamic version, while netsize will be reset to 0 at every new research, lastnetsize
		//will be equal to the last network size estimation, so if the simulation will stop during the actual node research, it will be printed
		//the last useful estimation (so lastnetsize)
		lastnetsize=0;
		//List of (normal and BR) messages that the actual node has received during the last simulation cycle +
		//partially list of messages received for a certain message ID. The actual node keeps them since when
		//it will receives all the BR messages (and so it can forward his one), it will need to compute the sum
		//of the size of all the BR messages+1 ('cause it counts itself too)
		inboxMessages = new ArrayList<MessageEx1>();
		//change config-exercise "debug" variable to true if you want a (really) verbose simulation, false otherwise
		debug=Configuration.getBoolean("debug");
	}
	/**
	 * Peer sim method used in order to create new nodes
	 */
	@Override
	public Object clone()
	{
		CDProtocolEx1 ip = null;
		try
		{
			ip = (CDProtocolEx1) super.clone();
		} catch (final CloneNotSupportedException e)
		{
			if(debug)System.out.println("HELP");
		}
		ip._linkableProtocolId = _linkableProtocolId;
		ip.messageTable = new HashMap<Integer,ArrayList<Node>>();
		ip.messageSender = new HashMap<Integer, Node>();
		ip.inboxMessages = new ArrayList<MessageEx1>();
		ip.netsize = 0;
		return ip;
	}

	/**
	 * This method is called by a node which wants to send a message m to the actual node
	 * @param m
	 */
	public void propagateMessage(MessageEx1 m)
	{
		inboxMessages.add(m);
	}
	/**
	 * Called only by EndSimulationController
	 * @return
	 */
	public int getNetSize()
	{
		return lastnetsize;
	}
	
	/**
	 * This method is called at the begin of the simulation in the static version. It simply creates a new message 
	 * with ID equal to the actual node's ID and send it to each one of its neighbors. Notice that for the dynamic 
	 * version this method will be override
	 * @param node_ the node which start the network size estimation
	 * @param protocolID
	 */
	public void startNewSearch(final Node node_ , final int protocolID)
	{
		LinkableEx1 linkableProtocol = ((LinkableEx1)node_.getProtocol(_linkableProtocolId));
		ArrayList<Node> nodeList = new ArrayList<Node>();//it will contains the actual node+all its neighbors
		for(int i = 0 ; i < linkableProtocol.degree() ; i++)//for each neighbor
		{
			MessageEx1 neighborMess = new MessageEx1(node_,(int)node_.getID());
			Node neighbor = linkableProtocol.getNeighbor(i);
			CDProtocolEx1 neighborCdProtocol = ((CDProtocolEx1)neighbor.getProtocol(protocolID));
			neighborCdProtocol.propagateMessage(neighborMess);
			nodeList.add(neighbor);
		}
		messageTable.put((int)node_.getID(), nodeList);
	}
	/**
	 * This method call is the last instruction executed during the nextCycle method (see below). The actual node needs 
	 * to call it only at the end of the method because in Java we cannot modify a list during a "for each" cycle
	 *  (it throws an exception) and so we can only "mark" each message that we want to delete
	 */
	public void removeMessages()
	{
		for(int i=inboxMessages.size()-1;i>=0;i--)
			if(inboxMessages.get(i).getCreator()==null)
				inboxMessages.remove(i);
	}
	/**
	 * This method check if the actual node has received all the message that it was waiting for and so:
	 * 1. Send a BR to the node which has sent the message to the actual node
	 * 2. OR end the research computing the network size estimation if the message ID has the same ID of the actual node
	 * Notice that either condition 1 or 2 could be not verified, since the node could still wait for some message
	 * @param m reference to the message which send
	 * @param node_
	 * @param protocolID
	 */
	public void checkEndOrSendBR(MessageEx1 m , Node node_,final int protocolID)
	{
		ArrayList<Node> nodeList = messageTable.get(m.getId());
		if(checkSendBR(nodeList, m))
		{
			if(m.getId()==node_.getID())
			{
				netsize=0;
				netsize+=computeSizeRemoveMessages(m);
				netsize++;
				if(debug)System.out.println(", and the search is finished with size "+netsize);
				lastnetsize=netsize;
				//...otherwise it means that it's still waiting for some BR message
			}
			else
			{
				if(debug)System.out.print(" the sender is "+messageSender.get(m.getId()).getID()+" ");
				sendBRMessage(node_, m, messageSender.get(m.getId()),protocolID, true);
				if(debug)System.out.println("");
			}
		}
	}
	
	/**
	 * This method creates a BR message. Notice that there are two kind  of BR message: if it is created as answer
	 * of an already seen message or for a "not already seen" (i.e. a new) message
	 * Notice that at the end of this method, if the BR message is not an answer to an already seen message,
	 * the actual node TRIES to remove the entry about the node that has started the research in its messageTable. 
	 * Look tryRemoveTableMessages for more details.
	 * @param creator the node which creates the BR message
	 * @param m used to identify the original node which has started the research
	 * @param originalNodeCdProtocol used to identify the BR message receiver
	 * @param notFake if false then this BR message is created because the node received a message that 
	 *					it has already seen, so it sends a BR message with size 0 (otherwise the node
	 *					will be counted twice) and the message will be NOT removed from the messageTable
	 */
	public void sendBRMessage(Node creator , MessageEx1 m , Node sender , int protocolID , boolean notFake)
	{
		CDProtocolEx1 senderCdProtocol = ((CDProtocolEx1)sender.getProtocol(protocolID));		
		MessageEx1 brMessage = new MessageEx1(creator,m.getId());
		brMessage.setBR();
		//now I have to choose what messages remove from inboxMessages 
		if(notFake)//if true then this is a normal BR message, so the node compute its estimation and remove all messages
		{
			int acc=computeSizeRemoveMessages(brMessage);
			brMessage.increaseSize(acc+1);
		}
		else//otherwise it removes only the "notFake" BR message
			m.toRemove();
		//...and send it to first node (the ones which sent me the original message)
		if(sender.isUp())
		{	
			if(debug)System.out.print(" sending BR message with size "+brMessage.size+" ");
			senderCdProtocol.propagateMessage(brMessage);
		}
		else
			if(debug)System.out.println(" BR from "+creator.getID()+" not sent beacause "+sender.getID()+" is down");
		//if this is a "notFake" BR message, then the relative entry of m in messageTable cannot be removed
		//because the actual node is waiting for some BR message
		if(notFake)
			tryRemoveTableMessages(m);
	}
	
	/**
	 * It is called when the actual node sends a (not fake) BR message. It check that there are no more messages inside
	 * inboxMessages about the message m. If it is true, then it removes the entry (m's ID,nodeList) in messageTable
	 * otherwise it does nothing (because other messages). We cannot remove this entry if there are other interested messages
	 * inside inboxMessages because otherwise the would be considered as new messages. An example is when a set of nodes send
	 * the same message to the actual node.
	 * @param m the message used as reference
	 */
	public void tryRemoveTableMessages(MessageEx1 m)
	{
		for(int i = inboxMessages.indexOf(m)+1; i < inboxMessages.size();i++)
		{
			if(inboxMessages.get(i).getId()==m.getId()&&inboxMessages.get(i).getCreator()!=null)
			{
				return;
			}
		}
		messageTable.remove(m.getId());
		messageSender.remove(m.getId());
	}
	
	/**
	 * This method is called if:
	 * 1. The search is finished and the actual node computes the network size
	 * 2. The actual node has to send a BR message (not as answer of an already seen message)
	 * It calls the toRemove method for each message m1 with the same ID of m
	 * It computes the sum of the size of all the messages
	 * @param m used as reference about what messages have to be removed
	 * @return the sum computed
	 */
	public int computeSizeRemoveMessages(MessageEx1 m)
	{
		int acc=0;
		for(MessageEx1 m1 : inboxMessages)
			if(m1.getId()==m.getId())//now that we can compute the brMessage size, we can remove all the BR messages
					{
						m1.toRemove();
						acc+=m1.size();
					}
		return acc;
	}
	
	/**
	 * This method is called every time that checkEndOrSendBR is called. 
	 * It checks if the actual node has received the BR messages from each node in nodeList
	 * @param nodeList the list of neighbors which have to send a BR message to the actual node
	 * @param m it is the BR message received when this method is called. It is used as reference in order
	 * to understand which message the actual node has to check. 
	 * @return true if all the nodes inside nodeList have sent their BR message, false otherwise 
	 */
	public boolean checkSendBR(ArrayList<Node> nodeList , MessageEx1 m)
	{
		for(Node n : nodeList)//so for each node that the actual node was waiting a BR message...
		{
				if(debug)System.out.print(" checkSendBR: searching "+n.getID());
				boolean found = false;
				for(MessageEx1 m1 : inboxMessages)//...I check if it is contained in the list of received message
				{
					//the method has to check that:
					//m1 is not null, because it could be null since it could have been passed as parameter of the toRemove method
					//the m1 creator is one of the actual node's neighbors in nodeList
					//m1 is a BR message of m
					//m1 is actually a BR message
					if(m1.getCreator()!=null&&n.getID()==m1.getCreator().getID()&&m1.getId()==m.getId()&&m1.isBR())
						{
							if(debug)System.out.print(" found!");
							found=true;
							break;
						}
				}
				if(!found)
				{
					if(debug)System.out.println(" but I'm still waiting for (at least) "+n.getID());
					return false;
				}
		}
		return true;
	}
	
	/**
	 * This is the most important (and complicated) method of the entire CD procol. It is an ovveride of the PeerSim's method,
	 * and it is called at every simulation cycle. As first thing it checks that it has at least one neighbor, otherwise the 
	 * search is obvious and it ends immediately. Otherwise, for each message m received in inboxMessages (or that was already inside it)
	 * this operations are executed:
	 * 1. if m was never seen before, then the actual node forward the message to all its neighbors (which have not sent the same message)
	 * Special case: if the actual node has only one neighbor, then it is the one that have sent the message, so
	 * the actual node send a BR message with size 1 immediately to its only neighbor
	 * 2. if m is a BR message, then the actual nodes check if it can send its BR message to the node which has sent the message to it
	 * Special case: the BR message has the same ID of the actual node, then if this is the last expected BR message the search is finished
	 * 3. otherwise this is a non-BR already seen message and so the actual node send a "fake BR message", i.e. a BR message with size 0 
	 * (0 because the actual node would have been counted twice)
	 * 4. all the "marked" messages are removed from inboxMessages
	 */
	
	@Override
	public void nextCycle(final Node node_, final int protocolID)
	{
		LinkableEx1 linkableProtocol = ((LinkableEx1)node_.getProtocol(_linkableProtocolId));
		if(linkableProtocol.degree()==0)//the actual node has no neighbors: the search is obvious and immediately ends
			{
				netsize=1;
				lastnetsize=netsize;
				if(debug)System.out.println("NODE "+node_.getID()+" has no neighbor, so its search is finished");
			}
		if(debug)System.out.println("CDProtocolEX1 Node "+node_.getID()+", this is my messageTable:");
		for(Integer i : messageTable.keySet())
		{
			if(debug)System.out.print("Message ID:"+i+" waiting for nodes: ");
			for(Node n : messageTable.get(i))
			{
				if(debug)System.out.print(n.getID()+" ");
			}
			if(debug)System.out.println("");
				
		}
		if(debug)System.out.println("NODE "+node_.getID()+", I received these messages:");
		//for each inbox message
		for (MessageEx1 m : inboxMessages)
		{	
			//m.creator is null if m is a BR message and the actual node has already sent its BR message
			if(m.getCreator()==null)
				continue;
			if(debug)System.out.print("ID="+m.getId()+" Creator="+m.getCreator().getID()+" isBR="+m.isBR());
			Node creator = m.getCreator();
			if(!messageTable.containsKey(m.getId()))//if the node has never seen this message
			{
				if(debug)System.out.print(", I have never seen this message with ID "+m.getId());
				//it will contains all the neighbors which the node will send the message
				ArrayList<Node> nodeList = new ArrayList<Node>();
				if(linkableProtocol.degree()==1)//if the actual node only neighbor is the one who sent it the new message...
				{
					if(debug)System.out.println(" BUT I have no other neighbor, so I send a BR message");
					sendBRMessage(node_,m,creator , protocolID ,true);//...then it sends back a BR message of size 1
				}
				else
				{
					if(debug)System.out.print(" and I propagated the message to ");
					for(int i = 0 ; i < linkableProtocol.degree() ; i++)
					{
						MessageEx1 neighborMess = new MessageEx1(node_,m.getId());
						Node neighbor = linkableProtocol.getNeighbor(i);
						boolean okSend=true;
						//if okSend=false it means that the actual node has received the same message from the i-th neighbor in the same cycle 
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
							CDProtocolEx1 neighborCdProtocol = ((CDProtocolEx1)neighbor.getProtocol(protocolID));
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
							if(debug)System.out.print(linkableProtocol._neighbors.get(i).getID());
							//to the first one the actual node sends a normal BR message with size 1
							if(i==0)
								{
									if(debug)System.out.println("(to him with size 1)");
									sendBRMessage(node_,m,linkableProtocol._neighbors.get(i),protocolID,true);
								}
							else//to the others a "fake" BR message with size 0
								sendBRMessage(node_,m,linkableProtocol._neighbors.get(i),protocolID,false);
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
			else if(m.isBR())//if true then m is a backward routing message
			{
				if(debug)System.out.print(", this is a BR message");
				checkEndOrSendBR(m, node_, protocolID);
			}
			else//the actual node has already seen this non-backward routing message: it sends back a "fake" BR message with size 0
			{
				if(debug)System.out.println(", I have already seen this message, sending a BR with size 0 to "+creator.getID());
					sendBRMessage(node_, m, creator , protocolID , false);
			}
		}
		removeMessages();//finally it can removes all the "marked" message from inboxMessages
		if(debug)System.out.println("messageTable at the end of the cycle:");
		for(Integer i : messageTable.keySet())
		{
			if(debug)System.out.print("ID="+i+" nodeList= ");
			for(Node n : messageTable.get(i))
			{
				if(debug)System.out.print(n.getID()+" ");
			}
			if(debug)System.out.println("");				
		}
		if(debug)System.out.println("inboxMessages at the end of the cycle:");
		for(MessageEx1 m : inboxMessages)
		{
			if(debug)System.out.println("ID="+m.getId()+" Creator="+m.getCreator().getID()+" isBR="+m.isBR());
		}
	}
}