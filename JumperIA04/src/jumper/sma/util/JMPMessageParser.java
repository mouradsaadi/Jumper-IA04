package jumper.sma.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import jumper.sma.bean.RequestBean;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JMPMessageParser {
	
	public static RequestBean toObject(String message){
		ObjectMapper objectMapper = new ObjectMapper();
		RequestBean rb = null;
		try {
			rb = objectMapper.readValue(message, RequestBean.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rb;
	}
	
	public static String toMessage(RequestBean rb){
		ObjectMapper mapper = new ObjectMapper();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			mapper.writeValue(baos, rb);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String rbJson = null;
		try {
			rbJson = baos.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rbJson;
	}
	
	public static void main(String[] args){
		RequestBean rb = toObject("{\"userid\":\"test\",\"url\":\"www.baidu.com\",\"tags\":[\"search\",\"monopole\",\"Chinese\",\"Main Land\"]}");
		System.out.println(rb.getUrl());
		System.out.println(rb.getUserid());
		System.out.println(rb.getTags());
		
		System.out.println(toMessage(rb));
	}
}
