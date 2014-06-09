package jumper.sma.bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ResponseBean {
	private int status = 0; //0: works fine 1: No corresponding result found
	private LinkedHashMap<String, ArrayList<String>> recLinks;
	
	public void setRecLinks(LinkedHashMap<String, ArrayList<String>> recUrls){
		this.recLinks = recUrls;
	}
	
	public LinkedHashMap<String, ArrayList<String>> getRecUrls(){
		return recLinks;
	}
	
	public void setStatus(int status){
		this.status = status;
	}
	
	public int getStatus(){
		return status;
	}
}
