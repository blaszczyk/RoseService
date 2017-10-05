package bn.blaszczyk.roseservice.web;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import bn.blaszczyk.rose.model.EntityModel;
import bn.blaszczyk.rose.model.Dto;
import bn.blaszczyk.rose.model.EntityField;
import bn.blaszczyk.rose.model.EnumField;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.rose.model.PrimitiveField;
import bn.blaszczyk.rose.model.PrimitiveType;
import bn.blaszczyk.rosecommon.dto.PreferenceDto;
import bn.blaszczyk.rosecommon.tools.CommonPreference;
import bn.blaszczyk.rosecommon.tools.FileConverter;
import bn.blaszczyk.rosecommon.tools.Preference;
import bn.blaszczyk.rosecommon.tools.TypeManager;
import bn.blaszczyk.roseservice.tools.ServicePreference;

public class HtmlTools {

	private static final Comparator<? super Entry<String, String>> KEY_COMPARATOR = (e1,e2) -> e1.getKey().compareTo(e2.getKey());
	
	public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy");

	public static String startPage()
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("Server", "server"))
			.append(" - ")
			.append(linkToWeb("Files", "file/"))
			.h1("Start");
		for(final Class<?> type : TypeManager.getEntityClasses())
		{
			final String name = type.getSimpleName();
			hb.append(linkToWeb(name, name.toLowerCase()))
				.append("<br>");
		}
		return hb.build();
	}
	
	public static String entityList(final EntityModel entityModel, final List<Dto> dtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"))
			.h1(entityModel.getSimpleClassName())
			.append("<form method=\"post\" action=\"/web/" + entityModel.getObjectName() + "/create\">")
			.append(input("submit", "", "create"))
			.append(input("hidden", "type", entityModel.getObjectName()))
			.append("</form>")
			.append(entityTable(entityModel, dtos));			
		return hb.build();
	}

	public static String entityView(final EntityModel entityModel, final Dto dto, final List<List<Dto>> subDtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"))
			.append(" - ")
			.append(linkToWeb(entityModel.getSimpleClassName(), entityModel.getObjectName()))
			.append(" - ")
			.append(linkToWeb("edit", entityModel.getObjectName(), dto.getId(), "update"))
			.h1(entityModel.getSimpleClassName() + " id=" + dto.getId())
			.append(primitivesTable(entityModel, dto));
		for(int i = 0; i < entityModel.getEntityFields().size(); i++)
		{
			final List<Dto> dtos = subDtos.get(i);
			if(dtos.isEmpty())
				continue;
			final EntityField field = entityModel.getEntityFields().get(i);
			if(field.getType().isSecondMany())
				hb.h2(field.getCapitalName())
					.append(entityTable(field.getEntityModel(), dtos));
			else
			{
				final Dto subDto = dtos.get(0);
				hb.h2(linkToWeb(field.getCapitalName(), field.getEntityModel().getObjectName(), subDto.getId()))
					.append(primitivesTable(field.getEntityModel(), subDto));
			}
		}
		return hb.build();
	}

	public static String entityEdit(final EntityModel entityModel, final Dto dto)
	{
		final String path = "/web/" + entityModel.getObjectName() + "/" + dto.getId();
		final HtmlBuilder hb = new HtmlBuilder();
		hb.h1(entityModel.getSimpleClassName() + " id=" + dto.getId())
			.append(linkToWeb("Cancel", entityModel.getObjectName(), dto.getId()))
			.append("<form method=\"post\" action=\"")
			.append(path)
			.append("/update\" accept-charset=\"UTF-8\">")
			.append(input("hidden", "id", dto.getId()))
			.append(input("hidden", "type", TypeManager.getClass(dto).getSimpleName()))
			.append(primitivesInputTable(entityModel, dto))
			.append(entitiesInputTable(entityModel, dto))
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
		status.entrySet().stream()
			.sorted(KEY_COMPARATOR)
			.forEachOrdered(entry -> {
				hb.append("<tr><td>")
					.append(entry.getKey())
					.append("</td><td>")
					.append(entry.getValue())
					.append("</td></tr>");
			});
		hb.append("</table>")
			.append(postButton("/web/restart", "Restart"))
			.append(postButton("/web/stop", "Stop"))
			.h2("Configuration")
			.append("<form method=\"post\" action=\"/web/server\" accept-charset=\"UTF-8\">")
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
	
	private static String primitivesTable(final EntityModel entityModel, final Dto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final Field field : entityModel.getFields()) {
			final String fieldName = field.getName();
			sb.append("<tr><td>")
				.append(fieldName)
				.append("</td><td>")
				.append(primitiveStringValue(field, dto))
				.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}

	private static String primitiveStringValue(final Field field, final Dto dto)
	{
		final Object value = dto.getFieldValue(field.getName());
		if(field instanceof EnumField)
			return value == null ? "" : value.toString();
		switch(((PrimitiveField)field).getType())
		{
		case BOOLEAN:
		case CHAR:
		case INT:
		case NUMERIC:
		case VARCHAR:
			if(value == null)
				return "";
			return String.valueOf(value);
		case DATE:
			final long time = value == null ? -1 : ((Long)value).longValue();
			return time == -1 ? "" : DATE_FORMAT.format(new Date(time));
		default:
			return "";
		}
	}
	
	private static String primitivesInputTable(final EntityModel entityModel, final Dto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final Field field : entityModel.getFields())
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
				sb.append(selectValue(fieldName, String.valueOf(primitiveStringValue(field, dto)), enumValuesAsString ));
			}
			else if(field instanceof PrimitiveField)
			{
				final PrimitiveField primitiveField = (PrimitiveField) field;
				if(primitiveField.getType().equals(PrimitiveType.BOOLEAN))
					sb.append(selectValue(fieldName, primitiveStringValue(field, dto), Arrays.asList("true","false")));
				else
					sb.append(input("text",fieldName,primitiveStringValue(field, dto)));
			}
			sb.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
	
	private static String entitiesInputTable(final EntityModel entityModel, final Dto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(final EntityField field : entityModel.getEntityFields())
		{
			final String fieldName = field.getName();
			final String defValue;
			if(field.getType().isSecondMany())
				defValue = Arrays.stream(dto.getEntityIds(fieldName)).mapToObj(String::valueOf).collect(Collectors.joining(","));
			else
				defValue = String.valueOf(dto.getEntityId(fieldName));
			sb.append("<tr><td>")
				.append(fieldName)
				.append("</td><td>")
				.append(input("text",fieldName, defValue))
				.append("</td></tr>");
		}
		return sb.append("</table>").toString();
	}
	
	private static String entityTable(final EntityModel entityModel, final List<Dto> dtos)
	{
		final StringBuilder sb = new StringBuilder("<table><tr><th>id");
		for(final Field field : entityModel.getFields())
			sb.append("</th><th>")
				.append(field.getName());
		sb.append("</th></tr>");
		for(final Dto dto : dtos)
		{
			sb.append("<tr><td>")
				.append(linkToWeb(String.valueOf(dto.getId()), entityModel.getObjectName(), dto.getId()  ));
			for(final Field field : entityModel.getFields())
				sb.append("</td><td>")
					.append(linkToWeb(primitiveStringValue(field, dto), entityModel.getObjectName(), dto.getId() ));
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
					.append(preferenceInput(preference, dto.get(preference)))
					.append("</td></tr>");
		for(final Preference preference : ServicePreference.values())
			if(dto.containsPreference(preference))
				sb.append("<tr><td>")
					.append(preference.getKey())
					.append("</td><td>")
					.append(preferenceInput(preference, dto.get(preference)))
					.append("</td></tr>");
		return sb.append("</table>").toString();
	}
	
	private static String preferenceInput(final Preference preference, final Object value)
	{
		if(preference.equals(CommonPreference.DB_PASSWORD))
			return input("password",preference.getKey(), value);
		if(preference.equals(CommonPreference.LOG_LEVEL))
			return selectValue(preference.getKey(), value, Arrays.asList("ALL","DEBUG","INFO","WARN","ERROR","NONE"));
		switch (preference.getType())
		{
		case STRING:
		case INT:
		case NUMERIC:
			return input("text",preference.getKey(), value);
		case BOOLEAN:
			return selectValue(preference.getKey(), String.valueOf(value), Arrays.asList("true","false"));
		}
		return "";
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

	public static String folderView(final String path)
	{
		final File folder = new FileConverter().fromPath(path);
		final HtmlBuilder hb = new HtmlBuilder();
		hb.append(linkToWeb("start"));
		if(folder.isDirectory())
		{
			hb.h2("file:" + path);
			if(path.contains("/"))
				hb.append(linkToWeb("..", "file", path.substring(0, path.indexOf('/')))).br();
			for(final File file : folder.listFiles())
			{
				final String subpath = "file" + (path == null || path.length() == 0 ? "" : "/") + path;
				if(file.isDirectory())
					hb.append(linkToWeb(file.getName(), subpath, file.getName())).br();
				else
					hb.append(linkToWeb(file.getName(), "..", subpath, file.getName())).br();
			}
		}
		return hb.build();
	}
}
