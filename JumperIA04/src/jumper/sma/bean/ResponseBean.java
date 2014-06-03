package jumper.sma.bean;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ResponseBean {
	LinkedHashMap<ArrayList<String>, String> recUrls;
	
	void setRecUrl(LinkedHashMap<ArrayList<String>, String> recUrls){
		this.recUrls = recUrls;
	}
	
	LinkedHashMap<ArrayList<String>, String> getRecUrls(){
		return recUrls;
	}
}
