package jumper.sma.agent;

import java.util.Iterator;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.wrapper.gateway.GatewayAgent;


public class JMPGatewayAgent extends GatewayAgent {
	
	/**
	 * 
	 */
//	private static final long serialVersionUID = 2667916985869508011L;
//	private JMPBean bean = null;
//	
//	@Override
//	protected void setup(){
//		super.setup();
//		System.out.println("Here i am created "+this.getAID());
//		addBehaviour(new CyclicBehaviour(this) {
//			
//			@Override
//			public void action() {
//				ACLMessage msg = myAgent.receive();
//				if(msg != null && bean != null){
//					bean.setMessage(msg.getContent());
//					releaseCommand(bean);
//				} else{
//					block();
//				}
//			}
//		});
//	}
//	
//	@Override
//	protected void processCommand(Object obj){
//		if(obj instanceof JMPBean){
//			bean = (JMPBean)obj;
//			ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
//			msg.addReceiver(new AID(bean.getReceiver(), AID.ISLOCALNAME));
//			msg.setContent(bean.getMessage());
//			this.send(msg);
//		}
//	}
	
}