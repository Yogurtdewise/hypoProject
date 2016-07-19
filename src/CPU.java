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
 * The CPU class represents the HYPO Machine CPU. It is used to obtain instructions
 *  from memory, decode them, fetch operands, and process the instruction.
 * </p><p>
 * This class follows the singleton pattern, as there should only be one CPU object per
 *  HYPO machine. Therefore, one must call CPU.getInstance() to obtain the only instance
 *  of the CPU class.
 * </p><p>
 * Core methods from "HYPO Simulator Pseudo Code - Spring 2013.doc" specification
 *  contained in this class:</br>
 *  	cpuExecuteProgram()</br>
 *  	fetchOperand(int opMode, int opReg)
 * </p>
 * 
 * The TIME_SLICE value was chosen to ensure each program runs for at least 4 cycles
 * (per HW2 specifications). Here, a cycle is considered a call to cpuExecuteProgram()
 * from its initial call to its return.
 * Program execution times:
 *  	P1: 8 cycles
 *  	P2: 7 cycles
 *  	P3: 4 cycles
 * 
 * @author Kenneth Chin
 *
 */
public final class CPU
{
	//Variables used to debug user programs.
	private static final long    USED_USER_PROGRAM_SIZE = 127;   //The size of the user program area.
	private static final boolean IS_DEBUG_MODE          = false; //If true, dump user program area.
	
	private static final int  OPCODE_MAX = 12;  //The largest value supported for opCode.
	private static final int  OPCODE_MIN = 0;   //The smallest value supported for opCode.
	private static final long TIME_SLICE = 250; //The minimum change in hardware.getClock() per time slice.
	
	private static int opcode;     //The opCode for the current instruction.
	private static int op1_mode;   //The mode of op1, for the current instruction.
	private static int op1_GPR;    //The GPR register number of op1, for the current instruction.
	private static int op2_mode;   //The mode of op2, for the current instruction.
	private static int op2_GPR;    //The GPR register number of op2, for the current instruction.

	private static Hardware hardware  = null; //The Hardware object to be used by this CPU.
	private static CPU singleInstance = null; //The singleton instance of the CPU class.
	
	/**
	 * Private constructor to prevent instantiation.
	 * @param hardware The Hardware object whose memory is to be used.
	 */
	private CPU(Hardware hardware)
	{
		CPU.hardware = hardware;
//Krishna: Where is the status assuming this will be used in fetch operand?
//Ken: We discussed this in HW1.
//		Status is a local variable of the individual methods that call fetch operand(doArithmetic, branch, ect..).
//		The status variable is initialized within the method. Status is not global.
	}
	
	/**
	 * Used to obtain the single instance of the CPU class.
	 * @param hardware The Hardware object whose memory is to be used.
	 * @return The single instance of the CPU class.
	 */
	public static CPU getInstance(Hardware hardware)
	{
		if(singleInstance == null)
			singleInstance = new CPU(hardware);
		return singleInstance;
	}
	
	/**
	 * Used to initialize global values for the current instruction's opcode, op1 mode/register,
	 *  and op2 mode/register.
	 */
	private static void initInstructions()
	{
		opcode   = 0;
		op1_mode = 0;
		op1_GPR  = 0;
		op2_mode = 0;
		op2_GPR  = 0;
	}
	
//Krishna: method name is wrong (result of copy and paste I guess)
//Ken: Fixed from HW1. You are right. I copied the format from one method to another.
	/**********************************************************************************************
	 * method: cpuExecuteProgram
	 * Task Description:
	 *  This is the only public method of the CPU class. It is used to execute a user program
	 *  found in the Hardware object passed to CPU when CPU.getInstance() was called. A user
	 *  program should be loaded using the AbsolueLoader class, prior to calling cpuExecuteProgram.
	 *  This method will execute each instruction one at a time until one of the following three
	 *  conditions are met:
	 *  	1.) The HALT op code is read.
	 *  	2.) The total execution time of all instructions run since this method was called
	 *  		 is greater than or equal to TIME_SLICE.
	 *  	3.) An error occurs.
	 *  If an error occurs, the appropriate StatusCode will be returned; otherwise
	 *   the HALT status is returned.
     *  
	 * Input parameters:
     * 	NONE
     * 
     * Output parameters
     * 	Hardware hardware: The Hardware object containing HYPO machine data.
     * 
     * Function return value:
     *  StatusCode.HALT:  Returned if the HALT opcode was read.
     *  StatusCode.ERROR: Returned if a cpuExecuteProgram reached an unexpected state.
     *  StatusCode.ERROR_CPU_INVALD_PC: Returned if an invalid PC value was read.
	 *  StatusCode.ERROR_DIVIDE_BY_ZERO: If an attempt to divide by zero is read.
	 *  StatusCode.ERROR_INVALD_DESTINATION_ADDRESS: If op1 is in immediate mode, where
	 *   such a mode is invalid (ie. move(), doArithmetic()).
	 *  StatusCode.ERROR_CPU_UNSUPPORTED_OPERATION: If an op code for push, pop, or
	 *   system call is read.
	 *  StatusCode.ERROR_FETCH_INVALID_ADDRESS: If fetch process read an invalid address.
	 *  StatusCode.ERROR_FETCH_INVALID_MODE: If fetch process read an invalid op mode.
	 *  StatusCode.ERROR_HARDWARE_SET_FAILED: If an attempt to set an invalid address
	 *   to hardware was made.
	 *  StatusCode.ERROR_CPU_DECODE_INVALID_OPCODE:
	 *  StatusCode.ERROR_CPU_DECODE_INVALID_OPMODE:
	 *  StatusCode.ERROR_CPU_DECODE_INVALID_GPR:
	 * 
	 * @return An long indicating the HALT status has been read, or an error has occurred.
	 **********************************************************************************************/
	public int cpuExecuteProgram()
	{	
		int  status     = StatusCode.OKAY.getValue(); //The return status of instruction execution.
		long clockStart = hardware.getClock(); //The clock time at the start of cpuExecuteProgram.
		long pc         = -1; //The pc address of the current instruction.
		
//Krishna: Should there be != error (OK) in the condition below?
//Ken: We discussed this in HW1. The condition below checked for the HALT code. The other exit condition
//		was != error. This is checked at the end of the loop.
		
		//Process loaded user program until halt opCode, time slice expired, or error status received.
		while(status != StatusCode.HALT.getValue() && hardware.getClock() - clockStart < TIME_SLICE)
		{	
			status = StatusCode.OKAY.getValue(); //Initialize status.
			pc = hardware.getPC();
			
			//Check for valid pc value.
			if(!Hardware.isUserProgram(pc))
			{
				System.err.println("CPU.execute PC is out of range! PC = "
						+ pc + " (MAX = " + (Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND - 1) + ")");
				return StatusCode.ERROR_CPU_INVALD_PC.getValue();
			}
			//Fetch (read) the word pointed to by PC.
			hardware.setMAR(pc);    //Set MAR
//Krishna: This PC increment should be here after memory reading the word into MBR
//Ken: Fixed from HW1. Duplicate of comment below.
			hardware.setMBR(hardware.getMemAddressValue(hardware.getMAR())); //Set MBR to value of memory at index MAR.
			try
			{
//Krishna: This PC increment should happen after memory reading the word into MBR
//Ken: Fixed from HW1.
				hardware.setPC(pc + 1); //Increment PC.
			}
			catch(IndexOutOfBoundsException e)
			{
				System.err.println(e.toString());
				return StatusCode.ERROR_HARDWARE_SET_FAILED.getValue();
			}
			status = decode(hardware.getMBR());     //Decode the instruction.
//Krishna: This is the best place to check for invalid gpr# 8 and 9 and < 0 and invalid mode > 6 and < 0
//Ken: We discussed this in HW1. This was addressed at all calls that set/get GPR values. For efficiency, this
//		has been fixed to also check for invalid values in the decode() method.
			if(status <0)
				return status;
			//Execute the instruction.
			try
			{
				status = executeInstruction();
			}
			catch(UnsupportedOperationException e)
			{
				System.err.println(e.toString());
				return StatusCode.ERROR_CPU_UNSUPPORTED_OPERATION.getValue();
			}
			catch(IllegalStateException e)
			{
				System.err.println(e.toString());
				return StatusCode.ERROR_UNEXPECTED_STATE.getValue();
			}
			catch(IndexOutOfBoundsException e)
			{
				System.err.println(e.toString());
				return StatusCode.ERROR_HARDWARE_SET_FAILED.getValue();
			}
			
			//Print the user program area if IS_DEBUG_MODE == true.
			if(IS_DEBUG_MODE)
				hardware.dumpMemory("User Program Area(single instuction): Post-Execution", 0, USED_USER_PROGRAM_SIZE);
			
			//If there was an error executing the instruction, stop processing and return error status.
			if(status < 0)
				return status;
		}
		
		/**
		 * Both HALT and time slice expiration may occur at the same time.
		 * Give precedence to HALT code, since there are no more instructions to process.
		 */
		if(status == StatusCode.HALT.getValue())
		{
			System.out.println("Read HALT instruction at PC = " + pc + ".");
			System.out.println();
			return status;
		}
		else if(hardware.getClock() - clockStart >= TIME_SLICE)
			return StatusCode.TIME_SLICE_EXPIRED.getValue();
		else
			//Unexpected error. This code should be unreachable. Return general error.
			return StatusCode.ERROR_UNEXPECTED_STATE.getValue();
	}
	
	/**
	 * Used to set IR, and decode the instruction into opcode, op1_mode, op1_GPR, op2_mode, and op2_GPR.
	 * @param mbr The long value contained in Hardware.getMBR().
	 */
	private int decode(long mbr)
	{
		hardware.setIR(mbr);
		int ir = (int)hardware.getIR();
		initInstructions();
		opcode   =  ir / 10000;
		op1_mode = (ir % 10000) / 1000;
		op1_GPR  = (ir % 1000)  / 100;
		op2_mode = (ir % 100)   / 10;
		op2_GPR  =  ir % 10;

//Krishna: This is the best place to check for invalid gpr# 8 and 9 and < 0 and invalid mode > 6 and < 0
//Ken: We discussed this in HW1. This was addressed at all calls that set/get GPR values. For efficiency, this
//		has been fixed to also check for invalid values here.
		
		//Ensure instruction values are supported.
		if(opcode < OPCODE_MIN || opcode > OPCODE_MAX)
		{
			System.err.println("CPU.decode: opcode(" + opcode + ") is unsupported!");
			return StatusCode.ERROR_CPU_DECODE_INVALID_OPCODE.getValue();
		}
		if(op1_mode < 0 || op1_mode > OpMode.values().length)
		{
			System.err.println("CPU.decode: op1_mode(" + op1_mode + ") is unsupported!");
			return StatusCode.ERROR_CPU_DECODE_INVALID_OPMODE.getValue();
		}
		if(op2_mode < 0 || op2_mode > OpMode.values().length)
		{
			System.err.println("CPU.decode: op2_mode(" + op2_mode + ") is unsupported!");
			return StatusCode.ERROR_CPU_DECODE_INVALID_OPMODE.getValue();
		}
		if(op1_GPR < 0 || op1_GPR >= Hardware.GPR_SIZE)
		{
			System.err.println("CPU.decode: op1_GPR(" + op1_GPR + ") is unsupported!");
			return StatusCode.ERROR_CPU_DECODE_INVALID_GPR.getValue();
		}
		if(op2_GPR < 0 || op2_GPR >= Hardware.GPR_SIZE)
		{
			System.err.println("CPU.decode: op2_GPR(" + op2_GPR + ") is unsupported!");
			return StatusCode.ERROR_CPU_DECODE_INVALID_GPR.getValue();
		}
		
		//Instruction values are supported.
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * This helper method determines which operation the current instruction's opcode indicates.
	 *  It then executes steps to process that operation.
	 * @return An int indicating success or failure of the operation.
	 * @throws UnsupportedOperationException Thrown if the Push or Pop, opcode is received, or
	 *  if an unsupported SystemCallID is read. These operations are unsupported until
	 *  future assignments.
	 * @throws IllegalStateException Thrown if the opcode is not valid. Only opcodes 0 to 12 are
	 *  valid.
	 */
	private int executeInstruction() throws UnsupportedOperationException, IllegalStateException
	{
		
		//Determine the operation, execute it, and return a status.
		switch(opcode){
		
			case 0: //Halt
				//Add HALT execution time to system clock.
				hardware.setClock(hardware.getClock() + Instructions.HALT.getExecutionTime());
//Krishna; Where is the Halt status value is set? Is it final value or enum?
//Ken: We discussed this in HW1. The value was set to an integer within the Instructions enum constructor.
//		Enums are implied as static, and the value can not be changed, effectively making it final(but not explicitly).
				return StatusCode.HALT.getValue();
				
			case 1: //Add
			case 2: //Subtract
			case 3: //Multiply
			case 4: //Divide
				try
				{
					return doArithmetic(opcode);
				}
				catch(IllegalStateException e)
				{
					System.err.println(e.getLocalizedMessage());
					return StatusCode.ERROR_UNEXPECTED_STATE.getValue();
				}
			case 5: //Move
				return move();
			case 6: //Branch
				try
				{
//Krishna: Invalid address in PC is not checked
//Ken: We discussed this in HW1. This was checked in hardware.setPC, however it is only checked to be between 0 and 9999.
//		This range has been fixed for HW2, since PC is guaranteed to execute ONLY user programs.
					//Get the value stored in memory at the address in PC, then set PC to that value.
					hardware.setPC(hardware.getMemAddressValue(hardware.getPC()));
					//Add Branch execution time to system clock.
					hardware.setClock(hardware.getClock() + Instructions.BRANCH.getExecutionTime());
					return StatusCode.OKAY.getValue();
				}
				catch(IndexOutOfBoundsException e)
				{
					System.err.println(e.getLocalizedMessage());
					return StatusCode.ERROR_HARDWARE_SET_FAILED.getValue();
				}
			case 7: //Branch on Minus
			case 8: //Branch on Plus
			case 9: //Branch on Zero
				try{
					return branchOnCondition(opcode);
				}
				catch(IllegalStateException e)
				{
					System.err.println(e.getLocalizedMessage());
					return StatusCode.ERROR_UNEXPECTED_STATE.getValue();
				}
			case 10: //Push
				throw new UnsupportedOperationException("CPU.executeInstruction "
						+ "Push not implemented!");
			case 11: //Pop
				throw new UnsupportedOperationException("CPU.executeInstruction "
						+ "Pop not implemented!");
			case 12: //System Call
				return doSystemCall(op1_mode);
			default: throw new IllegalStateException("CPU.executeInstruction should"
					+ " not reach default case! OpCode: " + opcode);
		}
	}
	
	/**
	 * Used to do arithmetic operations. Writes the result to GPR number = op1_GPR
	 *  if op1_mode is Register Mode, or writes to the memory address pointed to by
	 *  GPR number = op1_GPR otherwise. If op1_mode is Immediate mode, no result is
	 *  written, and an error status is returned.
	 * @param opCode An int indicating the instruction's OpCode. Only opCode 1(add),
	 *  2(subtract), 3(multiply), and 4(divide) opCodes are supported by this method.
	 * @return An int indicating the success or failure of the doArithmetic operation.
	 *  StatusCodes include:
	 *   StatusCode.OKAY: On successful operation completion.
	 *   StatusCode.ERROR_DIVIDE_BY_ZERO: If an attempt to divide by zero is read.
	 *   StatusCode.ERROR_INVALD_DESTINATION_ADDRESS: If op1 is in immediate mode.
	 * @throws IllegalStateException Thrown if opCode < 1, or > 4.
	 * @throws IndexOutOfBoundsException Thrown if an invalid address is read.
	 */
	private int doArithmetic(int opCode) throws IllegalStateException, IndexOutOfBoundsException
	{
		//Initialize variables;
		int  status = StatusCode.OKAY.getValue();
		long result = 0;  //The result of the arithmetic operation.
		long exTime = 0;  //The execution time of the arithmetic operation.
		FetchResult result_op1 = null; //The result of the fetch operation on op1.
		FetchResult result_op2 = null; //The result of the fetch operation on op2.
		
		//Get first operand value.
		result_op1 = fetchOperand(op1_mode, op1_GPR);
		status     = result_op1.getStatus();
//Krishna: It is better to check status here and save execution of two assignments in case of error
//Ken: This is no longer an option since I now use a FetchResult object to store fetchOperand values
//		(as discussed in HW1).
		if(status < 0)
			return status;
				
		//Get second operand value.
		result_op2 = fetchOperand(op2_mode, op2_GPR);
//Krishna: It is better to check status here and save execution of two assignments in case of error
//Ken: This is no longer an option since I now use a FetchResult object to store fetchOperand values
//		(as discussed in HW1).
		status     = result_op2.getStatus();
		if(status < 0)
			return status;
			
		switch(opCode)
		{
			case 1: //Add
				result = result_op1.getOpValue() + result_op2.getOpValue();
				exTime = Instructions.ADD.getExecutionTime();
				break;
				
			case 2: //Subtract
				result = result_op1.getOpValue() - result_op2.getOpValue();
				exTime = Instructions.SUBTRACT.getExecutionTime();
				break;
					
			case 3: //Multiply
				result = result_op1.getOpValue() * result_op2.getOpValue();
				exTime = Instructions.MULTIPLY.getExecutionTime();
				break;
					
			case 4: //Divide
				if(result_op2.getOpValue() == 0)
				{
					System.err.println("Run.doArithmetic Attempt to divide by zero!");
					return StatusCode.ERROR_DIVIDE_BY_ZERO.getValue();
				}
				result = result_op1.getOpValue() / result_op2.getOpValue();
				exTime = Instructions.DIVIDE.getExecutionTime();
				break;
				
			default: //This case should not occur, throw IllegalStateException.
				result = 0;
				throw new IllegalStateException("CPU.doArithmetic should"
						+ " not reach default case! OpCode: " + opCode);
		}
				
		//Obtained both operands and result. Place result in the destination pointed to by op1.
		if(op1_mode == OpMode.REGISTER_MODE.getValue())
//Krishna: Invalid register number is not checked
//Ken: We discussed this in HW1. This was checked by the Hardware.setGprRegister method.
			hardware.setGprRegister(op1_GPR, result);
		else if(op1_mode == OpMode.REGISTER_DEFERRED.getValue())
		{
			long value = hardware.getGprRegisterValue(op1_GPR);
			if(!Hardware.isUserProgram(value) && !Hardware.isUserDynamic(value))
				return StatusCode.ERROR_INVALD_DESTINATION_ADDRESS.getValue();
//Krishna: Where is invalid memory address is checked?
//Ken: We discussed this in HW1. This was checked by the Hardware.setMemAddressValue method,
//		however it is only checked to be between 0 and 9999.
//	   As discussed, fetchOperand is guaranteed to only fetch operands from user program/dynamic
//		areas. Therefore this has been fixed for HW2.
			hardware.setMemAddressValue(value, result);
		}
		else if(op1_mode == OpMode.IMMEDIATE_MODE.getValue())
		{
			System.err.println("Run.doArithmetic Read immediate mode for destination address!");
			return StatusCode.ERROR_INVALD_DESTINATION_ADDRESS.getValue();
		}
		else if(op1_mode == OpMode.DIRECT_MODE.getValue())
		{
			long value = result_op1.getOpAddress();
			if(!Hardware.isUserProgram(value) && !Hardware.isUserDynamic(value))
				return StatusCode.ERROR_INVALD_DESTINATION_ADDRESS.getValue();
//Krishna: Where is invalid memory address is checked?
//Ken: We discussed this in HW1. This was checked by the Hardware.setMemAddressValue method,
//		however it is only checked to be between 0 and 9999.
//	   As discussed, fetchOperand is guaranteed to only fetch operands from user program/dynamic
//		areas. Therefore this has been fixed for HW2.
			hardware.setMemAddressValue(value, result);
		}
		//Result set. Increment system clock by the appropriate execution time.
		hardware.setClock(hardware.getClock() + exTime);
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to move the value of op2 to the destination address indicated by op1.
	 * @return An int indicating success or failure.
	 * @throws IndexOutOfBoundsException Thrown if an invalid address is read.
	 */
	private int move() throws IndexOutOfBoundsException
	{
//Krishna: See add instruction for my comments
//Ken: Same response. Addresses and GPR values were checked by the set methods.
//		Initialize status and results of fetch operations.
		int  status   = -1;
		FetchResult result_op1 = null;
		FetchResult result_op2 = null;
		
		//Get op1 address.
		result_op1 = fetchOperand(op1_mode, op1_GPR);
		status = result_op1.getStatus();
		if(status < 0)
			return status;
		
		//Get op2 value.
		result_op2 = fetchOperand(op2_mode, op2_GPR);
		status = result_op2.getStatus();
		if(status < 0)
			return status;
		
		//Write op2 value to the destination pointed to by op1.
		if(op1_mode == OpMode.REGISTER_MODE.getValue())
			hardware.setGprRegister(op1_GPR, result_op2.getOpValue());
		else if(op1_mode == OpMode.REGISTER_DEFERRED.getValue())
			hardware.setMemAddressValue(hardware.getGprRegisterValue(op1_GPR)
					, result_op2.getOpValue());
		else if(op1_mode == OpMode.IMMEDIATE_MODE.getValue())
		{
			System.err.println("Run.move Read immediate mode for destination address!");
			return StatusCode.ERROR_INVALD_DESTINATION_ADDRESS.getValue();
		}
		else
			hardware.setMemAddressValue(result_op1.getOpAddress(), result_op2.getOpValue());
		
		//Op2 moved to Op1. Increment system clock by the move execution time.
		hardware.setClock(hardware.getClock() + Instructions.MOVE.getExecutionTime());
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to determine if operand 1 is satisfies the condition specified by opCode.
	 *  If true, then set PC to the address pointed to by PC(address in next word);
	 *  otherwise skip this instruction by incrementing PC by one.
	 * @param opCode An int indicating the instruction's opCode.
	 * @return An int indicating success or failure.
	 * @throws IllegalStateException Thrown if the specified opCode is not supported
	 *  by this method.
	 * @throws IndexOutOfBoundsException Thrown if an invalid address is read.
	 */
	private int branchOnCondition(int opCode) throws IllegalStateException, IndexOutOfBoundsException
	{
		//Initialize status and result of fetch operation..
		int status = -1;
		FetchResult result = null;
		
		//Get the value of operand 1.
		result = fetchOperand(op1_mode, op1_GPR);
		status = result.getStatus();
		if(status < 0)
			return status;
		
		//If the value of operand 1 satisfies the opCode condition, set PC to the
		// value pointed to by PC; otherwise increment PC by 1 (skip branch statement).
		switch(opCode)
		{
			case 7: //Branch on Minus
				//Add BrOnMinus execution time to system clock.
				hardware.setClock(hardware.getClock() + Instructions.BRANCH_ON_MINUS.getExecutionTime());
				//If operand 1 value is negative, branch.
				if(result.getOpValue() < 0)
				{
//Krishna: Invalid address in the PC is not checked before indexing memory
//Ken: We discussed this in HW1. This was checked by the Hardware.setPC method,
//		however it is only checked to be between 0 and 9999.
//	   This has been fixed from HW1(see Hardware.setPC), since PC is guaranteed to ONLY point to
//		instructions from user program areas.
					hardware.setPC(hardware.getMemAddressValue(hardware.getPC()));
					return StatusCode.OKAY.getValue();
				}
				break;
				
			case 8: //Branch on Plus
				//Add BrOnPlus execution time to system clock.
				hardware.setClock(hardware.getClock() + Instructions.BRANCH_ON_PLUS.getExecutionTime());
				//If operand 1 value is positive, branch.
				if(result.getOpValue() > 0)
				{
//Krishna: Invalid address in the PC is not checked before indexing memory
//Ken: We discussed this in HW1. This was checked by the Hardware.setPC method,
//		however it is only checked to be between 0 and 9999.
//	   This has been fixed from HW1(see Hardware.setPC), since PC is guaranteed to ONLY point to
//		instructions from user program areas.
					hardware.setPC(hardware.getMemAddressValue(hardware.getPC()));
					return StatusCode.OKAY.getValue();
				}
				break;
				
			case 9: //Branch on Zero
				//Add BrOnZero execution time to system clock.
				hardware.setClock(hardware.getClock() + Instructions.BRANCH_ON_ZERO.getExecutionTime());
				//If operand 1 value is zero, branch.
				if(result.getOpValue() == 0)
				{
//Krishna: Invalid address in the PC is not checked before indexing memory
//Ken: We discussed this in HW1. This was checked by the Hardware.setPC method,
//		however it is only checked to be between 0 and 9999.
//	   This has been fixed from HW1(see Hardware.setPC), since PC is guaranteed to ONLY point to
//		instructions from user program areas.
					hardware.setPC(hardware.getMemAddressValue(hardware.getPC()));
					return StatusCode.OKAY.getValue();
				}
				break;
				
			default: throw new IllegalStateException("CPU.branchOnCondition"
					+ " should not reach default case! OpCode: " + opCode);
		
		}

		//Operand 1 did not meet the condition of the branch. Skip branch by incrementing PC.
		hardware.setPC(hardware.getPC() + 1);
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to obtain the SystemCallID and perform the specified system call. Prints an error
	 *  message on failure. Returns a StatusCode specific to the specified system call (
	 *  {@link hypoProject.SystemCallID See the specific SystemCallID enum for possible StatusCode values.}
	 *  ), or one of the following error codes:
	 *    ERROR_CPU_SYS_CALL_INVALID_OPMODE: If the specified opMode was not immediate mode.
	 *    ERROR_CPU_SYS_CALL_DOES_NOT_EXIST: If the specified SystemCallID does not exist. or
	 *     is unsupported.
	 * @param opMode An int indicating the operator mode specified by the system call. Only
	 *  immediate mode should be used, since the SystemCallID should be located in the next
	 *  word.
	 * @return An int indicating a StatusCode value for this system call. See method description
	 *  for details.
	 */
	private int doSystemCall(int opMode)
	{
		//SystemCallID must be in next word, therefore the only valid opMode is immediate mode.
		if(opMode != OpMode.IMMEDIATE_MODE.getValue())
			return StatusCode.ERROR_CPU_SYS_CALL_INVALID_OPMODE.getValue();
		
		//The return value of this system call.
		int status = StatusCode.ERROR_CPU_SYS_CALL_DOES_NOT_EXIST.getValue();
		int opReg  = 0;  //GPR for this operand is irrelevant. Use 0 as a default.
		int sysID  = -1; //The SystemCallID associated with this system call (cast to an int).
		
		//Set PSR to OS_MODE.
		hardware.setPSR(Hardware.OS_MODE);
		
		//Obtain the next word.
		FetchResult result = fetchOperand(opMode, opReg);
		
		//If an error occurred, print an error and pass the result to the caller.
		if(result.getStatus() < 0)
		{
			System.err.println("CPU.doSystemCall: fetchOperand returned an error ("
					+ result.getStatus() + ").");
			return result.getStatus();
		}
		
		sysID = (int)result.getOpValue();
		
		//Perform the specified system call.
		for(SystemCallID call:SystemCallID.values())
		{
			if(call.getID() == sysID)
			{
				try
				{
					status = call.doCall(hardware, OS.getInstance(hardware));
				}
				catch(UnsupportedOperationException e)
				{
					//The specified system call is not supported. Print error, set PSR to
					// USER_MODE, and return StatusCode.ERROR_CPU_SYS_CALL_DOES_NOT_EXIST.
					System.err.println(e.getLocalizedMessage());
					hardware.setPSR(Hardware.USER_MODE);
					return status;
				}
				break;
			}
		}
		
		//Set PSR to USER_MODE.
		hardware.setPSR(Hardware.USER_MODE);
		
		//If the specified SystemCallID does not exist, print an error and return.
		if(status == StatusCode.ERROR_CPU_SYS_CALL_DOES_NOT_EXIST.getValue())
		{
			System.err.println("CPU.doSystemCall: The SystemCallID (" + sysID + ") does not exist!");
			return status;
		}
		else if(status < 0)
		{
			//If the System Call returned an error, print an error and return.
			System.err.println("CPU.doSystemCall: The SystemCallID.doCall returned an error ("
					+ status + ").");
			return status;
		}
		
		//System Call successful. Increment system clock by SystemCall execution time, and return.
		hardware.setClock(hardware.getClock() + Instructions.SYSTEM_CALL.getExecutionTime());
		return status;
	}
	
	/**********************************************************************************************
	 * method: fetchOperand
	 * Task Description:
	 *  Used to obtain the operand address and value, given the specified opMode
	 *   and opReg, and store the values into into a FetchResult object.
     *  
	 * Input parameters:
     * 	int opMode The op mode for this operand.
     *  int opReg  The GPR register for this operand.
     * 
     * Output parameters
     *  Hardware hardware: The Hardware object containing HYPO machine data.
     * 
     * Function return value:
     *  FetchResult: Containing operand address, value, and one of the following
     *   StatusCodes:
     *     StatusCode.OKAY: If the fetch operation was successful.
     *     StatusCode.ERROR_FETCH_INVALID_ADDRESS: If an invalid address was read.
     *     StatusCode.ERROR_FETCH_INVALID_MODE:    If an invalid op mode was read.
     * 
	 * Used to set opAddress and opValue into a FetchResult object, given the
	 *  specified opMode and opReg.
	 * @param opMode An int representing the HYPO machine opMode for an operand.
	 * @param opReg An int indicating the GPR register where a direct, indirect,
	 *  or unused value or address is located.
	 * @return A FetchResult object containing operand address, value, and a 
	 *  StatusCode indicating success or failure of the fetchOperand operation.
	 **********************************************************************************************/
	private FetchResult fetchOperand(int opMode, int opReg)
	{
		//The FetchResult object used to store operand address, value, and operation
		// completion status.
		FetchResult result = new FetchResult();
		
		switch(opMode)
		{
		
			case 1: //Register Mode (value is in GPR)
				result.setOpAddress(-1); //Not used, set to invalid address.
//Krishna: Invalid register numbers > 7 and < 0 is not checked before indexing
//Bug
//Ken: We discussed this in HW1. Invalid register numbers could not be accessed.
//		Hardware.getGprRegisterValue throws an IndexOutOfBounds Exception, and is
//		caught by Run's exception handler.
				result.setOpValue(hardware.getGprRegisterValue(opReg));
				break;
				
			case 2: //Register Deferred Mode (address is in GPR)
//Krishna: Invalid register numbers > 7 and < 0 is not checked before indexing
//Bug
//Ken: We discussed this in HW1. Invalid register numbers can not be accessed.
//		Hardware.getGpr returns an array of size Hardware.GPR_SIZE. Any attempt
//      to access an invalid register implicitly throws an IndexOutOfBounds Exception,
//      and was caught by Run's exception handler.
//     Removed Hardware.getGPR and Hardware.setGPR in favor of the more explicit
//      methods Hardware.getGprRegisterValue and Hardware.setGprRegisterValue
				result.setOpAddress(hardware.getGprRegisterValue(opReg));
				if(!isValidAddress(result.getOpAddress()))
				{
					result.setStatus(StatusCode.ERROR_FETCH_INVALID_ADDRESS.getValue());
					return result;
				}
				else
					result.setOpValue(hardware.getMemAddressValue((int)result.getOpAddress()));
				break;
				
			case 3: //Auto-Increment mode (Set opValue, then increment GPR value)
//Krishna: Invalid register numbers > 7 and < 0 is not checked before indexing
//Bug
//Ken: We discussed this in HW1. Invalid register numbers can not be accessed.
//		Hardware.getGprRegisterValue throws an IndexOutOfBounds Exception, and is
//		caught by Run's exception handler.
				result.setOpAddress(hardware.getGprRegisterValue(opReg));
				if(!isValidAddress(result.getOpAddress()))
				{
					result.setStatus(StatusCode.ERROR_FETCH_INVALID_ADDRESS.getValue());
					return result;
				}
				result.setOpValue(hardware.getMemAddressValue((int)result.getOpAddress()));
				hardware.setGprRegister(opReg, hardware.getGprRegisterValue(opReg) + 1);
				break;
				
			case 4: //Auto-Decrement Mode (decrement GPR value, then set opValue)
//Krishna: Invalid register numbers > 7 and < 0 is not checked before indexing
//Bug
//Ken: We discussed this in HW1. Invalid register numbers can not be accessed.
//		Hardware.getGprRegisterValue throws an IndexOutOfBounds Exception, and is
//		caught by Run's exception handler.
				hardware.setGprRegister(opReg, hardware.getGprRegisterValue(opReg) - 1);
				result.setOpAddress(hardware.getGprRegisterValue(opReg));
				if(!isValidAddress(result.getOpAddress()))
				{
					result.setStatus(StatusCode.ERROR_FETCH_INVALID_ADDRESS.getValue());
					return result;
				}
				result.setOpValue(hardware.getMemAddressValue(result.getOpAddress()));
				break;
			
			case 5: //Direct Mode (address is in next word)
//Krishna: Invalid register numbers > 7 and < 0 is not checked before indexing
//Bug
//Ken: We discussed this in HW1. Invalid register numbers can not be accessed.
//		Hardware.getGprRegisterValue throws an IndexOutOfBounds Exception, and is
//		caught by Run's exception handler.
				result.setOpAddress(hardware.getMemAddressValue(hardware.getPC()));
				hardware.setPC(hardware.getPC() + 1);
				if(!isValidAddress(result.getOpAddress()))
				{
					result.setStatus(StatusCode.ERROR_FETCH_INVALID_ADDRESS.getValue());
					return result;
				}
				result.setOpValue(hardware.getMemAddressValue(result.getOpAddress()));
				break;
				
			case 6: //Immediate Mode (next word contains opValue)
				result.setOpAddress(-1); //Not used, set to invalid address.
//Krishna: Invalid address in the PC is not checked before indexing memory
//Ken: We discussed this in HW1. This was checked by the Hardware.setPC method,
//		however it is only checked to be between 0 and 9999.
//	   This has been fixed from HW1(see Hardware.setPC), since PC is guaranteed to ONLY point to
//		instructions from user program areas. Since PC can not be set with invalid values,
//		Hardware.getPC is guaranteed to be valid.
				result.setOpValue(hardware.getMemAddressValue(hardware.getPC()));
				hardware.setPC(hardware.getPC() + 1);
				break;
				
			default: //Invalid mode
				System.err.println("CPU.fetchOperand The opMode of " + opMode
						+ " is invalid!");
				result.setStatus(StatusCode.ERROR_FETCH_INVALID_MODE.getValue());
				return result;
		}
		//Successfully fetched opAddress and opValue.
		result.setStatus(StatusCode.OKAY.getValue());
		return result;
	}
	
	/**
	 * This inner class is used to store the results of a fetchOperand() operation.
	 *  It stores the operand address, operand value, and return status of the
	 *  fetchOperand() method.
	 * @author Kenneth Chin
	 *
	 */
	private class FetchResult
	{
		private long opAddress = -1; //The operand's address.
		private long opValue   = 0;  //The operand's value.
		private int  status    = StatusCode.OKAY.getValue(); //The return status of fetchOperand().
		
		
		/**
		 * The constructor used to obtain a FetchResult object whose values
		 *  are initialized to default values.
		 */
		public FetchResult(){}
		
		/**
		 * Used to obtain the operand address of this FetchResult.
		 * @return A long indicating the operand address of this FetchResult.
		 */
		public long getOpAddress() {
			return opAddress;
		}

		/**
		 * Used to set the operand address of this FetchResult.
		 * @param opAddress A long indicating the operand address of this FetchResult.
		 */
		public void setOpAddress(long opAddress) {
			this.opAddress = opAddress;
		}

		/**
		 * Used to obtain the operand value of this FetchResult.
		 * @return A long indicating the operand value of this FetchResult.
		 */
		public long getOpValue() {
			return opValue;
		}

		/**
		 * Used to set the operand value of this FetchResult.
		 * @param opValue A long indicating the operand value of this FetchResult.
		 */
		public void setOpValue(long opValue) {
			this.opValue = opValue;
		}

		/**
		 * Used to obtain the status of this FetchResult.
		 * @return An int indicating the StatusCode of this FetchResult.
		 */
		public int getStatus() {
			return status;
		}

		/**
		 * Used to set the status of this FetchResult.
		 * @param status An int indicating the StatusCode of this FetchResult.
		 */
		public void setStatus(int status) {
			this.status = status;
		}
	}
	
	/**
	 * A helper method for the fetchOperand method. Prints an error message if the specified
	 *  address is not in the range 0 to Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND - 1.
	 * @param address The address of type long to be verified.
	 * @return A boolean indicating true if the specified address is valid, false otherwise.
	 */
	private boolean isValidAddress(long address)
	{
//Krishna: Checking for upper limit of 9999 is not correct since user processes cannot access
//			protected OS dynamic memory area. Hence, upper limit should be set to end of user
//			dynamic area
//Bug
//Ken: We discussed this in HW1. Since there was no way to tell which process is an OS process and 
//		which process is a user process, this was done intentionally.
//	   Fixed form HW1 since all processes are guaranteed to be ONLY user processes.
		if(!Hardware.isUserProgram(address) && !Hardware.isUserDynamic(address))
		{
			System.err.println("CPU.fetchOperand The address of " + address
					+ " is too large! (RANGE = (" + Hardware.USER_PROGRAM_LOWER_BOUND
					+ ", " + (Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND - 1) + ")");
			return false;
		}
		return true;
	}
	
}
