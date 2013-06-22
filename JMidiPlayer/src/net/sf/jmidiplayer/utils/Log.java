package net.sf.jmidiplayer.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Utility class that serves only for debugging purposes.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 * 
 */
public class Log {
	// Logging is activated by default
	protected static boolean LOG = true;

	/**
	 * Obtains the current time as a string.
	 * 
	 * @return the current time
	 */
	protected static String now() {
		return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance()
				.getTime());
	}

	/**
	 * Writes an information message to the log. If logging is not enabled, a
	 * call to this method has no effect.
	 * 
	 * @param message
	 *            the information message to be written
	 * @see #turnOnLogging()
	 * @see #turnOffLogging()
	 */
	public static void write(String message) {
		if (LOG)
			System.out.println(now() + " - " + message);
	}

	/**
	 * Writes an error message to the log. If logging is not enabled, a call to
	 * this method has no effect.
	 * 
	 * @param message
	 *            the error message to be written
	 * @see #turnOnLogging()
	 * @see #turnOffLogging()
	 */
	public static void error(String message) {
		if (LOG)
			System.err.println(now() + " - " + message);
	}

	/**
	 * Activates logging.
	 * 
	 * @see #turnOffLogging()
	 */
	public void turnOnLogging() {
		LOG = true;
	}

	/**
	 * Deactivates logging.
	 * 
	 * @see #turnOnLogging()
	 */
	public void turnOffLogging() {
		LOG = false;
	}

}