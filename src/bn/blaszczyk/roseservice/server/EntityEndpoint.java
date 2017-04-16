package bn.blaszczyk.roseservice.server;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;

import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.controller.HibernateController;
import bn.blaszczyk.roseservice.model.RoseDto;

public class EntityEndpoint implements Endpoint {
	
	private final static Gson GSON = new Gson();
	
	private final HibernateController controller;

	public EntityEndpoint(final HibernateController controller)
	{
		this.controller = controller;
	}
	
	@Override
	public int get(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		final PathOptions options = new PathOptions(path);
		if(!options.isValid())
		{
			return HttpServletResponse.SC_NOT_FOUND;
		}
		try
		{
			final Class<?> type = options.getType();
			final List<RoseDto> dtos;
			if(options.getId()<0)
				dtos = controller.getEntites(type)
						.stream()
						.map(RoseDto::new)
						.collect(Collectors.toList());
			else
				dtos = options.getIds()
						.stream()
						.map(i -> controller.getEntityById(type, i))
						.filter(e -> e != null)
						.map(RoseDto::new)
						.collect(Collectors.toList());
			response.getWriter().write(GSON.toJson(dtos));
			return HttpServletResponse.SC_OK;
		}
		catch (Exception e) 
		{
			throw new RoseException("error handling GET request", e);
		}
	}
	
	@Override
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		System.out.println("POST");
		return HttpServletResponse.SC_CREATED;
	}
	
	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final StringMap<?> stringMap = GSON.fromJson(request.getReader(), StringMap.class);
			final RoseDto dto = new RoseDto(stringMap);
			final Writable entity = (Writable) controller.getEntityById(dto.getType(), dto.getId());
			// TODO: update
			controller.update(entity);
			controller.commit();
			return HttpServletResponse.SC_NO_CONTENT;
		}
		catch (Exception e) 
		{
			throw new RoseException("error handling PUT request", e);
		}
	}
	
	@Override
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		System.out.println("DELETE");
		return HttpServletResponse.SC_NO_CONTENT;
	}

}
