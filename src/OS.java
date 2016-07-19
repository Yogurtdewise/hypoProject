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

import java.util.ArrayList;

/**
 * 
 * This class represents operations that a HYPO Machine Operating System may govern.
 *  Operations include:
 *  	1.) Process creation.
 *  	2.) PID management.
 *  	3.) Ready Queue(RQ) and Waiting Queue(WQ) management.
 *  	4.) User and OS Dynamic memory management.
 *  	5.) Process selection.
 *  	6.) Hardware Context Switching.
 *  
 * The OS class may also be viewed as the interface between the User's CLI(the Run class)
 *  with Hardware, and User Processes(which are stored in hardware) with CPU and Hardware.
 *  
 * This class follows the singleton pattern, as there should only be one OS object per
 *  HYPO machine. Therefore, one must call OS.getInstance() to obtain the only instance
 *  of the OS class.
 * 
 * @author Kenneth Chin
 *
 */
public final class OS
{
	private static ArrayList<String> processList = null; //A list of Process filenames whose PID is used as index.
	
	//Used to flag the end of a linked list.
	private static final int END_OF_LIST = (int)PCBController.END_OF_LIST;
	private static final int OS_MEMORY   = 0;  //Used to flag that OS Dynamic memory is being changed.
	private static final int USER_MEMORY = 1;  //Used to flag that User Dynamic memory is being changed.
	
	private static final int DEFAULT_STACK_SIZE = 10; //The default size of a process' stack.

	private static long os_FreeList;   //The address of the first node in the free OS Dynamic Memory linked list.
	private static long user_FreeList; //The address of the first node in the free User Dynamic Memory linked list.
	private static int  memoryMode;    //Used to flag which type of memory is being allocated. 
	
	private static long rqStart; //The first PCB in Ready Queue (RQ).
	private static long wqStart; //The first PCB in Waiting Queue (WQ).
	
	private static long pid; //The PID of the next new process.
	
	private static Hardware    hardware       = null; //The Hardware object whose memory is to be used.
	private static Loader      loader         = null; //The object used to load programs.
	private static OS          singleInstance = null; //The single instance of the OS class.
	
	
	/**
	 * The private constructor of the OS class.
	 * @param hardware The Hardware object whose memory is to be used.
	 */
	private OS(Hardware hardware)
	{
		OS.hardware = hardware;
		loader               = Loader.getInstance(hardware);
		init();
	}
	
	/**
	 * Used to obtain the single instance of the OS class.
	 * @return The single instance of the OS class.
	 */
	public static OS getInstance(Hardware hardware)
	{
		if(singleInstance == null)
			singleInstance = new OS(hardware);
		return singleInstance;
	}
	
	/**
	 * This helper method is used to initialize the OS class.
	 */
	private void init()
	{
		processList = new ArrayList<String>();
		processList.add("Reserved"); //PID 0 is reserved.
		memoryMode  = -1;
		pid         = 1; //PID 0 is reserved. Start count at 1.
		rqStart     = END_OF_LIST;
		wqStart     = END_OF_LIST;
		initOSFreeMem();
		initUserFreeMem();
	}
	
	/**
	 * Used to initialize the OS Dynamic Free Memory list. This is done by setting
	 *  the first node's "next node" address to END_OF_LIST, and the "Size" address
	 *  to be the total size of OS Dynamic memory. The class variable "os_FreeList"
	 *  is then set to point to the memory address of the first node.
	 */
	private void initOSFreeMem()
	{
		long start = Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND; //Start address of OS Dynamic Memory.
		long size  = Hardware.MEM_SIZE - start;              //Size of OS Dynamic Memory.
		
		//Initialize the os_FreeList in OS memory.
		hardware.setMemAddressValue(start, END_OF_LIST); //Next node is end of list.
		hardware.setMemAddressValue(start + 1, size);    //Size is stored in address after next node address.

		//Set os_FreeList to the first node address.
		os_FreeList = start;
	}
	
	/**
	 * Used to initialize the User Dynamic Free Memory list. This is done by setting
	 *  the first node's "next node" address to END_OF_LIST, and the "Size" address
	 *  to be the total size of User Dynamic memory. The class variable "user_FreeList"
	 *  is then set to point to the memory address of the first node.
	 */
	private void initUserFreeMem()
	{
		long start = Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND; //Start address of User Dynamic Memory.
		long size  = Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND - start; //Size of User Dynamic Memory.
	
		//Initialize the user_FreeList in User Dynamic memory.
		hardware.setMemAddressValue(start, END_OF_LIST); //Next node is end of list.
		hardware.setMemAddressValue(start + 1, size);    //Size is stored in address after next node address.
		
		//Set user_FreeList to the first node address.
		user_FreeList = start;
	}
	
	/**
	 * Used to create a new process whose program filename and priority are specified.
	 * To create a new process, the following steps are taken:
	 * 		(1) Allocate OS Dynamic Memory for a new PCB.
	 * 		(2) Create a new PCB for the new process.
	 * 		(3) Load the program into hardware user program memory (using Loader).
	 * 		(4) Allocate User Dynamic Memory for the new process' stack.
	 * 		(5) Set the new process' PCB values to those specified by the above actions,
	 * 			 or passed to this method(ie. priority).
	 * 		(6) Dump (display) the user program memory area for this process.
	 * 		(7) Dump (display) the PCB data for this process.
	 * 		(8) Add the new process' PCB to the Ready Queue(RQ).
	 * 
	 * The return value of this method is a StatusCode int value. The return values specific
	 *  to this method include:
	 *  	StatusCode.OKAY: If the new process was successfully created.
	 *  	StatusCode.ERROR_OS_INVALID_PRIORITY: If the specified priority is invalid.
	 *  	StatusCode.ERROR_OS_PCB_FAILED: If the new PCB failed to be created.
	 *  	
	 * @param filename A String indicating the file name or path of a Hypo Machine language
	 *  program file.
	 * @param priority A long indicating the new process' priority. 0 is the lowest priority,
	 *  PCBContoller.PRIORITY_MAX is the highest priority.
	 * @return An int whose value indicates a StatusCode value. See method details for return
	 *  values specific to this method.
	 * @throws NullPointerException Thrown if the specified filename is null or the empty String.
	 */
	public int createProcess(String filename, long priority)
	{
		//Ensure filename is not null or the empty String.
		if(filename == null || filename.compareTo("") == 0)
			throw new NullPointerException("OS.createProcess: The specified filename is empty!");
		
		//Ensure the specified priority is valid.
		if(priority < 0 || priority > PCBController.PRIORITY_MAX)
		{
			System.err.println("OS.createProcess: The priority(" + priority + ") is invalid!");
			return StatusCode.ERROR_OS_INVALID_PRIORITY.getValue();
		}
		
		//Declare and initialize local variables.
		long firstLine    = -1; //The memory address of the program's first line.
		long programStart = -1; //The memory address of the program's start.
		long programSize  = -1; //The total number of lines of the program.
		long pcbAddress   = -1; //The memory address of this program's PCB.
		long stackAddress = -1; //The memory address of this program's stack.
		
		//Get an OS Dynamic Memory allocation for this PCB.
		pcbAddress = allocateOSMemory(PCBController.PCBAddress.values().length);
		if(pcbAddress < 0)
		{
			System.err.println("OS.createProcess could not create process(" + filename
					+ ") due to OS Dynamic Memory allocation failure.");
			return (int)pcbAddress;
		}
		
		//Create the PCB for this process, and increment the pid counter.
		try
		{
			PCBController.initializePCB(pcbAddress, pid++, hardware);
		}
		catch(IndexOutOfBoundsException e)
		{
			System.err.println("OS.createProcess: Failed to initialize PCB!");
			System.err.println(e.getLocalizedMessage());
			return StatusCode.ERROR_OS_PCB_FAILED.getValue();
		}
		
		//Load specified program.
		programStart = loader.absoluteLoader(filename);
		//Display an error if an error status was returned by absoluteLoader. Return the error.
		if(programStart < 0)
		{
			System.err.println("OS.createProcess: Loader.absoluteLoader returned error status ("
					+ programStart + ").");
			return (int)programStart;
		}
		
		//Request an allocation of user dynamic memory for use as the process' stack area.
		stackAddress = allocateUserMemory(DEFAULT_STACK_SIZE);
		if(stackAddress < 0)
		{
			//Failed to obtain stack allocation. Print error, free PCB OS memory, & return error code.
			System.err.println("OS.createProcess: Failed to obtain stack allocation.");
			freeOSMemory(pcbAddress, PCBController.PCBAddress.values().length);
			return (int)stackAddress;
		}
		
		//Get the first line and size of the program.
		firstLine   = loader.getFirstAddress();
		programSize = loader.getProgramSize();
		
		//Set this program's PCB values for PC, STACK_START, STACK_SIZE, and PRIORITY.
		PCBController.PCBAddress.PC.saveContext(hardware, pcbAddress, programStart);
		PCBController.PCBAddress.STACK_START.saveContext(hardware, pcbAddress, stackAddress);
		PCBController.PCBAddress.STACK_SIZE.saveContext(hardware, pcbAddress, DEFAULT_STACK_SIZE);
		PCBController.PCBAddress.PRIORITY.saveContext(hardware, pcbAddress, priority);
		
		//Set this program's Stack Pointer(SP) to end of stack (stackAddress + DEFAULT_STACK_SIZE).
		//NOTE: SP is not used in HW2.
		PCBController.PCBAddress.SP.saveContext(hardware, pcbAddress, stackAddress + DEFAULT_STACK_SIZE);
		
		//Add this process to the processList.
		processList.ensureCapacity((int)PCBController.PCBAddress.PID.dispatch(hardware, pcbAddress));
		processList.add((int)PCBController.PCBAddress.PID.dispatch(hardware, pcbAddress), filename);
		
		//Dump program memory.
		hardware.dumpMemory("Create Process Memory Dump: \"" + filename + "\" (PCB start = " + pcbAddress
				+ ")", firstLine, programSize);
		
		//Dump the program's PCB information.
		System.out.println("Create Process PCB Dump: \"" + filename + "\" (PCB start = " + pcbAddress + ")");
		PCBController.dumpPCB(pcbAddress, hardware);
		
		//Insert the PCB into RQ. Return the StatusCode value indicating success or failure of the operation.
		return insertToRQ(pcbAddress);
	}
	
	/**
	 * Used to obtain the filename associated with the specified PID.
	 * If the specified PID is not valid (out of bounds or unused),
	 *  null is returned.
	 * NOTE: While this function was not required for HW2, it is included to improve
	 *  output readability.
	 * @param pid An int indicating the PID of the process who's filename is to
	 *  be obtained.
	 * @return A String representing the filename of the process whose PID was specified.
	 */
	public String getPIDName(int pid)
	{
		if(pid < 0 || pid >= processList.size())
			return null;
		return processList.get(pid);
	}
	
	/**
	 * This method is used to insert a new PCB into the Ready Queue(RQ).
	 *  The insertion algorithm is as follows:
	 *		(1) If there are no other PCB's in RQ, insert.
	 *		(2) If the new PCB has a higher priority than another in RQ, insert before the lower
	 *           priority PCB.
	 * 		(3) If the new PCB is of equal or lower priority than an existing PCB, insert after
	 * 			 existing PCBs of lower priority.
	 * @param pcbStartAddress A long indicating the starting address of the PCB to be inserted into RQ.
	 * @return An int defined by the StatusCode enum. StatusCode.OKAY.getValue() if the specified
	 *  PCB was successfully inserted into RQ, or StatusCode.ERROR_OS_RQ_INSERT_FAILED.getValue()
	 *  otherwise.
	 */
	public int insertToRQ(long pcbStartAddress)
	{
		//Ensure pcbStartAddress is a valid address.
		if(!PCBController.isValidPCBAddress(pcbStartAddress))
		{
			System.err.println("OS.insertToRQ: The PCB address (" + pcbStartAddress
					+ ") is invalid!");
			return StatusCode.ERROR_OS_RQ_INSERT_FAILED.getValue();
		}
		
		//Define and initialize local variables.
		long currentPCB      = rqStart;     //The PCB that is being compared to the new PCB.
		long previousPCB     = END_OF_LIST; //The PCB that was previously compared.
		//The priority of the PCB being inserted.
		long newPCB_Priority = PCBController.PCBAddress.PRIORITY.dispatch(hardware, pcbStartAddress);
		
		//Set the PCB to the Ready State.
		PCBController.PCBAddress.STATE.saveContext(hardware, pcbStartAddress
				, PCBController.ProcessState.READY.getValue());
		
		//If RQ is empty, insert this PCB as the first node.
		if(currentPCB == END_OF_LIST)
		{
			//Next node is end of RQ list.
			PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, pcbStartAddress, END_OF_LIST);
			rqStart = pcbStartAddress; //Set RQ pointer to this PCB.
			return StatusCode.OKAY.getValue();
		}
		
		//RQ is not empty. Find insertion point and insert PCB appropriately.
		while(currentPCB != END_OF_LIST)
		{
			//The PCB that currentPCB points to.
			long nextPCB = PCBController.PCBAddress.NEXT_PCB.dispatch(hardware, currentPCB);
			//Get the priority of the currentPCB.
			long currentPriority = PCBController.PCBAddress.PRIORITY.dispatch(hardware, currentPCB);
			
			//If new PCB is of higher priority than currentPCB's priority insert here (before currentPCB).
			if(currentPriority < newPCB_Priority)
			{
				//Set new PCB's next PCB to point to currentPCB.
				PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, pcbStartAddress, currentPCB);
				//If there was a previous PCB, it must point to the new PCB.
				if(previousPCB != END_OF_LIST)
					PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, previousPCB, pcbStartAddress);
				//If there was no previous PCB, then the new PCB is the start of the RQ linked list.
				else
					rqStart = pcbStartAddress; //Set RQ pointer to the new PCB.
				return StatusCode.OKAY.getValue();
			}
			//New PCB is of equal or lower priority than currentPCB.
			//If currentPCB is the last PCB in RQ, insert new PCB here (end of RQ).
			else if(nextPCB == END_OF_LIST)
			{
				//Set new PCB's next PCB to indicate END_OF_LIST.
				PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, pcbStartAddress, END_OF_LIST);
				//Set current PCB's next PCB value to point to the new PCB.
				PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, currentPCB, pcbStartAddress);
				return StatusCode.OKAY.getValue();
			}
			//currentPCB is not last in RQ. Prepare for next iteration.
			//If nextPCB's priority is less than the new PCB's, it will be inserted before nextPCB on the
			// next iteration. Else if nextPCB's priority is equal to the new PCB's, traversal will continue.
			else
			{
				previousPCB = currentPCB;
				currentPCB  = nextPCB;	
			}
		} //End of insertion loop.
		
		//Program execution should never reach this point. No insertion point found. Return error.
		System.err.println("OS.insertToRQ: Failed to insert PCB (address = " + pcbStartAddress
				+ ", priority = " + newPCB_Priority + ") into RQ! rqStart = " + rqStart);
		return StatusCode.ERROR_OS_RQ_INSERT_FAILED.getValue();
	}
	
	/**
	 * Used to insert a PCB into the beginning of the Waiting Queue(WQ).
	 * @param pcbStartAddress A long representing the first address of the PCB to be inserted.
	 * @return An int indicating a StatusCode value. Values include StatusCode.OKAY if
	 *  the specified PCB was successfully added to RQ, or StatusCode.ERROR_OS_WQ_INSERT_FAILED
	 *  if the specified pcbStartAddress is invalid.
	 */
	public int insertToWQ(long pcbStartAddress)
	{
		//Ensure pcbStartAddress is a valid address.
		if(!PCBController.isValidPCBAddress(pcbStartAddress))
		{
			System.err.println("OS.insertToWQ: The PCB address (" + pcbStartAddress
					+ ") is invalid!");
			return StatusCode.ERROR_OS_WQ_INSERT_FAILED.getValue();
		}
		
		//If WQ is empty, insert.
		if(wqStart == END_OF_LIST)
		{
			PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, pcbStartAddress, END_OF_LIST);
			wqStart = pcbStartAddress;
			return StatusCode.OKAY.getValue();
		}
		
		//WQ is not empty. Insert at the beginning of RQ.
		PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, pcbStartAddress, wqStart);
		wqStart = pcbStartAddress;
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to allocate a block of OS Dynamic Memory to the caller. If the specified size can
	 *  not be obtained, an error status is returned (see StatusCode enum). Otherwise the first
	 *  address of the allocated memory block is returned. The size of the returned memory
	 *  block is guaranteed to be AT LEAST the specified size(May be size + 1).
	 * NOTE: Memory blocks are ASCENDING, meaning the last allocation address is at
	 *  ((first allocation) + (size - 1)).
	 * @param size A long indicating the number of memory allocations being requested.
	 * @return A long indicating the first memory address of the requested memory block, or a StatusCode
	 *  indicating why the requested memory block could not be allocated.
	 */
	public long allocateOSMemory(long size)
	{
		//Ensure OS free memory exists.
		if(os_FreeList == END_OF_LIST)
		{
			System.err.println("OS.AllocateOSMemory: No free OS Dynamic Memory!");
			return StatusCode.ERROR_OS_NO_OS_MEM.getValue();
		}
		//Ensure os_FreeList is a valid address.
		if(!Hardware.isOSDynamic(os_FreeList))
		{
			System.err.println("OS.AllocateOSMemory.os_FreeList (" + os_FreeList
					+ ") is an invalid value!");
			return StatusCode.ERROR_OS_INVALID_OS_ADDRESS.getValue();
		}
		//Ensure the specified size is valid.
		if(size < 0)
		{
			System.err.println("OS.AllocateOSMemory was asked for " + size
					+ " memory allocations!");
			return StatusCode.ERROR_OS_INVALID_SIZE.getValue();
		}
		
		//Prepare to search for available memory blocks.
		long status = StatusCode.OKAY.getValue();
		memoryMode = OS_MEMORY;
		
		//Search for available memory blocks.
		try
		{
			status = allocateMemory(size, os_FreeList);
		}
		catch(IllegalStateException e)
		{
			System.err.println("OS.allocateOSMemory failed on allocateMemory() call!");
			System.err.println(e.getLocalizedMessage());
		}
		
		if(status < 0)
			return StatusCode.ERROR_OS_NO_OS_MEM.getValue();
		else
			return status;
		
	}
	
	/**
	 * Used to allocate a block of User Dynamic Memory to the caller. If the specified size can
	 *  not be obtained, an error status is returned (see StatusCode enum). Otherwise the first
	 *  address of the allocated memory block is returned. The size of the returned memory
	 *  block is guaranteed to be AT LEAST the specified size(May be size + 1).
	 * NOTE: Memory blocks are ASCENDING, meaning the last allocation address is at
	 *  ((first allocation) + (size - 1)).
	 * @param size A long indicating the number of memory allocations being requested.
	 * @return A long indicating the first memory address of the requested memory block, or a StatusCode
	 *  indicating why the requested memory block could not be allocated.
	 */
	public long allocateUserMemory(long size)
	{
		//Ensure user free memory exists.
		if(user_FreeList == END_OF_LIST)
		{
			System.err.println("OS.allocateUserMemory: No free User Dynamic memory!");
			return StatusCode.ERROR_OS_NO_USER_MEM.getValue();
		}
		//Ensure user_FreeList is a valid address.
		if(!Hardware.isUserDynamic(user_FreeList))
		{
			System.err.println("OS.allocateUserMemory.user_FreeList ("
					+ user_FreeList + ") is an invalid value!");
			return StatusCode.ERROR_OS_INVALID_USER_ADDRESS.getValue();
		}
		//Ensure the specified size is valid.
		if(size < 0)
		{
			System.err.println("OS.allocateUserMemory was asked for " + size
					+ " memory allocations!");
			return StatusCode.ERROR_OS_INVALID_SIZE.getValue();
		}
		
		//Prepare to search for available memory blocks.
		long status = StatusCode.OKAY.getValue();
		memoryMode = USER_MEMORY;
		
		//Search for available memory blocks.
		try
		{
			status = allocateMemory(size, user_FreeList);
		}
		catch(IllegalStateException e)
		{
			System.err.println("OS.allocateUserMemory failed on allocateMemory() call!");
			System.err.println(e.getLocalizedMessage());
		}
		
		if(status < 0)
			return StatusCode.ERROR_OS_NO_USER_MEM.getValue();
		else
			return status;
		
	}
	
	/**
	 * This helper method is used to allocate a block of memory of size "size".
	 *  The class variable "memoryMode" must be set to indicate if it is OS or User Dynamic Memory
	 *   being allocated. Otherwise a IllegalStateException is thrown.
	 *  This method will traverse the linked list whose first node is "firstNode"
	 *   until an available memory block is found or created, or until the entire linked list is
	 *   traversed without finding a free block of size "size".
	 *  If a suitable memory block can not be found, StatusCode.ERROR_OS_NO_MEM.getValue() is returned.
	 *  Else the first address of a memory block of size "size" is returned.
	 *  The size of the returned memory block is guaranteed to be AT LEAST the specified size
	 *  (May be size + 1).
	 *  NOTE: Memory blocks are ASCENDING, meaning the last allocation address is at
	 *  ((first allocation) + (size - 1)).
	 * @param size A long indicating the number of memory allocations being requested.
	 * @param firstNode A long indicating the memory address of the first node in a memory free list.
	 * @return A long indicating the first address of a memory block of the specified size, or
	 *  StatusCode.ERROR_OS_NO_MEM.getValue() indicating that no memory block of the specified size
	 *  was available.
	 * @throws IllegalStateException Thrown if the class variable "memoryMode" was not set prior to calling
	 *  this method. Valid states are OS_MEMORY or USER_MEMORY.
	 */
	private long allocateMemory(long size, long firstNode) throws IllegalStateException
	{
		//Ensure memoryMode was set.
		if(memoryMode != OS_MEMORY && memoryMode != USER_MEMORY)
			throw new IllegalStateException("OS.allocateMemory: memoryMode not set!");
		
		long currentNode  = firstNode;
		long previousNode = END_OF_LIST;
		
		//Minimum size is 2.
		if(size <= 1)
			size = 2;
		
		while(currentNode != END_OF_LIST)
		{
			long nextNode     = hardware.getMemAddressValue(currentNode);
			long availableMem = hardware.getMemAddressValue(currentNode + 1);
			
			//Available memory allocation found.
			if(availableMem >= size)
			{
				long remainingMem = availableMem - size;
				
				//If the current node can be split, do so (min node size = 2).
				if(remainingMem >= 2)
				{
					long newNode = currentNode + size;
					//If newNode is a valid address, create it. Else no new node is created.
					if(isValidAddress(newNode))
					{
						hardware.setMemAddressValue(newNode, nextNode);
						hardware.setMemAddressValue(newNode + 1, remainingMem);
						nextNode = newNode; //next node will be the new node.
					}
				}
				
				//Set previous nodes' next node to the node after the current node.
				if(previousNode != END_OF_LIST)
					hardware.setMemAddressValue(previousNode, nextNode);
				//No previous node, therefore this is the first node. Change the first node pointer.
				else
				{
					if(memoryMode == OS_MEMORY)
						os_FreeList = nextNode;
					if(memoryMode == USER_MEMORY)
						user_FreeList = nextNode;
				}
					
				//Per "Homework 2 PsudoCode" set the current node to point to END_OF_LIST.
				hardware.setMemAddressValue(currentNode, END_OF_LIST);
				
				//currentNode removed from free list. Return address of requested memory block.
				return currentNode;
			}
			else
			//The current free block is too small. Check next.
			{
				previousNode = currentNode;
				currentNode  = nextNode;
			}
			
		} //End of node search.
		
		//No free memory block matching size requirement found. Return error code.
		return StatusCode.ERROR_OS_NO_MEM.getValue();
	}
	
	/**
	 * Used to free an OS dynamic memory allocation. This is done by creating a new
	 *  os_FreeList node (using the memory block being freed) and adding it to the
	 *  beginning of the os_FreeList linked list.
	 * NOTE: Per "Homework2 - Spring 2010.doc", do not re-initialize values to 0.
	 *  However, to maintain the linked list, pcbStartAddress and pcbStartAddress + 1
	 *  must be changed to the next node's address and the size of the freed memory
	 *  block(respectively).
	 * Per instruction by Professor Krishnamoorthy, this method should not check if
	 *  the specified memory block is already free.
	 * @param startAddress A long indicating the first memory address of the memory
	 *  block to be freed.
	 * @param size A long indicating the number of memory allocations to be freed.
	 * @return An int representing a StatusCode enum. Possible values are:
	 * StatusCode.ERROR_OS_OS_FREE_FAILURE: If the specified memory block could not
	 *  be freed.
	 * StatusCode.OKAY: If the specified memory block was successfully added to the
	 *  os_FreeList linked list.
	 */
	public int freeOSMemory(long pcbStartAddress, long size)
	{
		//Validate the start address to be freed.
		if(!Hardware.isOSDynamic(pcbStartAddress))
		{
			System.err.println("OS.freeOSMemory was asked to free an invalid address ("
					+ pcbStartAddress + ")!");
			return StatusCode.ERROR_OS_OS_FREE_FAILURE.getValue();
		}
		
		//Validate the last address to be freed.
		if(!Hardware.isOSDynamic(pcbStartAddress + size - 1))
		{
			System.err.println("OS.freeOSMemory was asked to free an invalid address ("
					+ (pcbStartAddress + size - 1) + ")!");
			return StatusCode.ERROR_OS_OS_FREE_FAILURE.getValue();
		}
		
		//Minimum size is 2 (even if the user is unaware of it).
		if(size <= 1)
			size = 2;
		
		/**
		 *  For the purpose of displaying memory dumps and homework
		 *   correction, don't re-initialize the specified memory block.
		 */
		//Reinitialize the values of the specified memory block to 0.
		//for(long i = pcbStartAddress; i < pcbStartAddress + size; i++)
		//	hardware.setMemAddressValue(i, 0);
				
		//Set the next node and size of this free memory node.
		hardware.setMemAddressValue(pcbStartAddress, os_FreeList);
		hardware.setMemAddressValue(pcbStartAddress + 1, size);
		
		//Set os_FreeList to point to the newly created free node.
		os_FreeList = pcbStartAddress;
		
		//New OS free memory node successfully created and added to the os_FreeList linked list.
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to free a user dynamic memory allocation. This is done by creating a new
	 *  user_FreeList node (using the memory block being freed) and adding it to the
	 *  beginning of the user_FreeList linked list.
	 * NOTE: Per "Homework2 - Spring 2010.doc", do not re-initialize values to 0.
	 *  However, to maintain the linked list, startAddress and startAddress + 1
	 *  must be changed to the next node's address and the size of the freed memory
	 *  block(respectively).
	 * Per instruction by Professor Krishnamoorthy, this method should not check if
	 *  the specified memory block is already free.
	 * @param startAddress A long indicating the first memory address of the memory
	 *  block to be freed.
	 * @param size A long indicating the number of memory allocations to be freed.
	 * @return An int representing a StatusCode enum. Possible values are:
	 * StatusCode.ERROR_OS_USER_FREE_FAILURE: If the specified memory block could not
	 *  be freed.
	 * StatusCode.OKAY: If the specified memory block was successfully added to the
	 *  os_FreeList linked list.
	 */
	public int freeUserMemory(long startAddress, long size)
	{
		//Validate the start address to be freed.
		if(!Hardware.isUserDynamic(startAddress))
		{
			System.err.println("OS.freeUserMemory was asked to free an invalid address ("
					+ startAddress + ")!");
			return StatusCode.ERROR_OS_USER_FREE_FAILURE.getValue();
		}
		
		//Validate the last address to be freed.
		if(!Hardware.isUserDynamic(startAddress + size - 1))
		{
			System.err.println("OS.freeUserMemory was asked to free an invalid address ("
					+ (startAddress + size - 1) + ")!");
			return StatusCode.ERROR_OS_USER_FREE_FAILURE.getValue();
		}
		
		//Minimum size is 2 (even if the user is unaware of it).
		if(size <= 1)
			size = 2;
		
		/**
		 *  For the purpose of displaying memory dumps and homework
		 *   correction, don't re-initialize the specified memory block.
		 */
		//Reinitialize the values of the specified memory block to 0.
		//for(long i = startAddress; i < startAddress + size; i++)
		//hardware.setMemAddressValue(i, 0);
				
		//Set the next node and size of this free memory node.
		hardware.setMemAddressValue(startAddress, user_FreeList);
		hardware.setMemAddressValue(startAddress + 1, size);
		
		//Set user_FreeList to point to the newly created free node.
		user_FreeList = startAddress;
		
		//New user free memory node successfully created and added to the user_FreeList linked list.
		return StatusCode.OKAY.getValue();
	}
	
	/**
	 * Used to terminate a process.
	 * Frees user dynamic memory allocation for the specified PCB's stack, and
	 *  OS dynamic memory for the PCB itself.
	 * Also removes this process' PID from the processList ArrayList.
	 * This method DOES NOT remove the PCB from RQ/WQ. Therefore it is up to the
	 *  caller to do so.
	 * NOTE: "PerHomework2 - Spring 2010.doc", do not reset dynamic memory.
	 * NOTE: The HYPO Machine system does not track allocated user memory. Therefore, if
	 *  the a specified process used the mem_alloc SystemCall is terminated before calling
	 *  the mem_free SystemCall, the allocated memory can not be freed. This causes the
	 *  allocated memory to be permanently lost.
	 * @param startAddress A long representing the first address of the PCB to be terminated.
	 * @throws IndexOutOfBoundsException Thrown if startAddress is an invalid PCB start
	 *  address.
	 */
	public void terminateProcess(long pcbStartAddress) throws IndexOutOfBoundsException
	{
		//Ensure pcbStartAddress represents a valid PCB address.
		if(!PCBController.isValidPCBAddress(pcbStartAddress))
			throw new IndexOutOfBoundsException("OS.terminateProcess: The address " + pcbStartAddress
					+ " is not a valid PCB start address.");
		
		//Remove this process from the processList
		int pid = (int)PCBController.PCBAddress.PID.dispatch(hardware, pcbStartAddress);
		if(pid >= 0 && pid < processList.size())
			processList.set(pid, null);
			
		//Free user dynamic memory containing this PCB's stack.
		freeUserMemory(PCBController.PCBAddress.STACK_START.dispatch(hardware, pcbStartAddress)
					, DEFAULT_STACK_SIZE);
		//Free OS dynamic memory containing this PCB.
		freeOSMemory(pcbStartAddress, PCBController.PCBAddress.values().length);
	}
	
	/**
	 * This helper method is used to determine if a given address is valid.
	 * The class variable "memoryMode" must be set to indicate if it is OS or User Dynamic Memory
	 *   being checked. Otherwise an IllegalStateException is thrown.
	 * Used by allocateOSMemory(), allocateUserMemory(), and allocateMemory().
	 * @param address A long indicating the memory address to be validated.
	 * @return A boolean indicating true if the specified address is valid; false otherwise.
	 * @throws IllegalStateException Thrown if the class variable "memoryMode" was not set prior
	 *  to calling  this method. Valid states are OS_MEMORY or USER_MEMORY.
	 */
	private boolean isValidAddress(long address) throws IllegalStateException
	{
		//Ensure memoryMode was set.
		if(memoryMode != OS_MEMORY && memoryMode != USER_MEMORY)
			throw new IllegalStateException("OS.isValidAddress: memoryMode not set!");
		//Ensure address lies within the bounds of memory.
		if(address < 0 || address >= Hardware.MEM_SIZE)
			return false;
		//Ensure address is within the bounds of OS Dynamic Memory.
		if(memoryMode == OS_MEMORY && !Hardware.isOSDynamic(address))
			return false;
		//Ensure address is within the bounds of User Dynamic Memory.
		if(memoryMode == USER_MEMORY && !Hardware.isUserDynamic(address))
			return false;
		return true;	//address is within the bounds of the current memoryMode.
	}
	
	/**
	 * Used to obtain the first address of the first PCB node in Ready Queue(RQ).
	 * @return The long indicating the first address of the first PCB node in RQ,
	 *  or END_OF_LIST if RQ is empty.
	 */
	public long getRQ()
	{
		return rqStart;
	}
	
	/**
	 * Used to obtain the first address of the first PCB node in Waiting Queue(WQ).
	 * @return The long indicating the first address of the first PCB node in RQ,
	 *  or END_OF_LIST if RQ is empty.
	 */
	public long getWQ()
	{
		return wqStart;
	}
	
	/**
	 * Used to print all nodes in a queue's linked list.
	 * If the queue is empty, the message "The queue is empty." is printed.
	 * Otherwise a node header is printed, followed by a new line, followed by the PCB data.
	 * @param queuePtr A long indicating the memory address of the first node
	 *  in a queue, or END_OF_LIST (indicating an empty queue).
	 * @throws IndexOutOfBoundsException Thrown if startAddress is not within the bounds of
	 *  Hardware.OS_DYNAMIC_MEMORY_LOWER_BOUND and (Hardware.MEM_SIZE - PCBAddress.values().length).
	 * @throws IllegalStateException Thrown if a PCB's PID is not in processList.
	 */
	public void printQueue(long queuePtr) throws IndexOutOfBoundsException, IllegalStateException
	{
		//Validate queuePtr.
		if(queuePtr != END_OF_LIST && !PCBController.isValidPCBAddress(queuePtr))
			throw new IndexOutOfBoundsException("OS.printQueue: The queue starting"
					+ " at address(" + queuePtr + ") is not in valid!");
		
		long currentNode = queuePtr; //The PCB being printed.
		int  counter     = 1; //The node counter.
		
		//If the queue is empty, print a message and return.
		if(currentNode == END_OF_LIST)
		{
			System.out.println("The queue is empty.");
			System.out.println();
			return;
		}
		
		while(currentNode != END_OF_LIST)
		{
			int pid = (int)PCBController.PCBAddress.PID.dispatch(hardware, currentNode);
			String filename = getPIDName(pid);
			if(filename == null)
				throw new IllegalStateException("OS.printQueue: The PCB that starts at "
						+ currentNode + " is not in processList!");
			System.out.println("Node #" + counter++ + ": \"" + filename
					+ "\" (PCB start = " + currentNode + ")");
			PCBController.dumpPCB(currentNode, hardware);
			currentNode = PCBController.PCBAddress.NEXT_PCB.dispatch(hardware, currentNode);
		}
		System.out.println("End of Queue.");
		System.out.println();
	}
	
	/**
	 * Used to obtain the PCB start address of the first node in the Ready Queue(RQ).
	 * If a node exists, it is removed from RQ. If no nodes exist, END_OF_LIST is returned.
	 * @return A long indicating the PCB start address of the first node in the Ready Queue(RQ),
	 *  or END_OF_LIST if the RQ is empty.
	 */
	public long selectProcessFromRQ()
	{
		long processPCB = rqStart; //The address of the PCB to be selected.
		
		//Set rqStart to next node in RQ, and set selected node's next node to END_OF_LIST.
		if(rqStart != END_OF_LIST)
		{
			rqStart = PCBController.PCBAddress.NEXT_PCB.dispatch(hardware, processPCB);
			PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, processPCB, END_OF_LIST);
		
		}
		return processPCB;
	}
	
	/**
	 * Used to remove the specified PCB from the Waiting Queue(WQ).
	 * @param pcbStartAddress A long representing the first memory address of the PCB
	 *  to be removed from WQ.
	 * @return An int indicating a StatusCode value. Possible StatusCode values are
	 *  StatusCode.OKAY if the specified PCB was successfully removed from WQ or
	 *  StatusCode.ERROR_OS_WQ_REMOVE_FAILED if the specified PCB could not
	 *  be removed (ie. does/should not exist in WQ).
	 */
	public int removeFromWQ(long pcbStartAddress)
	{
		//Ensure pcbStartAddress is a valid address.
		if(!PCBController.isValidPCBAddress(pcbStartAddress))
		{
			System.err.println("OS.insertToWQ: The PCB address (" + pcbStartAddress
					+ ") is invalid!");
			return StatusCode.ERROR_OS_WQ_REMOVE_FAILED.getValue();
		}
		
		//If WQ is empty, the specified PCB can not be removed.
		if(wqStart == END_OF_LIST)
			return StatusCode.ERROR_OS_WQ_REMOVE_FAILED.getValue();
		
		//WQ is not empty. Find the specified PCB and remove it from WQ.
		long currentPCB  = wqStart;     //The PCB in WQ being examined for a match to pcbStartAddress.
		long previousPCB = END_OF_LIST; //The previous PCB examined in WQ.
		while(currentPCB != END_OF_LIST)
		{
			//Match found. Remove currentPCB from RQ and return StatusCode.OKAY
			if(currentPCB == pcbStartAddress)
			{
				//currentPCB is the only PCB in WQ. Remove it.
				if(previousPCB == END_OF_LIST)
				{
					PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, currentPCB, END_OF_LIST);
					wqStart = END_OF_LIST;
				}
				else
				{
					//There is more than one PCB in WQ. Remove currentPCB.
					long nextPCB = PCBController.PCBAddress.NEXT_PCB.dispatch(hardware, currentPCB);
					PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, previousPCB, nextPCB);
					PCBController.PCBAddress.NEXT_PCB.saveContext(hardware, currentPCB, END_OF_LIST);
				}
				return StatusCode.OKAY.getValue();
			}
		}
		//The specified PCB could not be found in WQ. Return failure StatusCode.
		return StatusCode.ERROR_OS_WQ_REMOVE_FAILED.getValue();
	}
	
	/**
	 * Used to remove and terminate all processes in the Ready Queue(RQ) linked list.
	 * Prints a message indicating RQ has been cleared on completion, or error
	 *  messages if there were problems clearing the RQ.
	 * This method should only be used when shutting down the HYPO Machine system.
	 * NOTE: The HYPO Machine system does not track allocated user memory. Therefore, if
	 *  any process in RQ used the mem_alloc SystemCall, terminating before calling
	 *  the mem_free SystemCall will causes the allocated memory to be permanently lost.
	 */
	public void clearRQ()
	{
		long currentPCB = selectProcessFromRQ(); //The PCB being removed from RQ/WQ.
		//Terminate the process removed from RQ. Continue removing and terminating until RQ is empty.
		while(currentPCB != END_OF_LIST)
		{
			try
			{
				terminateProcess(currentPCB);
			}
			catch(IndexOutOfBoundsException e)
			{
				//If current PCB is an invalid PCB address print an error, but continue
				// clearing RQ, since they system should already be shutting down.
				System.err.println("OS.clearRQ: The address " + currentPCB + ", found in RQ,"
						+ " is not a valid PCB start address.\r\n"
						+ "This PCB has been removed from RQ, but is not terminated.\r\n"
						+ "Continuing clearRQ process...");
			}
			currentPCB = selectProcessFromRQ();
		}
		System.out.println("RQ cleared.");
	}
	
	/**
	 * Used to remove and terminate all processes in the Waiting Queue(WQ) linked list.
	 * Prints a message indicating WQ has been cleared on completion, or error
	 *  messages if there were problems clearing the WQ.
	 * This method should only be used when shutting down the HYPO Machine system.
	 * NOTE: The HYPO Machine system does not track allocated user memory. Therefore, if
	 *  any process in WQ used the mem_alloc SystemCall, terminating before calling
	 *  the mem_free SystemCall will causes the allocated memory to be permanently lost.
	 */
	public void clearWQ()
	{
		int status      = StatusCode.OKAY.getValue(); //The return value of removeFromWQ().
		long currentPCB = wqStart; //The first PCB in WQ.
		
		//Remove the first node of WQ until WQ is empty.
		while(currentPCB != END_OF_LIST)
		{
			status = removeFromWQ(currentPCB);
			
			//If currentPCB is invalid, print an error and attempt to recover.
			if(status < 0)
			{
				System.err.println("OS.clearWQ: Could not remove the PCB(" + currentPCB
						+ ") from WQ!");
				long nextPCB = PCBController.PCBAddress.NEXT_PCB.dispatch(hardware, currentPCB);
				//If nextPCB is valid set wqStart to nextPCB (skip currentPCB).
				if(PCBController.isValidPCBAddress(nextPCB))
				{
					System.err.println("Attempting to remove the next PCB from WQ...");
					wqStart = nextPCB;
				}
				else
				{
					//The next PCB is invalid. Print an error and stop trying to terminate each
					// process in WQ. Set wqStart to END_OF_LIST, since the WQ is corrupt.
					System.err.println("WQ is corrupt. Can not recover. WQ cleared without terminating PCBs...");
					wqStart = END_OF_LIST;
					return;
				}
			} //End removeFromWQ() error condition.
			
			//Terminate the currentPCB.
			try
			{
				terminateProcess(currentPCB);
			}
			catch(IndexOutOfBoundsException e)
			{
				//currentPCB is an invalid. Print an error, but continue clearing WQ
				// since the system should be shutting down.
				System.err.println("OS.clearWQ: The PCB(" + currentPCB + ") is not valid.\r\n"
						+ "Continuing clearWQ process...");
			}
			
			//currentPCB successfully removed from WQ. Despite termination success, set currentPCB
			// to the first PCB in WQ.
			if(status >= 0)
				currentPCB = wqStart;
		}
		System.out.println("WQ cleared.");
	}
	
	/**
	 * Used to save(copy) CPU GPRs, SP, and PC to the specified PCB's GPR, SP, and PC indexes.
	 * @param pcbStartAddress A long indicating the first address of the PCB which is to store
	 *  the CPU's GPRs, SP, and PC values.
	 */
	public void saveContext(long pcbStartAddress)
	{
		//Per "Homework2 Pseudo Code - Correction.docx" pcbStartAddress assumed to be valid.
		
		//Copy GPRs to PCB's GPR indexes.
		for(int i = 0; i < Hardware.GPR_SIZE; i++)
		{
			long value = hardware.getGprRegisterValue(i); //The value to set this PCB's GPR value to.
			String addressString = "GPR_" + i; //The String representing i's PCBAddress GPR.
			PCBController.PCBAddress.valueOf(addressString).saveContext(hardware, pcbStartAddress, value);
		}
		
		//Copy SP and PC to PCB's SP and PC indexes.
		PCBController.PCBAddress.SP.saveContext(hardware, pcbStartAddress, hardware.getSP());
		PCBController.PCBAddress.PC.saveContext(hardware, pcbStartAddress, hardware.getPC());
	}
	
	/**
	 * Used to restore(copy) CPU GPRs, SP, and PC from the specified PCB's GPR, SP, and PC indexes.
	 * @param pcbStartAddress A long indicating the first address of the PCB which is to have its
	 *  GPRs, SP, and PC values copied to the CPU registers.
	 */
	public void dispatcher(long pcbStartAddress)
	{
		//Per "Homework2 Pseudo Code - Correction.docx" pcbStartAddress assumed to be valid.
		
		//Copy PCB GPR values to CPU's GPR.
		for(int i = 0; i < Hardware.GPR_SIZE; i++)
		{
			String addressString = "GPR_" + i; //The String representing i's PCBAddress GPR.
			hardware.setGprRegister(i
					, PCBController.PCBAddress.valueOf(addressString).dispatch(hardware, pcbStartAddress));
		}
		
		//Copy PCB's SP and PC values to CPU's SP and PC registers.
		hardware.setSP(PCBController.PCBAddress.SP.dispatch(hardware, pcbStartAddress));
		hardware.setPC(PCBController.PCBAddress.PC.dispatch(hardware, pcbStartAddress));
		
		//Set system mode(PSR) to user mode.
		hardware.setPSR(Hardware.USER_MODE);
	}
}
