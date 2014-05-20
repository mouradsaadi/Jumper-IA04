package jumper.sma.agent;

import java.util.logging.Logger;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class JMPClientAgent extends Agent {
	
	private int agent_id = -1;
	private ParallelBehaviour pb = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL); //reserved for handling multi-client message
	public static Logger logger = Logger.getLogger("Agents.JMPClientAgent");

	@Override
	protected void setup(){
		super.setup();
		ACLMessage msg = new ACLMessage(ACLMessage.SUBSCRIBE);
		msg.addReceiver(JMPController.identification);
		send(msg);
		
		pb.addSubBehaviour(new MessageHandler());
		
		addBehaviour(new ReceiveAgentID());
		
		logger.info(getAID().getLocalName() + "is ready.");
	}
	
	private class ReceiveAgentID extends Behaviour{
		
		private boolean isInitialised = false;
		
		@Override
		public void action() {
			//receive the agent id from controller
			
			MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchSender(JMPController.identification), 
					MessageTemplate.MatchPerformative(ACLMessage.INFORM));
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				agent_id = Integer.parseInt(msg.getContent());
				isInitialised = true;
				
				logger.info("Agent " + agent_id + " is initialised with ID = " + agent_id);			
			} else{
				block();
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return isInitialised;
		}
		
		@Override
		public int onEnd(){
			myAgent.addBehaviour(pb);
			return super.onEnd();
		}
		
	}
	
	private class MessageHandler extends Behaviour{

		@Override
		public void action() {
			
			//receive request message from the servlet or the propagate message from the controller.
			
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
												MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE));

			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){				
				if(msg.getPerformative() == ACLMessage.REQUEST){
					System.out.println("Client Agent " + agent_id + " received request.");
					
					ACLMessage toController = new ACLMessage(ACLMessage.PROPAGATE);
					toController.setContent(msg.getContent());
					toController.setConversationId(String.format("%d", agent_id));
					toController.addReceiver(JMPController.identification);
					myAgent.send(toController);
				} else{
					System.out.println("Client from : "+msg.getSender()+" content : "+msg.getContent());
					logger.info("Client Agent " + agent_id + " transfer the message '" + msg.getContent() + "' to servlet.");
					ACLMessage toServlet = new ACLMessage(ACLMessage.INFORM);
					toServlet.addReceiver(new AID("ControlContainer-1", AID.ISLOCALNAME));
					toServlet.setContent(msg.getContent());
					myAgent.send(toServlet);
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
