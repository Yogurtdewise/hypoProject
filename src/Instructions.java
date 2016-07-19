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
 * The Instructions enum represents all operand code instructions of the
 *  HYPO Machine, and their respective execution times. Each enumeration
 *  is self-explanatory by its name. The constructor values are the
 *  op code and execution time, respectively.
 * 
 * @author Kenneth Chin
 *
 */
public enum Instructions
{

	HALT           ( 0,12),
	ADD            ( 1, 3),
	SUBTRACT       ( 2, 3),
	MULTIPLY       ( 3, 6),
	DIVIDE         ( 4, 6),
	MOVE           ( 5, 2),
	BRANCH         ( 6, 2),
	BRANCH_ON_MINUS( 7, 4),
	BRANCH_ON_PLUS ( 8, 4),
	BRANCH_ON_ZERO ( 9, 4),
	PUSH           (10, 2),
	POP            (11, 2),
	SYSTEM_CALL    (12,12);
	
	private int opCode = -1; //The OpCode for this instruction.
	private int exTime = -1; //The execution time for this instruction.
	
	/**
	 * Private constructor for the Instructions enum.
	 * @param opCode The int value representing the HYPO machine OpCode for this
	 *  instruction.
	 * @param exTime The int value representing the HYPO machine execution time
	 *  for this instruction's OpCode.
	 */
	private Instructions(int opCode, int exTime)
	{
		this.opCode = opCode;
		this.exTime = exTime;
	}
	
	/**
	 * Returns the OpCode for this instruction.
	 * @return The int representing the the HYPO machine OpCode for this
	 *  instruction.
	 */
	public int getOpCode()
	{
		return opCode;
	}
	
	/**
	 * Returns the execution time for this instruction.
	 * @return The int value representing the HYPO machine execution time
	 *  for this instruction's OpCode.
	 */
	public int getExecutionTime()
	{
		return exTime;
	}
}
