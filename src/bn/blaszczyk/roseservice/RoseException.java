package bn.blaszczyk.roseservice;

public class RoseException extends Exception {

	private static final long serialVersionUID = 8175985689822621690L;

	public RoseException(String message)
	{
		super(message);
	}
	
	public RoseException(Throwable cause)
	{
		super(cause);
	}
	
	public RoseException(String message, Throwable cause)
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
	
}
