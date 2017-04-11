package bn.blaszczyk.roseservice.server;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import bn.blaszczyk.rose.model.Readable;

public class RoseDto extends LinkedHashMap<String, String>{
	
	private static final long serialVersionUID = 7851913867785528671L;
	
	private static final DateFormat DATE_FORMAT = DateFormat.getDateInstance();
	
	public RoseDto(final Readable entity)
	{
		put("type",entity.getEntityName());
		put("id", String.valueOf(entity.getId()));
		for(int i = 0; i < entity.getFieldCount(); i++)
		{
			String stringValue = null;
			final Object field = entity.getFieldValue(i);
			if(field instanceof Date)
				stringValue = DATE_FORMAT.format((Date)field);
			else if(field != null)
				stringValue = field.toString();
			put("f" + i,stringValue);
		}
		for(int i = 0; i < entity.getEntityCount(); i++)
		{
			if(entity.getRelationType(i).isSecondMany())
			{
				final List<Integer> ids = new ArrayList<>();
				for(final Readable subEntity : entity.getEntityValueMany(i))
					ids.add(subEntity.getId());
				put("e"+i,ids.toString());
			}
			else
			{
				Readable subEntity = entity.getEntityValueOne(i);
				if(subEntity != null)
					put("e"+i,String.valueOf(subEntity.getId()));
			}
		}
	}
	
}
