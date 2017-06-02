package bn.blaszczyk.roseservice.server;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.client.CommonClient;
import bn.blaszczyk.rosecommon.client.FileClient;
import bn.blaszczyk.rosecommon.client.ServiceConfigClient;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.tools.Preferences;
import bn.blaszczyk.roseservice.Launcher;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;
import static bn.blaszczyk.rosecommon.client.CommonClient.CODING_CHARSET;

public class ServerEndpoint implements Endpoint {
	
	private static final Gson GSON = new Gson();
	
	private final Launcher launcher;
	private final RoseServer server;

	public ServerEndpoint(final Launcher launcher)
	{
		this.launcher = launcher;
		this.server = launcher.getServer();
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
			else if(path.equals("config"))
			{
				final PreferenceDto dto = createAllPreferencesDto();
				responseString = GSON.toJson(dto);
			}
			else
				return HttpServletResponse.SC_NOT_FOUND;
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
		if(path.equals("stop"))
			new Thread(()->stopThreaded()).start();
		else if(path.equals("restart"))
			new Thread(()->restartThreaded()).start();
		else
			return HttpServletResponse.SC_NOT_FOUND;
		return HttpServletResponse.SC_NO_CONTENT;
	}

	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			if(path.equals("config"))
			{
				final String encodedRequestString = request.getReader().lines().collect(Collectors.joining("\r\n"));
				final String requestString = URLDecoder.decode(encodedRequestString, CODING_CHARSET);
				final StringMap<?> stringMap = GSON.fromJson(requestString, StringMap.class);
				final PreferenceDto dto = new PreferenceDto(stringMap);
				Arrays.stream(launcher.getPreferences())
						.flatMap(Arrays::stream)
						.filter(dto::containsPreference)
						.forEach(p -> putValue(p, dto.get(p)));
			}
			else
				return HttpServletResponse.SC_NOT_FOUND;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e,"error handling PUT request");
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
	
	private PreferenceDto createAllPreferencesDto()
	{
		final PreferenceDto dto = new PreferenceDto();
		Arrays.stream(launcher.getPreferences())
				.flatMap(Arrays::stream)
				.forEach(p -> dto.put(p, getValue(p)));
		return dto;
	}

	private void stopThreaded()
	{
		server.setEnabled(false);
		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e)
		{
		}
		launcher.stop();
		CommonClient.closeInstance();
		FileClient.closeInstance();
		ServiceConfigClient.closeInstance();
	}

	private void restartThreaded()
	{
		server.setEnabled(false);
		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e)
		{
		}
		launcher.stop();
		Preferences.clearCache();
		try
		{
			Thread.sleep(2000);
		}
		catch (InterruptedException e)
		{
		}
		launcher.launch();
	}
}
