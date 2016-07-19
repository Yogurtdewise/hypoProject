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
 * This enum is used to describe valid SystemCallID's of the HYPO Machine System.
 * Each SystemCallID's ID can be obtained using the getID() method.
 * Each SystemCallID has a doCall() method that performs the SystemCall specified
 *  by the SystemCallID.
 * As of HW2, only MEM_ALLOC and MEM_FREE perform actions other than throwing
 *  an UnsupportedOperationException.
 * NOTE: PSR is NOT set by any method of this enum.
 * 
 * @author Kenneth Chin
 *
 */
public enum SystemCallID
{
	/**
	 * Used to create a new process.
	 * Not implemented in HW2.
	 */
	PROCESS_CREATE(1)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.PROCESS_CREATE.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used to delete a process.
	 * Not implemented in HW2.
	 */
	PROCESS_DELETE(2)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.PROCESS_DELETE.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used to inquire about a process.
	 * Not implemented in HW2.
	 */
	PROCESS_INQUIRY(3)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.PROCESS_INQUIRY.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used to allocate user dynamic memory.
	 */
	MEM_ALLOC(4)
	{
		@Override
		/**
		 * Allocates a contiguous block of user dynamic memory.
		 * Input and Output for this operation is performed via hardware GPRs.
		 * The size of the block must be defined by hardware.getGprRegisterValue(2).
		 * The memory block is guaranteed to be of at least the specified size, if
		 *  such a block exists in the user_FreeList (see OS.user_FreeList).
		 * If there is not enough memory in the user_FreeList,
		 *  StatusCode.ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE is returned.
		 *  Otherwise, StatusCode.OKAY is returned on successful completion.
		 * Prints the SystemCall name, and the GPR input/output values below.
		 * 
		 * The GPR input/output of this System Call are as follows:
		 * 		INPUT:	GPR2 - The size of the requested user dynamic memory block.
		 * 		OUTPUT:	GPR0 - The return value (StatusCode) indicating success or
		 * 						failure of the MEM_ALLOC operation.
		 * 				GPR1 - The address in hardware where the requested
		 * 						user dynamic memory block starts.
		 * 
		 * NOTE: If the allocated memory block is larger than the requested size,
		 *  GPR2(size) will NOT be modified. This may cause the extra memory to be
		 *  permanently lost when memory is freed. This is caused by the fact that
		 *  the caller of this System Call probably won't know that extra memory
		 *  has been allocated. This behavior was specified in-class by Professor
		 *  Krishnamorthy.
		 *  
		 * @param hardware The Hardware object whose GPRs will be used for input/output.
		 * @param os The OS object that is managing the hardware.
		 * @return An int representing StatusCode.OKAY on successful memory allocation, or
		 *  StatusCode.ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE otherwise.
		 */
		public int doCall(Hardware hardware, OS os)
		{
			long size         = -1; //The size of the requested block.
			long startAddress = -1; //The return value of os.allocateUserMemory().
			
			//Get input from GPR.
			size = hardware.getGprRegisterValue(2);
			
			//Request the user dynamic memory block from OS.
			startAddress = os.allocateUserMemory(size);
			
			//Per "Homework2 - Spring 2010.doc" print info about this SystemCall.
			System.out.println("Performing SystemCall  : MEM_ALLOC");
			System.out.println("Input (Size    - GPR2) : " + size);
			
			//Set output values and return.
			if(startAddress < 0)
			{
				//Memory allocation failed. Set GPR0 error status and return error.
				hardware.setGprRegister(0,
						(long)StatusCode.ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE.getValue());
				
				//Print output info before returning.
				System.out.println("Output(Address - GPR1) : Unchanged");
				System.out.println("Output(Status  - GPR0) : "
						+ StatusCode.ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE.getValue());
				System.out.println();
				return StatusCode.ERROR_SYSTEMCALL_MEM_ALLOC_FAILURE.getValue();
			}
			else
			{
				//Memory allocation successful. Set GPR0 to OKAY, GPR1 to startAddress, and return OKAY.
				hardware.setGprRegister(0, (long)StatusCode.OKAY.getValue());
				hardware.setGprRegister(1, startAddress);
				
				//Print output info before returning.
				System.out.println("Output(Address - GPR1) : " + startAddress);
				System.out.println("Output(Status  - GPR0) : " + StatusCode.OKAY.getValue());
				System.out.println();
				return StatusCode.OKAY.getValue();
			}
		}
	},
	
	/**
	 * Used to release (free) user dynamic memory.
	 */
	MEM_FREE(5)
	{
		@Override
		/**
		 * Frees a specified block of contiguous user dynamic memory.
		 * Input and Output for this operation is performed via hardware GPRs.
		 * The start address of the block must be defined by hardware.getGprRegisterValue(1).
		 * The size of the block must be defined by hardware.getGprRegisterValue(2).
		 * Prints the SystemCall name, and the GPR input/output values below.
		 * 
		 * The GPR input/output of this System Call are as follows:
		 * 		INPUT:	GPR1 - The address in hardware where the user dynamic memory block
		 * 						to be released starts.
		 * 				GPR2 - The size of the user dynamic memory block to be released.
		 * 		OUTPUT:	GPR0 - The return value (StatusCode) indicating success or
		 * 						failure of the MEM_FREE operation.
		 * 
		 * Possible return values to GPR0 are:
		 * 		StatusCode.OKAY - If the operation was successful.
		 * 		StatusCode.ERROR_SYSTEMCALL_MEM_FREE_FAILURE - If the specified block
		 * 			of user dynamic memory could not be freed.
		 * 
		 * NOTE: Per "Homework2 - Spring 2010.doc" do not reset released dynamic memory.
		 * 
		 * @param hardware The Hardware object whose GPRs will be used for input/output.
		 * @param os The OS object that is managing the hardware.
		 * @return An int representing StatusCode.OKAY on successful memory recovery, or
		 *  StatusCode.ERROR_SYSTEMCALL_MEM_FREE_FAILURE otherwise.
		 */
		public int doCall(Hardware hardware, OS os)
		{
			//The return value of os.freeUserMemory().
			int  status       = StatusCode.OKAY.getValue();
			long startAddress = -1; //The start address of the block to be freed.
			long size         = -1; //The size of the block to be freed.
			
			//Get input values from GPRs.
			startAddress = hardware.getGprRegisterValue(1);
			size         = hardware.getGprRegisterValue(2);
			
			//Free the specified user dynamic memory block.
			status = os.freeUserMemory(startAddress, size);
			
			//Per "Homework2 - Spring 2010.doc" print info about this SystemCall.
			System.out.println("Performing SystemCall  : MEM_FREE");
			System.out.println("Input (Address - GPR1) : " + startAddress);
			System.out.println("Input (Size    - GPR2) : " + size);
			
			//Set output value to GPR0 and return.
			if(status < 0)
			{
				//Memory de-allocation failed. Set GPR0 to error status.
				hardware.setGprRegister(0, 
						(long)StatusCode.ERROR_SYSTEMCALL_MEM_FREE_FAILURE.getValue());
				System.out.println("Output(Status  - GPR0) : " + status);
				System.out.println();
				return StatusCode.ERROR_SYSTEMCALL_MEM_FREE_FAILURE.getValue();
			}
			else
			{
				//Memory de-allocation successful. Set GPR0 to OKAY.
				hardware.setGprRegister(0, StatusCode.OKAY.getValue());
				System.out.println("Output(Status  - GPR0) : " + status);
				System.out.println();
				return StatusCode.OKAY.getValue();
			}
		}
	},
	
	/**
	 * Used send a message to the process message queue.
	 * Not implemented in HW2.
	 */
	MSG_SEND(6)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.MSG_SEND.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used receive a message from the process message queue.
	 * Not implemented in HW2.
	 */
	MSG_RECEIVE(7)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.MSG_RECEIVE.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used get one character from the keyboard.
	 */
	IO_GETC(8)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.IC_GETC.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used print one character to the display (console).
	 */
	IO_PUTC(9)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.IC_PUTC.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used obtain the value of the system clock.
	 * Not implemented in HW2.
	 */
	TIME_GET(10)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.TIME_GET.doCall: Not implemented in HW2.");
		}
	},
	
	/**
	 * Used to set the system clock (does not force rescheduling).
	 * Not implemented in HW2.
	 */
	TIME_SET(11)
	{
		@Override
		public int doCall(Hardware hardware, OS os)
		{
			throw new UnsupportedOperationException
				("SystemCallID.TIME_SET.doCall: Not implemented in HW2.");
		}
	};
	
	//The SystemCallID for this system call.
	private int id = -1;
	
	/**
	 * The private constructor for the SystemCallID enumeration.
	 * @param id An int representing the SystemCallID for this system call.
	 */
	private SystemCallID(int id)
	{
		this.id = id;
	}
	
	/**
	 * Used to obtain the int representing the SystemCallID for this system call.
	 * @return An int representing the SystemCallID for this system call.
	 */
	public int getID()
	{
		return id;
	}
	
	/**
	 * Performs the function of this System Call.
	 * System Call input and output is done via hardware GPR values.
	 * This description should be overridden by the the specific SystemCallID's
	 *  doCall() method, unless unimplemented.
	 * NOTE: This was implemented with a return value, since GPR0 may not always
	 *  be used for return values.
	 * @param hardware The Hardware object whose GPRs will be used for input/output.
	 * @param os The OS object that is managing the hardware.
	 * @return An int representing a StatusCode. StatusCode.OKAY is used for successful
	 *  completion of the System Call. See the specific SystemCallID for other possible
	 *  return values.
	 * @throws UnsupportedOperationException Thrown if this System Call is not implemented.
	 */
	public abstract int doCall(Hardware hardware, OS os) throws UnsupportedOperationException;
}
