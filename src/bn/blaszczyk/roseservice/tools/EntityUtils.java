package bn.blaszczyk.roseservice.tools;

import java.math.BigDecimal;
import java.util.Set;

import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.Identifyable;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.model.RoseDto;

public final class EntityUtils {
	
	public static Object getPrimitiveValue(final Field field, final String stringValue) throws RoseException
	{
		if(stringValue == null || stringValue.equals("null"))
			return null;
		try
		{
			if(field instanceof EnumField)
			{
				final Class<?> enumType = TypeManager.getClass(((EnumField) field).getEnumType());
				for(final Object value : enumType.getEnumConstants())
					if(value.toString().equals(stringValue))
						return value;
			}
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField pField = (PrimitiveField) field;
				switch(pField.getType())
				{
				case BOOLEAN:
					return Boolean.parseBoolean(stringValue);
				case DATE:
					return RoseDto.DATE_FORMAT.parse(stringValue);
				case INT:
					return Integer.parseInt(stringValue);
				case NUMERIC:
					final BigDecimal numValue = (BigDecimal) RoseDto.BIG_DEC_FORMAT.parse(stringValue);
					checkNumeric(numValue,pField);
					return numValue;
				case CHAR:
				case VARCHAR:
					checkString(stringValue,pField);
					return stringValue;
				}
			}
		}
		catch(Exception e)
		{
			throw RoseException.wrap(e,"Error parsing primitive '" + stringValue + "' for " + field.getName());
		}
		return null;
	}
	
	public static String toStringSimple(Identifyable entity)
	{
		return String.format("%s id=%d", entity.getClass().getSimpleName(), entity.getId());
	}
	
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

	private static void checkNumeric(final BigDecimal value, final PrimitiveField field) throws RoseException
	{
		final int precision = field.getLength1();
		final int scale = field.getLength2();
		if(value.scale() > scale)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong scale; max scale=" + scale);
		if(value.precision() > precision)
			throw new RoseException("numeric value " + field.getName() + "=" + value + " has wrong precision; max precision=" + precision);
	}

	private static void checkString(final String value, final PrimitiveField field) throws RoseException
	{
		final int length = field.getLength1();
		if(value.length() > length)
			throw new RoseException("string value " + field.getName() + "='" + value + "' too long; max_length=" + length);
	}

}
