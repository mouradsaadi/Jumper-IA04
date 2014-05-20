package jumper.sma.agent;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.jdt.internal.compiler.ast.ThisReference;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class JMPController extends Agent {
	
	public final static AID identification = new AID("JMPController", AID.ISLOCALNAME);
	private ArrayList<AID> client_agents = new ArrayList<AID>(10);
	private static Logger logger = Logger.getLogger("Agents.JMPController");
	private ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
	
	@Override
	protected void setup(){
		super.setup();
		logger.info(getAID().getLocalName() + " is ready.");
		
		pb.addSubBehaviour(new MessageHandler());
		addBehaviour(new ReceiveSubscription());
	}
	
	private class ReceiveSubscription extends Behaviour{
		
		private int count = 0;
		
		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				System.out.println("message on controller agent : "+msg.getContent());
				client_agents.add(msg.getSender());
				
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(String.format("%d", count + 1));
				myAgent.send(reply);
				
				logger.info(myAgent.getAID().getLocalName() + " reply to agent with ID = " + (count + 1));
				
				count++;
			} else{
				block();
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return count == 10;
		}
		
		@Override
		public int onEnd(){
			addBehaviour(pb);
			return super.onEnd();
		}
	}
	
	private class MessageHandler extends Behaviour{

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE), 
												MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				
				//Receive message from client agent.
				if(msg.getPerformative() == ACLMessage.PROPAGATE){
					logger.info(myAgent.getAID().getLocalName() + " received message from client agent whose ID = " + msg.getConversationId());
					
					ACLMessage toDb = new ACLMessage(ACLMessage.REQUEST);
					toDb.addReceiver(JMPModeler.identification);
					
					//New conversation ID for the communication with the SPARQL executer. 
					toDb.setConversationId(msg.getConversationId());
					toDb.setContent(msg.getContent());
					myAgent.send(toDb);
					
					logger.info(myAgent.getAID().getLocalName() + " propagate the message to the SPARQL executer.");
				} else{
					
					//Receive message from SPARQL executer.
					logger.info(myAgent.getAID().getLocalName() + " received message from SPARQL executer.");
					
					int index = Integer.parseInt(msg.getConversationId()) - 1;
					ACLMessage toClient = new ACLMessage(ACLMessage.PROPAGATE);
					toClient.addReceiver(client_agents.get(index));
					toClient.setConversationId(msg.getConversationId());
					toClient.setContent(msg.getContent());
					myAgent.send(toClient);
					
					logger.info(myAgent.getAID().getLocalName() + " turn back the result to client agent whose ID = " + (index + 1));
				}
			} else{
				block();
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
