package jumper.sma.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;
import com.hp.hpl.jena.util.FileManager;

public class JMPKBModel {
	/*Base de connaissances singleton model*/
	private static JMPKBModel singleInstance = new JMPKBModel();
	private Model jmpModel = ModelFactory.createDefaultModel();
	private String jmpPrefix = null;
	
	//Public Path
	public static final String MODEL_PATH = "ontology/jumper.bc";
	public static final String PREFIX_PATH = "ontology/prefix.txt";
	
		
	private JMPKBModel(){
		//Init model
		InputStream is = FileManager.get().open(MODEL_PATH);
		jmpModel.read(is, null, "TURTLE");
		
		//Init prefix
		try {
			
			FileReader reader = new FileReader(JMPKBModel.PREFIX_PATH);
			BufferedReader br = new BufferedReader(reader);
			StringBuffer buffer = new StringBuffer();
			String tmp = "";		
			while((tmp = br.readLine()) != null){
				buffer.append(tmp);
			}
			br.close();
			reader.close();
			jmpPrefix = buffer.toString();
			
		} catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
	}
	
	static public JMPKBModel getInstance(){
		return singleInstance;
	}
	
	public Model getModel(){
		return jmpModel;
	}
	
	public String getPrefix(){
		return jmpPrefix;
	}
	
	/*Insert*/
	public void runUpdate(String instruction){
		UpdateRequest request = UpdateFactory.create(jmpPrefix + instruction);
		UpdateAction.execute(request, jmpModel);
		
		try {
			FileOutputStream fos = new FileOutputStream(JMPKBModel.MODEL_PATH);
			RDFDataMgr.write(fos, JMPKBModel.getInstance().getModel(), Lang.TURTLE);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	
	/*Query*/
	public ArrayList<String> runQuery_Array(String inst, String attributeName, boolean isLiteral){
		Query query = QueryFactory.create(inst);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, jmpModel);
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
	
	public LinkedHashMap<String, ArrayList<String>> runQuery_Map(String inst){
		Query query = QueryFactory.create(inst);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, jmpModel);
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
	
	public LinkedHashSet<String> runQuery_Set(String inst, String attributeName, boolean isLiteral){
		Query query = QueryFactory.create(inst);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, jmpModel);
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
	
	/*Util*/
	
	/*Get all tags converging to a specific theme*/
	public LinkedHashSet<String> allTagsConvergedOn(String theme){
		String tagQuery = 	"SELECT ?tag " +
							"WHERE{" +
							"	?tag a jumper:Tag." +
							"	OPTIONAL {?tag jumper:isSubTagOf ?ptag.}" +
							"	FILTER(bound(?ptag))}";
		
		/*Filter all tags*/
		LinkedHashSet<String> tags = runQuery_Set(jmpPrefix + tagQuery, "tag", false);
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
	
	public LinkedHashSet<String> allTagsConcernedWith(String userid){
		String tagQuery = 	"SELECT DISTINCT ?tag " +
							"WHERE {" +
							"	?user a jumper:User." +
							"	?link a jumper:FavoriteLink;" +
							"		jumper:hasTag ?tag;" +
							"		jumper:belongsTo ?user." +
							"	FILTER(?user = jmpbc:" + userid + ")}";
		
		return JMPKBModel.getInstance().runQuery_Set(jmpPrefix + tagQuery, "tag", false);
	}		
	
	/*For tag*/
	public LinkedHashSet<String> allLinksConcernedWith(String tag, String userid, ArrayList<String> sameLinks){
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
		
		return runQuery_Set(jmpPrefix + atcQuery, "url", true);
	}
	
	public String getUpperLevelTag(String tag){
		String uQuery = "SELECT ?ptag " +
						"WHERE{" +
						"	?tag a jumper:Tag;" +
						"		jumper:isSubTagOf ?ptag." +
						"	FILTER(?tag = jmpbc:" + tag + ")}";
		
		Query query = QueryFactory.create(jmpPrefix + uQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
		ResultSet r = queryExecution.execSelect();
		
		if(r.hasNext()){
			QuerySolution qs = r.next();
			return qs.getResource("ptag").getLocalName();
		}
		
		queryExecution.close();
		return null;
	}
	
	/*Return the theme of the given tag*/
	public String themeOfTag(String tag){
		String themeQuery = "SELECT ?theme " +
							"WHERE{" +
							"	?theme a jumper:Tag;" +
							"		jumper:hasSubTag ?tag." +
							"	OPTIONAL{?theme jumper:isSubTagOf ?n}." +
							"	FILTER(!bound(?n) && ?tag = jmpbc:" + tag + ")}";
		
		Query query = QueryFactory.create(jmpPrefix + themeQuery);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, JMPKBModel.getInstance().getModel());
		ResultSet r = queryExecution.execSelect();
		
		if(r.hasNext()){
			QuerySolution qs = r.next();
			return qs.getResource("theme").getLocalName();
		}
		
		queryExecution.close();
		return null;
	}
	
	/*Return all links of the given theme*/
	public LinkedHashSet<String> allLinkOfTheme(String theme, String userid, ArrayList<String> sameLinks){
		/*Get all sub tag urls*/
		String themeURLQuery = "SELECT ?url "+
								"WHERE{" +
								"	?theme a jumper:Tag;" +
								"		jumper:hasSubTag ?tag." +
								"	?link a jumper:FavoriteLink;" +
								"		jumper:hasTag ?tag;" +
								"		jumper:belongsTo ?user;" +
								"		jumper:url ?url." +
								"	OPTIONAL{?theme jumper:isSubTagOf ?n}." +
								"	FILTER(!bound(?n) && ?theme = jmpbc:" + theme + " && ?user != jmpbc:" + userid + ")}";
		
		LinkedHashSet<String> themeURLs = runQuery_Set(jmpPrefix + themeURLQuery, "url", true);
		
		/*theme urls*/
		LinkedHashSet<String> themeURLs_d = allLinksConcernedWith(theme, userid, sameLinks);
		if(themeURLs_d != null && themeURLs != null)
			themeURLs.addAll(themeURLs_d);
		
		if(themeURLs != null || themeURLs_d != null)
			return themeURLs == null? themeURLs_d : themeURLs;
		else
			return null;
	}
}
