package bn.blaszczyk.roseservice.web;

public class HtmlBuilder {
	
	private final StringBuilder sb;
	
	private boolean evaluated = false;
	
	public HtmlBuilder()
	{
		sb = new StringBuilder("<html><body>");
	}
	
	public HtmlBuilder append(final String text)
	{
		sb.append(text);
		return this;
	}
	
	public HtmlBuilder header(final String text)
	{
		sb.append("<h1>")
			.append(text)
			.append("</h1>");
		return this;
	}
	
	@Override
	public String toString()
	{
		if(evaluated)
			throw new IllegalStateException("HtmlBuilder.toString must only be invoked once.");
		evaluated = true;
		return sb.append("</body></html>").toString();
	}
	
}
