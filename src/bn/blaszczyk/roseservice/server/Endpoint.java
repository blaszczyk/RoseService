package bn.blaszczyk.roseservice.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rose.RoseException;

public interface Endpoint {

	public int get(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException;
	public int post(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException;
	public int put(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException;
	public int delete(final String path, final HttpServletRequest request, final HttpServletResponse response) throws RoseException;
	
}
