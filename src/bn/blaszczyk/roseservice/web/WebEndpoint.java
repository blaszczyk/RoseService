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
				{
					responseString = buildEntityView(entity, pathOptions.getId());
				}
				else
				{
					final List<RoseDto> dtos = client.getDtos(type.getSimpleName().toLowerCase());
					responseString = HtmlTools.entityList(entity,dtos);
				}
			}

			response.getWriter().write(responseString);
			return HttpServletResponse.SC_OK;
		}
		catch (IOException e) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
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

	@Override
	public int post(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
	@Override
	public int put(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
	@Override
	public int delete(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
}
