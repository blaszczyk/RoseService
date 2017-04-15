package bn.blaszczyk.roseservice.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bn.blaszczyk.roseservice.tools.TypeManager;

public class PathOptions
{
	private final Class<?> type;
	private int id = -1;
	private final List<Integer> ids = new ArrayList<>();
	private boolean valid;
	
	public PathOptions(final String path)
	{
		final String[] options = path.split("\\/");
		type = TypeManager.getClass(options[0]);
		valid = type != null;
		if(options.length > 1)
			try
			{
				Arrays.stream(options[1].split("\\,"))
						.map(String::trim)
						.map(Integer::parseInt)
						.forEach(i -> ids.add(i));
				if(!ids.isEmpty())
					id = ids.get(0);
			}
			catch (Exception e) 
			{
				valid = false;
			}
	}

	public int getId()
	{
		return id;
	}
	
	public List<Integer> getIds()
	{
		return ids;
	}
	
	public boolean hasId()
	{
		return id >= 0;
	}

	public boolean isValid()
	{
		return valid;
	}

	public Class<?> getType()
	{
		return type;
	}
	
	@Override
	public String toString()
	{
		if(!valid)
			return "Invalid Path Options";
		return type.getSimpleName() + " id=" + id;
	}
	
}

