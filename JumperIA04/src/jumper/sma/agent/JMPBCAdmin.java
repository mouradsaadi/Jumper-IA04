package jumper.sma.agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.util.JMPMessageParser;

public class JMPBCAdmin extends Agent {
	
	/*Elements needed for inserting a link: userid, url, operation, tags*/
	
	public final static AID identification = new AID("JMPBCAdmin", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPBCAdmin");
	
	//Operation type definition
	private static final int UPDATE_INSERT_LINK_OP = 0;
	private static final int UPDATE_ADD_TAG_OP = 1;		//Add new tag to specific link
	
	//Const parameter
	private static final String MODEL_PATH = "ontology/jumper.bc";
	private static final String PREFIX_PATH = "ontology/prefix.txt";
	
	private String prefix = "";
	private Model model;
	
	@Override
	protected void setup() {
		// TODO Auto-generated method stub
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
			
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		addBehaviour(new OperationExecuter());
	}
	
	private class OperationExecuter extends Behaviour{

		@Override
		public void action() {
			// TODO Auto-generated method stub
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.PROPAGATE);
			ACLMessage message = myAgent.receive(mt);
			if(message != null){
				logger.info(myAgent.getAID().getLocalName() + " received message from " + message.getSender().getLocalName());
				
				RequestBean rb = JMPMessageParser.toObject(message.getContent());
				
				//Check firstly the existence of the user
				if(!existenceVerification(rb.getUserid().toLowerCase())){
					logger.warning(myAgent.getAID().getLocalName() + ": No corresponding ID.");
					return;
				}
				
				logger.info(getAID().getLocalName() + " comfirmed the uniqueness of ID.");
				updateOperation(0, rb);
				
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
		
		private void updateOperation(int op, RequestBean rb){
			switch (op) {
			case UPDATE_INSERT_LINK_OP:
				/*cut the 'http://' part and replace all '/' with '*' */
				constructLink(rb.getUrl(), 
						rb.getTags(), rb.getUserid().trim().toLowerCase().replaceAll("\\s", "_"));
				break;
			case UPDATE_ADD_TAG_OP:
				break;
			default:
				break;
			}
		}
		
		private void constructLink(String url, LinkedHashMap<String, String> tags, String id){
			//Create link
			String linkID = url.substring(7).replaceAll("/", "-") + "-" + id ;
			String createFL = 	"INSERT {" +
								"jmpbc:" + linkID + " a jumper:FavoriteLink;" +
								"jumper:url \"" + url + "\"^^xsd:anyURI;";
			
			Set<Entry<String, String>> es = tags.entrySet();
			Iterator<Entry<String, String>> it = es.iterator();
			//Create new tag && add tags to link
			while(it.hasNext()) {
				Entry<String, String> taginfo = it.next();
				String tag = taginfo.getKey().trim().toLowerCase().replaceAll("\\s", "_");		//Key as the tag
				String uTag = taginfo.getValue().trim().toLowerCase().replaceAll("\\s", "_");	//value as the upper tag
				
				String createTag = 	"INSERT{" +
						"jmpbc:" + tag + " a jumper:Tag;";
				if(!uTag.isEmpty()){
					createTag += "jumper:isSubTagOf jmpbc:" + uTag;
				}
				createTag += "}WHERE{" +
							 "	 FILTER NOT EXISTS{" + 
							 "jmpbc:" + tag + " a jumper:Tag;";
				if(!uTag.isEmpty()){
					createTag += "	jumper:isSubTagOf jmpbc:" + uTag;
				}
				
				createTag += "}}";
				runUpdate(createTag);
				createFL += "	jumper:hasTag jmpbc:" + tag + ";";
			}
			
			//Add constraint part && user
			createFL += "jumper:belongsTo jmpbc:" + id +".}" +
						"WHERE{" + 
						"	FILTER NOT EXISTS{" + 
						"		jmpbc:" + linkID + " a jumper:FavoriteLink;}}";

			runUpdate(createFL);
			
			logger.info(getAID().getLocalName() + " complete the operation.");
		}
		
		private void runUpdate(String instruction){
			UpdateRequest request = UpdateFactory.create(prefix + instruction);
			UpdateAction.execute(request, model);
			
			try {
				FileOutputStream fos = new FileOutputStream(MODEL_PATH);
				RDFDataMgr.write(fos, model, Lang.TURTLE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
