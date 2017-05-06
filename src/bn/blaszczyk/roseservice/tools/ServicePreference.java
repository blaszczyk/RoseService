package bn.blaszczyk.roseservice.tools;

import bn.blaszczyk.rosecommon.tools.Preference;
import bn.blaszczyk.rosecommon.tools.Preference.Type;

public enum ServicePreference implements Preference {
	;
	
	private final Type type;
	private final String key;
	private final Object defaultValue;
	
	private ServicePreference(final Type type, final String key, final Object defaultValue)
	{
		if(defaultValue != null && !type.getType().isInstance(defaultValue))
			throw new IllegalArgumentException("preference " + key + "of type " + type + " has false default value class: " + defaultValue.getClass());
		this.type = type;
		this.key = key;
		this.defaultValue = defaultValue;
	}

	@Override
	public Type getType()
	{
		return type;
	}

	@Override
	public String getKey()
	{
		return key;
	}

	@Override
	public Object getDefaultValue()
	{
		return type.getType().cast(defaultValue);
	}
	
}
