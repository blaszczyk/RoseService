package bn.blaszczyk.roseservice;

public class RoseException extends Exception {

	private static final long serialVersionUID = 8175985689822621690L;

	public RoseException(final String message)
	{
		super(message);
	}
	
	public RoseException(final Throwable cause)
	{
		super(cause);
	}
	
	public RoseException(final String message, final Throwable cause)
	{
		super(message, cause);
	}
	
	public String getFullMessage()
	{
		StringBuilder sb = new StringBuilder(getMessage());
		Throwable cause = getCause();
		while(cause != null)
		{
			sb.append("\n").append(cause.getMessage());
			cause = cause.getCause();
		}
		return sb.toString();
	}

	public static RoseException wrap(final Exception cause, final String message)
	{
		if(cause instanceof RoseException)
			return (RoseException)cause;
		return new RoseException(message, cause);
	}
	
}
