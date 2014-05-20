package jumper.sma.agent;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;

public class JumperGatewayBehaviour extends Behaviour{
	//add conversation id with millisec
	private AID receiver;
	private String message;
	
	public JumperGatewayBehaviour(AID receiver, String message){
		this.receiver = receiver;
		this.message = message;
	}
	
	@Override
	public void action() {
		//step = 1
		//Send message to client agent
		ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
		msg.addReceiver(new AID("JMPClient-1", AID.ISLOCALNAME));
		
	}

	@Override
	public boolean done() {
		return false;
	}
	
	public boolean getResult() {
		return false;
	}

}
