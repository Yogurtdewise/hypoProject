/**
 * <p>
 * <li>hypoProject</li>
 * <li>By: Kenneth Chin</li>
 * <li>Date: 04/15/2016</li>
 * <li>For: Homework# 2 (HW2)
 * </p>
 * <p>
 * Purpose:</br>
 * The hypoProject program follows specifications from the documents and in-class lecture
 *  from Professor Suban Krishnamoorthy of Framingham State University for his Spring 2016
 *  CSCI.465 - Operating System Internals class.</br>
 *  Documents include:</br>
 *  	<li>HYPO Machine - Spring 2013.doc</li>
 *  	<li>operating systems MTOPS.doc</li>
 *  	<li>Homework2 - Spring 2010.doc</li>
 *  	<li>Homework2 Pseudo Code - Correction.docx"HYPO Machine - Spring 2013.doc"</li>
 *  </br>
 *  All specifications are followed as closely as possible, where program functionality and
 *   JAVA convention conflict with such specification.
 *  </p>
 *  <p>
 * Convention Notes:</br>
 * 	Per Professor Krishnamoorthy, comments following JavaDoc convention are okay.
 *	 Methods from HW1 may retain commenting structure from HW1 specification.</br>
 *	Per Professor Krishnamoorthy, initialize all status variables to
 *	 StatusCode.OKAY instead of StatusCode.ERROR_UNEXPECTED_STATE.
 * </p>
 **/
package hypoProject;

/**
 * The OpMode enum represents all operand modes of the HYPO Machine.</br>
 *  Each enumeration is self-explanatory by its name. The constructor value
 *  is the op mode value associated to the op mode.
 * 
 * @author Kenneth Chin
 *
 */
public enum OpMode
{

	INVALID          (0),
	REGISTER_MODE    (1),
	REGISTER_DEFERRED(2),
	AUTO_INCREMENT   (3),
	AUTO_DECREMENT   (4),
	DIRECT_MODE      (5),
	IMMEDIATE_MODE   (6);
	
	private int value = -1; //The OpMode value.
	
	/**
	 * The private constructor for OpMode.
	 * @param value An int whose value represents the decimal OpMode for the HYPO machine.
	 */
	private OpMode(int value)
	{
		this.value = value;
	}
	
	/**
	 * Used to obtain the integer value for this OpMode.
	 * @return An int whose value represents the decimal OpMode for the HYPO machine.
	 */
	public int getValue()
	{
		return value;
	}
	
}
