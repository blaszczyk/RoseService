package bn.blaszczyk.roseservice.tools;

import java.util.Set;

import bn.blaszczyk.rose.model.Identifyable;
import bn.blaszczyk.rose.model.Readable;

public final class EntityUtils {

	public static String toStringPrimitives(Readable entity)
	{
		if(entity == null)
			return "";
		final StringBuilder builder = new StringBuilder();
		builder.append(entity.getEntityName())
				.append("(").append(entity.getId()).append("){");
		final int fieldCount = entity.getFieldCount();
		for(int i = 0; i < entity.getFieldCount(); i++)
			builder.append(" ").append(entity.getFieldName(i)).append("=").append(entity.getFieldValue(i))
					.append( ( i < fieldCount - 1 ) ? "," : " }");
		return builder.toString();
	}
	
	public static String toStringFull(Readable entity)
	{
		if(entity == null)
			return "";
		final StringBuilder builder = new StringBuilder(toStringPrimitives(entity));
		final int entityCount = entity.getEntityCount();	
		for(int i = 0; i < entityCount; i++)
		{
			builder.append("\r\n\t").append(entity.getEntityName(i)).append("=");
			if(entity.getRelationType(i).isSecondMany())
			{
				builder.append(entity.getEntityClass(i).getSimpleName()).append("(");
				Set<? extends Identifyable> entities = entity.getEntityValueMany(i);
				boolean first = true;
				for(Identifyable id : entities)
				{
					if(first)
						first = false;
					else
						builder.append(",");
					builder.append(id.getId());
				}
				builder.append(")");
			}
			else
				builder.append(toStringPrimitives(entity.getEntityValueOne(i)));
		}
		return builder.toString();
	}
	
	public static boolean equals(Readable i1, Readable i2)
	{
		if(i1 == i2)
			return true;
		if(i1 == null)
			return i2 == null;
		if(i2 == null)
			return false;
		if(! TypeManager.convertType(i1.getClass()).equals(TypeManager.convertType(i2.getClass())))
			return false;
		return i1.getId().equals(i2.getId());
	}
}
