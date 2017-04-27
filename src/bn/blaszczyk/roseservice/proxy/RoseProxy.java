package bn.blaszczyk.roseservice.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.tools.EntityUtils;
import bn.blaszczyk.roseservice.tools.TypeManager;

public class RoseProxy implements InvocationHandler {

	private static final Class<?>[] INTERFACES = new Class<?>[]{Writable.class};
	
	private static Logger LOGGER = Logger.getLogger(RoseProxy.class);
	
	public static Writable create(final RoseDto dto, final EntityAccess access) throws RoseException
	{
		final RoseProxy handler = new RoseProxy(dto, access);
		final ClassLoader loader = dto.getType().getClassLoader();
		final Writable proxy = (Writable) Proxy.newProxyInstance(loader, INTERFACES, handler);
		return proxy;
	}

	private final EntityAccess access;

	private final Entity entityModel;
	
	private final Writable entity;
	
	private final boolean[] fetched;
	private final List<List<Integer>> allIds;
	
	private RoseProxy(final RoseDto dto, final EntityAccess access) throws RoseException
	{
		try
		{
			this.access = access;
			entity = (Writable) dto.getType().newInstance();
			entityModel = TypeManager.getEntity(entity);
			
			fetched = new boolean[entity.getEntityCount()];
			Arrays.fill(fetched, false);
			allIds = new ArrayList<>(entity.getEntityCount());
			
			entity.setId(dto.getId());
			for(int i = 0; i < entity.getFieldCount(); i++)
				setPrimitive(i, dto.getFieldValue(i));
			for(int i = 0; i < entity.getEntityCount(); i++)
				setEntity(i, dto);
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw RoseException.wrap(e, "Error creating proxy for " + dto);
		}
	}
	
	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	{
		final int index = getFetchIndex(method, args);
		if(index >= 0)
			fetchIfRequired(index);
		return method.invoke(entity, args);
	}

	private void setPrimitive(final int index, final String dtoValue) throws RoseException
	{
		final Field field = entityModel.getFields().get(index);
		final Object value = EntityUtils.getPrimitiveValue(field, dtoValue);
		entity.setField(index, value);
	}

	private void setEntity(final int index, final RoseDto dto)
	{
		final List<Integer> ids;
		if(entity.getRelationType(index).isSecondMany())
			ids = dto.getEntityIds(index);
		else
			ids = Collections.singletonList(dto.getEntityId(index));
		allIds.set(index, ids);
	}
	
	private int getFetchIndex(final Method method, final Object[] args)
	{
		final String methodName = method.getName();
		if(! methodName.startsWith("get"))
			return -1;
		final String choppedName = methodName.substring(3);
		if(choppedName.equals("EntityValueOne") || choppedName.equals("EntityValueMany"))
			if(args.length == 1 && args[0] instanceof Integer)
				return (Integer)args[0];
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			final EntityField field = entityModel.getEntityFields().get(i);
			if( choppedName.equals( field.getCapitalName() + (field.getType().isSecondMany() ? "s" : "")  ) )
				return i;
		}
		return -1;
	}
	
	private void fetchIfRequired(final int index)
	{
		if(fetched[index])
			return;
		final EntityField field = entityModel.getEntityFields().get(index);
		final Class<? extends Readable> type = TypeManager.getClass(field.getEntity());
		final List<Integer> ids = allIds.get(index);
		try
		{
			if(field.getType().isSecondMany())
				access.getMany(type, ids)
						.stream()
						.forEach( e -> entity.addEntity(index, e));
			else
			{
				final int id = ids.get(0);
				if(id >= 0)
					entity.setEntity(index, access.getOne(type, id));
			}
			fetched[index] = true;
		}
		catch (RoseException e) 
		{
			LOGGER.error(String.format("Unable to fetch %s %s for %s", type.getSimpleName(),ids.toString(), EntityUtils.toStringSimple(entity) ), e);
		}
	}
	
}
