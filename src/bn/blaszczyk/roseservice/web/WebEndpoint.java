package bn.blaszczyk.roseservice.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.client.RoseClient;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.server.Endpoint;
import bn.blaszczyk.roseservice.server.PathOptions;
import bn.blaszczyk.roseservice.tools.TypeManager;

public class WebEndpoint implements Endpoint {
	
	private static RoseClient client = new RoseClient();
	
	@Override
	public int get(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		try
		{
			String responseString = "";
			if(path.equals(""))
				responseString = HtmlTools.startPage();
			else
			{
				final PathOptions pathOptions = new PathOptions(path);
				if(!pathOptions.isValid())
					return HttpServletResponse.SC_NOT_FOUND;
				final Class<?> type = pathOptions.getType();
				final Entity entity = TypeManager.getEntity(type);
				if(pathOptions.hasId())
					if(pathOptions.hasOptions() && pathOptions.getOptions()[0].equals("update"))
						responseString = buildEntityEdit(entity, pathOptions.getId());
					else
						responseString = buildEntityView(entity, pathOptions.getId());
				else
					responseString = buildEntitiesList(entity);
			}
			response.getWriter().write(responseString);
			return HttpServletResponse.SC_OK;
		}
		catch (IOException e) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
	}
	
	@Override
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		final PathOptions pathOptions = new PathOptions(path);
		if(!pathOptions.isValid() || !pathOptions.hasOptions())
			return HttpServletResponse.SC_NOT_FOUND;
		try
		{
			String responseString = "";
			switch (pathOptions.getOptions()[0])
			{
			case "update":
				final RoseDto dto = new RoseDto(request.getParameterMap());
				client.putDto(dto);
				responseString = this.buildEntityView(TypeManager.getEntity(dto.getType()), dto.getId());
				break;
			case "create":
				// ...
				break;
			case "delete":
				// ...
				break;
			default:
				return HttpServletResponse.SC_NOT_FOUND;
			}
			response.getWriter().write(responseString);
			return HttpServletResponse.SC_OK;
		}
		catch (Exception e) 
		{
			// TODO: handle
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
	}
	
	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_BAD_REQUEST;
	}
	
	@Override
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_BAD_REQUEST;
	}
	
	private String buildEntitiesList(final Entity entity)
	{
		final List<RoseDto> dtos = client.getDtos(entity.getObjectName());
		return HtmlTools.entityList(entity,dtos);
	}

	private String buildEntityEdit(final Entity entity, final int id)
	{
		final RoseDto dto = client.getDto(entity.getObjectName(), id);
		return HtmlTools.entityEdit(entity, dto);
	}

	private String buildEntityView(final Entity entity, final int id)
	{
		String responseString;
		final RoseDto dto = client.getDto(entity.getObjectName(), id);
		final List<List<RoseDto>> subDtos = new ArrayList<>(entity.getEntityFields().size());
		for(int i = 0; i < entity.getEntityFields().size(); i++)
		{
			final EntityField field = entity.getEntityFields().get(i);
			final String subEntityName = field.getEntity().getObjectName();
			if(field.getType().isSecondMany())
			{
				subDtos.add(client.getDtos(subEntityName,dto.getEntityIds(i)));
			}
			else
			{
				final int subId = dto.getEntityId(i);
				if(subId < 0)
					subDtos.add(Collections.emptyList());
				else
					subDtos.add(Collections.singletonList(client.getDto(subEntityName, subId)));
			}
		}
		responseString = HtmlTools.entityView(entity, dto,subDtos);
		return responseString;
	}

}
