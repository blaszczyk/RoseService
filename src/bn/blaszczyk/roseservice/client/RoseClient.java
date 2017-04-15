package bn.blaszczyk.roseservice.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.roseservice.model.RoseDto;

public class RoseClient {

	private static final Gson GSON = new Gson();
	
	private final WebClient webClient;

	public RoseClient()
	{
		webClient = WebClient.create("http://localhost:4053/entity");
	}
	
	public RoseDto getDto(final String typeName, final int id)
	{
		webClient.replacePath("/" + typeName + "/" + id);
		webClient.resetQuery();
		final Response response = webClient.get();
		final Object o = response.getEntity();
		if(o instanceof InputStream)
		{
			final Scanner scanner = new Scanner((InputStream)o);
			while (scanner.hasNextLine())
			{
				final String jsonResponse = scanner.nextLine();
				final RoseDto dto = new RoseDto( GSON.fromJson(jsonResponse, LinkedHashMap.class));
				scanner.close();
				return dto;
			}
			scanner.close();
		}
		return null;
	}
	
	public List<RoseDto> getDtos(final String typeName)
	{
		webClient.replacePath("/" + typeName);
		webClient.resetQuery();
		final Response response = webClient.get();
		final Object o = response.getEntity();
		final List<RoseDto> allDtos = new ArrayList<>();
		if(o instanceof InputStream)
		{
			final Scanner scanner = new Scanner((InputStream)o);
			while (scanner.hasNextLine())
			{
				final String jsonResponse = scanner.nextLine();
				final List<?> dtos = GSON.fromJson(jsonResponse, List.class);
				for(final Object oo : dtos)
				{
					if(oo instanceof StringMap<?>)
					{
						final StringMap<?> stringMap = (StringMap<?>)oo;
						allDtos.add(new RoseDto(stringMap));
					}
				}
			}
			scanner.close();
		}
		return allDtos;
	}

}
