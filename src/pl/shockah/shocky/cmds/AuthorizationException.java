package pl.shockah.shocky.cmds;

public class AuthorizationException extends RuntimeException {
	private static final long serialVersionUID = -8852728904302675440L;
	
	public AuthorizationException(String message) {
		super(message);
	}
}
