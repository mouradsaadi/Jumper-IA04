package jumper.sma.agent;

import java.util.logging.Logger;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.util.JMPMessageParser;

public class JMPModeler extends Agent {
	
	public final static AID identification = new AID("JMPModeler", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPModeler");
	
	@Override
	protected void setup(){
		super.setup();
		
		addBehaviour(new RequestHandler());
	}
	
	private class RequestHandler extends Behaviour{

		@Override
		public void action() {
			
			//request received from JMPController
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROXY);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				logger.info(myAgent.getAID().getLocalName() + " received the request from controller.");
				ACLMessage dmsg = null;
				if((dmsg = disptachMessage(msg)) != null){
					myAgent.send(dmsg);
				}
			} else{
				block();
			}
		}
		
		private ACLMessage disptachMessage(ACLMessage messageReceived){
			RequestBean rb = JMPMessageParser.toObject(messageReceived.getContent());
			
			ACLMessage dmsg = new ACLMessage(ACLMessage.PROPAGATE);
			dmsg.setContent(messageReceived.getContent());
			dmsg.setConversationId(messageReceived.getConversationId());
			
			if(rb.getOperation() == 0){
				dmsg.addReceiver(JMPInterrogate.identification);
			} else if(rb.getOperation() == 1){
				dmsg.addReceiver(JMPBCAdmin.identification);
			} else{
				logger.warning(myAgent.getAID().getLocalName() + " can not find corresponding action to this message.");
				dmsg = null;
			}
			
			return dmsg;
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
