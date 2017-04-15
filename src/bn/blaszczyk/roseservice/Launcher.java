package bn.blaszczyk.roseservice;

import org.apache.log4j.Logger;

import bn.blaszczyk.roseservice.controller.HibernateController;
import bn.blaszczyk.roseservice.server.EntityEndpoint;
import bn.blaszczyk.roseservice.server.RoseHandler;
import bn.blaszczyk.roseservice.server.RoseServer;
import bn.blaszczyk.roseservice.tools.TypeManager;
import bn.blaszczyk.roseservice.web.WebEndpoint;

public class Launcher {
	
	public Launcher()
	{
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args)
	{
		if(args.length == 0)
		{
			System.out.println("No Rose model file specified.");
			System.exit(1);
		}
		TypeManager.parseRoseFile(Launcher.class.getClassLoader().getResourceAsStream(args[0]));
		final HibernateController controller = new HibernateController();
		final RoseHandler handler = new RoseHandler();
		handler.registerEndpoint("entity", new EntityEndpoint(controller));
		handler.registerEndpoint("web", new WebEndpoint());
		final RoseServer server = new RoseServer(4053, handler);

		try
		{
			for(Class<?> type : TypeManager.getEntityClasses())
				controller.loadEntities(type);
			server.startServer();
		}
		catch(RoseException e)
		{
			Logger.getLogger(Launcher.class).error("Error starting rose service", e);
		}
	}
	
}
