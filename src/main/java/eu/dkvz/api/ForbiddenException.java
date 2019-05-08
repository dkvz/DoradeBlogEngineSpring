package eu.dkvz.api;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

	public ForbiddenException() {
		super("Forbidden");
	}

	public ForbiddenException(String message) {
		super(message);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1658122236429911397L;

}

