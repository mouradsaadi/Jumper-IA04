package jumper.sma.agent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.util.JMPMessageParser;

public class JMPInterrogate extends Agent {
	
	public final static AID identification = new AID("JMPInterrogate", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPInterrogate");
	
	//Const parameter
	private static final String MODEL_PATH = "ontology/jumper.bc";
	private static final String PREFIX_PATH = "ontology/prefix.txt";
	
	private String prefix = "";
	private Model model;
	
	@Override
	protected void setup(){
		super.setup();
		
		//init model
		model = ModelFactory.createDefaultModel();
		InputStream is = FileManager.get().open(MODEL_PATH);
		model.read(is, null, "TURTLE");
		
		//init prefix
		try {
			FileReader reader = new FileReader(PREFIX_PATH);
			BufferedReader br = new BufferedReader(reader);
			StringBuffer buffer = new StringBuffer();
			String tmp = "";		
			while((tmp = br.readLine()) != null){
				buffer.append(tmp);
			}
			br.close();
			reader.close();
			prefix = buffer.toString();
		} catch(IOException e){
			e.printStackTrace();
		}
		
		addBehaviour(new QueryExecuter());
	}
	
	private class QueryExecuter extends Behaviour{

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
			ACLMessage msg = myAgent.receive(mt);
			if(msg != null){
				logger.info(myAgent.getAID().getLocalName() + " received the message from " + msg.getSender().getLocalName());
				
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
			Query equery = QueryFactory.create(prefix + extistenceQuery);
			QueryExecution qexe = QueryExecutionFactory.create(equery, model);
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
			runQuery(prefix + tagQueyStr);
		}
		
		private void runQuery(String inst){
			Query query = QueryFactory.create(inst);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
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
