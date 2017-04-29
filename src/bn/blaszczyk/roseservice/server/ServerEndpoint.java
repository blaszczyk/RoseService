package bn.blaszczyk.roseservice.server;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import bn.blaszczyk.rosecommon.RoseException;

import static bn.blaszczyk.rosecommon.client.RoseClient.CODING_CHARSET;

public class ServerEndpoint implements Endpoint {
	
	private static final Gson GSON = new Gson();
	
	private final RoseServer server;

	public ServerEndpoint(final RoseServer server)
	{
		this.server = server;
	}
	
	@Override
	public int get(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			String responseString = "";
			if(path.equals("status"))
			{
				final Map<String,String> status = server.getHandler().getStatus();
				responseString = GSON.toJson(status);
			}
			final String encodedResponceString = URLEncoder.encode(responseString, CODING_CHARSET);
			response.getWriter().write(encodedResponceString);
			return HttpServletResponse.SC_OK;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on GET@server/" + path);
		}
	}
	
	@Override
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}
	
	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		if(path.equals("stop"))
		{
			// stop server
		}
		return HttpServletResponse.SC_NO_CONTENT;
	}
	
	@Override
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}

	@Override
	public Map<String, String> status()
	{
		final Map<String,String> status = new HashMap<>();
		status.put("endpoint /server", "active");
		return status;
	}
	
}
