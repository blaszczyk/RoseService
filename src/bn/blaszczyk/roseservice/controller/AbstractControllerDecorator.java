package bn.blaszczyk.roseservice.controller;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;

public abstract class AbstractControllerDecorator implements ModelController {

	protected final ModelController controller;
	
	public AbstractControllerDecorator(final ModelController controller)
	{
		this.controller = controller;
	}
	
	@Override
	public List<? extends Readable> getEntities(Class<? extends Readable> type) throws RoseException
	{
		return controller.getEntities(type);
	}
	
	@Override
	public int getEntityCount(Class<? extends Readable> type) throws RoseException
	{
		return controller.getEntityCount(type);
	}
	
	@Override
	public Readable getEntityById(Class<? extends Readable> type, int id) throws RoseException
	{
		return controller.getEntityById(type, id);
	}
	
	@Override
	public <T extends Readable> T createNew(Class<T> type) throws RoseException
	{
		return controller.createNew(type);
	}
	
	@Override
	public Writable createCopy(Writable entity) throws RoseException
	{
		return controller.createCopy(entity);
	}
	
	@Override
	public void update(Writable... entities) throws RoseException
	{
		controller.update(entities);
	}
	
	@Override
	public void delete(Writable entity) throws RoseException
	{
		controller.delete(entity);
	}
	
}
