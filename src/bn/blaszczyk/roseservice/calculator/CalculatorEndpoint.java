package bn.blaszczyk.roseservice.calculator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import bn.blaszczyk.rose.RoseException;
import bn.blaszczyk.roseservice.server.Endpoint;

public class CalculatorEndpoint implements Endpoint {

	private static final Pattern LONG_PATTERN = Pattern.compile("\\([0-9\\*\\+\\-]*\\)");
	
	private static final Pattern DOUBLE_PATTERN = Pattern.compile("\\([0-9\\*\\+\\-\\/\\.]*\\)");


	@Override
	public int get(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{

        response.setContentType("text/html; charset=utf-8");
        try
        {
        	final String responseString;
            if(validateLong(path))
            	responseString = evaluateRequestLong(path);
            else if(validateDouble(path))
            	responseString = evaluateRequestDouble(path);
            else
            	return HttpServletResponse.SC_BAD_REQUEST;
            response.getWriter().write(responseString);
        }
        catch(Exception e)
        {
        	return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
		return HttpServletResponse.SC_OK;
	}

	@Override
	public int post(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}

	@Override
	public int put(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}

	@Override
	public int delete(String path, HttpServletRequest request, HttpServletResponse response) throws RoseException
	{
		return HttpServletResponse.SC_NOT_FOUND;
	}

	@Override
	public Map<String, String> status()
	{
		return Collections.singletonMap("endpoint /calc", "active");
	}

	private String evaluateRequestLong(String target) throws IOException
	{
		final String requestString = target.replaceAll("\\s+", "")
				.replaceAll("(?<=[0-9])\\(", "*(")
				.replaceAll("\\)(?=[0-9])", ")*");
		return String.valueOf(evaluateLong(requestString));
	}

	private String evaluateRequestDouble(String target) throws IOException
	{
		final String requestString = target.replaceAll("\\s+", "")
				.replaceAll("(?<=[0-9])\\(", "*(")
				.replaceAll("\\)(?=[0-9])", ")*");
		return String.valueOf(evaluateDouble(requestString));
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
    			.map(s -> s.map( CalculatorEndpoint::parseDouble )
    					.reduce(1., CalculatorEndpoint::multiply))
    			.reduce(0., CalculatorEndpoint::add);
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
	
}
