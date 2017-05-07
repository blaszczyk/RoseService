package bn.blaszczyk.roseservice.server;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rosecommon.RoseException;
import bn.blaszczyk.rosecommon.tools.FileConverter;

public class FileEndpoint implements Endpoint {
	
	private final FileConverter converter = new FileConverter();

	@Override
	public int get(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final boolean existsRequest = request.getParameterMap().containsKey("exists");
			final File file = converter.fromPath(path);
			if(existsRequest)
				response.getWriter().write(Boolean.toString(file.exists()));
			else
			{
				if(!file.exists())
					return HttpServletResponse.SC_NOT_FOUND;
				Files.copy(file.toPath(), response.getOutputStream());
			}
		}
		catch (final Exception e) 
		{
			throw RoseException.wrap(e, "Error GET@/file");
		}
		return HttpServletResponse.SC_OK;
	}

	@Override
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_BAD_REQUEST;
	}

	@Override
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final File file = converter.fromPath(path);
			if(!file.isFile())
				return HttpServletResponse.SC_NOT_FOUND;
			Files.copy(request.getInputStream(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		catch (final Exception e) 
		{
			throw RoseException.wrap(e, "Error PUT@/file");
		}
		return HttpServletResponse.SC_NO_CONTENT;
	}

	@Override
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException
	{
		try
		{
			final File file = converter.fromPath(path);
			if(!file.exists())
				return HttpServletResponse.SC_NOT_FOUND;
			file.delete();
		}
		catch (final Exception e) 
		{
			throw RoseException.wrap(e, "Error DELETE@/file");
		}
		return HttpServletResponse.SC_NO_CONTENT;
	}

	@Override
	public Map<String, String> status()
	{
		return Collections.singletonMap("endpoint /path", "active");
	}

}
