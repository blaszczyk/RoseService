package bn.blaszczyk.roseservice.web;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.roseservice.RoseException;
import bn.blaszczyk.roseservice.client.RoseClient;
import bn.blaszczyk.roseservice.model.RoseDto;
import bn.blaszczyk.roseservice.server.Endpoint;
import bn.blaszczyk.roseservice.server.PathOptions;

public class WebEndpoint implements Endpoint {
	
	private static RoseClient client = new RoseClient();
	
	@Override
	public int get(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		try
		{
			String responseString = "";
			if(path.equals(""))
				responseString = HtmlTools.startPage();
			else
			{
				final PathOptions pathOptions = new PathOptions(path);
				if(!pathOptions.isValid())
					return HttpServletResponse.SC_NOT_FOUND;
				final Class<?> type = pathOptions.getType();
				if(pathOptions.hasId())
				{
					final RoseDto dto = client.getDto(type.getSimpleName().toLowerCase(), pathOptions.getId());
					responseString = HtmlTools.entityView(type, dto);
				}
				else
				{
					final List<RoseDto> dtos = client.getDtos(type.getSimpleName().toLowerCase());
					responseString = HtmlTools.entityList(type,dtos);
				}
			}

			response.getWriter().write(responseString);
			return HttpServletResponse.SC_OK;
		}
		catch (IOException e) {
			return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
	}

	@Override
	public int post(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
	@Override
	public int put(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
	@Override
	public int delete(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_OK;
	}
	
}
