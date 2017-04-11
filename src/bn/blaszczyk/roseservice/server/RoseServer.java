package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.io.Writer;

import org.eclipse.jetty.server.Server;

import bn.blaszczyk.roseservice.RoseException;

public class RoseServer extends Server {

	public void stopServer(final Writer responseWriter) throws IOException
	{
    	System.out.println("Server stop");
    	responseWriter.write("<h1> und tschüss </h1>");
        new Thread(() -> {
        	try{
        		Thread.sleep(3000);
        		stop();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	System.exit(0);
        }).start();
        return;
	}
	
	public RoseServer(final int port, final RoseHandler handler)
	{
		super(port);
		setHandler(handler);
		
		// TODO Auto-generated constructor stub
	}

	public void startServer() throws RoseException
	{
		try
		{
			start();
			join();
		}
		catch (Exception e)
		{
			throw new RoseException("Cannor start server " + this, e);
		}
	}
	
}
