package bn.blaszczyk.roseservice.proxy;

import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;

import java.util.List;

import bn.blaszczyk.rose.model.Readable;

public interface EntityAccess {
	
	public Writable getOne(final Class<? extends Readable> type, final int id) throws RoseException;
	
	public List<Writable> getMany(final Class<? extends Readable> type, final List<Integer> ids) throws RoseException;
	
}
