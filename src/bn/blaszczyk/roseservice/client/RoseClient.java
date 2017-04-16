package bn.blaszczyk.roseservice.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	public List<RoseDto> getDtos(final String typeName, final List<Integer> entityIds)
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		return queryDtos("/" + typeName + "/" + commaSeparated(entityIds));
	}

	public RoseDto postDto(final RoseDto dto)
	{
		final String path = "/" + dto.getType().getSimpleName().toLowerCase();
		webClient.replacePath(path);
		webClient.resetQuery();
		final String response = webClient.post(GSON.toJson(dto),String.class);
		final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
		return new RoseDto(stringMap);
		//TODO: handle error
	}

	public void putDto(final RoseDto dto)
	{
		final String path = "/" + dto.getType().getSimpleName().toLowerCase() + "/" + dto.getId();
		webClient.replacePath(path);
		webClient.resetQuery();
		webClient.put(GSON.toJson(dto));
		//TODO: handle error
	}

	public void deleteByID(final String typeName, final int id)
	{
		final String path = "/" + typeName + "/" + id;
		webClient.replacePath(path);
		webClient.resetQuery();
		webClient.delete();
		//TODO: handle error
	}

	private List<RoseDto> queryDtos(final String path)
	{
		webClient.replacePath(path);
		webClient.resetQuery();
		final List<RoseDto> dtos = new ArrayList<>();
		final String response = webClient.get(String.class);
		final StringMap<?>[] stringMaps = GSON.fromJson(response, StringMap[].class);
		Arrays.stream(stringMaps)
			.map(RoseDto::new)
			.forEach( dto -> dtos.add(dto));
		return dtos;
	}
	
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
