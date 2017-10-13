package bn.blaszczyk.roseservice.server;

import org.eclipse.jetty.server.Server;

import bn.blaszczyk.rose.RoseException;

public class RoseServer {

	private final Server server;	
	private final RoseHandler handler;

	public RoseServer(final int port, final RoseHandler handler)
	{
		this.handler = handler;
		server = new Server(port);
		server.setHandler(handler);
	}

	public void startServer() throws RoseException
	{
		try
		{
			server.start();
		}
		catch (Exception e)
		{
			throw new RoseException("Cannot start server " + this, e);
		}
	}

	public void stopServer() throws RoseException
	{
		try
		{
			server.stop();
		}
		catch (Exception e)
		{
			throw new RoseException("Cannot stop server " + this, e);
		}
		finally
		{
			server.destroy();
		}
	}
	
	public RoseHandler getHandler()
	{
		return handler;
	}

	public void setEnabled(final boolean enabled)
	{
		handler.setEnabled(enabled);
	}

}
