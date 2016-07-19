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
 * This class represents a process' Program Control Block(PCB), which should be located
 *  in the HYPO Mahine's OS Dynamic Memory area. It includes methods for initialization,
 *  PCB memory dumps, and PCB start address validation.
 * The PCBAddress enum is used to set and get PCB values, as well as to determine the
 *  memory offset for each value.
 * The ProcessState and ReasonForWait enums are used to specify and interpret valid
 *  values for the PCBAddress.STATE and PCBAddress.WAIT_CODE addresses.
 * Other public constants used to define valid PCB values include:
 * 	PRIORITY_MAX: Defines the largest valid value of PCBAddress.PRIORITY.
 *  DEFAULT_PRIORITY: Defines the default value of PCBAddress.PRIORITY.
 *  END_OF_LIST: Indicates PCBAddress.NEXT_PCB has no next PCB.
 * @author Kenneth Chin
 *
 */
public final class PCBController
{
	
	public static final long PRIORITY_MAX     = 255; //The maximum priority allowed (highest priority).
	public static final long DEFAULT_PRIORITY = 128; //The default priority of a process.
	public static final long END_OF_LIST = -1;       //A flag indicating the end of a PCB list.

	private PCBController(Hardware hardware){}

	/**
	 * Used to initialize a new PCB node. The new PCB will start at the specified address
	 *  and have the specified PID. Its priority will be set to the default priority of
	 *  128. All other values are initialized to 0.
	 * @param startAddress A long indicating the starting address of this PCB.
	 * @param pid A long indicating the ProcessID (PID) of this PCB.
	 * @param hardware The Hardware object whose memory will contain this PCB.
	 * @throws IndexOutOfBoundsException Thrown if startAddress or (startAddress + PCBAddress.values().length)
	 *  is out of OS Dynamic Memory bounds.
	 */
	public static void initializePCB(long startAddress, long pid, Hardware hardware)
		throws IndexOutOfBoundsException
	{
		//Throw IndexOutOfBoundsException if startAddress is not valid. 
		if(!isValidPCBAddress(startAddress))
			throw new IndexOutOfBoundsException("PCBController.initializePCB: The PCB start"
					+ "address(" + startAddress + ") is not in valid!");
		
		//Initialize all PCB values to 0.
		for(long i = startAddress; i < (startAddress + PCBAddress.values().length); i++)
			hardware.setMemAddressValue(i, 0);
		
		//Initialize the PCB's PID, priority, state, and next PCB values.
		PCBAddress.PID.saveContext(hardware, startAddress, pid);
		PCBAddress.PRIORITY.saveContext(hardware, startAddress, DEFAULT_PRIORITY);
		PCBAddress.STATE.saveContext(hardware, startAddress, ProcessState.READY.getValue());
		PCBAddress.NEXT_PCB.saveContext(hardware, startAddress, END_OF_LIST);
	}
	
	/**
	 * Used to print all PCB values of a PCB that starts at the given "startAddress".
	 *  Values are printed in accordance to the specifications provided in-class by
	 *  Professor Suban Krishnamoorthy (NOT as specified in "Homework2 - Spring 2010.doc").
	 * @param startAddress A long indicating the starting address of the PCB whose values are to be printed.
	 * @param hardware The Hardware object where the PCB is stored.
	 * @throws IndexOutOfBoundsException Thrown if startAddress is not within the bounds of
	 *  Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND and (Hardware.MEM_SIZE - PCBAddress.values().length).
	 */
	public static void dumpPCB(long startAddress, Hardware hardware) throws IndexOutOfBoundsException
	{
		//Throw IndexOutOfBoundsException if startAddress is not valid. 
		if(!isValidPCBAddress(startAddress))
			throw new IndexOutOfBoundsException("PCBController.dumpPCB: The PCB start address("
					+ startAddress + ") is not in valid!");
		
		
		//The long associated with this PCB's STATE value.
		long   stateVal = PCBAddress.STATE.dispatch(hardware, startAddress);
		String stateStr = null; //The String name associated with a PCB's STATE value.
		
		//The long associated with this PCB's WAIT_CODE value.
		long   waitVal  = PCBAddress.WAIT_CODE.dispatch(hardware, startAddress);
		String waitStr  = null; //The String name associated with a PCB's WAIT_CODE value.
		
		//Get the String name associated with this PCB's STATE value.
		for(ProcessState state:ProcessState.values())
		{
			if(state.getValue() == stateVal)
				stateStr = state.toString();
		}
		
		//Get the String name associated with this PCB's STATE value.
		for(ReasonForWait reason:ReasonForWait.values())
		{
			if(reason.getValue() == waitVal)
				waitStr = reason.toString();
		}
		
		//Print NEXT_PCB, PID, STATE, WAIT_CODE, and PRIORITY values on one line.
		System.out.println("Next = " + PCBAddress.NEXT_PCB.dispatch(hardware, startAddress)
				+ "\tPID = " + PCBAddress.PID.dispatch(hardware, startAddress)
				+ "\t\tState = " + stateStr + "\t\tReason = " + waitStr
				+ "\tPriority = " + PCBAddress.PRIORITY.dispatch(hardware, startAddress));
		//Print table header.
		System.out.println("GPR\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\tSP\tPC");
		
		//Print GPR0 address.
		System.out.print(startAddress + PCBAddress.GPR_0.getIndex() + "\t");
		
		//Print PCB GPR values
		for(long i = 0; i < Hardware.GPR_SIZE; i++)
		{
			//Print GPR value.
			String indexString = "GPR_" + i;
			System.out.print(PCBAddress.valueOf(indexString).dispatch(hardware, startAddress) + "\t");
		}
		
		//Print SP and PC.
		System.out.println(PCBAddress.SP.dispatch(hardware, startAddress)
				+ "\t" +PCBAddress.PC.dispatch(hardware, startAddress));
		System.out.println();
	}
	
	/**
	 * This method is used to determine if a specified PCB start address is within the
	 *  bounds of Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND and
	 *  (Hardware.MEM_SIZE - PCBAddress.values().length).
	 * If startAddress is within bounds (valid), return true; else return false.
	 * @param startAddress A long indicating the starting address of the PCB to be verified.
	 * @return A boolean indicating true if the specified PCB start address is valid for use as
	 *  a PCB, or false otherwise.
	 */
	public static boolean isValidPCBAddress(long startAddress)
	{
		//Ensure startAddress is valid.
		if(!Hardware.isOSDynamic(startAddress))
			return false;
		
		//Ensure the PCB will not exceed OS Dynamic Memory bounds.
		if(!Hardware.isOSDynamic(startAddress + PCBAddress.values().length))
			return false;
		return true;
	}
	
	/**
	 * This enumeration is used to describe possible "Reasons for Waiting".
	 * A PCB that is sent to the Waiting Queue(WQ) will remain in WQ until
	 *  the reason's event occurs.
	 * These values represent possible values stored in a PCB's
	 *  PCBAddress.WAIT_CODE index.
	 * @author Kenneth Chin
	 *
	 */
	public enum ReasonForWait
	{
		/**
		 * The process is not waiting, and should therefore
		 *  have a State of RUNNING or READY.
		 */
		NOT_WAITING(0),
		
		/**
		 * Process is waiting for a message.
		 */
		MESSAGE_ARRIVAL_EVENT(1),
		
		/**
		 * Process is waiting for an input event.
		 */
		INPUT_COMPLETION_EVENT(2),
		
		/**
		 * Process is waiting for an output event.
		 */
		OUTPUT_COMPLETION_EVENT(3);
		
		private int value = -1;
		
		/**
		 * The private constructor of the ReasonForWait enum.
		 * @param value An int value to be set in the PCBAddress.WAIT_CODE index
		 *  of a PCB that represents this "Reason for Waiting".
		 */
		private ReasonForWait(int value)
		{
			this.value = value;
		}
		
		/**
		 * Used to obtain the int value to be set in the PCBAddress.WAIT_CODE index
		 *  of a PCB that represents this "Reason for Waiting".
		 * @return An int value to be set in the PCBAddress.WAIT_CODE index
		 *  of a PCB that represents this "Reason for Waiting".
		 */
		public int getValue()
		{
			return value;
		}
	}
	
	/**
	 * This enumeration is used to indicate the state of a process.
	 * @author Kenneth Chin
	 *
	 */
	public enum ProcessState
	{
		/**
		 * The process is ready to use the CPU.
		 */
		READY(1),
		
		/**
		 * The process is currently using the CPU.
		 */
		RUNNING(2),
		
		/**
		 * The process is waiting for an event. (Ex. I/O to return)
		 */
		WAITING(3);
		
		private long value = -1; //The long value representing this ProcessState.
		
		/**
		 * The private constructor for the ProcessState enum.
		 * @param value The long value representing this ProcessState.
		 */
		private ProcessState(long value)
		{
			this.value = value;
		}
		
		/**
		 * Used to obtain the long value representing this ProcessState.
		 * @return The long value representing this ProcessState.
		 */
		public long getValue()
		{
			return value;
		}
	}
	
	/**
	 * This enumeration is used to index the offset of PCB data.
	 *  
	 *  Example:
	 *  	If a PCB starts at address 1000, then STATE is at
	 *  	1000 + PCBAddress.STATE.getIndex() = 1002.
	 *  
	 * Additionally, the convenience methods of saveContext() and dispatch()
	 *  may be used to set/get PCB values, given the PCB's start address.
	 *  
	 * @author Kenneth Chin
	 *
	 */
	public enum PCBAddress
	{
		/**
		 * The address of the next PCB node.
		 */
		NEXT_PCB(0),
		
		/**
		 * The PID of the process a PCB represents.
		 */
		PID(1),
		
		/**
		 * The state of a process. A state may be either
		 *  "Waiting", "Running", or "Ready".
		 */
		STATE(2),
		
		/**
		 * A code representing why a process is in the "Waiting" state.
		 */
		WAIT_CODE(3),
		
		/**
		 * A process' priority. This value may be 0 to 255. Default is 127.
		 */
		PRIORITY(4),
		
		/**
		 * The starting address of a process' stack.
		 */
		STACK_START(5),
		
		/**
		 * The size of a process' stack.
		 */
		STACK_SIZE(6),
		
		/**
		 * The starting address of a process' messages.
		 */
		MSG_QUEUE_START(7),
		
		/**
		 * The maximum size of all messages for a process.
		 */
		MSG_QUEUE_SIZE(8),
		
		/**
		 * The number of messages for a process.
		 */
		MSG_REMAINING(9),
		
		/**
		 * The value of GPR0.
		 */
		GPR_0(10),
		
		/**
		 * The value of GPR1.
		 */
		GPR_1(11),
		
		/**
		 * The value of GPR2.
		 */
		GPR_2(12),
		
		/**
		 * The value of GPR3.
		 */
		GPR_3(13),
		
		/**
		 * The value of GPR4.
		 */
		GPR_4(14),
		
		/**
		 * The value of GPR5.
		 */
		GPR_5(15),
		
		/**
		 * The value of GPR6.
		 */
		GPR_6(16),
		
		/**
		 * The value of GPR7.
		 */
		GPR_7(17),
		
		/**
		 * The value of the Stack Pointer.
		 */
		SP(18),
		
		/**
		 * The value of Program Counter.
		 */
		PC(19),
		
		/**
		 * A value that may be used for unspecified data.
		 */
		EXTRA(20);
		
		/**
		 * The number of addresses (offset) from a PCB's start address,
		 *  where this PCBAddress can be found.
		 *  
		 * Ex. PCB start address is 1000, then STATE is at
		 *  1000 + index = 1002.
		 */
		private long index = -1;
		
		/**
		 * Private constructor of the PCBAddress enum.
		 * Used to set the index for each enumeration.
		 * @param index The number of addresses (offset) from a PCB's
		 *  start address, where this PCBAddress can be found.
		 */
		private PCBAddress(long index)
		{
			this.index = index;
		}
		
		/**
		 * Used to obtain the number of addresses (offset) from a PCB's
		 *  start address, where this PCBAddress can be found.
		 * @return An long indicating the number of addresses (offset)
		 *  from a PCB's start address, where this PCBAddress can be found.
		 */
		public long getIndex()
		{
			return index;
		}
		
		/**
		 * Used to store the specified value into the PCB who's start address
		 *  is startAddress at this index's address.
		 * @param hardware The hardware object that contains the PCB to used in
		 *  the saving process.
		 * @param startAddress A long indicating the starting address of the PCB.
		 * @param value The long value to be saved to this PCB index.
		 * @return A boolean indicating true, if the value was successfully saved
		 *  or false otherwise.
		 */
		public boolean saveContext(Hardware hardware, long startAddress, long value)
		{
			if(isValidPCBAddress(startAddress))
			{
				hardware.setMemAddressValue(startAddress + this.getIndex(), value);
				return true;
			}else
				return false;
		}
	
		/**
		 * Used to obtain the value stored for this index, for the PCB who's start
		 *  address is specified.
		 * @param hardware The hardware object that contains the PCB to used in
		 *  the dispatch process.
		 * @param startAddress A long indicating the starting address of the PCB.
		 * @return A long indicating the value stored in the PCB at this index.
		 * @throws IndexOutOfBoundsException Thrown if startAddress + this.getIndex()
		 *  is not a valid PCB address.
		 */
		public long dispatch(Hardware hardware, long startAddress) throws IndexOutOfBoundsException
		{
			
			if(isValidPCBAddress(startAddress))
				return hardware.getMemAddressValue(startAddress + this.getIndex());
			else
				throw new IndexOutOfBoundsException("PCBController." + this.toString()
						+ ": The address " + startAddress + " is not a valid PCB address");
		}
	}
}
