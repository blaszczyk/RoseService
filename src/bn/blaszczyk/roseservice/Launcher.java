 package bn.blaszczyk.roseservice;

import org.apache.log4j.Logger;

import bn.blaszczyk.roseservice.calculator.CalculatorEndpoint;
import bn.blaszczyk.roseservice.controller.CacheController;
import bn.blaszczyk.roseservice.controller.ConsistencyController;
import bn.blaszczyk.roseservice.controller.HibernateController;
import bn.blaszczyk.roseservice.controller.ModelController;
import bn.blaszczyk.roseservice.server.EntityEndpoint;
import bn.blaszczyk.roseservice.server.RoseHandler;
import bn.blaszczyk.roseservice.server.RoseServer;
import bn.blaszczyk.roseservice.server.ServerEndpoint;
import bn.blaszczyk.roseservice.tools.TypeManager;
import bn.blaszczyk.roseservice.web.WebEndpoint;

import bn.blaszczyk.rose.model.Readable;

public class Launcher {
	
	private Launcher( final String[] args)
	{
		if(args.length == 0)
		{
			System.out.println("No Rose model file specified.");
			System.exit(1);
		}
		TypeManager.parseRoseFile(Launcher.class.getClassLoader().getResourceAsStream(args[0]));
	}
	
	public void launch()
	{
		final HibernateController hibernateController = new HibernateController();
		final CacheController cacheController = new CacheController(hibernateController);
		final ModelController controller = new ConsistencyController(cacheController);
		
		final RoseHandler handler = new RoseHandler();
		final RoseServer server = new RoseServer(4053, handler);
		
		handler.registerEndpoint("entity", new EntityEndpoint(controller));
		handler.registerEndpoint("web", new WebEndpoint());
		handler.registerEndpoint("server", new ServerEndpoint(server));
		handler.registerEndpoint("calc", new CalculatorEndpoint());
		
		try
		{
			for(Class<? extends Readable> type : TypeManager.getEntityClasses())
				cacheController.synchronize(type);
			server.startServer();
		}
		catch(RoseException e)
		{
			Logger.getLogger(Launcher.class).error("Error starting rose service", e);
		}
	}
	
	public static void main(String[] args)
	{
		new Launcher(args).launch();
	}
	
}
