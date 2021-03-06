package jumper.sma.boot;

import java.util.logging.Logger;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;


public class JMPSMABoot {
	
	private static Logger logger = Logger.getLogger("Boot");
	public static String MAIN_PROPERTIES_FILE = "config/Mainproperties.txt";
	public static String PROPERTIES_FILE = "config/properties.txt";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Runtime rt = Runtime.instance();
		Profile mp = null;
		Profile p = null;
		try{
			mp = new ProfileImpl(MAIN_PROPERTIES_FILE );
			p = new ProfileImpl(PROPERTIES_FILE);
			AgentContainer mc = rt.createMainContainer(mp);
			AgentContainer jc = rt.createAgentContainer(p);
			logger.info("Main container "+mc.getName()+ " is ready");
			
			AgentController controller = jc.createNewAgent("JMPController"
					, "jumper.sma.agent.JMPController", null);
			controller.start();
			
			AgentController modeler = jc.createNewAgent("JMPModeler"
					, "jumper.sma.agent.JMPModeler", null);
			modeler.start();
			
			AgentController bcAdmin = jc.createNewAgent("JMPBCAdmin"
					, "jumper.sma.agent.JMPBCAdmin", null);
			bcAdmin.start();
			
			AgentController bcQuery = jc.createNewAgent("JMPInterrogate"
					, "jumper.sma.agent.JMPInterrogate", null);
			bcQuery.start();
			
			AgentController client = jc.createNewAgent("JMPClient"
					, "jumper.sma.agent.JMPClientAgent", null);
			client.start();
		} 
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
}
