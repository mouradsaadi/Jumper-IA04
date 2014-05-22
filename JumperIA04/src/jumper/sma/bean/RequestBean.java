package jumper.sma.bean;

import java.util.ArrayList;

public class RequestBean {
	
	private Integer operation; //0 for query, 1 for insertion.
	private Integer type; //0: concern with client.(mainly on the login authentication, or creation of account)
						  //1: concern with the favorite link and tag.
	//user
	private String id;
	private String password;
	//favorite link
	private String name; //This is the name of a link, not the user.
	private String url;
	private ArrayList<String> tags;
	
	public RequestBean(){
		super();
		operation = -1;
		type = -1;
		id = null;
		password = null;
		name = null;
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
	
	public String getId(){
		return id;
	}
	
	public String getPassword(){
		return password;
	}
	
	public String getName(){
		return name;
	}
	
	public String getUrl(){
		return url;
	}
	
	public ArrayList<String> getTags(){
		return tags;
	}
	
	//Setter
	public void setOperation(Integer op){
		operation = op;
	}
	
	public void setType(Integer t){
		type = t;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	public void setPassword(String password){
		this.password = password;
	}
	
	public void setName(String link_name){
		name = link_name;
	}
	
	public void setUrl(String url){
		this.url = url;
	}
	
	public void setTags(ArrayList<String> tags){
		this.tags = tags;
	}
}
