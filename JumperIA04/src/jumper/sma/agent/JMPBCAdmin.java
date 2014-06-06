package jumper.sma.agent;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.util.JMPKBModel;
import jumper.sma.util.JMPMessageParser;

public class JMPBCAdmin extends Agent {
	
	/*Elements needed for inserting a link: userid, url, operation, tags*/
	
	public final static AID identification = new AID("JMPBCAdmin", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPBCAdmin");
	
	@Override
	protected void setup() {
		// TODO Auto-generated method stub
		super.setup();
		
		addBehaviour(new OperationExecuter());
	}
	
	private class OperationExecuter extends Behaviour{

		@Override
		public void action() {
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
				updateOperation(rb);
				
			} else{
				block();
			}
		}
		
		private boolean existenceVerification(String userid){
			String extistenceQuery = "SELECT ?user " +
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
		
		private void updateOperation(RequestBean rb){
			/*cut the 'http://' part and replace all '/' with '-' */
			constructLink(rb.getUrl(), 
					rb.getTags(), rb.getUserid().trim().toLowerCase().replaceAll("\\s", "_"));
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
				String uTag = null;
				if(taginfo.getValue() != null){
					uTag = taginfo.getValue().trim().toLowerCase().replaceAll("\\s", "_");	//value as the upper tag
				}
				
				String createTag = 	"INSERT{" +
						"jmpbc:" + tag + " a jumper:Tag;";
				if(uTag != null && !uTag.isEmpty()){
					createTag += "jumper:isSubTagOf jmpbc:" + uTag;
				}
				createTag += "}WHERE{" +
							 "	 FILTER NOT EXISTS{" + 
							 "jmpbc:" + tag + " a jumper:Tag;";
				if(uTag != null && !uTag.isEmpty()){
					createTag += "	jumper:isSubTagOf jmpbc:" + uTag;
				}
				
				createTag += "}}";
				JMPKBModel.getInstance().runUpdate(createTag);
				createFL += "	jumper:hasTag jmpbc:" + tag + ";";
				
				if(uTag != null && !uTag.isEmpty()){
					/*Get the theme of parent tag*/
					String theme = JMPKBModel.getInstance().themeOfTag(uTag);
					if(theme == null)
						theme = uTag;
					
					/*Insert new tag to this theme*/
					String updateTheme = "INSERT DATA{" +
										"jmpbc:" + theme + " jumper:hasSubTag jmpbc:" + tag + ".}";
					JMPKBModel.getInstance().runUpdate(updateTheme);
				}
			}
			
			//Add constraint part && user
			createFL += "jumper:belongsTo jmpbc:" + id +".}" +
						"WHERE{" + 
						"	FILTER NOT EXISTS{" + 
						"		jmpbc:" + linkID + " a jumper:FavoriteLink;}}";

			JMPKBModel.getInstance().runUpdate(createFL);
			
			logger.info(getAID().getLocalName() + " complete the operation.");
		}
		
		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
