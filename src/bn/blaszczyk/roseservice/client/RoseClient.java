package bn.blaszczyk.roseservice.client;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
		final List<RoseDto> dtos = queryDtos("/" + typeName + "/" + id);
		return dtos.isEmpty() ? null : dtos.get(0);
	}
	
	public List<RoseDto> getDtos(final String typeName)
	{
		return queryDtos("/" + typeName);
	}

	public List<RoseDto> getDtos(String typeName, List<Integer> entityIds)
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		return queryDtos("/" + typeName + "/" + commaSeparated(entityIds));
	}
	
	private List<RoseDto> queryDtos(final String path)
	{
		webClient.replacePath(path);
		webClient.resetQuery();
		final Response response = webClient.get();
		final Object stream = response.getEntity();
		final List<RoseDto> dtos = new ArrayList<>();
		if(stream instanceof InputStream)
		{
			final Scanner scanner = new Scanner((InputStream)stream);
			while (scanner.hasNextLine())
			{
				final String jsonResponse = scanner.nextLine();
				final StringMap<?>[] stringMaps = GSON.fromJson(jsonResponse, StringMap[].class);
				Arrays.stream(stringMaps)
					.map(RoseDto::new)
					.forEach( dto -> dtos.add(dto));
			}
			scanner.close();
		}
		return dtos;
	};
	
	private static String commaSeparated(final List<?> list)
	{
		boolean first = true;
		final StringBuilder sb = new StringBuilder();
		for(final Object o : list)
		{
			if(first)
				first = false;
			else
				sb.append(",");
			sb.append(o);
		}
		return sb.toString();
	}

}
