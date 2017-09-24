package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.tools.Preference;
import bn.blaszczyk.rosecommon.tools.Preferences;

public class RoseHandler extends AbstractHandler {

	private final static String GET = "GET";
	private final static String POST = "POST";
	private final static String PUT = "PUT";
	private final static String DELETE = "DELETE";
	
	private final static Logger LOGGER = LogManager.getLogger(RoseHandler.class);
	
	private final Map<String, Endpoint> endpoints = new HashMap<>();
	
	private boolean enabled = true;
	private int requestCount = 0;
	private int failedRequestCount = 0;

	public void registerEndpointOptional(final String path, final Endpoint endpoint, final Preference optionalityPreference)
	{
		if(optionalityPreference != null)
			if(! Preferences.getBooleanValue(optionalityPreference) )
				return;
		registerEndpoint(path, endpoint);
	}
	public void registerEndpoint(final String path, final Endpoint endpoint)
	{
		LOGGER.info("registering endpoint " + endpoint.getClass().getSimpleName() + " at /" + path);
		endpoints.put(path, endpoint);
	}
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
		final String logContext = request.getMethod() + " at " + target + " from " + request.getRemoteHost();
		LOGGER.info("request: " + logContext);
		if(!enabled)
		{
			response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
			baseRequest.setHandled(true);
			return;
		}
		final String[] path = target.substring(1).split("\\/",2);
		final Endpoint endpoint = endpoints.get(path[0]);
		if(endpoint == null)
		{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			baseRequest.setHandled(true);
			LOGGER.warn("Endpoint not found: " + logContext);
			return;
		}
		int responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
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
		}
		catch(RoseException e)
		{
			response.getWriter().write(e.getFullMessage());
			LOGGER.error("internal server error", e);
		}
		catch(Exception e)
		{
			response.getWriter().write(e.getMessage());
			LOGGER.error("internal server error", e);
		}
		finally
		{
			response.setStatus(responseCode);
			if(responseCode >= 300)
			{
				LOGGER.error( "responding " + responseCode + " to " + logContext);
				failedRequestCount++;
			}
			requestCount++;
			baseRequest.setHandled(true);
			LOGGER.debug("request handled: " + logContext);
		}
	}

	public Map<String, String> getStatus()
	{
		final Map<String,String> status = new HashMap<>();
		endpoints.forEach((p,e) -> status.put("endpoint " + p, e.getClass().getSimpleName()));
		status.put("requests", String.valueOf(requestCount));
		status.put("requests failed", String.valueOf(failedRequestCount));
		return status;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled  = enabled;
	}
	
}
