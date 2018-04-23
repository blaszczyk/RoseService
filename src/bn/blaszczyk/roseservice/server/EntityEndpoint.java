package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.*;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.DtoContainer;
import bn.blaszczyk.rose.model.DtoLinkType;
import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.controller.ModelController;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class EntityEndpoint implements Endpoint {

	private static final Logger LOGGER = LogManager.getLogger(EntityEndpoint.class);
	
	private static final Gson GSON = new Gson();
	
	private final ModelController controller;

	public EntityEndpoint(final ModelController controller)
	{
		this.controller = controller;
	}
	
	@Override
	public int get(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final String responseString;
			final DtoLinkType oneLinkType = getLinkType(request.getParameterMap().get("one"));
			final DtoLinkType manyLinkType = getLinkType(request.getParameterMap().get("many"));
			if(path == null || path.isEmpty())
			{
				final DtoContainer container = TypeManager.newDtoContainer();
				for(final Class<? extends Readable> type : TypeManager.getEntityClasses())
				{
					final String typeName = type.getSimpleName().toLowerCase();
					final List<Integer> ids = parseIds(request.getParameterValues(typeName));
					controller.getEntitiesByIds(type, ids)
						.stream()
						.map(e -> EntityUtils.toDto(e,oneLinkType,manyLinkType))
						.forEach(container::put);
				}
				responseString = GSON.toJson(container);
			}
			else
			{
				final PathOptions pathOptions = new PathOptions(path);
				if(!pathOptions.hasType())
					return HttpServletResponse.SC_NOT_FOUND;
				final Class<? extends Readable> type = pathOptions.getType();
				if(pathOptions.hasOptions())
					switch (pathOptions.getOptions()[0])
					{
					case "count":
						final Map<String, String> query = transformQuery(request);
						responseString = Integer.toString(controller.getEntityCount(type,query));
						break;
					case "id":
						final List<Integer> ids = controller.getIds(type);
						responseString = GSON.toJson(ids);
						break;
					default:
						return HttpServletResponse.SC_NOT_FOUND;
					}
				else if(pathOptions.hasId())
				{
					final Readable entity = controller.getEntityById(type, pathOptions.getId());
					final Dto dto = EntityUtils.toDto(entity,oneLinkType,manyLinkType);
					responseString = GSON.toJson(dto);
				}
				else
				{
					final String[] queryId = request.getParameterMap().get("id");
					final List<Dto> dtos;
					if(queryId == null)
					{
						final Map<String, String> query = transformQuery(request);
						dtos = controller.getEntities(type,query)
							.stream()
							.map(e -> EntityUtils.toDto(e,oneLinkType,manyLinkType))
							.collect(Collectors.toList());
					}
					else
					{
						final List<Integer> ids = parseIds(queryId);
						dtos = controller.getEntitiesByIds(type, ids)
							.stream()
							.map(e -> EntityUtils.toDto(e,oneLinkType,manyLinkType))
							.collect(Collectors.toList());
					}
					responseString = GSON.toJson(dtos);
				}
			}
			response.getWriter().write(responseString);
			response.setHeader("Content", "application/json");
			return HttpServletResponse.SC_OK;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e,"error handling GET request");
		}
	}

	@Override
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final PathOptions pathOptions = new PathOptions(path);
			if(!pathOptions.hasType() || pathOptions.hasId() || pathOptions.hasOptions())
				return HttpServletResponse.SC_NOT_FOUND;
			
			final Dto dto = getRequestDto(request, pathOptions.getType());
			LOGGER.debug("posting dto " + dto );

			final Writable entity = (Writable) controller.createNew(TypeManager.getClass(dto).asSubclass(Writable.class));
			update(entity, dto);
			
//			TODO: test sth. like this:
//			final Writable entity = RoseProxy.create(dto, new EntityAccessAdapter(controller));
//			controller.createNew(entity);
						
			final String responseString = GSON.toJson(EntityUtils.toDto(entity));
			response.getWriter().write(responseString);
			return HttpServletResponse.SC_CREATED;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e,"error handling POST request");
		}
	}
	
	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final PathOptions pathOptions = new PathOptions(path);
			if(!pathOptions.hasType())
				return HttpServletResponse.SC_NOT_FOUND;
			final Dto dto = getRequestDto(request,pathOptions.getType());
			LOGGER.debug("putting dto " + dto);
			if(pathOptions.getId() != dto.getId())
				return HttpServletResponse.SC_BAD_REQUEST;
			final Writable entity = (Writable) controller.getEntityById(TypeManager.getClass(dto), dto.getId());
			update(entity, dto);
			return HttpServletResponse.SC_NO_CONTENT;
		}
		catch (Exception e) 
		{
			throw RoseException.wrap(e,"error handling PUT request");
		}
	}
	
	@Override
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final PathOptions pathOptions = new PathOptions(path);
			if(!pathOptions.hasType())
				return HttpServletResponse.SC_NOT_FOUND;
			if(!pathOptions.hasId() || pathOptions.hasOptions())
				return HttpServletResponse.SC_NOT_FOUND;
			final Writable entity = (Writable) controller.getEntityById(pathOptions.getType(), pathOptions.getId());
			controller.delete(entity);
			return HttpServletResponse.SC_NO_CONTENT;
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"error handling DELETE request");
		}
	}

	private Dto getRequestDto(final HttpServletRequest request, final Class<? extends Readable> type) throws IOException, UnsupportedEncodingException, RoseException
	{
		final String requestString = request.getReader().lines().collect(Collectors.joining("\r\n"));
		return GSON.fromJson(requestString, TypeManager.getDtoClass(type));
	}
	
	private void update(final Writable entity, final Dto dto) throws RoseException
	{
		try
		{
			final EntityModel entityModel = TypeManager.getEntityModel(entity);
			for(int i = 0; i < entityModel.getFields().size(); i++)
			{
				final Field field = entityModel.getFields().get(i);
				final Object dtoValue = dto.getFieldValue(field.getName());
				final Object value = EntityUtils.toEntityValue(field, dtoValue);
				entity.setField(i, value);
			}
			for(int i = 0; i < entity.getEntityCount(); i++)
			{
				final String fieldName = entity.getEntityName(i);
				if(entity.getRelationType(i).isSecondMany())
					updateMany(entity,i,dto.getEntityIds(fieldName));
				else
					updateOne(entity,i,dto.getEntityId(fieldName));
			}
			controller.update(entity);
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"error updating entity with dto " + dto.toString());
		}
	}

	private void updateOne(final Writable entity, final int index, final Integer entityId) throws RoseException
	{
		final Writable oldEntity = (Writable) entity.getEntityValueOne(index);
		if( oldEntity == null || oldEntity.getId() != entityId)
		{
			final Writable newEntity;
			if(entityId < 0)
				newEntity = null;
			else
				newEntity = (Writable) controller.getEntityById(entity.getEntityClass(index), entityId);
			entity.setEntity(index, newEntity);
			if(oldEntity != null)
				controller.update(oldEntity);
			if(newEntity != null)
				controller.update(newEntity);
		}
	}

	private void updateMany(final Writable entity, final int index, final Integer[] entityIds) throws RoseException
	{
		if(entityIds == null)
			return;
		final Set<Integer> ids = Arrays.stream(entityIds).collect(Collectors.toSet());
		final Set<? extends Readable> subEntities = new HashSet<>(entity.getEntityValueMany(index));
		for(final Readable subEntity : subEntities)
			if(ids.contains(subEntity.getId()))
				ids.remove(subEntity.getId());
			else
			{
				entity.removeEntity(index, (Writable) subEntity);
				controller.update((Writable)subEntity);
			}
		for(final Integer id : ids)
		{
			final Writable subEntity = (Writable) controller.getEntityById(entity.getEntityClass(index), id);
			entity.addEntity(index, subEntity);
			controller.update(subEntity);
		}
	}

	private Map<String, String> transformQuery(final HttpServletRequest request)
	{
		final Map<String,String> query = request.getParameterMap().entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, e -> 
					Arrays.stream(e.getValue()).collect(Collectors.joining(","))
				));
		return query;
	}

	private static List<Integer> parseIds(final String[] parameterValues)
	{
		if(parameterValues == null || parameterValues.length == 0)
			return Collections.emptyList();
		return Arrays.stream(parameterValues)
			.filter(s -> s != null)
			.map(s -> s.split("\\D+"))
			.flatMap(Arrays::stream)
			.map(String::trim)
			.filter(s -> !s.isEmpty())
			.map(Integer::parseInt)
			.collect(Collectors.toList());
	}

	private static DtoLinkType getLinkType(final String[] parameterValues)
	{
		if(parameterValues == null || parameterValues.length == 0)
			return DtoLinkType.NONE;
		final DtoLinkType type = DtoLinkType.valueOf(parameterValues[0].toUpperCase());
		return type != null ? type : DtoLinkType.NONE;
	}

}
