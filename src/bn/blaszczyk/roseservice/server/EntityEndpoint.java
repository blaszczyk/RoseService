package bn.blaszczyk.roseservice.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.*;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;


import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.controller.ModelController;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.tools.EntityUtils;
import bn.blaszczyk.rosecommon.tools.TypeManager;

import static bn.blaszczyk.rosecommon.client.CommonClient.CODING_CHARSET;

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
				final List<RoseDto> dtos;
				if(pathOptions.hasId())
					dtos = pathOptions.getIds()
										.stream()
										.map(i -> getById(type, i))
										.filter(e -> e != null)
										.map(RoseDto::new)
										.collect(Collectors.toList());
				else
					dtos = controller.getEntities(type)
										.stream()
										.map(RoseDto::new)
										.collect(Collectors.toList());
				responseString = GSON.toJson(dtos);
			}
			writeEncodedResponse(response, responseString);
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
			
			final RoseDto dto = getRequestDto(request);
			LOGGER.debug("posting dto " + dto );
			if(!pathOptions.getType().equals(dto.getType()))
				return HttpServletResponse.SC_BAD_REQUEST;
			final Writable entity = (Writable) controller.createNew(dto.getType());
			update(entity, dto);
			final String responseString = GSON.toJson(new RoseDto(entity));
			writeEncodedResponse(response, responseString);
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
			final RoseDto dto = getRequestDto(request);
			LOGGER.debug("putting dto " + dto);
			if(pathOptions.getId() != dto.getId() || !pathOptions.getType().equals(dto.getType()))
				return HttpServletResponse.SC_BAD_REQUEST;
			final Writable entity = (Writable) controller.getEntityById(dto.getType(), dto.getId());
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

	@Override
	public Map<String, String> status()
	{
		final Map<String,String> status = new HashMap<>();
		status.put("endpoint /entity", "active");
		status.put("database connection", "active");
		return status;
	}

	private RoseDto getRequestDto(final HttpServletRequest request)	throws IOException, UnsupportedEncodingException, RoseException
	{
		final String encodedRequestString = request.getReader().lines().collect(Collectors.joining("\r\n"));
		final String requestString = URLDecoder.decode(encodedRequestString, CODING_CHARSET);
		final StringMap<?> stringMap = GSON.fromJson(requestString, StringMap.class);
		final RoseDto dto = new RoseDto(stringMap);
		return dto;
	}
	
	private void writeEncodedResponse(final HttpServletResponse response, final String responseString) throws UnsupportedEncodingException, IOException
	{
		final String encodedResponseString = URLEncoder.encode(responseString, CODING_CHARSET);
		response.getWriter().write(encodedResponseString);
	}
	
	private Readable getById(final Class<? extends Readable> type, final int id)
	{
		try
		{
			return controller.getEntityById(type, id);
		}
		catch (RoseException e) 
		{
			LOGGER.error("Error getting " + type.getSimpleName() + " with id=" + id, e);
			return null;
		}
	}
	
	private void update(final Writable entity, final RoseDto dto) throws RoseException
	{
		try
		{
			final Entity entityModel = TypeManager.getEntity(entity);
			for(int i = 0; i < entityModel.getFields().size(); i++)
			{
				final Field field = entityModel.getFields().get(i);
				final String dtoValue = dto.getFieldValue(field.getName());
				final Object value = EntityUtils.getPrimitiveValue(field, dtoValue);
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
		if( oldEntity == null || !oldEntity.getId().equals(entityId))
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

	private void updateMany(final Writable entity, final int index, final List<Integer> entityIds) throws RoseException
	{
		final Set<Integer> ids = new TreeSet<>(entityIds);
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
