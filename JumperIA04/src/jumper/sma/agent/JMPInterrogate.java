package jumper.sma.agent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.logging.Logger;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jumper.sma.bean.RequestBean;
import jumper.sma.bean.ResponseBean;
import jumper.sma.util.JMPKBModel;
import jumper.sma.util.JMPMessageParser;

public class JMPInterrogate extends Agent {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5893686881813455005L;
	public final static AID identification = new AID("JMPInterrogate", AID.ISLOCALNAME);
	private static Logger logger = Logger.getLogger("Agents.JMPInterrogate");
	
	@Override
	protected void setup(){
		super.setup();
		
		addBehaviour(new QueryExecuter());
	}
	
	private class QueryExecuter extends Behaviour{

		/**
		 * 
		 */
		private static final long serialVersionUID = -4386098140448847177L;

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
					LinkedHashMap<String, ArrayList<String>> recLinks = getRecommendingLink(rb.getUserid().trim().toLowerCase());
					
					/*Parse to message*/
					ResponseBean resBean = new ResponseBean();
					resBean.setStatus((recLinks == null)? 1 : 0);
					resBean.setRecLinks(recLinks);
					
					/*Send to controller*/
					ACLMessage toController = new ACLMessage(ACLMessage.INFORM);
					toController.setConversationId(msg.getConversationId());
					toController.setContent(JMPMessageParser.toMessage(resBean));
					toController.addReceiver(JMPController.identification);
					myAgent.send(toController);					
				} else{
					logger.warning(myAgent.getAID().getLocalName() + ": No corresponding ID.");
				}
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
		
		private ArrayList<String> checkSameUrl(String userid){
			String csQuery	=	"SELECT DISTINCT (?url AS ?same_url) " +
								"WHERE{" +
								"	?link1 a jumper:FavoriteLink;" +
								"	jumper:belongsTo ?user1;" +
								"	jumper:hasTag ?tag1;" +
								"	jumper:url ?url." +
								"?link2 a jumper:FavoriteLink;" +
								"	jumper:belongsTo ?user2;" +
								"	jumper:hasTag ?tag2;" +
								"	jumper:url ?url." +
								"	FILTER(?user1 = jmpbc:" + userid + " && ?user2 != ?user1)}";
			
			return runQuery_Array(JMPKBModel.getInstance().getPrefix() + csQuery, "same_url", true);
		}
		
		private LinkedHashMap<String, ArrayList<String>> getRecommendingLink(String userid){
			logger.info(getLocalName() + ": start search.");
			
			ArrayList<String> sameLink = checkSameUrl(userid);
			String tagQueyStr = "SELECT DISTINCT (?url2 AS ?url) ?tag " +
								"WHERE{" +
								"	?user1 a jumper:User." +
								"	?user2 a jumper:User." +
								"	?link1 a jumper:FavoriteLink;" +
								"		jumper:belongsTo ?user1;" +
								"		jumper:hasTag ?tag;" +
								"		jumper:url ?url1." +
								"	?link2 a jumper:FavoriteLink;" +
								"		jumper:belongsTo ?user2;" +
								"		jumper:hasTag ?tag;" +
								"		jumper:url ?url2.";
			if(sameLink != null){
				tagQueyStr += 	"	FILTER NOT EXISTS{" +
								"		FILTER(?url2 IN (";
				for(int i = 0; i < sameLink.size(); i++){
					tagQueyStr += String.format("\"%s\"^^xsd:anyURI", sameLink.get(i));
					if(i == sameLink.size() - 1){
						tagQueyStr += "))}";
					} else{
						tagQueyStr += ", ";
					}
				}
			}
			tagQueyStr += "	FILTER(?user1 = jmpbc:" + userid + " && ?user2 != ?user1)}";
			
			LinkedHashMap<String, ArrayList<String>> result = runQuery_Map(JMPKBModel.getInstance().getPrefix() + tagQueyStr);
			
			//In this case, we can find some urls which do not belong to the user but have the same tag that the user possesses.
			if(result != null){
				logger.info(getLocalName() + ": search of rec link end in phase 1.");
				return result;
			}
			
			//In this case, we can only recommend some URLs with the same theme as what the user possesses.
			logger.info(getLocalName() + ": search of rec link start phase 2.");
			LinkedHashSet<String> tags = allTagsConcernedWith(userid);
			if(tags == null){
				logger.info(getLocalName() + ": exception with no tag.");
				return null;
			}
			logger.info(getLocalName() + ": search of rec link end in phase 2.");
			return rec_search(tags, userid, sameLink);
		}
		
		private LinkedHashMap<String, ArrayList<String>> rec_search(LinkedHashSet<String> tags, String userid, ArrayList<String> sameLinks){
			LinkedHashSet<String> ttags = new LinkedHashSet<String>();	//Contain all theme concerned with the user.
			LinkedHashMap<String, ArrayList<String>> recLink = new LinkedHashMap<String, ArrayList<String>>(); //All recommending links classified by theme.
			
			for (String tag : tags) {
				/*Search theme(root tag)*/
				String theme = tag;
				String tmp = tag;
				while((tmp = getUpperLevelTag(tmp)) != null){
					theme = tmp;
				}
				ttags.add(theme);
			}
			
			/*Search all link concerned with theme in the set*/
			/*Plan 1(Better in performance, but not complete)*/
//			int count = 0;
//			for (String theme : ttags) {
//				LinkedHashSet<String> urlSet = allLinksConcernedWith(theme, userid, sameLinks);
//				if(urlSet != null){
//					ArrayList<String> urls = new ArrayList<String>(urlSet);
//					/*Add all links to the theme*/
//					if(!urls.isEmpty())
//						count ++;
//					recLink.put(theme, urls);
//				}
//			}
			
			/*Plan 2(Consuming but complete)*/
			int count = 0;
			for (String theme : ttags) {
				LinkedHashSet<String> themeURLs = new LinkedHashSet<String>();
				LinkedHashSet<String> tmpTags = allTagsConvergedOn(theme);
				System.out.println(tmpTags);
				for (String tag : tmpTags) {
					LinkedHashSet<String> tmpURLs = allLinksConcernedWith(tag, userid, sameLinks);
					if(tmpURLs != null)
						themeURLs.addAll(tmpURLs);
				}
				
				/*For those who add directly the theme as a tag*/
				LinkedHashSet<String> tmpURLs = allLinksConcernedWith(theme, userid, sameLinks);
				if(tmpURLs != null)
					themeURLs.addAll(tmpURLs);
				
				if(!themeURLs.isEmpty())
					count++;
				
				recLink.put(theme, new ArrayList<String>(themeURLs));
			}
			
			return count > 0? recLink : null;
		}
		
		/*For tag*/
		private LinkedHashSet<String> allLinksConcernedWith(String tag, String userid, ArrayList<String> sameLinks){
			String atcQuery = "SELECT ?url " + 
							"WHERE{" +
							"	?link a jumper:FavoriteLink;" +
							"		jumper:belongsTo ?user;" +
							"		jumper:hasTag ?tag;" +
							"		jumper:url ?url.";
			if(sameLinks != null){
				atcQuery += 	"	FILTER NOT EXISTS{" +
								"		FILTER(?url IN (";
				for(int i = 0; i < sameLinks.size(); i++){
					atcQuery += String.format("\"%s\"^^xsd:anyURI", sameLinks.get(i));
					if(i == sameLinks.size() - 1){
						atcQuery += "))}";
					} else{
						atcQuery += ", ";
					}
				}
			}
			atcQuery += "	FILTER(?user != jmpbc:" + userid + "&& ?tag = jmpbc:" + tag + ")}";
			
			return runQuery_Set(JMPKBModel.getInstance().getPrefix() + atcQuery, "url", true);
		}
		
		private String getUpperLevelTag(String tag){
			String uQuery = "SELECT ?ptag " +
							"WHERE{" +
							"	?tag a jumper:Tag;" +
							"		jumper:isSubTagOf ?ptag." +
							"	FILTER(?tag = jmpbc:" + tag + ")}";
			
			Query query = QueryFactory.create(JMPKBModel.getInstance().getPrefix() + uQuery);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
			ResultSet r = queryExecution.execSelect();
			
			if(r.hasNext()){
				QuerySolution qs = r.next();
				return qs.getResource("ptag").getLocalName();
			}
			
			queryExecution.close();
			return null;
		}
		
		private LinkedHashSet<String> allTagsConcernedWith(String userid){
			String tagQuery = 	"SELECT DISTINCT ?tag " +
								"WHERE {" +
								"	?user a jumper:User." +
								"	?link a jumper:FavoriteLink;" +
								"		jumper:hasTag ?tag;" +
								"		jumper:belongsTo ?user." +
								"	FILTER(?user = jmpbc:" + userid + ")}";
			
			return runQuery_Set(JMPKBModel.getInstance().getPrefix() + tagQuery, "tag", false);
		}		
		
		/*Get all tags converging to a specific theme*/
		private LinkedHashSet<String> allTagsConvergedOn(String theme){
			String tagQuery = 	"SELECT ?tag " +
								"WHERE{" +
								"	?tag a jumper:Tag." +
								"	OPTIONAL {?tag jumper:isSubTagOf ?ptag.}" +
								"	FILTER(bound(?ptag))}";
			
			/*Filter all tags*/
			LinkedHashSet<String> tags = runQuery_Set(JMPKBModel.getInstance().getPrefix() + tagQuery, "tag", false);
			LinkedHashSet<String> result = new LinkedHashSet<String>();
			for (String tag : tags) {
				String tmp = tag;
				String t = tag;
				while((tmp = getUpperLevelTag(tmp)) != null){
					t = tmp;
				}
				
				if(t.equalsIgnoreCase(theme)){
					result.add(tag);
				}
			}
			return result.isEmpty()? null : result;
		}
		
		private ArrayList<String> runQuery_Array(String inst, String attributeName, boolean isLiteral){
			Query query = QueryFactory.create(inst);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
			ResultSet r = queryExecution.execSelect();
			
			//Save the result to array
			ArrayList<String> result = new ArrayList<String>();
			while(r.hasNext()){
				QuerySolution qs = r.next();
				if(isLiteral)
					result.add(qs.getLiteral(attributeName).getString());
				else
					result.add(qs.getResource(attributeName).getLocalName());
			}
			
			queryExecution.close();
			
			return result.isEmpty()? null : result;
		}
		
		private LinkedHashMap<String, ArrayList<String>> runQuery_Map(String inst){
			Query query = QueryFactory.create(inst);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
			ResultSet r = queryExecution.execSelect();
			
			LinkedHashMap<String, ArrayList<String>> result = new LinkedHashMap<String, ArrayList<String>>();
			
			while(r.hasNext()){
				QuerySolution qs = r.next();
				
				/*Tag and url*/
				String tag = qs.getResource("tag").getLocalName();
				String url = qs.getLiteral("url").getLexicalForm();
				
				if(!result.containsKey(tag)){
					result.put(tag, new ArrayList<String>());
				} 
				result.get(tag).add(url);
			}
			
			queryExecution.close();
			
			return result.isEmpty()? null : result;
		}
		
		private LinkedHashSet<String> runQuery_Set(String inst, String attributeName, boolean isLiteral){
			Query query = QueryFactory.create(inst);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
			ResultSet r = queryExecution.execSelect();
			
			LinkedHashSet<String> result = new LinkedHashSet<String>();
			
			while(r.hasNext()){
				QuerySolution qs = r.next();
				if(isLiteral){
					result.add(qs.getLiteral(attributeName).getLexicalForm());
				} else{
					result.add(qs.getResource(attributeName).getLocalName());
				}
			}
			
			queryExecution.close();
			
			return result.isEmpty()? null : result;
		}
		
		@Override
		public boolean done() {
			return false;
		}
		
	}
}
