package bn.blaszczyk.roseservice.server;

import bn.blaszczyk.roseservice.tools.TypeManager;

public class PathOptions
{
	private final Class<?> type;
	private int id = -1;
	private boolean valid;
	
	public PathOptions(final String path)
	{
		final String[] options = path.split("\\/");
		type = TypeManager.getClass(options[0]);
		valid = type != null;
		if(options.length > 1)
			try
			{
				id = Integer.parseInt(options[1].trim());
			}
			catch (NumberFormatException e) 
			{
				valid = false;
			}
	}

	public int getId()
	{
		return id;
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

