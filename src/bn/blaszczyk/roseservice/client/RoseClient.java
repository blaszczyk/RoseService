package bn.blaszczyk.roseservice.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cxf.jaxrs.client.WebClient;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.model.RoseDto;

public class RoseClient {

	private static final Gson GSON = new Gson();
	
	private final WebClient webClient;

	public RoseClient()
	{
		webClient = WebClient.create("http://localhost:4053");
	}
	
	public RoseDto getDto(final String typeName, final int id) throws RoseException
	{
		final List<RoseDto> dtos = getDtos(typeName + "/" + id);
		if(dtos.size() != 1)
			throw new RoseException("error on GET@/" + typeName + "/" + id + "; found:" + dtos);
		return dtos.get(0);
	}
	
	public List<RoseDto> getDtos(final String path) throws RoseException
	{
		try
		{
			webClient.replacePath("/entity/" + path);
			webClient.resetQuery();
			final List<RoseDto> dtos = new ArrayList<>();
			final String response = webClient.get(String.class);
			final StringMap<?>[] stringMaps = GSON.fromJson(response, StringMap[].class);
			for(StringMap<?> stringMap : stringMaps)
				dtos.add(new RoseDto(stringMap));
			return dtos;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "Error on GET@" + path);
		}
	}

	public List<RoseDto> getDtos(final String typeName, final List<Integer> entityIds) throws RoseException
	{
		if(entityIds.isEmpty())
			return Collections.emptyList();
		return getDtos(typeName + "/" + commaSeparated(entityIds));
	}

	public RoseDto postDto(final RoseDto dto) throws RoseException
	{
		final String path = pathForType(dto);
		try
		{
			webClient.replacePath(path);
			webClient.resetQuery();
			final String response = webClient.post(GSON.toJson(dto),String.class);
			final StringMap<?> stringMap = GSON.fromJson(response, StringMap.class);
			return new RoseDto(stringMap);
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error on POST@" + path);
		}
	}

	public void putDto(final RoseDto dto) throws RoseException
	{
		final String path = pathFor(dto);
		try
		{
			webClient.replacePath(path);
			webClient.resetQuery();
			webClient.put(GSON.toJson(dto));
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on PUT@" + path);
		}
	}

	public void deleteByID(final String typeName, final int id) throws RoseException
	{
		final String path = "/entity/" + typeName + "/" + id;

		try
		{
			webClient.replacePath(path);
			webClient.resetQuery();
			webClient.delete();
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on DELETE@" + path);
		}
	}
	
	public Map<String,String> getServerStatus() throws RoseException
	{
		try
		{
			webClient.replacePath("/server/status");
			webClient.resetQuery();
			final String response = webClient.get(String.class);
			final StringMap<?> status = GSON.fromJson(response, StringMap.class);
			return status.entrySet().stream().
				collect(Collectors.toMap(e -> e.getKey(), e -> String.valueOf(e.getValue())));
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on GET@/server/status");
		}
	}
	
	private String pathForType(final RoseDto dto) throws RoseException
	{
		final Class<?> type = dto.getType();
		if(type == null)
			throw new RoseException("missing type in " + dto);
		return "/entity/" + type.getSimpleName().toLowerCase();
	}
	
	private String pathFor(final RoseDto dto) throws RoseException
	{
		final int id = dto.getId();
		if(id < 0)
			throw new RoseException("invalie id " + id);
		return pathForType(dto) + "/" + id;
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
