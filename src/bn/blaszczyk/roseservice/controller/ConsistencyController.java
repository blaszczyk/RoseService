package bn.blaszczyk.roseservice.controller;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;

public class ConsistencyController extends AbstractControllerDecorator implements ModelController {
	
	public ConsistencyController(final ModelController controller)
	{
		super(controller);
	}

	@Override
	public void update(Writable... entities) throws RoseException
	{
		final Date date = new Date();
		for(final Writable entity : entities)
			if(entity instanceof Timestamped)
				((Timestamped)entity).setTimestamp(date);
		super.update(entities);
	}
	
	@Override
	public void delete(Writable entity) throws RoseException
	{
		if(entity == null)
			return;
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			if(entity.getRelationType(i).isSecondMany())
			{
				final Set<? extends Readable> set = new TreeSet<>( entity.getEntityValueMany(i));
				for(Readable subEntity : set)
					if(subEntity != null)
						entity.removeEntity(i, (Writable) subEntity);
			}
			else
				if(entity.getEntityValueOne(i) != null)
					entity.setEntity(i, null);
		}
		super.delete(entity);
	}
	
	// TODO: other (manual) consistency checks
	
}
