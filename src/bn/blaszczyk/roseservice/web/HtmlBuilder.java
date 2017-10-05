package bn.blaszczyk.roseservice.web;

public class HtmlBuilder {
	
	private final StringBuilder sb;
	
	private boolean evaluated = false;
	
	public HtmlBuilder()
	{
		sb = new StringBuilder("<html><body>");
	}
	
	public HtmlBuilder append(final Object text)
	{
		sb.append(text);
		return this;
	}
	
	public HtmlBuilder h1(final String text)
	{
		sb.append("<h1>")
			.append(text)
			.append("</h1>");
		return this;
	}
	
	public HtmlBuilder h2(final String text)
	{
		sb.append("<h2>")
			.append(text)
			.append("</h2>");
		return this;
	}

	public HtmlBuilder br()
	{
		sb.append("<br/>");
		return this;
	}
	
	public String build()
	{
		if(evaluated)
			throw new IllegalStateException("HtmlBuilder.toString must only be invoked once.");
		evaluated = true;
		return sb.append("</body></html>").toString();
	}
	
}
