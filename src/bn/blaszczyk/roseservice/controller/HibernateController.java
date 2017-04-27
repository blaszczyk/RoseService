
package bn.blaszczyk.roseservice.controller;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

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
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.impl.SessionImpl;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rose.model.Writable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.tools.EntityUtils;

import static bn.blaszczyk.roseservice.tools.Preferences.*;

public class HibernateController implements ModelController {
	
	private static final Logger LOGGER = Logger.getLogger(HibernateController.class);
	private static final Calendar calendar = Calendar.getInstance();

	private static final String KEY_URL = "hibernate.connection.url";
	private static final String KEY_USER = "hibernate.connection.username";
	private static final String KEY_PW = "hibernate.connection.password";

	private static final String TIMESTAMP = "timestamp";
	
	private SessionFactory sessionFactory;
	private Session session;

	private String dbFullUrl;
	private String dbMessage;
	private boolean connected = false;
	private boolean lockSession = false;
	private Timer timer = new Timer(5000, e -> checkConnection(e));

	public HibernateController()
	{
		configureDataBase();
		timer.setInitialDelay(1000);
		timer.start();
	}

	@Override
	public List<Readable> getEntities(Class<? extends Readable> type) throws RoseException
	{
		try
		{
			lockSession(true);
			LOGGER.debug("start load" + " " + type.getSimpleName());
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
			LOGGER.debug("end load entities: " + type.getName() + " count=" + list.size());
			return list.stream().map(Readable.class::cast).collect(Collectors.toList());
		}
		catch(HibernateException e)
		{
			throw new RoseException("error loading entities: " + type.getName(), e);
		}
	}

	@Override
	public Readable getEntityById(final Class<? extends Readable> type, int id)
	{
		final Criteria criteria = getSession().createCriteria(type);
		criteria.add(Restrictions.idEq(id));
		final List<?> entities = criteria.list();
		if(entities.size() != 1)
			return null;
		return (Readable) entities.get(0);
	}

	@Override
	public int getEntityCount(Class<? extends Readable> type)
	{
		final Object oCount = getSession().createCriteria(type).setProjection(Projections.rowCount()).uniqueResult();
		if(oCount instanceof Number)
			return ((Number)oCount).intValue();
		return 0;
	}

	@Override
	public void update(Writable... entities) throws RoseException
	{
		final Session session = getSession();
		Transaction transaction = null;
		try
		{
			lockSession(true);
			LOGGER.debug("start update");
			transaction = session.beginTransaction();
			for(Writable entity : entities)
			{
				if(entity == null)
					continue;
				if(entity.getId() < 0)
				{
					LOGGER.warn("saving new entity:\r\n" + EntityUtils.toStringFull(entity));
					final Integer id = (Integer) session.save(entity);
					entity.setId(id);
				}
				else
				{
					LOGGER.debug("updating entity:\r\n" + EntityUtils.toStringFull(entity));
					session.update(entity);
				}
			}
			transaction.commit();
			LOGGER.debug("end update");
			lockSession(false);
		}
		catch(HibernateException e)
		{
			throw new RoseException("error saving or updating entities to database",e);
		}
	}

	@Override
	public void delete(Writable entity) throws RoseException
	{
		if(entity == null)
			return;
		LOGGER.warn("delete entity:\r\n" + EntityUtils.toStringFull(entity));
		final Set<Writable> changedEntities = new LinkedHashSet<>();
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			if(entity.getRelationType(i).isSecondMany())
			{
				Set<? extends Readable> set = new TreeSet<>( entity.getEntityValueMany(i));
				for(Readable subEntity : set)
					if(subEntity != null)
						changedEntities.add((Writable) subEntity);
			}
			else
				if(entity.getEntityValueOne(i) != null)
					changedEntities.add((Writable) entity.getEntityValueOne(i));
		}
		update(changedEntities.toArray(new Writable[changedEntities.size()]));
		try
		{
			lockSession(true);
			Session sesson = getSession();
			sesson.beginTransaction();
			sesson.delete(entity);
			sesson.getTransaction().commit();
			lockSession(false);
			LOGGER.debug("entity deleted: " + EntityUtils.toStringSimple(entity));
		}
		catch(HibernateException e)
		{
			throw new RoseException("error deleting " + entity, e);
		}
	}

	@Override
	public <T extends Readable> T createNew(Class<T> type) throws RoseException
	{
		try
		{
			T entity = type.newInstance();
			lockSession(true);
			Session session = getSession();
			session.beginTransaction();
			entity.setId((Integer) session.save(entity));
			session.getTransaction().commit();
			lockSession(false);
			LOGGER.info("new entity: " + EntityUtils.toStringPrimitives(entity));
			return entity;
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RoseException("unable to create new " + type.getName(), e);
		}
	}
	
	@Override
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

	private void configureDataBase()
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
	
}
