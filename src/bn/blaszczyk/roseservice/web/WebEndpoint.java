package bn.blaszczyk.roseservice.web;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rosecommon.client.RoseClient;
import bn.blaszczyk.rosecommon.client.ServiceConfigClient;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
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
				final EntityModel entityModel = TypeManager.getEntityModel(type);
				if(pathOptions.hasId())
					if(pathOptions.hasOptions() && pathOptions.getOptions()[0].equals("update"))
						responseString = buildEntityEdit(entityModel, pathOptions.getId());
					else
						responseString = buildEntityView(entityModel, pathOptions.getId());
				else
					responseString = buildEntitiesList(entityModel);
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
					final Dto updateDto = toDto(request.getParameterMap());
					client.putDto(updateDto);
					responseString = buildEntityView(TypeManager.getEntityModel(updateDto), updateDto.getId());
					break;
				case "create":
					Dto createDto = toDto(request.getParameterMap());
					createDto = client.postDto(createDto);
					responseString = buildEntityEdit(TypeManager.getEntityModel(createDto), createDto.getId());
					break;
				case "delete":
					client.deleteByID(pathOptions.getType().getSimpleName().toLowerCase(), pathOptions.getId());
					responseString = buildEntitiesList(TypeManager.getEntityModel(pathOptions.getType()));
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
	
	private String buildEntitiesList(final EntityModel entityModel) throws RoseException
	{
		final List<Dto> dtos = client.getDtos(entityModel.getObjectName(), TypeManager.getClass(entityModel));
		return HtmlTools.entityList(entityModel,dtos);
	}

	private String buildEntityEdit(final EntityModel entityModel, final int id) throws RoseException
	{
		final Dto dto = client.getDto(TypeManager.getClass(entityModel), id);
		return HtmlTools.entityEdit(entityModel, dto);
	}

	private String buildEntityView(final EntityModel entityModel, final int id) throws RoseException
	{
		final Class<? extends Readable> type = TypeManager.getClass(entityModel);
		final Dto dto = client.getDto(type, id);
		final List<List<Dto>> subDtos = new ArrayList<>(entityModel.getEntityFields().size());
		for(final EntityField field : entityModel.getEntityFields())
		{
			final String fieldName = field.getName();
			final Class<? extends Readable> subType = TypeManager.getClass(field.getEntityModel());
			if(field.getType().isSecondMany())
			{
				subDtos.add(client.getDtos(subType,dto.getEntityIds(fieldName)));
			}
			else
			{
				final int subId = dto.getEntityId(fieldName);
				if(subId < 0)
					subDtos.add(Collections.emptyList());
				else
					subDtos.add(Collections.singletonList(client.getDto(subType, subId)));
			}
		}
		return HtmlTools.entityView(entityModel, dto,subDtos);
	}
	
	private String buildServerControls() throws RoseException
	{
		final Map<String, String> status = serviceConfigClient.getServerStatus();
		final PreferenceDto preferences = serviceConfigClient.getPreferences();
		return HtmlTools.serverControls(status,preferences);
	}

	
	private Dto toDto(final Map<String, String[]> parameterMap) throws RoseException
	{
		try
		{
			final String type = parameterMap.get("type")[0];
			final Dto dto = TypeManager.newDtoInstance(type);
			if(parameterMap.containsKey("id"))
				dto.setId(Integer.parseInt(parameterMap.get("id")[0]));
			final EntityModel entityModel = TypeManager.getEntityModel(dto);
			for(final Field field : entityModel.getFields())
			{
				final String stringValue = parameterMap.get(field.getName())[0];
				if(field instanceof EnumField)
					dto.setFieldValue(field.getName(), enumValue(field, stringValue));
				else if(field instanceof PrimitiveField)
					dto.setFieldValue(field.getName(), primitiveValue(field, stringValue));
			}
			for(final EntityField field : entityModel.getEntityFields())
			{
				final String stringValue = parameterMap.get(field.getName())[0];
				if(field.getType().isSecondMany())
					dto.setEntityIds(field.getName(), parseIds(stringValue));
				else
					dto.setEntityId(field.getName(), Integer.parseInt(stringValue));
			}
			return dto;
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "error converting parameter map to Dto");
		}
	}

	private int[] parseIds(final String stringValue)
	{
		return Arrays.stream(stringValue.split("\\,"))
				.filter(s -> !s.isEmpty())
				.mapToInt(Integer::parseInt)
				.toArray();
	}

	private Object primitiveValue(final Field field, final String stringValue) throws RoseException
	{
		switch(((PrimitiveField)field).getType())
		{
		case BOOLEAN:
			return Boolean.parseBoolean(stringValue);
		case DATE:
			try
			{
				final Date date = HtmlTools.DATE_FORMAT.parse(stringValue);
				return date.getTime();
			}
			catch (ParseException e)
			{
				throw new RoseException("error parsing date " + stringValue, e);
			}
		case INT:
			return Integer.parseInt(stringValue);
		case NUMERIC:
		case VARCHAR:
		case CHAR:
			return stringValue;
		}
		return null;
	}

	private Object enumValue(final Field field, final String stringValue)
	{
		final Class<?> enumType = TypeManager.getClass(((EnumField) field).getEnumType());
		for(final Object value : enumType.getEnumConstants())
			if(value.toString().equals(stringValue))
				return value;
		return null;
	}

}
