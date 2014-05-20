package jumper.sma.agent;

import java.util.logging.Logger;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class JMPInterrogate extends Agent {
	
	public final static AID identification = new AID("JMPModeler", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPInterrogate");
	
	@Override
	protected void setup(){
		super.setup();
		
		addBehaviour(new RequestHandler());
	}
	
	private class RequestHandler extends Behaviour{

		@Override
		public void action() {
			logger.info(myAgent.getAID().getLocalName() + " received the request from controller.");
			
			//request received from JMPController
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				ACLMessage reply = msg.createReply();
				reply.setPerformative(ACLMessage.INFORM);
				reply.setContent(msg.getContent() + " - After Execution");
				myAgent.send(reply);
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
