package bn.blaszczyk.roseservice.tools;

import bn.blaszczyk.rosecommon.tools.Preference;

import static bn.blaszczyk.rosecommon.tools.Preference.Type.*;

public enum ServicePreference implements Preference {

	ENTITY_ENDPOINT_ACTIVE(BOOLEAN,"entityendpointactive",true,true),
	SERVICE_ENDPOINT_ACTIVE(BOOLEAN,"serviceendpointactive",true,true),
	WEB_ENDPOINT_ACTIVE(BOOLEAN,"webendpointactive",false,true),
	CALC_ENDPOINT_ACTIVE(BOOLEAN,"calcendpointactive",false,true),
	FILE_ENDPOINT_ACTIVE(BOOLEAN,"fileendpointactive",false,true);
	
	private final Type type;
	private final String key;
	private final Object defaultValue;
	private final boolean needsCaching;

	private ServicePreference(final Type type, final String key, final Object defaultValue, final boolean needsCaching)
	{
		if(defaultValue != null && !type.getType().isInstance(defaultValue))
			throw new IllegalArgumentException("preference " + key + "of type " + type + " has false default value class: " + defaultValue.getClass());
		this.type = type;
		this.key = key;
		this.defaultValue = defaultValue;
		this.needsCaching = needsCaching;
	}
	
	private ServicePreference(final Type type, final String key, final Object defaultValue)
	{
		this(type, key, defaultValue, false);
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
	
	@Override
	public boolean needsCaching()
	{
		return needsCaching;
	}
	
}
