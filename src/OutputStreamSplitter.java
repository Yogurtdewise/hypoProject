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

import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>
 * This class writes output to multiple output streams.
 *  For each of the OutputStream methods, it overrides the method and
 *  performs that method on each of the OutputStreams specified in
 *  the OutputStreamSplitter constructor.
 * </p><p>
 * Other solutions to the problem would be:</br>
 *   (1) To append a file at each call to System.out, System.err, and every throw/catch.</br>
 *   (2) Use the non-standard library: org.apache.commons.io.output.TeeOutputStream
 * </p>
 * 
 * @author Kenneth Chin
 *
 */
public class OutputStreamSplitter extends OutputStream
{
	OutputStream[] outputStreams;
	
	/**
	 * The public constructor for OutputStreamSplitter.</br>
	 * Creates an OutputStream object that outputs to the
	 *  specified OutputStreams.
	 * @param outputStreams All OutputStream objects to output to
	 *  (separated by commas, per JAVA syntax).
	 */
	public OutputStreamSplitter(OutputStream... outputStreams)
	{
		this.outputStreams= outputStreams; 
	}
	

	//Overridden methods of the OutputStream super class.
	
	@Override
	public void write(int b) throws IOException
	{
		for (OutputStream out: outputStreams)
			out.write(b);			
	}
	
	@Override
	public void write(byte[] b) throws IOException
	{
		for (OutputStream out: outputStreams)
			out.write(b);
	}
 
	@Override
	public void write(byte[] b, int off, int len) throws IOException
	{
		for (OutputStream out: outputStreams)
			out.write(b, off, len);
	}
 
	@Override
	public void flush() throws IOException
	{
		for (OutputStream out: outputStreams)
			out.flush();
	}
 
	@Override
	public void close() throws IOException
	{
		for (OutputStream out: outputStreams)
			out.close();
	}
}
