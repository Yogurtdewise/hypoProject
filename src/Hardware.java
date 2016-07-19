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
 * The Hardware class is primarily a data class. It stores HYPO Machine memory, GPR registers,
 *  MAR, MBR, PC, SP, PSR, IR, and System Clock. Getters and setters for each hardware component
 *  are provided, as well as a dumpMemory method to display a subset of the hardware's values.
 * </p><p>
 * This class follows the singleton pattern, as there should only be one Hardware object per
 *  HYPO machine. Therefore, one must call Hardware.getInstance() to obtain the only instance
 *  of the Hardware class.</br>
 * </p><p>
 * Core methods from "HYPO Simulator Pseudo Code - Spring 2013.doc" specification
 *  contained in this class:</br>
 *  	dumpMemory(String displayString, long startAddress, long size)
 * </p>
 * 
 * @author Kenneth Chin
 *
 */
public final class Hardware {
	
	//Note: Java does not support arrays with indexes of type long. Indexes must be cast to type int.
	public static final int MEM_SIZE = 10000; //The size of the memory array
	public static final int GPR_SIZE = 8; 	  //The size of the gpr array
	
	//The lowest index of memory which may be used by a specific process.
	public static final int USER_PROGRAM_LOWER_BOUND        = 0;    //Range    0 - 1499
	public static final int USER_DYNAMIC_MEMORY_LOWER_BOUND = 1500; //Range 1500 - 5499
	public static final int OS_DYNAMIC_MEMORY_LOWER_BOUND   = 5500; //Range 5500 - 9999
	
	//PSR flags.
	public static final long USER_MODE = 2; //Used to indicate user mode in the PSR.
	public static final long OS_MODE   = 1; //Used to indicate OS mode in the PSR.

	private long[] memory = new long[MEM_SIZE]; //RAM, addresses 0 to (MEM_SIZE -1)
	private long[] gpr    = new long[GPR_SIZE]; //General purpose registers, 0 to 7
	private long   mar;		//Memory address buffer
	private long   mbr;		//Memory buffer register
	private long   clock;	//System clock
	private long   pc;		//Program Counter
	private long   sp;		//Stack pointer
	private long   psr;		//Processor status register
	private long   ir;		//instruction register
	
	private static Hardware singleInstance = null; //The singleton instance of Hardware class.
	
	private static Hardware backup = null; //The backup of singleInstance's current state.
	
	/**
	 * Private constructor for the Hardware class prevents instantiation.
	 */
	private Hardware()
	{
		initializeSystem();
	}
	
	/**
	 * Used to obtain the single instance of the Hardware class.
	 * @return The single instance of the Hardware class.
	 */
	public static Hardware getInstance()
	{
		if(singleInstance == null)
			singleInstance = new Hardware();
		return singleInstance;
	}
	
	/**
	 * Used to initialize all Hardware components to 0.
	 */
	private void initializeSystem()
	{
		for(int i = 0; i < MEM_SIZE; i++)
			memory[i] = 0;
		for(int i = 0; i < GPR_SIZE; i++)
			gpr[i] = 0;
		mar   = 0;
		mbr   = 0;
		clock = 0;
		pc    = 0;
		sp    = 0;
		psr   = OS_MODE; //OS is has control. Initialize to OS_MODE.
		ir    = 0;
	}
	
	/**********************************************************************************************
	 * method: dumpMemory
	 * Task Description:
	 *  Utility method used to print GPR, PC, SP, System Clock, and specified memory address
	 *   values. The format follows the specification from "HYPO Simulator Pseudo Code -
	 *   Spring 2013.doc".
	 *  First, a specified String is printed, followed by GPR values, followed by table column
	 *   titles for address offsets. Next, each row starts with the beginning address of the row,
	 *   followed by the 10 address values. If the specified start address is larger than the
	 *   memory size or less than 0, a IndexOutOfBoundsException thrown. If the specified size
	 *   extends beyond the maximum size of memory, then all memory values form startAddress to
	 *   MEM_SIZE-1 are printed, followed by an error message. In either error case, the final 2
	 *   lines printed are the values of the System Clock and PSR, respectively.
     *  
	 * Input parameters:
     * 	String displayString: The String to be printed before hardware values are printed.
     *  long   startAddress:  A long indicating the first memory address whose value is to be printed.
     *  long   size:          A long indicating the number of sequential memory values to print,
     *                         beginning at startAddress. Therefore the memory address values printed
     *                         will be from startAddress to (startAddress + size).
     * 
     * Output parameters
     * 	NONE
     * 
     * Function return value:
     *  NONE
     *  Throws IndexOutOfBoundsException if startAddress is less than 0 or >= MEM_SIZE,
	 *   or if startAddress + size >= MEM_SIZE.
     *  	
	 * @param displayString The String to be printed before hardware values are printed.
	 * @param startAddress A long indicating the first memory address whose value is to be printed.
	 * @param size A long indicating the number of sequential memory values to print, beginning
	 *  at startAddress. Therefore the memory address values printed will be from startAddress
	 *  to (startAddress + size).
	 * @throws IndexOutOfBoundsException Thrown under 3 conditions.
	 * 	1.) If startAddress is less than 0 or >= MEM_SIZE.
	 *  2.) If startAddress + size >= MEM_SIZE.
	 *  3.) If psr != OS_MODE and psr != USER_MODE.
	 * See Exception.getLocalizedMessage() for specific error details.
	 **********************************************************************************************/
	public void dumpMemory(String displayString, long startAddress, long size) throws IndexOutOfBoundsException
	{
		System.out.println(displayString);
		//Print GPR row title, GPR values, followed by two columns of zeros for unused columns.
		System.out.print("GPRs:\t\t");
		for(int i=0; i< GPR_SIZE; i++)
		{
//krishna: Supposed to print SP and PC here. value of PC and SP are not displayed
//Bug to fix
//Ken: We discussed this. I explicitly asked about this in-class. You misunderstood me
//		and told me not to print SP and PC.
//	   THis is fixed from HW1.
			System.out.print(gpr[i]);
			//Print SP then PC respectively, for last two values of the row..
			if(i == (GPR_SIZE-1))
				System.out.println("\t" + sp + "\t" + pc);
			else
				System.out.print("\t");
		}
		//Print memory column headers.
		System.out.print("Address\t\t+0\t+1\t+2\t+3\t+4\t+5\t+6\t+7\t+8\t+9");
		if(startAddress < 0 || startAddress >= MEM_SIZE)
			throw new IndexOutOfBoundsException("Hardware.dumpMemory: The startAddress of "
					+ startAddress + "is too big! (Max is " + (MEM_SIZE - 1) + ").");
		else
		{
			//Print row titles, and address values.
			int rowTitle = (int)startAddress;
			for(int i = (int)startAddress; i < (startAddress + size); i++)
			{
				if(i >= MEM_SIZE)
					throw new IndexOutOfBoundsException("Hardware.dumpMemory: startAddress + size is too big! "
							+"Terminating memory dump...");
				//Start a new line for the next row. Set the title for the next row.
				if(((i - startAddress)%10) == 0)
				{
					System.out.print("\r\n" + rowTitle + "\t\t");
					rowTitle = rowTitle + 10;
				}
				System.out.print(memory[i]);
				//If i is not the end of the row, add a tab.
				if(((i - startAddress)%10) != 9 && i != (startAddress + size - 1))
					System.out.print("\t");
			} //Done printing row titles, and address values.
			
			//Print System Clock and PSR value as String name.
			System.out.print("\r\nClock = " + clock + "\r\n");
			
			if(psr == Hardware.OS_MODE)
				System.out.println("PSR   = OS_MODE");
			else if(psr == Hardware.USER_MODE)
				System.out.println("PSR   = USER_MODE");
			else
				throw new IndexOutOfBoundsException("Hardware.dumpMemory: The PSR value of " + psr
						+ " is invalid!");
			System.out.println();
		}
	}

	/**
	 * Used to determine if the specified memory address is within
	 *  OS dynamic memory bounds.
	 * @param address The long indicating the memory address to be tested.
	 * @return A boolean indicating true if the specified address is within
	 *  OS dynamic memory bounds, false otherwise.
	 */
	public static boolean isOSDynamic(long address)
	{
		if(address < OS_DYNAMIC_MEMORY_LOWER_BOUND || address >= MEM_SIZE)
			return false;
		return true;
	}
	
	/**
	 * Used to determine if the specified memory address is within
	 *  User dynamic memory bounds.
	 * @param address The long indicating the memory address to be tested.
	 * @return A boolean indicating true if the specified address is within
	 *  User dynamic memory bounds, false otherwise.
	 */
	public static boolean isUserDynamic(long address)
	{
		if(address < USER_DYNAMIC_MEMORY_LOWER_BOUND || address >= OS_DYNAMIC_MEMORY_LOWER_BOUND)
			return false;
		return true;
	}
	
	/**
	 * Used to determine if the specified memory address is within
	 *  User program memory bounds.
	 * @param address The long indicating the memory address to be tested.
	 * @return A boolean indicating true if the specified address is within
	 *  User program memory bounds, false otherwise.
	 */
	public static boolean isUserProgram(long address)
	{
		if(address < USER_PROGRAM_LOWER_BOUND || address >= USER_DYNAMIC_MEMORY_LOWER_BOUND)
			return false;
		return true;
	}
	
	//Getters and Setter Methods
	
	/**
	 * Used to obtain the the value stored in the specified memory address.
	 * @param address The long value describing the address of memory whose value is to be obtained.
	 * @return The memory array of type long.
	 * @throws IndexOutOfBoundsException Thrown if address is not between 0 and (MEM_SIZE - 1).
	 */
	public long getMemAddressValue(long address) throws IllegalArgumentException
	{
		if(address < 0 || address >= MEM_SIZE)
			throw new IndexOutOfBoundsException("Hardware.getMemAddressValue the address " + address 
					+ " does not exist!");
		return memory[(int)address];
	}

	/**
	 * Used to set the value of a single memory address.
	 * @param address The long value describing the address of memory whose value is to be set.
	 * @param value The long value to be stored in the specified memory address.
	 * @throws IndexOutOfBoundsException Thrown if address is not between 0 and (MEM_SIZE - 1).
	 */
	public void setMemAddressValue(long address, long value) throws IndexOutOfBoundsException
	{
		if(address < 0 || address >= MEM_SIZE)
			throw new IndexOutOfBoundsException("Hardware.setMemAddressValue the address " + address 
					+ " does not exist!");
		else
			this.memory[(int)address] = value;
	}

	/**
	 * Used to set the memory array.
	 * @param memory The long array of size MEM_SIZE, that is to be set.
	 * @throws IllegalArgumentException Thrown if memory is not of size MEM_SIZE.
	 */
	public void setMemory(long[] memory) throws IllegalArgumentException
	{
		if(memory.length != MEM_SIZE)
			throw new IllegalArgumentException("Hardware.setMemory array must be of size " + MEM_SIZE + ".");
		else
			this.memory = memory;
	}

	/**
	 * Used to obtain the value of a single GPR register.
	 * @param gprNum The long value representing the GPR register whose value is to be obtained.
	 *  Value must be less than GPR_SIZE and greater than or equal to 0.
	 * @return The long value stored in the specified GPR register.
	 * @throws IndexOutOfBoundsException Thrown if gprNum is not between 0 and (GPR_SIZE -1).
	 */
	public long getGprRegisterValue(long gprNum) throws IllegalArgumentException
	{
		if(gprNum < 0 || gprNum >= GPR_SIZE)
			throw new IndexOutOfBoundsException("Hardware.getRegisterValue register " + gprNum
					+ " does not exist!");
		return gpr[(int)gprNum];
	}

	/**
	 * Used to set a single GPR register.
	 * @param gprNum The long value representing the GPR register to be set. Value must be
	 *  less than GPR_SIZE and greater than or equal to 0.
	 * @param value The long value to place into the specified register. 
	 * @throws IndexOutOfBoundsException Thrown if gprNum is not between 0 and (GPR_SIZE -1).
	 */
	public void setGprRegister(long gprNum, long value) throws IllegalArgumentException
	{
		if(gprNum < 0 || gprNum >= GPR_SIZE)
			throw new IndexOutOfBoundsException("Hardware.setRegister register " + gprNum
					+ " does not exist!");
		else
			this.gpr[(int)gprNum] = value;
	}

	/**
	 * Used to obtain the address of MAR (Memory Address Register).
	 * @return The long address stored in MAR.
	 */
	public long getMAR()
	{
		return mar;
	}

	/**
	 * Used to set the address of MAR (Memory Address Register).
	 * @param mar The address of type long to set the MAR to.
	 */
	public void setMAR(long mar)
	{
		this.mar = mar;
	}

	/**
	 * Used to obtain the address of MBR (Memory Buffer Register).
	 * @return The long address stored in MAR.
	 */
	public long getMBR()
	{
		return mbr;
	}

	/**
	 * Used to set the address of MBR (Memory Buffer Register).
	 * @param mbr The address of type long to set the MBR to.
	 */
	public void setMBR(long mbr)
	{
		this.mbr = mbr;
	}

	/**
	 * Used to obtain the time of the System Clock.
	 * @return The long value stored in the System Clock.
	 */
	public long getClock()
	{
		return clock;
	}

	/**
	 * Used to set the System Clock time.
	 * @param clock The value of type long to set the System Clock to.
	 */
	public void setClock(long clock)
	{
		this.clock = clock;
	}

	/**
	 * Used to obtain the address of the PC (Program Counter).
	 * @return The address of type long, stored in the PC.
	 */
	public long getPC()
	{
		return pc;
	}

	/**
	 * Used to set the address of the PC (Program Counter).
	 * @param pc The address of type long to set PC to.
	 * @throws IndexOutOfBoundsException Thrown if pc is >= MEM_SIZE, or
	 *  less than zero.
	 */
	public void setPC(long pc)
	{
		if(!Hardware.isUserProgram(pc))
			throw new IndexOutOfBoundsException("Hardware.setPC the pc value of " + pc 
				+ " is invalid! (MAX = " + MEM_SIZE + ")");
		this.pc = pc;
	}

	/**
	 * Used to obtain the address of the SP (Stack Pointer).
	 * @return The address of type long, stored in SP.
	 */
	public long getSP()
	{
		return sp;
	}

	/**
	 * Used to set the address of SP (Stack Pointer).
	 * @param sp The address of type long to set SP to.
	 * @throws IndexOutOfBoundsException Thrown if the specified sp address
	 *  is not in User Dynamic Memory.
	 */
	public void setSP(long sp)
	{
		if(!Hardware.isUserDynamic(sp))
			throw new IndexOutOfBoundsException("Hardware.setSP the sp value of " + sp 
				+ " is invalid! (RANGE = (" + USER_DYNAMIC_MEMORY_LOWER_BOUND + ", "
				+ (OS_DYNAMIC_MEMORY_LOWER_BOUND - 1) + ")");
		this.sp = sp;
	}

	/**
	 * Used to obtain the value of the PSR (Processor Status Register).
	 * @return The value of type long, stored in the PSR.
	 */
	public long getPSR()
	{
		return psr;
	}

	/**
	 * Used to set the value of the PSR (Processor Status Register). 
	 * @param psr The value of type long to set the PSR to.
	 * @throws IndexOutOfBoundsException Thrown if psr is not Hardware.OS_Mode or Hardware.USER_Mode.
	 */
	public void setPSR(long psr) throws IndexOutOfBoundsException
	{
		if(psr != OS_MODE && psr != USER_MODE)
			throw new IndexOutOfBoundsException("Hardware.setPSR: The PSR value of " + psr + " is invalid!");
		this.psr = psr;
	}

	/**
	 * Used to obtain the value of the IR (Instruction Register).
	 * @return The value of type long, stored in the IR.
	 */
	public long getIR()
	{
		return ir;
	}

	/**
	 * Used to set the value of the IR (Instruction Register).
	 * @param ir The value of type long to set the IR to.
	 */
	public void setIR(long ir)
	{
		this.ir = ir;
	}
	
	/**
	 * Used to create a backup of this Hardware object's current state.
	 * This backup is provided as a means of error recovery, without having
	 *  to rollback each operation.
	 * See doRestore() method to restore this Hardware object to the state
	 *  saved by the most recent call to this method.
	 * To avoid unchecked memory usage, only one backup is allowed at a time.
	 *  Therefore, calling this method destroys any previous backup.
	 * NOTE: While this operation is impossible within the confines
	 *  of HYPO Machine memory, this can be thought of as a backup
	 *  to a disk. A better approach would be to rollback any error.
	 *  However, considering the homework criteria, rollback is outside
	 *  the scope of the project.
	 */
	public void doBackup()
	{
		//Create and set a new Hardware object to act as a backup.
		Hardware.backup = new Hardware();
		
		//Backup RAM.
		for(long i = 0; i < MEM_SIZE; i++)
			backup.setMemAddressValue(i, getMemAddressValue(i));
		
		//Backup GPR's
		for(long i = 0; i < GPR_SIZE; i++)
			backup.setGprRegister(i, getGprRegisterValue(i));
		
		//Backup MAR, MBR, System Clock, PC, SP, PSR, and IR.
		backup.setMAR(getMAR());
		backup.setMBR(getMBR());
		backup.setClock(getClock());
		backup.setPC(getPC());
		//If SP hasn't been set by any process, leave SP at initial value. Else copy over.
		if(getSP() != 0)
			backup.setSP(getSP());
		backup.setPSR(getPSR());
		backup.setIR(getIR());
		
		//Backup complete. Do not backup the backup object. Only one backup allowed.
	}
	
	/**
	 * Used to restore this Hardware object to a state created by
	 *  doBackup().
	 * This backup is provided as a means of error recovery, without having
	 *  to rollback each operation.
	 * Since only one backup is allowed at a time, multiple calls to doRestore()
	 *  will only restore this Hardware object to the state saved by the most
	 *  recent call to doBackup().
	 * @throws IllegalStateException Thrown if doBackup() method has not been
	 *  called prior to calling this method (ie. no backup exists).
	 */
	public void doRestore() throws IllegalStateException
	{
		//Ensure a backup exists.
		if(backup == null)
			throw new IllegalStateException("Hardware.doRestore: Method called without creating a backup!");
		
		//Restore RAM.
		for(long i = 0; i < MEM_SIZE; i++)
			setMemAddressValue(i, backup.getMemAddressValue(i));
		
		//Restore GPR's
		for(long i = 0; i < GPR_SIZE; i++)
			setGprRegister(i, backup.getGprRegisterValue(i));
		
		//Restore MAR, MBR, System Clock, PC, SP, PSR, and IR.
		setMAR(backup.getMAR());
		setMBR(backup.getMBR());
		setClock(backup.getClock());
		//If SP hasn't been set by any process, leave SP at initial value. Else copy over.
		if(getSP() != 0)
			backup.setSP(getSP());
		setPC(backup.getPC());
		setPSR(backup.getPSR());
		setIR(backup.getIR());
	}
}
