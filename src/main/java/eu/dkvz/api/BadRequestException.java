package eu.dkvz.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

	
	private String message;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -902161692476057359L;

	public BadRequestException() {
		this.message = "HTTP Bad Request Exception";
	}
	
	public BadRequestException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
	
	
}
