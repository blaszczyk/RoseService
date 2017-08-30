package bn.blaszczyk.roseservice.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rosecommon.client.RoseClient;
import bn.blaszczyk.rosecommon.client.ServiceConfigClient;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.tools.TypeManager;
import bn.blaszczyk.roseservice.server.Endpoint;
import bn.blaszczyk.roseservice.server.PathOptions;

public class WebEndpoint implements Endpoint {
	
	private final RoseClient client;
	private final ServiceConfigClient serviceConfigClient;
	
	public WebEndpoint(final String url)
	{
		client = new RoseClient(url);
		serviceConfigClient = ServiceConfigClient.newInstance(url);
	}
	
	@Override
	public int get(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		try
		{
			String responseString = "";
			if(path.equals(""))
				responseString = HtmlTools.startPage();
			else if(path.equals("server"))
				responseString = buildServerControls();
			else
			{
				final PathOptions pathOptions = new PathOptions(path);
				if(!pathOptions.isValid())
					return HttpServletResponse.SC_NOT_FOUND;
				final Class<? extends Readable> type = pathOptions.getType();
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
		try
		{
			String responseString = "";
			if(path.equals("stop"))
			{
				serviceConfigClient.postStopRequest();
				responseString = new HtmlBuilder().h2("Server stopped").build();
			}
			else if(path.equals("restart"))
			{
				serviceConfigClient.postRestartRequest();
				responseString = new HtmlBuilder().h2("Server restarting").append(HtmlTools.linkToWeb("go to start")).build();
			}
			else if(path.equals("server"))
			{
				final PreferenceDto dto = new PreferenceDto(request.getParameterMap());
				serviceConfigClient.putPreferences(dto);
				responseString = buildServerControls();
			}
			else
			{
				final PathOptions pathOptions = new PathOptions(path);
				if(!pathOptions.isValid() || !pathOptions.hasOptions())
					return HttpServletResponse.SC_NOT_FOUND;
				
				switch (pathOptions.getOptions()[0])
				{
				case "update":
					final RoseDto updateDto = new RoseDto(request.getParameterMap());
					client.putDto(updateDto);
					responseString = buildEntityView(TypeManager.getEntity(updateDto.getType()), updateDto.getId());
					break;
				case "create":
					RoseDto createDto = new RoseDto(request.getParameterMap());
					createDto = client.postDto(createDto);
					responseString = buildEntityEdit(TypeManager.getEntity(createDto.getType()), createDto.getId());
					break;
				case "delete":
					client.deleteByID(pathOptions.getType().getSimpleName().toLowerCase(), pathOptions.getId());
					responseString = buildEntitiesList(TypeManager.getEntity(pathOptions.getType()));
					break;
				default:
					return HttpServletResponse.SC_NOT_FOUND;
				}
			}
			response.getWriter().write(responseString);
			return HttpServletResponse.SC_OK;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e, "error on POST@" + path);
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
	
	@Override
	public Map<String, String> status()
	{
		final Map<String,String> status = new HashMap<>();
		status.put("endpoint /web", "active");
		return status;
	}
	
	private String buildEntitiesList(final Entity entity) throws RoseException
	{
		final List<RoseDto> dtos = client.getDtos(entity.getObjectName());
		return HtmlTools.entityList(entity,dtos);
	}

	private String buildEntityEdit(final Entity entity, final int id) throws RoseException
	{
		final RoseDto dto = client.getDto(entity.getObjectName(), id);
		return HtmlTools.entityEdit(entity, dto);
	}

	private String buildEntityView(final Entity entity, final int id) throws RoseException
	{
		final RoseDto dto = client.getDto(entity.getObjectName(), id);
		final List<List<RoseDto>> subDtos = new ArrayList<>(entity.getEntityFields().size());
		for(final EntityField field : entity.getEntityFields())
		{
			final String subEntityName = field.getEntity().getObjectName();
			final String fieldName = field.getName();
			if(field.getType().isSecondMany())
			{
				subDtos.add(client.getDtos(subEntityName,dto.getEntityIds(fieldName)));
			}
			else
			{
				final int subId = dto.getEntityId(fieldName);
				if(subId < 0)
					subDtos.add(Collections.emptyList());
				else
					subDtos.add(Collections.singletonList(client.getDto(subEntityName, subId)));
			}
		}
		return HtmlTools.entityView(entity, dto,subDtos);
	}
	
	private String buildServerControls() throws RoseException
	{
		final Map<String, String> status = serviceConfigClient.getServerStatus();
		final PreferenceDto preferences = serviceConfigClient.getPreferences();
		return HtmlTools.serverControls(status,preferences);
	}

}
