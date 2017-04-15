package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import bn.blaszczyk.roseservice.RoseException;

public class RoseHandler extends AbstractHandler {

	private final static String GET = "GET";
	private final static String POST = "POST";
	private final static String PUT = "PUT";
	private final static String DELETE = "DELETE";
	
	private final static Logger LOGGER = Logger.getLogger(RoseHandler.class);
	
	private final Map<String, Endpoint> endpoints = new HashMap<>();
	
	public void registerEndpoint(final String path, final Endpoint endpoint)
	{
		endpoints.put(path, endpoint);
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
		LOGGER.info("getting request at " + target);
		final String[] path = target.substring(1).split("\\/",2);
		
		Scanner scanner = new Scanner(request.getInputStream());
		while (scanner.hasNextLine())
		{
			System.out.println("req: " + scanner.nextLine());
		}
		scanner.close();
		
		final Endpoint endpoint = endpoints.get(path[0]);
		if(endpoint == null)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			return;
		}
		final int responseCode;
		final String method = request.getMethod();
		final String subPath = path.length == 2 ? path[1] : "";
        try
        {
    		switch(method)
    		{
    		case GET:
    			responseCode = endpoint.get(subPath, request, response);
    			break;
    		case POST:
    			responseCode = endpoint.post(subPath, request, response);
    			break;
    		case PUT:
    			responseCode = endpoint.put(subPath, request, response);
    			break;
    		case DELETE:
    			responseCode = endpoint.delete(subPath, request, response);
    			break;
   			default:
   				responseCode = HttpServletResponse.SC_BAD_REQUEST;
    		}
    		response.setStatus(responseCode);
        }
        catch(RoseException e)
        {
        	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	response.getWriter().write("<h2>error</h2>");
        	e.printStackTrace(response.getWriter());
        	e.printStackTrace();
        }
        finally
        {
        	baseRequest.setHandled(true);
        }
	}
	
}
