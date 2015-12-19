package de.ilpt.plugins.googlefit;

import java.io.StringWriter;
import java.io.PrintWriter;

public class ExceptionMessageProvider {

	public static String getExceptionMessage(Exception e) {
    	StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionDetails = sw.toString();
		return exceptionDetails;
    }
}