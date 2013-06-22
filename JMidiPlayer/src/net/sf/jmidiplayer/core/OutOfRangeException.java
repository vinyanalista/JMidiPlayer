package net.sf.jmidiplayer.core;

/**
 * Exception to be thrown when some argument is out of range.
 * 
 * @author Antonio Vinicius Menezes Medeiros <vinyanalista@gmail.com>
 * @since 1.0
 */
public class OutOfRangeException extends Exception {

	private static final long serialVersionUID = 1L;

	/* Attributes */

	private int high;
	private int low;
	private String message;

	/**
	 * Constructs an <code>OutOfRangeException</code> and initializes it with
	 * the mismatched dimensions.
	 * 
	 * @param low
	 *            the lower bound of the range of valid values
	 * @param high
	 *            the higher bound of the range of valid values
	 */
	public OutOfRangeException(int low, int high) {
		this.low = low;
		this.high = high;
		message = "value must be in the range of " + low + " to " + high;
	}

	/**
	 * Constructs an <code>OutOfRangeException</code> and initializes it with
	 * the mismatched dimensions.
	 * 
	 * @param wrong
	 *            the argument that is out of range
	 * @param low
	 *            the lower bound of the range of valid values
	 * @param high
	 *            the higher bound of the range of valid values
	 */
	public OutOfRangeException(int wrong, int low, int high) {
		this.low = low;
		this.high = high;
		message = wrong + " is not a value in the range of " + low + " to "
				+ high;
	}

	/**
	 * Returns the upper bound of the range of valid values.
	 * 
	 * @return the upper bound of the range
	 */
	public int getHigh() {
		return high;
	}

	/**
	 * Returns the lower bound of the range of valid values.
	 * 
	 * @return the lower bound of the range
	 */
	public int getLow() {
		return low;
	}

	@Override
	public String getMessage() {
		return message;
	}

}