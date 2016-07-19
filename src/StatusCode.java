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
 * <p>
 * The StatusCode enum represents all possible exit codes of the hypoProject
 *  program. All enumerations whose value is >= 0 are "successful" status codes.
 *  All others (negative values) are error codes.
 *  </p><p>
 * Gaps in StatusCode values are intentional; as future updates to the hypoProject
 *  program may require this enumeration to be expanded.
 * </p>
 * 
 * @author Kenneth Chin
 *
 */
public enum StatusCode
{
	/**
	 * The running process is waiting for a message.
	 *  Send to Waiting Queue(WQ).
	 */
	WAITING_FOR_MESSAGE(4),
	
	/**
	 * The running process used all of its CPU timeslice.
	 */
	TIME_SLICE_EXPIRED(3),
	
	/**
	 * Halt command received.
	 */
	HALT(1),
	
	/**
	 * No error, status OKAY. May be used as a generic
	 *  "successful" status code.
	 */
	OKAY(0),
	
//Krishna: Just ERROR is not much helpful from user point of view.
//			It could be like Unknown status or unexpected error
//Ken: Fixed for HW2.
	/**
	 * A generic error used to initialize status variables, or
	 *  to indicate an unexpected program state.
	 */
	ERROR_UNEXPECTED_STATE(-1),
	
	
	
	//Errors generally associated with the Run class.
	
	/**
	 * Run.setStreams failed to redirect the PrintStreams,
	 *  or Run.main failed to append to the PrintStream.
	 */
	ERROR_PRINTSTREAM_FAILURE(-2),
	
	/**
	 * Run.closeStreams failed to close the FileOutputStream.
	 * Fatal error.
	 */
	ERROR_FILEOUTPUTSTREAM_FAILURE(-3),
	
	/**
	 * Run.main() received an invalid address.
	 */
	ERROR_RUN_INVALID_ADDRESS(-4),
	
	/**
	 * Run.main() could not obtain the filename associated
	 *  with a PID. This name is used for files whose
	 *  name could not be obtained.
	 */
	ERROR_RUN_INVALID_PID(-5),
	
	/**
	 * Run.main() could not select a process, since RQ is empty.
	 * Fatal error.
	 */
	ERROR_RUN_EMPTY_RQ(6),
	
	
	
	//Errors generally associated with the Hardware class.
	
	/**
	 * Hardware.dumpMemory address out of bounds.
	 */
	ERROR_DUMP_MEMORY_FAILED(-10),
	
	/**
	 * Hardware.set* attempt to set an invalid address.
	 */
	ERROR_HARDWARE_SET_FAILED(-11),

	
	
	//Errors generally associated with the Loader class.
	
	/**
	 * Loader.absoluteLoader method called without instance.
	 */
	ERROR_LOADER_MISUSE(-15),
	
	/**
	 * Loader.absoluteLoader unable to open file.
	 */
	ERROR_FILE_OPEN(-16),
	
	/**
	 * Loader.absoluteLoader unable to read file.
	 */
	ERROR_FILE_READ(-17),
	
	/**
	 * Loader.absoluteLoader could not close the input file stream.
	 */
	ERROR_FILE_CLOSE(-18),
	
	/**
	 * Loader.absoluteLoader read an invalid PC value.
	 */
	ERROR_LOADER_INVALID_PC(-19),
	
	/**
	 * Loader.absoluteLoader read an invalid address value.
	 */
	ERROR_LOADER_INVALID_ADDRESS(-20),
	
	/**
	 * Loader.absoluteLoader missing end of program indicator.
	 */
	ERROR_NO_END_OF_PROGRAM(-21),

	
	
	//Errors generally associated with the CPU class.
	
	/**
	 * CPU.fetchOperand received an invalid op mode.
	 */
	ERROR_FETCH_INVALID_MODE(-25),
	
	/**
	 * CPU.fetchOperand received an invalid address.
	 */
	ERROR_FETCH_INVALID_ADDRESS(-26),
	
	/**
	 * CPU.cpuExecuteProgram move or doArithmetic operation read
	 *  an immediate mode for op1 (invalid operation).
	 */
	ERROR_INVALD_DESTINATION_ADDRESS(-27),
	
	/**
	 * CPU.cpuExecuteProgram doArithmetic operation attempt
	 *  to divide by zero.
	 */
	ERROR_DIVIDE_BY_ZERO(-28),
	
	/**
	 * CPU.cpuExecuteProgram read an invalid PC value.
	 */
	ERROR_CPU_INVALD_PC(-29),
	
	/**
	 * CPU.cpuExecuteProgram read an op code for an unsupported
	 *  operation.
	 */
	ERROR_CPU_UNSUPPORTED_OPERATION(-30),
	
	/**
	 * CPU.decode read an unsupported opCode.
	 */
	ERROR_CPU_DECODE_INVALID_OPCODE(-31),
	
	/**
	 * CPU.decode read an unsupported opMode.
	 */
	ERROR_CPU_DECODE_INVALID_OPMODE(-32),
	
	/**
	 * CPU.decode read an unsupported GPR number.
	 */
	ERROR_CPU_DECODE_INVALID_GPR(-33),
	
	/**
	 * CPU.doSystemCall received an invalid opMode.
	 */
	ERROR_CPU_SYS_CALL_INVALID_OPMODE(-34),
	
	/**
	 * CPU.doSystemCall received an invalid opMode.
	 */
	ERROR_CPU_SYS_CALL_DOES_NOT_EXIST(-35),

	
	//Errors generally associated with the OS class.
	//NOTE: These errors may be fatal if they occur while creating
	// the "Null Process". See Run.loadNullProcess().
	
	/**
	 * OS.allocatMemory can not allocate OS memory
	 *  since there is no free memory to allocate.
	 */
	ERROR_OS_NO_MEM(-40),
	
	/**
	 * OS.allocateOSMemory can not allocate OS memory
	 *  since there is not enough free memory to allocate.
	 */
	ERROR_OS_NO_OS_MEM(-41),
	
	/**
	 * OS.allocateOSMemory: os_FreeList is an invalid value.
	 */
	ERROR_OS_INVALID_OS_ADDRESS(-42),
	
	/**
	 * OS.allocateOSMemory can not allocate user memory
	 *  since there is not enough free memory to allocate.
	 */
	ERROR_OS_NO_USER_MEM(-43),
	
	/**
	 * OS.allocateOSMemory: user_FreeList is an invalid value.
	 */
	ERROR_OS_INVALID_USER_ADDRESS(-44),
	
	/**
	 * A OS method was passed a memory size < 0.
	 */
	ERROR_OS_INVALID_SIZE(-45),
	
	/**
	 * OS.createProcess Received an invalid priority value.
	 */
	ERROR_OS_INVALID_PRIORITY(-46),
	
	/**
	 * OS.createProcess failed to create a valid PCB.
	 */
	ERROR_OS_PCB_FAILED(-47),
	
	/**
	 * OS.insertToRQ failed to insert PCB into RQ.
	 */
	ERROR_OS_RQ_INSERT_FAILED(-48),
	
	/**
	 * OS.insertToWQ failed to insert PCB into WQ.
	 */
	ERROR_OS_WQ_INSERT_FAILED(-49),
	
	/**
	 * OS.removeFromWQ failed to remove PCB into WQ.
	 */
	ERROR_OS_WQ_REMOVE_FAILED(-50),
	
	/**
	 * OS.freeOSMemory failed to free the specified allocation.
	 */
	ERROR_OS_OS_FREE_FAILURE(-51),
	
	/**
	 * OS.freeUserMemory failed to free the specified allocation.
	 */
	ERROR_OS_USER_FREE_FAILURE(-52),
	
	
	//Errors generally associated with the SystemCallID class.
	
	/**
	 * SystemCallID.MEM_ALLOC.doCall could not allocate the
	 *  requested user dynamic memory.
	 */
	ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE(-55),
	
	/**
	 * SystemCallID.MEM_FREE.doCall could not free the
	 *  specified user dynamic memory block.
	 */
	ERROR_SYSTEMCALL_MEM_FREE_FAILURE(-56);
	
	
	private int value = 0; //The int value of this StatusCode.
	
	/**
	 * The private constructor for the StatusCode enum.
	 * @param value //The int value associated with this StatusCode.
	 */
	private StatusCode(int value)
	{
		this.value = value;
	}
	
	/**
	 * Used to obtain the int value for this StatusCode.
	 * @return A int used to interpret StatusCodes.
	 */
	public int getValue()
	{
		return value;
	}
}
