package bn.blaszczyk.roseservice.server;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.internal.StringMap;


import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.controller.ModelController;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.tools.TypeManager;

public class EntityEndpoint implements Endpoint {
	
	private static final Logger LOGGER = Logger.getLogger(EntityEndpoint.class);
	
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
		if(!pathOptions.isValid() || pathOptions.hasOptions())
			return HttpServletResponse.SC_NOT_FOUND;
		try
		{
			final Class<? extends Readable> type = pathOptions.getType();
			final List<RoseDto> dtos;
			if(pathOptions.getId()<0)
				dtos = controller.getEntities(type)
						.stream()
						.map(RoseDto::new)
						.collect(Collectors.toList());
			else
				dtos = pathOptions.getIds()
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
			final StringMap<?> stringMap = GSON.fromJson(request.getReader(), StringMap.class);
			final RoseDto dto = new RoseDto(stringMap);
			LOGGER.debug("posting dto " + dto );
			if(!pathOptions.getType().equals(dto.getType()))
				return HttpServletResponse.SC_BAD_REQUEST;
			final Writable entity = (Writable) controller.createNew(dto.getType());
			update(entity, dto);
			response.getWriter().write(GSON.toJson(new RoseDto(entity)));
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
			final StringMap<?> stringMap = GSON.fromJson(request.getReader(), StringMap.class);
			final RoseDto dto = new RoseDto(stringMap);
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
	
	private void update(final Writable entity, final RoseDto dto) throws RoseException
	{
		try
		{
			final Entity entityModel = TypeManager.getEntity(entity);
			for(int i = 0; i < entityModel.getFields().size(); i++)
			{
				final Field field = entityModel.getFields().get(i);
				final String dtoValue = dto.getFieldValue(i);
				final Object value = getPrimitiveValue(field, dtoValue);
				entity.setField(i, value);
			}
			for(int i = 0; i < entity.getEntityCount(); i++)
			{
				if(entity.getRelationType(i).isSecondMany())
					updateMany(entity,i,dto.getEntityIds(i));
				else
					updateOne(entity,i,dto.getEntityId(i));
			}
			controller.update(entity);
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"error updating entity with dto " + dto.toString());
		}
	}
	
	private Object getPrimitiveValue(final Field field, final String dtoValue) throws RoseException
	{
		if(dtoValue == null || dtoValue.equals("null"))
			return null;
		try
		{
			if(field instanceof EnumField)
			{
				final Class<?> enumType = TypeManager.getClass(((EnumField) field).getEnumType());
				for(final Object value : enumType.getEnumConstants())
					if(value.toString().equals(dtoValue))
						return value;
			}
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField pField = (PrimitiveField) field;
				switch(pField.getType())
				{
				case BOOLEAN:
					return Boolean.parseBoolean(dtoValue);
				case DATE:
					return RoseDto.DATE_FORMAT.parse(dtoValue);
				case INT:
					return Integer.parseInt(dtoValue);
				case NUMERIC:
					final BigDecimal numValue = (BigDecimal) RoseDto.BIG_DEC_FORMAT.parse(dtoValue);
					checkNumeric(numValue,pField);
					return numValue;
				case CHAR:
				case VARCHAR:
					checkString(dtoValue,pField);
					return dtoValue;
				}
			}
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"Error parsing primitive '" + dtoValue + "' for " + field.getName());
		}
		return null;
	}

	private void updateOne(final Writable entity, final int index, final Integer entityId) throws RoseException
	{
		Writable subEntity = (Writable) entity.getEntityValueOne(index);
		if( subEntity == null || !subEntity.getId().equals(entityId))
		{
			if(entityId < 0)
				subEntity = null;
			else
				subEntity = (Writable) controller.getEntityById(entity.getEntityClass(index), entityId);
			entity.setEntity(index, subEntity);
			controller.update(subEntity);
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

	private void checkNumeric(final BigDecimal value, final PrimitiveField field) throws RoseException
	{
		final int precision = field.getLength1();
		final int scale = field.getLength2();
		if(value.scale() > scale)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong scale; max scale=" + scale);
		if(value.precision() > precision)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong precision; max precision=" + precision);
	}

	private void checkString(final String value, final PrimitiveField field) throws RoseException
	{
		final int length = field.getLength1();
		if(value.length() > length)
			throw new RoseException("string value " + field.getName() + "='" + value + "' too long; max_length=" + length);
	}

}
