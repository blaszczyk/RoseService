package bn.blaszczyk.roseservice.server;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rosecommon.client.CommonClient;
import bn.blaszczyk.rosecommon.client.FileClient;
import bn.blaszczyk.rosecommon.client.ServiceConfigClient;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preferences;
import bn.blaszczyk.roseservice.Launcher;

import static bn.blaszczyk.rosecommon.tools.Preferences.*;

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
			if(path.equals("ping"))
			{
				responseString = Long.toString(System.currentTimeMillis());
			}
			else if(path.equals("status"))
			{
				final Map<String,String> status = server.getHandler().getStatus();
				status.put("uptime jvm", formatTime(launcher.getJvmUptime()));
				status.put("uptime service", formatTime(launcher.getServiceUptime()));
				responseString = GSON.toJson(status);
			}
			else if(path.equals("config"))
			{
				final PreferenceDto dto = createAllPreferencesDto();
				responseString = GSON.toJson(dto);
			}
			else
				return HttpServletResponse.SC_NOT_FOUND;
			response.getWriter().write(responseString);
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
			new Thread(()->stopThreaded(),"Thread-stop-server").start();
		else if(path.equals("restart"))
			new Thread(()->restartThreaded(),"Thread-restart-server").start();
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
				final String requestString = request.getReader().lines().collect(Collectors.joining("\r\n"));
				final Map<?,?> stringMap = GSON.fromJson(requestString, Map.class);
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
	
	private PreferenceDto createAllPreferencesDto()
	{
		final PreferenceDto dto = new PreferenceDto();
		Arrays.stream(launcher.getPreferences())
				.flatMap(Arrays::stream)
				.filter(p -> !p.equals(CommonPreference.SERVICE_HOST))
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
		System.exit(0);
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
		try
		{
			launcher.launch();
		}
		catch (RoseException e) 
		{
			LogManager.getLogger(ServerEndpoint.class).error("Error restarting service", e);
		}
	}

	private static String formatTime(long time)
	{
		final long milliseconds = time % 1000;
		time = time / 1000;
		final long seconds = time % 60;
		time = time / 60;
		final long minutes = time % 60;
		time = time / 60;
		final long hours = time % 24;
		final long days = time / 24;
		return (days > 0 ? ( days + "d " ) : "") + String.format("%2d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds);
	}
}
