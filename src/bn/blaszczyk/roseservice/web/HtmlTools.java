package bn.blaszczyk.roseservice.web;

import java.util.List;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.Field;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.tools.TypeManager;

public class HtmlTools {

	public static String startPage()
	{
		final StringBuilder sb = new StringBuilder("<html><body>");
		sb.append("<h1>" + TypeManager.getMainClass().getSimpleName() + "</h1>");
		for(Class<?> type : TypeManager.getEntityClasses())
		{
			final String name = type.getSimpleName();
			sb.append(linkTo(name, "/web/" + name.toLowerCase()))
				.append("<br>");
//			sb.append("<form><action=\"/")
//				.append(name.toLowerCase())
//				.append("\" method = \"get\"><input type=\"submit\" value=\"")
//				.append(name)
//				.append("\"></form>");
		}
		return sb.append("</body></html>").toString();
	}
	
	public static String entityList(final Class<?> type, final List<RoseDto> dtos)
	{

		final Entity entity = TypeManager.getEntity(type);
		final StringBuilder sb = new StringBuilder("<html><body>");
		sb.append("<h1>")
			.append(type.getSimpleName())
			.append("</h1><table><tr><th>id");
		for(final Field field : entity.getFields())
			sb.append("</th><th>")
				.append(field.getName());
		sb.append("</th>");
		for(final RoseDto dto : dtos)
		{
			final String path = "/web/" + type.getSimpleName().toLowerCase() + "/" + dto.getId();
			sb.append("</tr><tr><td>")
				.append(linkTo(String.valueOf(dto.getId()), path ));
			for(int i = 0; i < entity.getFields().size();i++)
				sb.append("</td><td>")
					.append(dto.getFieldValue(i));
			sb.append("</td>");
		}
		sb.append("</tr></table>")
			.append("</body></html>");			
		return sb.toString();
	}

	public static String entityView(final Class<?> type, final RoseDto dto)
	{
		final StringBuilder sb = new StringBuilder("<html><body>");
		final Entity entity = TypeManager.getEntity(type);
		sb.append("<h1>")
			.append(type.getSimpleName())
			.append(" id=")
			.append(dto.getId())
			.append("</h1><table>");
		for(int i = 0; i < entity.getFields().size();i++)
			sb.append("<tr><td>")
				.append(entity.getFields().get(i).getName())
				.append("</td><td>")
				.append(dto.getFieldValue(i))
				.append("</td></tr>");
		sb.append("</table>")
			.append("</body></html>");
		return sb.toString();
	}
	
	private static String linkTo(final String text, final String path)
	{
		return new StringBuilder("<a href=\"")
				.append(path)
				.append("\">")
				.append(text)
				.append("</a>")
				.toString();
	}
	
	
}
