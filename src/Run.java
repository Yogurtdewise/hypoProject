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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Scanner;

/**
 * <p>
 * This class is the main class of the hypoProject program.</br>
 * It represents the interface between the user, the HYPO Machine, and the Java VM.
 * </p><p>
 * The purpose of the hypoProject is to perform the following actions:</br>
 *  <p>
 *     (1) Obtain a file name from console input. 
 *          The specified file should be a text file, written in HYPO Machine language.
 *     (2) Read the specified file, and load it into hypoProject.Hardware memory.
 *     (3) Display a relevant subset of memory data to console.
 *     (4) Execute the loaded instructions as per HYPO Machine specifications.
 *     (5) Display the post-execution subset of memory data to console.
 *     (6) Upon any fatal error, an error message is printed to console and the
 *     		hypoProject processes is terminated. The JVM is terminated with an
 *     		exit status specific to the type of error that occurred.
 *     (7) Upon any non-fatal error, an error message is printed to console and
 *          the hypoProject process is allowed to continue.
 *     (8) All console input and output shall be written to a text file located
 *          in the hypoProject program's directory.
 *     (9) Upon any termination of the hypoProject, the JVM shall return an exit
 *          status >=0 upon successful termination, or < 0 upon error.
 *  </p>
 *          
 * @author Kenneth Chin
 *
 */
public class Run {
	
	/**
	 * The total size of all anticipated used user dynamic memory. This number of memory
	 *  allocations has been pre-determined by HW2 specifications. Per in-class specification,
	 *  only dump these addresses. To determine the anticipated size, the following are summed.
	 *  
	 * 		Null Process Stack  = 10
	 * 		Program 1 stack     = 10
	 * 		Program 1 mem_alloc = 150
	 * 		Program 2 stack     = 10
	 * 		Program 2 mem_alloc = 75
	 * 		Program 3 stack     = 10
	 * 		Program 3 mem_alloc = 100
	 * 		Sum                 = 365
	 *  
	 */
	private static final long    USED_USER_DYNAMIC_SIZE = 365;
	
	//Variables used to debug user programs.
	private static final long    USED_USER_PROGRAM_SIZE = 127;   //The size of the user program area.
	private static final boolean IS_DEBUG_MODE          = false; //If true, dump user program area.
	
	//The file to which System.out and System.err are written to, in parallel with console.
	private static final File   OUTPUT_FILE   = new File("Chin - HW2_hypoProject_output.txt");
	
	//The file containing the "Null Process" program.
	private static final String NULL_FILENAME = "Chin - HW2_NullProcess_Machine.txt";
	private static final long   NULL_PRIORITY = 0; //The priority of the "Null Process".
	
	//The FileOutputStream used to write outputFile by setStreams().
	private static FileOutputStream fileOutput;
	private static PrintStream stdOutOld = null; //System.out default PrintStream
	private static PrintStream stdErrOld = null; //System.err default PrintStream
	private static Scanner     scan      = null; //The console input object.
	
	private static boolean shutdown   = false; //Flag used to indicate the HYPO Machine should shutdown.
	private static int     exitStatus = StatusCode.OKAY.getValue(); //Used to pass a System.exit(exitStatus).
	
	private static Hardware hardware  = null; //The Hardware object used to store HYPO machine data.
	private static CPU      cpu       = null; //The object used to execute the user program.
	private static OS		os		  = null; //The object used to handle process management.
	
	/**
	 * <p>
	 * Used to catch any Throwable exceptions not caught by other try/catch
	 *  statements in the hypoProject.
	 * </p><p>
	 * Displays information pertinent to the exception, then terminates the JVM.
	 *  The exit status of StatusCode.ERROR_HARDWARE_SET_FAILED is used if
	 *  the exception was IndexOutOfBoundsException(the most likely cause of an
	 *  uncaught error) or the exit status of StatusCode.ERROR (unspecified error)
	 *  is used otherwise.
	 * </p><p>
	 * NOTE: This is called before the thread terminates.
	 * </p>
	 */
	private static Thread.UncaughtExceptionHandler globalExceptionHandler
		= new Thread.UncaughtExceptionHandler()
    {
        @Override
        public void uncaughtException(Thread t, Throwable e)
        {	
        	System.err.println();
        	System.err.println("An unexpected error occured. Closing hypoProject...");
        	System.err.println("Thread Name: " + t.getName());
        	System.err.println("Exception: " + e.toString());
        	System.err.println("Stacktrace:");
        	e.printStackTrace();
        	if(e instanceof IndexOutOfBoundsException)
        	{
        		System.exit(StatusCode.ERROR_HARDWARE_SET_FAILED.getValue());
        	}
        	System.exit(StatusCode.ERROR_UNEXPECTED_STATE.getValue());
        }
    };

    /**********************************************************************************************
	 * method: main
	 *
     *  Task Description: This method is the entry point to the hypoProject program.
     *   It governs the sub-tasks of:
     *   	 (1) Setting up the Java VM and initializing the HYPO Machine system. Tasks include:
     *   			a.) Setting up Java VM I/O streams.
     *   			b.) Setting up Java UncaughtExceptionHandler
     *   			c.) Initializing HYPO Machine hardware, CPU, and OS.
     *   			d.) Loading the "Null Process" and adding it to the Ready Queue(RQ).
     *  	 (2) Obtaining user console input for interrupt selection.
     *  	 (3) Processing user-selected interrupts. See the specific interrupt for details.
     *  	 (4) If the user selected the "Shutdown" interrupt or if there was a fatal error
     *  		  terminate all processes, and shutdown the Java VM.
     *  	 (5) If the user selected the "Run Program" interrupt, obtain program file name
     *  		  from user, load program, and add program to RQ.
     *  	 (5) If the user selected the "IO_GETC" or "IO_PUTC", print a message indicating
     *  		  that the interrupt is not implemented in HW2.
     *  	 (7) If the user selected any interrupt other than shutdown, perform the
     *  		  following tasks:
     *  			a.) Dump the User Program memory (first interrupt only).
     *  			b.) Dump the Ready Queue(RQ) and Waiting Queue(WQ).
     *  			c.) Select a process to run from RQ, and perform "dispatch" context switching.
     *  			d.) Dump RQ and the selected process' PCB.
     *  			e.) Execute the selected process until time-slice expiration,
     *  				 HALT command read, waiting, or fatal error.
     *  			f.) Dump User Dynamic memory.
     *  			g.) If the selected process ended due to time-slice expiration, perform
     *  				 "save" context switching.
     *  			h.) If the selected process ended due to reading the HALT command, terminate
     *  				 the process.
     *  			i.) If the selected process ended due to waiting, insert the process into WQ.
     *  			g.) If the selected process ended due to fatal error, terminate all processes
     *  				 and exit the Java VM with the appropriate error code.
     *  	 (8) Return to step 2.
     *  
	 * Input parameters:
     * 	NONE
     * 
     * Output parameters
     * 	exitStatus - The Java VM allows for one output value when calling System.exit(exitStatus).
     * 				  There is only one exitStatus specific to main. All others are method-specific.
     * 				  See "Function return value" below for details.
     * 
     * Function return value:
     *  Return values specific to main:
     * 		StatusCode.ERROR_RUN_EMPTY_RQ - Returned if selectProcessFromRQ() returned
     * 		 END_OF_LIST(-1).
     *   {@link hypoProject.StatusCode See the StatusCode enum for all possible return values.}
     *  	
	 * @param args Arguments passed to the hypoProject program are ignored.
	 **********************************************************************************************/
	public static void main(String[] args)
	{
		boolean  isFirstRun   = true; //Used to flag if pre-scheduling memory dump should be done.
		
		init(); //Set up the Java VM and initialize the HYPO Machine system.
		
		//The main loop of the HYPO Machine.
		while(!shutdown)
		{
			String filename  = null; //The filename of a process.
			long runningPCB  = -1; //The first address of the PCB in the running state.
			int  status      = StatusCode.OKAY.getValue(); //The return value of CPU.cpuExecuteProgram.
			
			//Check for process interrupt.
			checkAndProcessInterrupt();
			//If the user chose "Shutdown", or a fatal error occurred stop processing and go to JAVA VM shutdown.
			if(shutdown == true)
				break;
			
			//Print the user program area if IS_DEBUG_MODE == true.
			if(IS_DEBUG_MODE)
				hardware.dumpMemory("User Program Area(time-slice): Pre-Execution", 0, USED_USER_PROGRAM_SIZE);
			
			//Dump RQ.
			try
			{
				System.out.println("RQ: Before CPU scheduling...");
				os.printQueue(os.getRQ());
			}catch(IndexOutOfBoundsException e)
			{
				//RQ pointer invalid.
				System.err.println("Run.main: Could not print RQ. The RQ pointer of "
						+ os.getRQ() + " is invalid!");
			}catch(IllegalStateException e)
			{
				//PID is not matched to a process name.
				System.err.println("Run.main: Could not print RQ.");
				System.err.println(e.getLocalizedMessage());
			}
			
			//Dump WQ.
			try
			{
				System.out.println("WQ: Before CPU scheduling...");
				os.printQueue(os.getWQ());
			}catch(IndexOutOfBoundsException e)
			{
				//WQ pointer invalid.
				System.err.println("Run.main: Could not print WQ. The WQ pointer of "
						+ os.getWQ() + " is invalid!");
			}catch(IllegalStateException e)
			{
				//PID is not matched to a process name.
				System.err.println("Run.main: Could not print WQ.");
				System.err.println(e.getLocalizedMessage());
			}
			
			//Per Professor Krishnamoorth, dump pre-scheduling user dynamic memory on first run only.
			if(isFirstRun)
			{
				try
				{
					hardware.dumpMemory("User Dynamic Memory before CPU scheduling (first run only)..."
							, Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND, USED_USER_DYNAMIC_SIZE);
				}
				catch(IndexOutOfBoundsException e)
				{
					//One of the dumpMemory() arguments is invalid, or PSR is invalid.
					System.err.println("Run.main: Could not print Memory Dump.");
					System.err.println(e.getLocalizedMessage());
				}
				isFirstRun = false;
			}
			
			//OS is scheduling. Set PSR to OS_MODE.
			hardware.setPSR(Hardware.OS_MODE);
			
			//Select next process from RQ to give CPU.
			do
			{
				runningPCB = os.selectProcessFromRQ();
				/**
				 * If RQ is empty, "Null Process" does not exist. HYPO Machine system can not execute instructions.
				 * Fatal error. Print error and shutdown HYPO Machine system.
				 */
				if(runningPCB < 0)
				{
					System.err.println("Run.main: Attempted to select process when RQ is empty!");
					System.err.println("HYPO Machine has no instructions to execute!");
					exitStatus = StatusCode.ERROR_RUN_EMPTY_RQ.getValue();
					Interrupt.SHUTDOWN.executeISR();
					break;
				}
				if(!Hardware.isOSDynamic(runningPCB))
				{
					//Invalid PCB address. Print error and attempt to select the next process in RQ.
					System.err.println("Run.main: Received invalid running PCB address (" + runningPCB + ").");
					System.out.println("Invalid PCB removed from RQ. Selecting next process...");
				}
				
			}while(!Hardware.isOSDynamic(runningPCB));
			
			//For readability, display the name of the selected process.
			filename = os.getPIDName((int)PCBController.PCBAddress.PID.dispatch(hardware, runningPCB));
			if(filename == null)
			{
				//If the PID associated with this PCB is not in the os.processList. Print error,
				// give filename an error value, and continue processing.
				System.err.println("The selected process (PCB start = " + runningPCB + ") is not in the process list!");
				filename = StatusCode.ERROR_RUN_INVALID_PID.toString();
			}
			System.out.println("Selected: \"" + filename + "\" (PCB start = " + runningPCB + ") for CPU");
			System.out.println();
			
			//Restore hardware context using dispatcher.
			os.dispatcher(runningPCB);
			
			//Dump RQ
			try
			{
				System.out.println("RQ: After selecting process from RQ...");
				os.printQueue(os.getRQ());
			}catch(IndexOutOfBoundsException e)
			{
				//RQ pointer invalid.
				System.err.println("Run.main: Could not print RQ. The RQ pointer of "
						+ os.getRQ() + " is invalid!");
			}catch(IllegalStateException e)
			{
				//PID is not matched to a process name.
				System.err.println("Run.main: Could not print RQ.");
				System.err.println(e.getLocalizedMessage());
			}
			
			//Dump the running PCB context.
			//Also fulfills process info requirement from "Homework2 - Spring 2010.doc".
			System.out.println("Running PCB(" + runningPCB + ") context for \"" + filename + "\"...");
			PCBController.dumpPCB(runningPCB, hardware);
			
			//OS is releasing control. Set PSR to USER_MODE.
			hardware.setPSR(Hardware.USER_MODE);
			
			//Execute instructions of the running process using the CPU
			status = cpu.cpuExecuteProgram();

//Krishna: How this is going to work when there are 3 processes (programs) running?
//Ken: HW1 did not have, nor did it mention multitasking. Most of this method was re-written for HW2.
//	   Per "Homework2 - Spring 2010.doc", "Dump the content of the program memory area only
//		once after each process creation using memory dump function written in Homework #1."
//	   This is done when OS.isrCreateProcess is performed. The user program area is never dumped again.
			
			//Dump user dynamic memory.
			hardware.dumpMemory("User Dynamic Memory after execute program..."
					, Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND, USED_USER_DYNAMIC_SIZE);
			
			//OS is scheduling. Set PSR to OS_MODE.
			hardware.setPSR(Hardware.OS_MODE);
			
			//If the running process read a HALT instruction or there was an error, terminate process.
			if(status < 0 || status == StatusCode.HALT.getValue())
			{
				long pid = -1; //The PID of runningPCB.
				String statusName = null; //The String name associated with the status value.
				
				pid = PCBController.PCBAddress.PID.dispatch(hardware, runningPCB);
				
				//If HALT is read, change status to OKAY to indicate normal execution.
				if(status == StatusCode.HALT.getValue())
					status = StatusCode.OKAY.getValue();
				
				statusName = getStatusCodeName(status);
				
				//If there was an error, print error message.
				if(status < 0)
					System.err.println("Run.main: cpuExecuteProgram returned an error. Forcing termination...");
				
				//Print PID, Name, and status of process that is terminating.
				System.out.println("Terminating PID #" + pid + " \"" + filename
						+ "\" (PCB start = " + runningPCB + ") with exit status "
						+ statusName + "(" + status + ")...");
				
				//Note: Running PCB is not in RQ/WQ. No need to remove.
				os.terminateProcess(runningPCB);
			}
			//If the running process' time slice expired, save context and insert PCB to RQ.
			else if(status == StatusCode.TIME_SLICE_EXPIRED.getValue())
			{
				os.saveContext(runningPCB);
				os.insertToRQ(runningPCB);
			}
			//If the running process must wait for a message. Set reason for wait then send to WQ.
			else if(status == StatusCode.WAITING_FOR_MESSAGE.getValue())
			{
				PCBController.PCBAddress.WAIT_CODE.saveContext(hardware, runningPCB
						, PCBController.ReasonForWait.MESSAGE_ARRIVAL_EVENT.getValue());
				os.insertToWQ(runningPCB);
			}
			//Unknown program error. Print error message and shutdown HYPO Machine system.
			else
			{
				System.err.println("Run.main: Unexpected status " + getStatusCodeName(status)
						+ "(" + status + ").");
				break;
			}
			
		} //End of HYPO Machine main loop.
		
		//Shutdown System.
		shutdown();
	}
	
	/**
	 * Used to set up the Java VM, and initialize the HYPO Machine system.
	 */
	private static void init()
	{
		//Set Java VM parameters.
		scan = new Scanner(System.in);
		setStreams(OUTPUT_FILE);       //Split output into System.out, System.err, and to outputFile.
		setUncaughtExecptionHandler(); //Set the UncaughtExceptionHanlder for this thread.
		
		//Initialize the HYPO Machine system.
		System.out.println("HYPO Machine is loading...");
		hardware = Hardware.getInstance();       //The Hardware object used to store HYPO machine data.
		cpu      = CPU.getInstance(hardware);    //The object used to execute the user program.
		os		 = OS.getInstance(hardware);     //The object used to handle process management.
		loadNullProcess();
		System.out.println("HYPO Machine Finished initialization!");
	}
	
	/**
	 * Used to load the "Null Process" into user dynamic memory, and add it to the
	 *  Ready Queue(RQ). The "Null Process" has a priority of NULL_PRIORITY(0),
	 *  and represents the CPU idling.
	 * Failure to load the "Null Process" is a fatal error, and will flag the HYPO
	 *  Machine system to shutdown.
	 */
	private static void loadNullProcess()
	{
		int status = StatusCode.OKAY.getValue(); //The return status of os.createProcess.
		
		//Create the Null Process.
		System.out.println("Creating Null Process...");
		status = os.createProcess(NULL_FILENAME, NULL_PRIORITY);
		
		//If process creation failed, print error information, set exitStatus call shutdown ISR, return to caller.
		if(status < 0)
		{
			System.err.println("Run.loadNullProcess: Failed to load the Null Process!");
			System.err.println("StatusCode: " + getStatusCodeName(status) + "(" + status + ")");
			exitStatus = status;
			Interrupt.SHUTDOWN.executeISR();
			return;
		}
		
		//Process creation successful.
		System.out.println("Null Process successfully loaded...");
	}
	
	
	/**
	 * Used to display a menu of all HYPO Machine system interrupts, and prompt
	 *  a user to to choose one. No user input is obtained. The cursor is left
	 *  on the same line as the user prompt.
	 */
	private static void printMenu()
	{
		System.out.println();
		System.out.println("***********************************************");
		System.out.println();
		System.out.println("  0     No Interrupt");
		System.out.println("  1     Run Program");
		System.out.println("  2     Shutdown System");
		System.out.println("  3     Input  Operation Completion (io_getc)");
		System.out.println("  4     Output Operation Completion (io_putc)");
		System.out.println();
		System.out.println("***********************************************");
		System.out.println();
		System.out.print("Please choose an interrupt number: ");
	}
	
	/**
	 * Used to obtain user console input from the current line. Also writes
	 *  the input to the FileOutputStream.
	 * @return A String indicating the value typed by the user on the current line
	 *  of the console prior to pressing the enter/return key.
	 * @throws IOException Thrown if there was a problem writing to the FileOutputStream.
	 */
	private static String getUserInput() throws IOException
	{
		String  formatedUserInput = null; //userInput with \r\n appended to it for writing to file.
		String  userInput = scan.nextLine();
		
		//Append user input to outputFile (since it's not printed to System.out).
		formatedUserInput = userInput + "\r\n";
		fileOutput.write(formatedUserInput.getBytes());
		
		System.out.println();
		
		return userInput;
	}
	
	/**
	 * Used to display an interrupt menu and execute the interrupt specified
	 *  by user console input. If the specified user console input can not be
	 *  parsed to a valid interruptID, the menu is displayed again. Otherwise,
	 *  the interrupt is executed.
	 * If the Shutdown interrupt is chosen or any fatal error occurs, the class
	 *  variable "shutdown" is set to true, indicating the HYPO Machine System
	 *  should shutdown.
	 * All other errors print an error message, and allow the HYPO Machine
	 *  system to continue processing.
	 * NOTE: Per Professor Krishnamoorthy's in-class specification, no interrupt
	 *  should return a value other than isrShutdownSystem(). Therefore interrupt
	 *  errors must be handled either here, or by the individual interrupt.
	 *  If a fatal error(which requires shutdown) has occurred, the class variable
	 *  exitStatus is set to reflect the error, and the shutdown ISR is called.
	 *  As of HW2, the return value of this method is not used. It is left as-is
	 *  since implementation of future ISR's may make the return value useful.
	 * @return An int indicating the Interrupt value executed.
	 */
	private static int checkAndProcessInterrupt()
	{
		String userInput = null; //The console input provided by the user.
		int parsedInput  = 0; //The int represented by the userInput String.
		
		//Display interrupt menu and get user input.
		printMenu();
		try
		{
			userInput = getUserInput();
		}
		catch(IOException e)
		{
			/**
			 * Fatal error occurred. If the HYPO Machine system output can not be output to file,
			 *  then it can not fulfill its purpose. Java VM must shutdown. 
			 * Print error, set the exitStatus, call shutdown ISR, return to caller.
			 */
			System.err.println("Run.checkAndProcessInterrupt: Could not write to FileOutputStream.");
			e.printStackTrace();
			exitStatus = StatusCode.ERROR_FILEOUTPUTSTREAM_FAILURE.getValue();
			Interrupt.SHUTDOWN.executeISR();
			return Interrupt.UNEXPECTED_ERROR.getInterruptID();
		}
		
		//Parse the userInput String into an int value.
		try
		{
			parsedInput = Integer.parseInt(userInput);
		}
		catch(NumberFormatException e)
		{
			//The user's input was could not be parsed to an Integer. Ask the user to try again.
			System.out.println("The value \"" + userInput + "\" is not valid. Please choose an option.");
			return checkAndProcessInterrupt();
		}
		
		//Execute the specified interrupt, if possible.
		if(parsedInput != Interrupt.UNEXPECTED_ERROR.getInterruptID())
		{
			for(Interrupt interrupt:Interrupt.values())
			{
				if(parsedInput == interrupt.getInterruptID())
				{
					interrupt.executeISR();
					return interrupt.getInterruptID();
				}
			}
		}
		
		//userInput did not match any interrupt ID's.
		System.out.println("The value \"" + userInput + "\" is not valid. Please choose an option.");
		return checkAndProcessInterrupt();
	}
	
	/**
	 * This method represents one of the supported interrupts of the HYPO Machine system.
	 * Used to obtain a HYPO Machine machine language file name or path from the user and
	 *  run the program.
	 * Since it is easier to backup the state of the hardware, than it is to create rollback
	 *  functions for each error condition, hardware.doBackup() and hardware.doRestore()
	 *  are used to "unload" programs that were partially and unsuccessfully loaded.
	 * If the process was successfully created, a confirmation message is printed.
	 *  Otherwise, an error message is printed. If the error was fatal, the class variable
	 *  exitStatus is set, and the HYPO Machine must shutdown. For all other errors, hardware
	 *  is restored to its pre-createProcess() state, and the HYPO Machine system is allowed
	 *  to continue.
	 */
	private static void isrRunProgramInterrupt()
	{
		String userInput = null; //The String representing the user's console input.
		int    status = StatusCode.OKAY.getValue(); //The return status of a method call.
		
		//Get user program file name from System.in
		System.out.print("Enter machine code program name: ");
		try
		{
			userInput = getUserInput();
		}
		catch(IOException e)
		{
			/**
			 * Fatal error occurred. If the HYPO Machine system output can not be output to file,
			 *  then it can not fulfill its purpose. Java VM must shutdown. 
			 * Print error, set the exitStatus, call shutdown ISR, return to caller.
			 */
			System.err.println("Run.isrRunProgramInterrupt could not write to FileOutputStream!");
			e.printStackTrace();
			exitStatus = StatusCode.ERROR_FILEOUTPUTSTREAM_FAILURE.getValue();
			Interrupt.SHUTDOWN.executeISR();
			return;
		}
		
		//If the specified file is null or the empty String, ask the user to try again.
		if(userInput == null || userInput.compareTo("") == 0)
		{
			System.out.println("No file name was read. Please try again.");
			isrRunProgramInterrupt();
		}
		
		//Backup the current hardware state, in case of error.
		hardware.doBackup();
		
		//Create the specified process.
		status = os.createProcess(userInput, PCBController.DEFAULT_PRIORITY);
		
		/*
		 * If process creation failed, print error information, restore hardware to its pre-createProcess()
		 *  state, and return to caller to continue execution.
		 */
		if(status < 0)
		{
			System.err.println("Run.isrRunProgramInterrupt: Failed to create the specified process.");
			System.err.println("StatusCode: " + getStatusCodeName(status) + "(" + status + ")");
			hardware.doRestore();
			return;
		}
		
		//Process creation successful.
		System.out.println("isrRunProgramInterrupt Complete!");
		System.out.println();
	}
	
	/**
	 * This method represents one of the supported interrupts of the HYPO Machine system.
	 * Used to shut down the HYPO Machine system. To do so, all processes in the Ready Queue(RQ)
	 *  and Waiting Queue(WQ) are terminated and removed from the queue.
	 * This method is the only way the HYPO Machine system can be shutdown.
	 */
	private static void isrShutdownSystem()
	{
		//Remove and terminate all processes in RQ and WQ.
		os.clearRQ();
		os.clearWQ();
		
		//Set the class variable "shutdown" to true.
		shutdown = true;
	}
	
	
	
	/**
	 * This method represents one of the supported interrupts of the HYPO Machine system.
	 * Its use is not made clear in HW2.
	 * Print a message that this interrupt is not implemented.
	 */
	private static void isrInputCompletionInterrupt()
	{
		System.out.println("This interrupt is not implemented in HW2!");
		System.out.println();
	}
	
	/**
	 * This method represents one of the supported interrupts of the HYPO Machine system.
	 * Its use is not made clear in HW2.
	 * Print a message that this interrupt is not implemented.
	 */
	private static void isrOutputCompletionInterrupt()
	{
		System.out.println("This interrupt is not implemented in HW2!");
		System.out.println();
	}
	
	/**
	 * This helper method is used to get the String associated with a StatusCode
	 *  int value. If the specified value statusCode can not be matched to a
	 *  StatusCode int value, null is returned.
	 * @param statusCode An int indicating the StatusCode value whose String is to be obtained.
	 * @return The String name associated with the specified StatusCode int value, or null if
	 *  the specified int value has no StatusCode int value match.
	 */
	private static String getStatusCodeName(int statusCode)
	{
		for(StatusCode status: StatusCode.values())
			if(status.getValue() == statusCode)
				return status.toString();
		return null;
	}
	
	/**
	 * Used to notify the user of HYPO Machine system shutdown,
	 *  close all output streams, and exit the Java VM.
	 */
	private static void shutdown()
	{
		System.out.println("Shutting down Hypo Machine system...");
		closeStreams();
		System.out.println("Exit Status = " + getStatusCodeName(exitStatus) + "(" + exitStatus + ")");
		System.exit(exitStatus);
	}
	
	/**
	 * This enum is used as an iterable method of identifying and executing
	 *  HYPO Machine interrupts.
	 * Each interrupt is given an interruptID, which corresponds to the menu
	 *  selection from the printMenu() method.
	 * Each interrupt also has a executeISR() method that sets the PSR and
	 *  calls the ISR appropriate to the Interrupt.
	 * The UNEXPECTED_ERROR Interrupt is provided to identify invalid interrupts.
	 *  
	 * @author Kenneth Chin
	 *
	 */
	private enum Interrupt
	{
		/**
		 * Used to identify invalid interrupts.
		 */
		UNEXPECTED_ERROR(-1)
		{
			@Override
			public void executeISR()
			{
				//Do nothing.
			}
		},
		
		/**
		 * Used to identify a selection of "No Interrupt".
		 */
		NO_INTERUPT(0)
		{
			@Override
			public void executeISR()
			{
				//Do nothing.
			}
		},
		
		/**
		 * Used to identify a selection of "Run Program".
		 */
		RUN_PROGRAM(1)
		{
			@Override
			public void executeISR()
			{
				//OS is in control. Set PSR to OS_MODE.
				hardware.setPSR(Hardware.OS_MODE);
				
				//Perform interrupt operation.
				isrRunProgramInterrupt();
				
				//OS is releasing control. Set PSR to USER_MODE.
				hardware.setPSR(Hardware.USER_MODE);
			}
		},
		
		/**
		 * Used to identify a selection of "Shutdown".
		 */
		SHUTDOWN(2)
		{
			@Override
			public void executeISR()
			{
				//OS is in control. Set PSR to OS_MODE.
				hardware.setPSR(Hardware.OS_MODE);
				
				//Perform interrupt operation.
				isrShutdownSystem();
			}
		},
		
		/**
		 * Used to identify a selection of "Input  Operation Completion (io_getc)".
		 */
		IO_GETC(3)
		{
			@Override
			public void executeISR()
			{
				//OS is in control. Set PSR to OS_MODE.
				hardware.setPSR(Hardware.OS_MODE);
				
				//Perform interrupt operation.
				isrInputCompletionInterrupt();
				
				//OS is releasing control. Set PSR to USER_MODE.
				hardware.setPSR(Hardware.USER_MODE);
			}
		},
		
		/**
		 * Used to identify a selection of "Output Operation Completion (io_putc)".
		 */
		IO_PUTC(4)
		{
			@Override
			public void executeISR()
			{
				//OS is in control. Set PSR to OS_MODE.
				hardware.setPSR(Hardware.OS_MODE);
				
				//Perform interrupt operation.
				isrOutputCompletionInterrupt();
				
				//OS is releasing control. Set PSR to USER_MODE.
				hardware.setPSR(Hardware.USER_MODE);
			}
		};
		
		private int interruptID; //The interrupt ID of this interrupt.
		
		/**
		 * The private constructor of the Interrupts enum.
		 * @param interuptID An int indicating the interrupt ID of this interrupt.
		 */
		private Interrupt(int interruptID)
		{
			this.interruptID = interruptID;
		}
		
		/**
		 * Used to obtain the integer interrupt ID of this interrupt.
		 * @return An int representing the interrupt ID of this interrupt.
		 */
		public int getInterruptID()
		{
			return interruptID;
		}
		
		/**
		 * Used to execute the Interrupt Service Routine(ISR) associated with this interrupt.
		 */
		public abstract void executeISR();
		
	}
	
	/**
	 * 
	 * Used to set System.out and System.err to new PrintStreams that output to
	 *  console and the specified file in parallel. The fileOutput object of the
	 *  Run class is set to the FileOutputStream that writes to the specified
	 *  file. This is done so that fileOutput may be written to separately from
	 *  the console (such as appending user input).</br>
	 *  If the specified file can not be opened and written to, then hypoProject
	 *  is terminated with StatusCode.ERROR_PRINTSTREAM_FAILURE.
	 * @param outputFile The File object to have System.out and System.err
	 *  written to.
	 */
	private static void setStreams(File outputFile)
	{
		//Preserve the original output streams in case of error, and for closing.
		stdOutOld = System.out;
		stdErrOld = System.err;
		
		try
		{   //Create a file output stream.
			fileOutput= new FileOutputStream(outputFile);
			
			//Create dual output streams for System.out/fileOutput and System.err/fileOutput.
			OutputStreamSplitter multiOut = new OutputStreamSplitter(System.out, fileOutput);
			OutputStreamSplitter multiErr = new OutputStreamSplitter(System.err, fileOutput);
			
			//Redirect System.out and System.err to the OutputStreamSplitter outputs.
			PrintStream stdOut = new PrintStream(multiOut);
			PrintStream stdErr = new PrintStream(multiErr);
			System.setOut(stdOut);
			System.setErr(stdErr);
		}
		catch (FileNotFoundException e)
		{
			//Return System.out and System.err to their original PrintStream.
			System.setOut(stdOutOld);
			System.setErr(stdErrOld);
			
			//Print error message and stacktrace, then quit hypoProject.
			System.err.println("Run.setStreams could not redirect output.");
			e.printStackTrace();
			System.exit(StatusCode.ERROR_PRINTSTREAM_FAILURE.getValue());
		}
	}

	/**
	 * Used to flush and close the custom PrintStreams generated by Run.setStreams.
	 *  Returns control of the default PrintStreams to System.out and System.err.
	 *  Flushes and closes the FileOutputStream fileOutput.</br>
	 *  If the FileOutputStream fails to close, then an error message is displayed,
	 *   and hypoProject is terminated with StatusCode.ERROR_FILEOUTPUTSTREAM_FAILURE.
	 * Also closes the console input stream.
	 */
	private static void closeStreams()
	{
		//Close the input stream.
		scan.close();
		
		//Flush and close the custom System.out and System.err PrintStreams
		System.out.flush();
		System.out.close();
		System.err.flush();
		System.err.close();
		
		//Return control of the default PrintStreams to System.out and System.err.
		System.setOut(stdOutOld);
		System.setErr(stdErrOld);
		
		//Flush and close the FileOutputStream.
		try
		{
			fileOutput.flush();
			fileOutput.close();
		}
		catch (IOException e)
		{
			System.err.println("Run.closeStreams failed to close fileOutput!");
			e.printStackTrace();
			System.exit(StatusCode.ERROR_FILEOUTPUTSTREAM_FAILURE.getValue());
		}
	}
	
	/**
	 * Used to set the UncaughtExecptionHandler, globalExceptionHandler.
	 */
	private static void setUncaughtExecptionHandler()
	{
	    Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);
	    Thread.currentThread().setUncaughtExceptionHandler(globalExceptionHandler);
	}
}
