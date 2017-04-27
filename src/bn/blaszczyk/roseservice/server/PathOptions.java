package bn.blaszczyk.roseservice.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import bn.blaszczyk.rose.model.Readable;
import bn.blaszczyk.rosecommon.tools.TypeManager;

public class PathOptions
{
	private static final Pattern COMMA_SEPARATED_INTS = Pattern.compile("^[0-9]+(\\,[0-9]+)*$");
	
	private final Class<? extends Readable> type;
	private int id = -1;
	private final List<Integer> ids = new ArrayList<>();
	private boolean valid;
	private String[] options;
	
	public PathOptions(final String path)
	{
		final String[] options = path.split("\\/");
		type = TypeManager.getClass(options[0]);
		valid = type != null;
		if(options.length > 1)
		{
			if( COMMA_SEPARATED_INTS.matcher(options[1]).matches() )
			{
				Arrays.stream(options[1].split("\\,"))
						.map(String::trim)
						.map(Integer::parseInt)
						.forEach(i -> ids.add(i));
				if(!ids.isEmpty())
					id = ids.get(0);
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
	
	@Override
	public String toString()
	{
		if(!valid)
			return "Invalid Path Options";
		return type.getSimpleName() + " id=" + id;
	}
	
}

