package bn.blaszczyk.roseservice.server;

import org.eclipse.jetty.server.Server;

import bn.blaszczyk.rosecommon.RoseException;

public class RoseServer extends Server {

	private final RoseHandler handler;

	public RoseServer(final int port, final RoseHandler handler)
	{
		super(port);
		this.handler = handler;
		setHandler(handler);
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
			throw new RoseException("Cannot start server " + this, e);
		}
	}
	
	public RoseHandler getHandler()
	{
		return handler;
	}

}
