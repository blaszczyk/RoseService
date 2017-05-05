 package bn.blaszczyk.roseservice;

import org.apache.log4j.Logger;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.controller.*;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.LoggerConfigurator;
import bn.blaszczyk.rosecommon.tools.Preferences;
import bn.blaszczyk.rosecommon.tools.TypeManager;
import bn.blaszczyk.roseservice.calculator.CalculatorEndpoint;
import bn.blaszczyk.roseservice.server.*;
import bn.blaszczyk.roseservice.web.WebEndpoint;
import bn.blaszczyk.rose.model.Readable;

public class Launcher {
	
	private static final int PORT = 4053;
	
	private Launcher( final String[] args)
	{
		if(args.length == 0)
		{
			System.out.println("No Rose model file specified.");
			System.exit(1);
		}
		TypeManager.parseRoseFile(Launcher.class.getClassLoader().getResourceAsStream(args[0]));
		Preferences.setMainClass(RoseServer.class);
		LoggerConfigurator.configureLogger(CommonPreference.BASE_DIRECTORY, CommonPreference.LOG_LEVEL);
	}
	
	public void launch()
	{
		final HibernateController hibernateController = new HibernateController();
		final CacheController cacheController = new CacheController(hibernateController);
		final ModelController controller = new ConsistencyController(cacheController);
		
		final RoseHandler handler = new RoseHandler();
		final RoseServer server = new RoseServer(PORT, handler);
		
		handler.registerEndpoint("entity", new EntityEndpoint(controller));
		handler.registerEndpoint("web", new WebEndpoint("http://localhost:" + PORT));
		handler.registerEndpoint("server", new ServerEndpoint(server));
		handler.registerEndpoint("calc", new CalculatorEndpoint());
		
		try
		{
			for(Class<? extends Readable> type : TypeManager.getEntityClasses())
				cacheController.getEntities(type);
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
