package bn.blaszczyk.roseservice.server;

import java.util.Arrays;
import java.util.regex.Pattern;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class PathOptions
{
	private static final Pattern INT_PATTERN = Pattern.compile("^[0-9]+$");
	
	private final Class<? extends Readable> type;
	private int id = -1;
	private String[] options;
	
	public PathOptions(final String path)
	{
		final String[] options = path.split("\\/");
		type = TypeManager.getClass(options[0]);
		if(options.length > 1)
		{
			if( INT_PATTERN.matcher(options[1]).matches() )
			{
				id = Integer.parseInt(options[1].trim());
				if(options.length > 2)
					this.options = Arrays.copyOfRange(options, 2, options.length);
			}
			else
				this.options = Arrays.copyOfRange(options, 1, options.length);
		}
		if(this.options == null)
			this.options = new String[0];
	}

	public int getId()
	{
		return id;
	}
	
	public boolean hasId()
	{
		return id >= 0;
	}

	public boolean hasType()
	{
		return type != null;
	}

	public Class<? extends Readable> getType()
	{
		return type;
	}
	
	public boolean hasOptions()
	{
		return options.length > 0;
	}
	
	public String[] getOptions()
	{
		return options;
	}
	
}

