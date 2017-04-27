
package bn.blaszczyk.roseservice.controller;
import java.util.*;

import org.apache.log4j.Logger;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.tools.EntityUtils;
import bn.blaszczyk.roseservice.tools.TypeManager;

public class CacheController extends AbstractControllerDecorator implements ModelController {
	
	private static final Logger LOGGER = Logger.getLogger(CacheController.class);

	private final Map<Class<?>,Map<Integer,Readable>> allEntities = new HashMap<>();

	public CacheController(final ModelController controller)
	{
		super(controller);
		for(Class<? extends Readable> type : TypeManager.getEntityClasses())
			allEntities.put(type, new TreeMap<>());
	}

	@Override
	public List<Readable> getEntities(final Class<? extends Readable> type) throws RoseException
	{
		final Map<Integer,Readable> entities = allEntities.get(type);
		if(entities.isEmpty())
			synchronize(type);
		return Collections.unmodifiableList(new ArrayList<Readable>(entities.values()));
	}

	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		return allEntities.get(type).size();
	}

	@Override
	public Readable getEntityById(Class<? extends Readable> type, int id) throws RoseException
	{
		if(!hasEntityId(type, id))
			return addEntity(controller.getEntityById(type, id));
		return allEntities.get(type).get(id);
	}

	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		final T entity = controller.createNew(type);
		addEntity(entity);
		LOGGER.info("buffering new entity: " + EntityUtils.toStringSimple(entity));
		return entity;
	}

	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final Writable copy = controller.createCopy(entity);
		addEntity(copy);
		return copy;
	}

	@Override
	public void delete(Writable entity) throws RoseException
	{
		allEntities.get(TypeManager.convertType(entity.getClass())).remove(entity);
		LOGGER.debug("entity removed from buffer: " + EntityUtils.toStringSimple(entity));
	}
	
	public void synchronize(final Class<? extends Readable> type) throws RoseException
	{
		final Map<Integer,Readable> entities = allEntities.get(type);
		entities.clear();
		controller.getEntities(type)
			.stream()
			.forEach( e -> entities.put(e.getId(), e));
	}

	public void clearEntities(final Class<?> type)
	{
		allEntities.get(type).clear();
	}
	
	private boolean hasEntityId(final Class<? extends Readable> type, final Integer id)
	{
		return allEntities.get(type).containsKey(id);
	}

	private Readable addEntity(final Readable entity)
	{
		final Map<Integer,Readable> entities = allEntities.get(TypeManager.getClass(entity));
		final Integer id = entity.getId();
		if(entities.containsKey(id))
		{
			LOGGER.error("Trying to add entity with duplicate id: " + EntityUtils.toStringSimple(entity));
			return entities.get(id);
		}
		entities.put(id, entity);
		return entity;
	}

}
