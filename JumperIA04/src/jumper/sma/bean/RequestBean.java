
package jumper.sma.bean;

import java.util.LinkedHashMap;

public class RequestBean {
	
	private Integer operation; 	//0 for query, 1 for insertion.
	private Integer type; 	   	//0: concern with client.(mainly on the login authentication, or creation of account)
						  		//1: concern with the favorite link and tag.
	//user
	private String userid;
	//favorite link
	private String url;
	private LinkedHashMap<String, String> tags;
	
	public RequestBean(){
		super();
		operation = -1;
		type = -1;
		userid = null;
		url = null;
		tags = null;
	}
	
	//Getter
	public Integer getOperation(){
		return operation;
	}
	
	public Integer getType(){
		return type;
	}
	
	public String getUserid(){
		return userid;
	}
	
	public String getUrl(){
		return url;
	}
	
	public LinkedHashMap<String, String> getTags(){
		return tags;
	}
	
	//Setter
	public void setOperation(Integer op){
		operation = op;
	}
	
	public void setType(Integer t){
		type = t;
	}
	
	public void setUserid(String id){
		this.userid = id;
	}
	
	public void setUrl(String url){
		this.url = url;
	}
	
	public void setTags(LinkedHashMap<String, String> tags){
		this.tags = tags;
	}
}
