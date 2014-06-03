package jumper.sma.agent;

import java.util.logging.Logger;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.util.JMPKBModel;
import jumper.sma.util.JMPMessageParser;

public class JMPInterrogate extends Agent {
	
	public final static AID identification = new AID("JMPInterrogate", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPInterrogate");
	
	@Override
	protected void setup(){
		super.setup();
		
		addBehaviour(new QueryExecuter());
	}
	
	private class QueryExecuter extends Behaviour{

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				logger.info(myAgent.getAID().getLocalName() + " received the message from " + msg.getSender().getLocalName());
								
				//Start query
				RequestBean rb = JMPMessageParser.toObject(msg.getContent());
				String userid = rb.getUserid().trim().toLowerCase();
				if(existenceVerification(userid)){
					getAllTagsConcernedWith(rb.getUserid().trim().toLowerCase());
				} else{
					logger.warning(myAgent.getAID().getLocalName() + ": No corresponding ID.");
				}
			} else{
				block();
			}
		}
		
		private boolean existenceVerification(String userid){
			String extistenceQuery = "SELECT ?user" +
									 "WHERE{" + 
									 "	?user a jumper:User." +
									 "	FILTER (?user=jmpbc:" + userid + ")}";
			Query equery = QueryFactory.create(JMPKBModel.getInstance().getPrefix() + extistenceQuery);
			QueryExecution qexe = QueryExecutionFactory.create(equery, JMPKBModel.getInstance().getModel());
			ResultSet resultSet = qexe.execSelect();
			if(resultSet.hasNext()){
				return true;
			} else{
				return false;
			}
		}
		
		private void getAllTagsConcernedWith(String userid){
			String tagQueyStr = "SELECT ?tag " +
								"WHERE{" +
								"	?user a jumper:User." +
								"	?link a jumper:FavoriteLink;" +
								"		jumper:belongsTo ?user;" +
								"		jumper:hasTag ?tag." +
								"	FILTER(?user = jmpbc:" + userid + ")}";
			runQuery(JMPKBModel.getInstance().getPrefix() + tagQueyStr);
		}
		
		private void runQuery(String inst){
			Query query = QueryFactory.create(inst);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
			ResultSet r = queryExecution.execSelect();
			ResultSetFormatter.out(System.out,r);
			queryExecution.close();
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
