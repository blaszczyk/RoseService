package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.*;

import com.google.gson.Gson;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.rose.model.Dto;
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
		final PathOptions pathOptions = new PathOptions(path);
		if(!pathOptions.isValid())
			return HttpServletResponse.SC_NOT_FOUND;
		try
		{
			final Class<? extends Readable> type = pathOptions.getType();
			final String responseString;
			if(pathOptions.hasOptions())
				switch (pathOptions.getOptions()[0])
				{
				case "count":
					responseString = Integer.toString(controller.getEntityCount(type));
					break;
				case "id":
					final List<Integer> ids = controller.getIds(type);
					responseString = GSON.toJson(ids);
					break;
				default:
					return HttpServletResponse.SC_NOT_FOUND;
				}
			else
			{
				final List<Dto> dtos;
				if(pathOptions.hasId())
					dtos = controller.getEntitiesByIds(type, pathOptions.getIds())
						.stream()
						.map(EntityUtils::toDtoSilent)
						.collect(Collectors.toList());
				else
					dtos = controller.getEntities(type)
						.stream()
						.map(EntityUtils::toDtoSilent)
						.collect(Collectors.toList());
				responseString = GSON.toJson(dtos);
			}
			response.getWriter().write(responseString);
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
			if(!pathOptions.isValid() || pathOptions.hasId() || pathOptions.hasOptions())
				return HttpServletResponse.SC_NOT_FOUND;
			
			final Dto dto = getRequestDto(request, pathOptions.getType());
			LOGGER.debug("posting dto " + dto );
			final Writable entity = (Writable) controller.createNew(TypeManager.getClass(dto));
			update(entity, dto);
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
			if(!pathOptions.isValid())
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
			if(!pathOptions.isValid())
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

	private void updateMany(final Writable entity, final int index, final int[] entityIds) throws RoseException
	{
		final Set<Integer> ids = Arrays.stream(entityIds).mapToObj(Integer::new).collect(Collectors.toSet());
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

}
