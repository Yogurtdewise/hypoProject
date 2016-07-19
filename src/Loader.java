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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p>
 * The Loader class is responsible for reading a HYPO Machine machine-language
 *  text file, and loading the program into HYPO Machine User Program Memory. It also
 *  contains methods for determining the specified program's start address in HYPO Machine
 *  memory, and program size.
 *  </p><p>
 * This class follows the singleton pattern, as there should only be one Loader object per
 *  HYPO machine. Therefore, one must call Loader.getInstance() to obtain the only instance
 *  of the Hardware class.
 *  </p><p>
 * Core methods from "HYPO Simulator Pseudo Code - Spring 2013.doc" specification
 *  contained in this class:</br>
 *  	absoluteLoader(String filename)
 *  </p>
 * 
 * @author Kenneth Chin
 *
 */
public final class Loader
{
	
	//The size of the Program Counter.
	public static final int PC_SIZE = Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND;

	private static final String PWD = System.getProperty("user.dir"); //The current working directory.
	
	private static File           program;      //The File object of the specified program.
	private static BufferedReader reader;       //The Buffered reader for the specified program.
	private static String         currentLine;  //The last line read by reader.
	private static long           address;	    //The address value from currentLine.
	private static long           content;      //The content of address from currentLine.
	private static long           programSize;  //The number of lines loaded into memory.
	private static long           firstAddress; //The memory address of the program's first line.
	private static boolean        isFirstLine;  //Has the first line of the user program has been read?
	
	private static Hardware hardware = null;   //The Hardware object whose memory is to be loaded.
	private static Loader singleInstance = null; //The singleton instance of Loader.
	
	
	/**
	 * Private constructor to prevent instantiation.
	 * @param hardware The Hardware object whose memory is to be loaded.
	 */
	private Loader(Hardware hardware)
	{
		initLoader();
		Loader.hardware = hardware;
	}
	
	/**
	 * Used to obtain the single instance of the Loader class.
	 * @param hardware The Hardware object whose memory is to be loaded.
	 * @return The single instance of the Loader class.
	 */
	public static Loader getInstance(Hardware hardware)
	{
		if(singleInstance == null)
			singleInstance = new Loader(hardware);
		return singleInstance;
	}
	
	/**
	 * Used to initialize all variables associated with a single user program.
	 */
	private void initLoader()
	{
		program     = null;
		reader      = null;
		currentLine = null;
		address     = Hardware.MEM_SIZE; //Initialize to an invalid address.
		content     = 0;
		programSize = 0;
		firstAddress= -1;
		isFirstLine = true;
	}
	
	/**********************************************************************************************
	 * method: absoluteLoader
	 * Task Description:
	 *  Read and parse a specified machine language program file, and load the contents into
	 *  Hardware.memory. Ensures the program file was properly formatted, and address/PC values
	 *  are within the User Program Memory area. If at any point parsing or reading fails, return
	 *  an appropriate error code; else return the PC value of the program's start.
     *  
	 * Input parameters:
     * 	String filename: A String indicating the file name of the machine language program to load.
     *   The file must exist in the hypoProject working directory.
     * 
     * Output parameters
     *  Hardware hardware:   The Hardware object containing HYPO machine data.
     * 	long firstAddress:   The address pointed to by the user program's first line.
     *  long programSize:    The number of lines of the user program.
	 *	boolean isFirstLine: A flag indicating if the first line of the user program has been read.
     * 
     * Function return value:
     *  Hardware.USER_PROGRAM_LOWER_BOUND to (Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND - 1):
     *   The PC value of the program's start in the User Program Memory area.
     *  StatusCode.ERROR_FILE_OPEN:  Returned if there was an error opening the specified file.
     *  StatusCode.ERROR_FILE_READ:  Returned if there was an error reading or parsing the specified
     *   file.
     *  StatusCode.ERROR_FILE_CLOSE: Returned if there was an error closing the specified file.
     *  StatusCode.ERROR_LOADER_INVALID_ADDRESS: Returned if absoluteLoader read an invalid address
     *   from the specified file.
     *  StatusCode.ERROR_NO_END_OF_PROGRAM: Returned if the end of file was reached without reading
     *   an end of program indicator.
	 *
	 * @param filename A String indicating the file name of the machine language program to load.
	 *  The file must exist in the hypoProject working directory.
	 * @return The PC value of the program's start, or an error code if the program could not be
	 *  read/parsed.
	 **********************************************************************************************/
	public long absoluteLoader(String filename)
	{
		initLoader();
		//Open program file.
		if(!openFile(filename))
			return StatusCode.ERROR_FILE_OPEN.getValue();
		//Read program until End of Program or End of File.
		do
		{
			//Read a line from file. If failed, return error status.
			if(!readLine(reader))
			{
				closeReader(reader);
				return StatusCode.ERROR_FILE_READ.getValue();
			}
			//If EOF has not been reached, parse and load the line.
			if(currentLine != null)
			{
//Krishna: No need to compute the program size
//Ken: We discussed this in HW1. A hard-coded program size was not discussed in-class.
//	   I am keeping this function in HW2 to reduce output on OS.createProcess.
//		This will provide a way to print only the user program area of the newly created
//		process, as opposed to the entire user program area.
				programSize++; //The program file has a line, increment programSize.
				if(!parseLine(currentLine)) //Parse currentLine. If improperly formatted, return ERROR_FILE_OPEN.
					return StatusCode.ERROR_FILE_READ.getValue();
//Krishna: Upper limit is not valid since the program area is limited to o to 1499 as stated in the class.
//Ken: We discussed this in HW1. This is fixed for HW2, since programs are guaranteed to be ONLY user programs.
				if(Hardware.isUserProgram(address))
				{   //Address is valid.
					if(isFirstLine) //Retain the first address for getFirstAddress().
					{
						firstAddress = address;
						isFirstLine  = false;
					}
					//Set the content to the specified memory address.
					hardware.setMemAddressValue(address, content);
				}
				else if(address < 0)
				{   //End of Program Encountered.
//Krishna: What is PC_SIZE value?
//Ken: We discussed this in HW1. PC_SIZE is defined as a class variable, and equal to
//		Hardware.USER_DYNAMIC_MEMORY_LOWER_BOUND (1500).
					if(content < 0 && content >= PC_SIZE)
					{ //Ensure PC is valid.
						System.err.println("Loader.absoluteLoader: End of Program encountered with an "
								+ "invalid PC value of: " + content);
						closeReader(reader);
						return StatusCode.ERROR_LOADER_INVALID_PC.getValue();
					}
					//PC is valid. Display result text, and close the BufferedReader.
					System.out.println("Loader.absoluteLoader: End of Program encountered. "
							+ "Program starts at: " + content);
					System.out.println();
					if(!closeReader(reader)) //Ensure the file stream is closed.
						return StatusCode.ERROR_FILE_CLOSE.getValue();
					return (int)content; //Successful read and load of program.
				}
				else
				{   //Invalid address.
					System.err.println("Loader.absoluteLoader: Address (" + address + ") out of bounds. "
							+ "PC_SIZE = " + PC_SIZE);
					closeReader(reader);
					return StatusCode.ERROR_LOADER_INVALID_ADDRESS.getValue();
				}
			}
		}while(currentLine != null); //If currentline == null, then EOF reached. Stop reading the file.
		
		System.err.println("Loader.absoluteLoader: End of File encountered without End of Program indicator!");
		closeReader(reader);
		return StatusCode.ERROR_NO_END_OF_PROGRAM.getValue();
	}
	
	/**
	 * Used to obtain the number of lines loaded by the most recent call to Loader.absoluteLoader().
	 *  If no call to Loader.absoluteLoader() has been made, the program size default is zero.
	 * @return A long indicating the number of lines loaded by the most recent call to Loader.absoluteLoader().
	 */
	public long getProgramSize()
	{
		return programSize;
	}
	
	/**
	 * Used to obtain the address of the first line of the user program (not to be confused with program start).
	 *  If no call to Loader.absoluteLoader() has been made, the default address is -1.
	 * @return A long representing the first address of the program loaded by Loader.absoluteLoader().
	 *  If Loader.absoluteLoader() has not been previously run, then the default value of -1 is returned.
	 */
	public long getFirstAddress()
	{
		return firstAddress;
	}
	
	/**
	 * Used to create a File and BufferedReader object of the given filename String.
	 *  Prints an error to console if there is a problem opening the file.
	 * @param filename A String indicating the program name to load, found in the hypoProject working directory.
	 * @return A boolean indicating false if there was a problem opening the file, true otherwise.
	 */
	private static boolean openFile(String filename)
	{
		if(filename == null){
			System.err.println("Loader.openFile filename is null.");
			return false;
		}
		program = new File(PWD + File.separator + filename);
		if(!program.exists())
		{
			System.err.println("Loader.openFile file does not exist.");
			return false;
		}
		return makeReader(program);
	}
	
	/**
	 * Helper method for openFile. Creates a BufferedReader to read the file.
	 *  Prints an error message and stack trace if there is a problem creating the BufferedReader.
	 * @param program The File object that is to have its text read.
	 * @return A boolean indicating true if the BufferedReader was successfully created, false otherwise.
	 */
	private static boolean makeReader(File program){
		try
		{
			reader = new BufferedReader(new FileReader(program));
			return true;
			
		}
		catch (FileNotFoundException e)
		{
			System.out.println("AbsolteLoader.makeReader could not load the file!");
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Sets currentLine to the next line from the specified BufferedReader. If currentLine is set to null,
	 *  then the end of file(EOF) has been reached. Prints an error message to the console if there was a
	 *  problem reading the file.
	 * @param reader The BufferedReader from which the next line is to be obtained.
	 * @return A boolean indicating true if the next line or EOF was read, false otherwise.
	 */
	private static boolean readLine(BufferedReader reader)
	{
		try
		{
			if(!reader.ready())
			{
				System.err.println("Loader.readLine reader is not ready.");
				return false;
			}
			currentLine = reader.readLine();
			return true;
		}
		catch (IOException e)
		{
			System.err.println("Loader.readLine failed!");
			e.printStackTrace();
			return false;
			
		}
	}
	
	/**
	 * Used to split the currentLine String into an address and address content. Assigns these values to
	 * 	Loader.address and Loader.content as type long. Prints an error message if there
	 *  is a formatting error in the given String. Expected format: long + whitespace + long
	 * @param currentLine The String to be parsed into address and content, given the format:
	 * 	long + whitespace + long
	 * @return A boolean indicating true if the given String was successfully parsed, false otherwise.
	 */
	private static boolean parseLine(String currentLine)
	{
		if(currentLine == null)
		{
			System.err.println("Loader.parseLine currentLine is null!");
			return false;
		}
		else
		{
			String[] tokenArray = currentLine.split("\\s+"); //Tokenize around whitespaces.
			if(tokenArray.length != 2)
			{
				System.err.println("Loader.parseLine currentLine is improperly formatted.");
				return false;
			}
			//Convert address String to long value and assign to Loader.address.
			try
			{
				Loader.address = Long.parseLong(tokenArray[0]);
			}
			catch(NumberFormatException e)
			{
				System.err.println("Loader.parseLine address is not a long value ("
						+ tokenArray[0] + ").");
				return false;
			}
			
			//Convert content String to long value and assign to Loader.content.
			try
			{
				Loader.content = Long.parseLong(tokenArray[1]);
			}
			catch(NumberFormatException e)
			{
				System.err.println("Loader.parseLine content is not a long value ("
						+ tokenArray[1] + ").");
				return false;
			}
			return true; //currentLine successfully parsed.
		}
	}
	
	/**
	 * Used to close a BufferedReader stream. Prints an error message to the console if there was an
	 *  error closing the stream.
	 * @param reader The BufferedReader to be closed.
	 * @return A boolean indicating true if the BufferedReader stream was successfully closed, false otherwise.
	 */
	private static boolean closeReader(BufferedReader reader)
	{
		try
		{
			reader.close();
			reader = null;
			return true;
		}
		catch (IOException e)
		{
			System.err.println("Loader.closeReader failed to close the BufferedReader.");
			e.printStackTrace();
			reader = null;
			return false;
		}
	}
}
