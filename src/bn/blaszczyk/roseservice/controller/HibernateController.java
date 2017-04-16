
package bn.blaszczyk.roseservice.controller;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.*;

import javax.swing.Timer;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Restrictions;
import org.hibernate.impl.SessionImpl;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Timestamped;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.tools.EntityUtils;
import bn.blaszczyk.roseservice.tools.TypeManager;

import static bn.blaszczyk.roseservice.tools.Preferences.*;

public class HibernateController {
	
	private static final Logger LOGGER = Logger.getLogger(HibernateController.class);
	private static final Calendar calendar = Calendar.getInstance();

	private static final String KEY_URL = "hibernate.connection.url";
	private static final String KEY_USER = "hibernate.connection.username";
	private static final String KEY_PW = "hibernate.connection.password";

	private static final String TIMESTAMP = "timestamp";
	
	private SessionFactory sessionFactory;
	private Session session;

	private final Map<Class<?>,List<Readable>> entityLists = new HashMap<>();
	private final Set<Writable> changedEntitys = new LinkedHashSet<>();
	private String dbFullUrl;
	private String dbMessage;
	private boolean connected = false;
	private boolean lockSession = false;
	private int newEntityCount = -1;
	private Timer timer = new Timer(5000, e -> checkConnection(e));

	public HibernateController()
	{
		configureDataBase();
		for(Class<? extends Readable> type : TypeManager.getEntityClasses())
			entityLists.put(type, new ArrayList<>());
		timer.setInitialDelay(1000);
		timer.start();
	}

	public void configureDataBase()
	{
		String dburl = getStringValue(DB_HOST,null);
		String dbport = getStringValue(DB_PORT,null);
		String dbname = getStringValue(DB_NAME,null);
		String dbuser = getStringValue(DB_USER,null);
		String dbpassword = getStringValue(DB_PASSWORD,null);

		Configuration configuration = new AnnotationConfiguration().configure();
		if(dburl != null && dbport != null && dbname != null)
			configuration.setProperty(KEY_URL, String.format("jdbc:mysql://%s:%s/%s",dburl,dbport,dbname));
		if(dbuser != null)
			configuration.setProperty(KEY_USER, dbuser);
		if(dbpassword != null)
			configuration.setProperty(KEY_PW, dbpassword);
		sessionFactory = configuration.buildSessionFactory();
		dbFullUrl = configuration.getProperty(KEY_URL);
		dbMessage = "database" + " " + dbFullUrl;
	}

	private Session getSession()
	{
		if(session == null || !session.isOpen())
		{
			LOGGER.debug("opening session");
			try
			{
				session = sessionFactory.openSession();
				LOGGER.info("session open");
			}
			catch (HibernateException e) {
				LOGGER.error("no open session", e);
			}
		}
		return session;
	}

	public void delete(Writable entity) throws RoseException
	{
		if(entity == null)
			return;
		LOGGER.warn("delete entity:\r\n" + EntityUtils.toStringFull(entity));
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			if(entity.getRelationType(i).isSecondMany())
			{
				Set<? extends Readable> set = new TreeSet<>( entity.getEntityValueMany(i));
				for(Readable subEntity : set)
				{
					if(subEntity != null)
					{
						changedEntitys.add((Writable) subEntity);
						entity.removeEntity(i, (Writable) subEntity);
					}
				}
			}
			else
			{
				if(entity.getEntityValueOne(i) != null)
				{
					changedEntitys.add((Writable) entity.getEntityValueOne(i));
					entity.setEntity(i, null);
				}
			}
		}
		commit();
		try
		{
			lockSession(true);
			LOGGER.info(dbMessage + " - " + "delete" + " " + EntityUtils.toStringPrimitives(entity));
			Session sesson = getSession();
			sesson.beginTransaction();
			sesson.delete(entity);
			sesson.getTransaction().commit();
			lockSession(false);
			getEntites(TypeManager.convertType(entity.getClass())).remove(entity);
			LOGGER.debug("entity deleted: " + EntityUtils.toStringPrimitives(entity));
		}
		catch(HibernateException e)
		{
			throw new RoseException("error deleting " + entity, e);
		}
	}
	
	private boolean sessionLocked()
	{
		return lockSession;
	}
	
	private void lockSession(boolean lockSession)
	{
		if(this.lockSession && lockSession)
			LOGGER.error("access attempt to locked session");
		else
			LOGGER.debug( (lockSession ? "" : "un") + "locking session" );
		this.lockSession = lockSession;
	}
	
	public List<?> listQuery( String query) throws RoseException
	{
		lockSession(true);
		SQLQuery sqlQuery = session.createSQLQuery(query);
		try
		{
			List<?> list = sqlQuery.setResultTransformer(Criteria.ROOT_ENTITY).list();
			if(list == null)
				return Collections.emptyList();
			return list;
		}
		catch(HibernateException e)
		{
			throw new RoseException("Unable to execute query '" + query + "'", e);
		}
		finally 
		{
			lockSession(false);
		}
	}

	public <T extends Readable> T createNew(Class<T> type) throws RoseException
	{
		try
		{
			T entity = type.newInstance();
			lockSession(true);
			LOGGER.info(dbMessage + " - " + "create" + " " + type.getName());
			Session session = getSession();
			session.beginTransaction();
			entity.setId((Integer) session.save(entity));
			session.getTransaction().commit();
			lockSession(false);
			if(!entityLists.get(type).isEmpty())
				getEntites(type).add(entity);
			LOGGER.info("new entity: " + EntityUtils.toStringPrimitives(entity));
			return entity;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RoseException("unable to create new " + type.getName(), e);
		}
	}
	
	public Writable createCopy(Writable entity) throws RoseException
	{
		Writable copy = createNew(entity.getClass());
		for(int i = 0; i < copy.getFieldCount(); i++)
			copy.setField( i, copy.getFieldValue(i));
		for(int i = 0; i < copy.getEntityCount(); i++)
			switch(copy.getRelationType(i))
			{
			case ONETOONE:
//				Writable subCopy = createCopy( (Writable) copy.getEntityValue(i) );
//				copy.setEntity( i, subCopy );
				break;
			case ONETOMANY:
				for( Readable o : copy.getEntityValueMany(i) )
					copy.addEntity( i, createCopy((Writable) o));
				break;
			case MANYTOONE:
				copy.setEntity(i, (Writable) copy.getEntityValueOne(i));
				break;
			case MANYTOMANY:
				break;
			}
		return copy;
	}
	
	public void commit() throws RoseException
	{
		Session session = getSession();
		Transaction transaction = null;
		try
		{
			lockSession(true);
			LOGGER.info(dbMessage + " - " + "saving");
			transaction = session.beginTransaction();
			for(Writable entity : changedEntitys)
			{
				if(entity == null)
					continue;
				if(entity.getId() < 0)
				{
					LOGGER.warn("saving new entity:\r\n" + EntityUtils.toStringFull(entity));
					Integer id = (Integer) session.save(entity);
					entity.setId(id);
				}
				else
				{
					LOGGER.debug("updating entity:\r\n" + EntityUtils.toStringFull(entity));
					session.update(entity);
				}
			}
			LOGGER.debug("commiting transaction");
			transaction.commit();
			lockSession(false);
			changedEntitys.clear();
		}
		catch(HibernateException e)
		{
			throw new RoseException("error saving or updating entities to database",e);
		}
	}
	
	public void closeSession()
	{
		timer.stop();
		if(session != null)
		{
			LOGGER.debug("closing session " + session);
			session.close();
			LOGGER.debug("session closed");
		}
		session = null;
	}
	
	public void loadEntities(Class<?> type) throws RoseException
	{
		try
		{
			lockSession(true);
			LOGGER.info(dbMessage + " - " + "load" + " " + type.getSimpleName());
			Session session = getSession();
			Criteria criteria = session.createCriteria(type);

			int fetchTimeSpan = getIntegerValue(FETCH_TIMESPAN, Integer.MAX_VALUE);
			if(fetchTimeSpan != Integer.MAX_VALUE)
			{
				calendar.setTime(new Date());
				calendar.add(Calendar.DATE, - fetchTimeSpan);
				criteria.add( Expression.ge(TIMESTAMP,calendar.getTime()));
				LOGGER.debug("fetch entity age restriction: " + fetchTimeSpan + " days");
			}
			
			List<?> list = criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
			lockSession(false);
			List<Readable> entities = entityLists.get(type);
			entities.clear();
			for(Object o : list)
			{
				entities.add((Readable) o);
				LOGGER.debug("load entity " + EntityUtils.toStringFull((Readable)o));
			}
			LOGGER.debug("finished loading entities: " + type.getName() + " count=" + entities.size());
		}
		catch(HibernateException e)
		{
			throw new RoseException("error loading entities: " + type.getName(), e);
		}
	}

	private void checkConnection(ActionEvent e)
	{
		if(sessionLocked())
			return;
		String message;
		boolean wasConnected = connected;
		if(session instanceof SessionImpl)
		{
			try
			{
				connected = ((SessionImpl)session).connection().isValid(10);
			}
			catch (HibernateException | SQLException e1)
			{
				if(wasConnected)
				{
					LOGGER.error("no connection to " + dbFullUrl, e1);
				}
				connected = false;
			}
			message =  connected ? "connected" : "disconnected" ;
		}
		else
			message = "unknown";
		LOGGER.debug(dbMessage + " - " + message);
	}

	public List<Readable> getEntites(Class<?> type)
	{
		List<Readable> entities = entityLists.get(type);
		if(entities.isEmpty())
		{
			try
			{
				loadEntities(type);
			}
			catch(RoseException e)
			{
				String message = "error fetching entities from database";
				LOGGER.error(message, e);
			}
		}
		return entities;
	}

	public void update(Writable... entities)
	{
		for(Writable entity : entities)
		{
			if(entity == null)
				return;
			if(entity.getId() < 0)
			{
				entity.setId(newEntityCount--);
				List<Readable> list = getEntites(entity.getClass());
				if(list != null)
					list.add(entity);
			}
			changedEntitys.add(entity);
			if(entity instanceof Timestamped)
				((Timestamped)entity).setTimestamp(new Date());
		}
	}

	public void rollback()
	{
		changedEntitys.clear();
	}

	public void clearEntities(Class<?> type)
	{
		List<Readable> entities = entityLists.get(type);
		if(entities == null)
			entityLists.put(type, new ArrayList<>());
		else
			entities.clear();
	}

	public Readable getEntityById(final Class<?> type, int id)
	{
		final Criteria criteria = getSession().createCriteria(type);
		criteria.add(Restrictions.idEq(id));
		final List<?> entities = criteria.list();
		if(entities.size() != 1)
			return null;
		return (Readable) entities.get(0);
	}

}
