package bn.blaszczyk.roseservice.rest;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ErrorHandler;

public class CalculatorService extends AbstractHandler {
	
	private static final String IMG_TAG = "<img src=\"calculator.png\" alt=\"Calculator\" style=\"width:32px;height:32px;\">";

	private static final Pattern LONG_PATTERN = Pattern.compile("\\([0-9\\*\\+\\-]*\\)");
	
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\([0-9\\*\\+\\-\\/\\.]*\\)");
	
	private static Server server;
	
	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException
	{
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        final String requestData = target.substring(1);
        try
        {
        	System.out.println(target);
        	if(requestData.contains("favicon.ico"))
        		Files.copy(new File("C:/Users/Michael/Desktop/Java Projects/RoseService/calculator.ico").toPath(), response.getOutputStream());
        	else if(requestData.contains("calculator.png"))
        		Files.copy(new File("C:/Users/Michael/Desktop/Java Projects/RoseService/calculator.png").toPath(), response.getOutputStream());
        	else if(requestData.contains("kill"))
            	stopServer(response.getWriter());
            else if(validateLong(requestData))
            	evaluateRequestLong(requestData, response.getWriter());
            else if(validateDouble(requestData))
            	evaluateRequestDouble(requestData, response.getWriter());
            else
            	response.getWriter().write("<h2>invalid request: " + requestData + "</h2>");
        }
        catch(Exception e)
        {
        	response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        	response.getWriter().write("<h2>error</h2>");
        	e.printStackTrace(response.getWriter());
        	e.printStackTrace();
        }
        finally
        {
        	baseRequest.setHandled(true);
        }
	}

	private void evaluateRequestLong(String target, Writer writer) throws IOException
	{
		final String requestString = target.replaceAll("\\s+", "")
				.replaceAll("(?<=[0-9])\\(", "*(")
				.replaceAll("\\)(?=[0-9])", ")*");
		final String responseString = requestString + "=" + evaluateLong(requestString);
		writer.write( "<h1>" + IMG_TAG + responseString + "</h1>");
		System.out.println("result: " + responseString);
	}

	private void evaluateRequestDouble(String target, Writer writer) throws IOException
	{
		final String requestString = target.replaceAll("\\s+", "")
				.replaceAll("(?<=[0-9])\\(", "*(")
				.replaceAll("\\)(?=[0-9])", ")*");
		final String responseString = requestString + "=" + evaluateDouble(requestString);
		writer.write( "<h1>" + IMG_TAG + responseString + "</h1>");
		System.out.println("result: " + responseString);
	}
	
	private void stopServer(final Writer responseWriter) throws IOException
	{
    	System.out.println("Server stop");
    	responseWriter.write("<h1> und tschüss </h1>");
        new Thread(() -> {
        	try{
        		Thread.sleep(3000);
        		server.stop();
        	}
        	catch(Exception e)
        	{
        		e.printStackTrace();
        	}
        	System.exit(0);
        }).start();
        return;
	}
	
	private static long evaluateLong(final String expression)
	{
		System.out.println("evaluating: " + expression);
		final Matcher matcher = LONG_PATTERN.matcher(expression);
		if(matcher.find())
		{
			final int start = matcher.start();
			final int end = matcher.end();
			final String reducedExpression = expression.substring(0,start) 
											+ evaluateInnerLong( expression.substring(start+1, end-1) ) 
											+ expression.substring(end);
			return evaluateLong(reducedExpression);
		}
		return evaluateInnerLong(expression);
	}

	private static long evaluateInnerLong(final String expression)
	{
		System.out.println("evaluating: " + expression);
		return Collections.singletonList(expression)
    			.stream()
    			.map(s -> s.replaceAll("(?<=[0-9])\\-", "+-")
    					.replaceAll("\\-\\-", "")
    					.split("\\+"))
    			.flatMap(Arrays::stream)
    			.map(s -> s.split("\\*"))
    			.map(Arrays::stream)
    			.map(s -> s.map( Long::parseLong )
    					.reduce(1L, Math::multiplyExact))
    			.reduce(0L, Math::addExact)
    			.longValue();
	}
	
	private static double evaluateDouble(final String expression)
	{
		System.out.println("evaluating: " + expression);
		final Matcher matcher = DOUBLE_PATTERN.matcher(expression);
		if(matcher.find())
		{
			final int start = matcher.start();
			final int end = matcher.end();
			final String reducedExpression = expression.substring(0,start) 
											+ evaluateInnerDouble( expression.substring(start+1, end-1) ) 
											+ expression.substring(end);
			return evaluateDouble(reducedExpression);
		}
		return evaluateInnerDouble(expression);
	}

	private static double evaluateInnerDouble(final String expression)
	{
		System.out.println("evaluating: " + expression);
		return Collections.singletonList(expression)
    			.stream()
    			.map(s -> s.replaceAll("(?<=[0-9])\\-", "+-")
    					.replaceAll("(?<=[0-9\\(\\)])\\/", "*/")
    					.replaceAll("\\-\\-", "")
    					.split("\\+"))
    			.flatMap(Arrays::stream)
    			.map(s -> s.split("\\*"))
    			.map(Arrays::stream)
    			.map(s -> s.map( CalculatorService::parseDouble )
    					.reduce(1., CalculatorService::multiply))
    			.reduce(0., CalculatorService::add);
	}
	
	private static double multiply(final double d1, final double d2)
	{
		return d1*d2;
	}
	
	private static double add(final double d1, final double d2)
	{
		return d1+d2;
	}
	
	private static double parseDouble(String text)
	{
		if(text.charAt(0) == '/')
			return 1. / Double.parseDouble(text.substring(1));
		return Double.parseDouble(text);
	}
	
	private static boolean validateDouble(final String expression)
	{
		String reducedExpression = "(" + expression + ")";
		while(DOUBLE_PATTERN.matcher(reducedExpression).find())
		{
			reducedExpression = reducedExpression.replaceAll(DOUBLE_PATTERN.pattern(), "");
		}
		return reducedExpression.length() == 0;
	}
	
	private static boolean validateLong(final String expression)
	{
		String reducedExpression = "(" + expression + ")";
		while(LONG_PATTERN.matcher(reducedExpression).find())
		{
			reducedExpression = reducedExpression.replaceAll(LONG_PATTERN.pattern(), "");
		}
		return reducedExpression.length() == 0;
	}
	
    public static void main( String[] args ) throws Exception
    {
    	System.out.println("Server start");
        server = new Server(1337);
        server.setHandler(new CalculatorService());
        server.setErrorHandler(new ErrorHandler(){

			@Override
			public void handle(String target, Request baseRequest, HttpServletRequest request,
					HttpServletResponse response) throws IOException
			{
				super.handle(target, baseRequest, request, response);
				System.err.println(target);
			}
        	
        });

        server.start();
        server.join();
    }
	
}
