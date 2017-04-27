package bn.blaszczyk.roseservice.controller;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;

public interface ModelController 
{
	
	public List<? extends Readable> getEntities(Class<? extends Readable> type) throws RoseException;
	
	public int getEntityCount(Class<? extends Readable> type) throws RoseException;
	
	public Readable getEntityById(Class<? extends Readable> type, int id) throws RoseException;
	
	public <T extends Readable> T createNew(Class<T> type) throws RoseException;
	
	public Writable createCopy(Writable entity) throws RoseException;
	
	public void update(Writable... entities) throws RoseException;

	public void delete(Writable entity) throws RoseException;
	
}