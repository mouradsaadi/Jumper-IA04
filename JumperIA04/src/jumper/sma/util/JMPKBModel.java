package jumper.sma.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
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
}
