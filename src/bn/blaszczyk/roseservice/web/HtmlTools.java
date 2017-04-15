package bn.blaszczyk.roseservice.web;

import java.util.List;

import bn.blaszczyk.rose.model.Entity;
import bn.blaszczyk.rose.model.EntityField;
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
	
	public static String entityList(final Entity entity, final List<RoseDto> dtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.header(entity.getSimpleClassName())
			.append(entityTable(entity, dtos));			
		return hb.toString();
	}

	public static String entityView(final Entity entity, final RoseDto dto, final List<List<RoseDto>> subDtos)
	{
		final HtmlBuilder hb = new HtmlBuilder();
		hb.header(entity.getSimpleClassName() + " id=" + dto.getId())
			.append(primitivesTable(entity, dto));
		for(int i = 0; i < entity.getEntityFields().size(); i++)
		{
			if(subDtos.get(i).isEmpty())
				continue;
			final EntityField field = entity.getEntityFields().get(i);
			if(field.getType().isSecondMany())
				hb.header(field.getCapitalName())
					.append(entityTable(field.getEntity(), subDtos.get(i)));
			else
			{
				final RoseDto subDto = subDtos.get(i).get(0);
				hb.header(linkTo(field.getCapitalName(),"/web/" + field.getEntity().getObjectName() + "/" + subDto.getId()))
					.append(primitivesTable(field.getEntity(), subDto));
			}
		}
		return hb.toString();
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
	
	private static String primitivesTable(final Entity entity, final RoseDto dto)
	{
		final StringBuilder sb = new StringBuilder("<table>");
		for(int i = 0; i < entity.getFields().size();i++)
			sb.append("<tr><td>")
				.append(entity.getFields().get(i).getName())
				.append("</td><td>")
				.append(dto.getFieldValue(i))
				.append("</td></tr>");
		return sb.append("</table>").toString();
	}
	
	private static String entityTable(final Entity entity, final List<RoseDto> dtos)
	{
		final StringBuilder sb = new StringBuilder("<table><tr><th>id");
		for(final Field field : entity.getFields())
			sb.append("</th><th>")
				.append(field.getName());
		sb.append("</th>");
		for(final RoseDto dto : dtos)
		{
			final String path = "/web/" + entity.getObjectName() + "/" + dto.getId();
			sb.append("</tr><tr><td>")
				.append(linkTo(String.valueOf(dto.getId()), path ));
			for(int i = 0; i < entity.getFields().size();i++)
				sb.append("</td><td>")
					.append(dto.getFieldValue(i));
			sb.append("</td>");
		}	
		return sb.append("</tr></table>").toString();
	}
	
}
