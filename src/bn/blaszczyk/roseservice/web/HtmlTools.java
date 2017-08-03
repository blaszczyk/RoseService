package bn.blaszczyk.roseservice.web;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.PrimitiveType;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.dto.RoseDto;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.Preference;
import bn.blaszczyk.rosecommon.tools.TypeManager;
import bn.blaszczyk.roseservice.tools.ServicePreference;

public class HtmlTools {

	public static String startPage()
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("Server", "server"))
			.h1("Start");
		for(Class<?> type : TypeManager.getEntityClasses())
		{
			final String name = type.getSimpleName();
			hb.append(linkToWeb(name, name.toLowerCase()))
				.append("<br>");
		}
		return hb.build();
	}
	
	public static String entityList(final Entity entity, final List<RoseDto> dtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"))
			.h1(entity.getSimpleClassName())
			.append("<form method=\"post\" action=\"/web/" + entity.getObjectName() + "/create\">")
			.append(input("submit", "", "create"))
			.append(input("hidden", "type", entity.getObjectName()))
			.append("</form>")
			.append(entityTable(entity, dtos));			
		return hb.build();
	}

	public static String entityView(final Entity entity, final RoseDto dto, final List<List<RoseDto>> subDtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"))
			.append(" - ")
			.append(linkToWeb(entity.getSimpleClassName(), entity.getObjectName()))
			.append(" - ")
			.append(linkToWeb("edit", entity.getObjectName(), dto.getId(), "update"))
			.h1(entity.getSimpleClassName() + " id=" + dto.getId())
			.append(primitivesTable(entity, dto));
		for(int i = 0; i < entity.getEntityFields().size(); i++)
		{
			final List<RoseDto> dtos = subDtos.get(i);
			if(dtos.isEmpty())
				continue;
			final EntityField field = entity.getEntityFields().get(i);
			if(field.getType().isSecondMany())
				hb.h2(field.getCapitalName())
					.append(entityTable(field.getEntity(), dtos));
			else
			{
				final RoseDto subDto = dtos.get(0);
				hb.h2(linkToWeb(field.getCapitalName(), field.getEntity().getObjectName(), subDto.getId()))
					.append(primitivesTable(field.getEntity(), subDto));
			}
		}
		return hb.build();
	}

	public static String entityEdit(final Entity entity, final RoseDto dto)
	{
		final String path = "/web/" + entity.getObjectName() + "/" + dto.getId();
		final HtmlBuilder hb = new HtmlBuilder();
		hb.h1(entity.getSimpleClassName() + " id=" + dto.getId())
			.append(linkToWeb("Cancel", entity.getObjectName(), dto.getId()))
			.append("<form method=\"post\" action=\"")
			.append(path)
			.append("/update\">")
			.append(input("hidden", "id", dto.getId()))
			.append(input("hidden", "type", dto.getType().getSimpleName()))
			.append(primitivesInputTable(entity, dto))
			.append(entitiesInputTable(entity, dto))
			.append(input("submit","", "Save"))
			.append("</form>")
			.append(postButton(path + "/delete", "Delete"));
		return hb.build();
	}

	public static String serverControls(final Map<String, String> status, final PreferenceDto preferences)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"))
			.h1("Server Controls")
			.h2("Status")
			.append("<table>");
		for(final Entry<String,String> entry : status.entrySet())
			hb.append("<tr><td>")
				.append(entry.getKey())
				.append("</td><td>")
				.append(entry.getValue())
				.append("</td></tr>");
		hb.append("</table>")
			.append(postButton("/web/restart", "Restart"))
			.append(postButton("/web/stop", "Stop"))
			.h2("Configuration")
			.append("<form method=\"post\" action=\"/web/server\">")
			.append(preferencesInputTable(preferences))
			.append(input("submit","", "Save"))
			.append("</form>");
		return hb.build();
	}

	public static String linkToWeb(final String text, final Object... path )
	{
		final StringBuilder sb = new StringBuilder("<a href=\"/web");
		for(final Object subPath : path)
			sb.append("/")
				.append(subPath);
		return sb.append("\">")
				.append(text)
				.append("</a>")
				.toString();
	}
	
	public static String linkTo(final String text, final Object... path )
	{
		final StringBuilder sb = new StringBuilder("<a href=\"");
		for(final Object subPath : path)
			sb.append("/")
				.append(subPath);
		return sb.append("\">")
				.append(text)
				.append("</a>")
				.toString();
	}
	
	public static String postButton(final String path, final String label)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<form method=\"post\" action=\"");
		sb.append(path);
		sb.append("\">");
		sb.append(input("submit", "", label));
		sb.append("</form>");
		return sb.toString();
	}
	
	public static String input(final String type, final String name, final Object value, final String... attributes)
	{
		final StringBuilder sb = new StringBuilder("<input type=\"")
				.append(type)
				.append("\" name=\"")
				.append(name)
				.append("\" value=\"")
				.append(value)
				.append("\"");
		for(final String attribute : attributes)
			sb.append(" ").append(attribute);
		sb.append(">");
		return sb.toString();
	}
	
	private static String primitivesTable(final Entity entity, final RoseDto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final Field field : entity.getFields()) {
			final String fieldName = field.getName();
			sb.append("<tr><td>")
				.append(fieldName)
				.append("</td><td>")
				.append(dto.getFieldValue(fieldName))
				.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
	
	private static String primitivesInputTable(final Entity entity, final RoseDto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final Field field : entity.getFields())
		{
			final String fieldName = field.getName();
			sb.append("<tr><td>")
				.append(field.getName())
				.append("</td><td>");
			if(field instanceof EnumField)
			{
				final EnumField enumField = (EnumField) field;
				final Class<?> enumClass = TypeManager.getClass(enumField.getEnumType());
				final List<String> enumValuesAsString = Arrays.asList(enumClass.getEnumConstants())
						.stream()
						.map(String::valueOf)
						.collect(Collectors.toList());
				sb.append(selectValue(fieldName, dto.getFieldValue(fieldName), enumValuesAsString ));
			}
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField primitiveField = (PrimitiveField) field;
				if(primitiveField.getType().equals(PrimitiveType.BOOLEAN))
					sb.append(selectValue(fieldName, dto.getFieldValue(fieldName), Arrays.asList("true","false")));
				else
					sb.append(input("text",fieldName,dto.getFieldValue(fieldName)));
			}
			sb.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
	
	private static String entitiesInputTable(final Entity entity, final RoseDto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final EntityField field : entity.getEntityFields())
		{
			final String fieldName = field.getName();
			final String defValue = String.valueOf( field.getType().isSecondMany() ? dto.getEntityIds(fieldName) : dto.getEntityId(fieldName));
			sb.append("<tr><td>")
				.append(fieldName)
				.append("</td><td>")
				.append(input("text",fieldName, defValue))
				.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
	
	private static String entityTable(final Entity entity, final List<RoseDto> dtos)
	{
		final StringBuilder sb = new StringBuilder("<table><tr><th>id");
		for(final Field field : entity.getFields())
			sb.append("</th><th>")
				.append(field.getName());
		sb.append("</th></tr>");
		for(final RoseDto dto : dtos)
		{
			sb.append("<tr><td>")
				.append(linkToWeb(String.valueOf(dto.getId()), entity.getObjectName(), dto.getId()  ));
			for(final Field field : entity.getFields())
				sb.append("</td><td>")
					.append(linkToWeb(String.valueOf(dto.getFieldValue(field.getName())), entity.getObjectName(), dto.getId() ));
			sb.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
		
	private static String preferencesInputTable(final PreferenceDto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final Preference preference : CommonPreference.values())
			if(dto.containsPreference(preference))
				sb.append("<tr><td>")
					.append(preference.getKey())
					.append("</td><td>")
					.append(input("text",preference.getKey(), dto.get(preference)))
					.append("</td></tr>");
		for(final Preference preference : ServicePreference.values())
			if(dto.containsPreference(preference))
				sb.append("<tr><td>")
					.append(preference.getKey())
					.append("</td><td>")
					.append(input("text",preference.getKey(), dto.get(preference)))
					.append("</td></tr>");
		return sb.append("</table>").toString();
	}
	
	public static <T> String selectValue(final String name, final T selectedValue, final Iterable<T> values)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("<select name=\"")
		.append(name)
		.append("\">");
		for(final T value : values)
			sb.append("<option value=\"")
			.append(String.valueOf(value))
			.append("\"")
			.append(value.equals(selectedValue) ? " selected " : "")
		    .append(">")
			.append(String.valueOf(value))
			.append("</option>");
		sb.append("</select>");
		return sb.toString();
	}
}
