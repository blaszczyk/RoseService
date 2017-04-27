package bn.blaszczyk.roseservice.controller;

import java.util.ArrayList;
import java.util.List;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.client.RoseClient;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.proxy.EntityAccess;
import bn.blaszczyk.roseservice.proxy.RoseProxy;

public class RestController implements ModelController, EntityAccess {
	
	private final RoseClient client;
	private EntityAccess access = this;
	
	public RestController(final RoseClient client)
	{
		this.client = client;
	}
	
	public void setEntityAccess(final EntityAccess access)
	{
		this.access = access;
	}
	
	@Override
	public List<? extends Readable> getEntities(final Class<? extends Readable> type) throws RoseException
	{
		final List<RoseDto> dtos = client.getDtos(type.getSimpleName());
		return createProxys(dtos);
	}
	
	@Override
	public int getEntityCount(final Class<? extends Readable> type) throws RoseException
	{
		return client.getCount(type.getSimpleName().toLowerCase());
	}
	
	@Override
	public Readable getEntityById(final Class<? extends Readable> type, int id) throws RoseException
	{
		final RoseDto dto = client.getDto(type.getSimpleName().toLowerCase(), id);
		return RoseProxy.create(dto, access);
	}
	
	@Override
	public <T extends Readable> T createNew(final Class<T> type) throws RoseException
	{
		try
		{
			final T entity = type.newInstance();
			final RoseDto dto = new RoseDto(entity);
			client.postDto(dto);
			return entity;
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e, "Error creating new " + type.getSimpleName());
		}
	}
	
	@Override
	public Writable createCopy(final Writable entity) throws RoseException
	{
		final RoseDto dto = new RoseDto(entity);
		client.postDto(dto);
		return entity;
	}
	
	@Override
	public void update(final Writable... entities) throws RoseException
	{
		for(final Writable entity : entities)
			client.putDto(new RoseDto(entity));
	}
	
	@Override
	public void delete(final Writable entity) throws RoseException
	{
		client.deleteByID(entity.getEntityName().toLowerCase(), entity.getId());
	}

	@Override
	public Writable getOne(final Class<? extends Readable> type, final int id) throws RoseException
	{
		return (Writable) getEntityById(type, id);
	}

	@Override
	public List<Writable> getMany(Class<? extends Readable> type, final List<Integer> ids) throws RoseException
	{
		final List<RoseDto> dtos = client.getDtos(type.getSimpleName().toLowerCase(), ids);
		return createProxys(dtos);
	}

	private List<Writable> createProxys(final List<RoseDto> dtos) throws RoseException
	{
		final List<Writable> entities = new ArrayList<>(dtos.size());
		for(final RoseDto dto : dtos)
			entities.add(RoseProxy.create(dto, access));
		return entities;
	}
	
}
