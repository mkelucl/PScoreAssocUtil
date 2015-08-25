// Copyright (c) 2015, Marton Kelemen
// All rights reserved.

// Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

// 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

// 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the 
// documentation and/or other materials provided with the distribution.

// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
// THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
// WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
// ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


package com.application.io;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.exec.*;

import com.application.data.gendat.GeneticDataBase;
import com.application.data.gendat.Sample;
import com.application.data.gendat.Variant;
import com.application.utils.HelperMethods;

/**
 * Bridges between this java app and the PSCOREASSOC application
 *
 */
public class PScoreAssocBridge {

	// pscoreassoc.exe myOutput.dat --annotfile nod2.annot --weightfile nod2weights.txt --outfile scoreassocOutput.txt

	public static String MIN_LOGP_IDENTIFIED = "SLP ="; // the bit in the pscoreassoc output that is just before the actualy -log(p) value 
	
	
	// version that uses the native Java external process functions
	public static Double callPscoreAssoc(String pscoreAssocFileLocation, String genotypesDatFile, String annotationFile, String weightsFile, int weightfactor)
	{
		// I. produce arguments in a string array that follows the following format:
		// pscoreassoc.exe myOutput.dat --annotfile nod2.annot --weightfile nod2weights.txt
		String[] arguments = new String[8];
		arguments[0] = pscoreAssocFileLocation;
		arguments[1] = genotypesDatFile;
		arguments[2] = "--annotfile";
		arguments[3] = annotationFile;
		arguments[4] = "--weightfile";
		arguments[5] = weightsFile;
		arguments[6] = "--weightfactor"; // buggy
		arguments[7] = String.valueOf(weightfactor);

		// II. execute Pscoreassoc runtime
	    Runtime runtime = Runtime.getRuntime(); // A Runtime object has methods for dealing with the OS
	    Process process;     // Process tracks one external native process
	    BufferedReader is;  // reader for output of process
	    String line;
	    Double minLogP = Double.NaN;
	    try 
	    { 
	    	process = runtime.exec(arguments);  
		    System.out.println("PSCOREASSOC exec");
		    is = new BufferedReader(new InputStreamReader(process.getInputStream()));
	    
		    // III. parse output from ScoreAssoc
		    boolean foundPvalue = false;
		    while ((line = is.readLine()) != null && foundPvalue == false)
		    {
		    	//System.out.println(line);
		    	// find the line that contains the logP value
		    	if(line.contains(MIN_LOGP_IDENTIFIED) == true)
		    	{	
		    		// need to disable regex for this split: http://stackoverflow.com/questions/6374050/string-split-not-on-regular-expression
		    		//String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].split(" "); // -log(p) =     1.45
		    		String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].trim().split(" ")[0];
		    		
		    		minLogP = HelperMethods.tryParseDouble(value); // the actual value has some whitespace around it by that is .trim()-ed away
		    		
		    		System.out.println("minLogP: " + minLogP + " from: " + value + " Array contains " + Arrays.toString(line.split(MIN_LOGP_IDENTIFIED)));
		    		foundPvalue = true;
		    	}
		    }
		    
		    // do I need to close this?
		    is.close();
		    
		    System.out.println("PSCOREASSOC EOF");
		    System.out.flush();
		    
		    // IV. wait until PScoreassoc exits
		    try {
		      process.waitFor();  // wait for process to complete
		    } catch (InterruptedException e) { System.err.println(e);  return Double.NaN; }
		    
		    
		    System.out.println("Process done, exit status was " + process.exitValue()); // dont use System.err, as that would break the bluster
		    
		    
	    } catch (IOException e1) {e1.printStackTrace(); }



		return minLogP;
	}
	
	/*private static String TEMP_OUTPUTFILENAME = "scoreOutput.txt";
	private static String TEMP_BASHFILE = "pstest.sh";
	
	
	// version that depending on OS either calls Pscoreassoc directly (win) or produces a bash script that calls pscoreassoc (linux)
	public synchronized static Double callPscoreAssoc_script(String pscoreAssocFileLocation, String genotypesDatFile, String annotationFile, String weightsFile, int weightFactor)
	{
		// I. get Runtime
	    Runtime runtime = Runtime.getRuntime(); // A Runtime object has methods for dealing with the OS
	    Process process;     // Process tracks one external native process

	    Double minLogP = Double.NaN;
	    try 
	    { 
	    	
	    	
	    	// II. need to decide how to call PscoreAssoc	
	    	// if windows OS this is simple, simply call the Pscoreassoc executable directly
	    	if( System.getProperty("os.name").toLowerCase().contains("win") == true)
	    	{ System.out.println("PSCOREASSOC exec on Windows");
	    		// produce arguments in a string array that follows the following format:
	    		// pscoreassoc.exe myOutput.dat --annotfile nod2.annot --weightfile nod2weights.txt --outfile scoreOutput.txt;
	    		String[] arguments = new String[9];
	    		arguments[0] = pscoreAssocFileLocation;
	    		arguments[1] = genotypesDatFile;
	    		arguments[2] = "--annotfile";
	    		arguments[3] = annotationFile;
	    		arguments[4] = "--weightfile";
	    		arguments[5] = weightsFile;
	    		arguments[6] = "--weightfactor"; 
	    		arguments[7] = "10";
	    		arguments[8] = "--outfile " + TEMP_OUTPUTFILENAME;
	    		
	    		process = runtime.exec(arguments);  
	    		//System.out.println(Arrays.toString(arguments));
	    	}
	    	else  // if its linux or mac, we cannot do that, due to platform dependent bugs of calling external processes...
	    	{	  // SOLUTION: need to write a bash script that calls PScoreAssoc, and call that instead
	    		  // otherwise Pscreoassoc will randomly quit/hang and crash the app
	    		 System.out.println("PSCOREASSOC exec on Linux");
	    		//List<String> pscoreBashLines = BashInterface.prepareBashOutput(pscoreAssocFileLocation, genotypesDatFile, annotationFile, weightsFile, TEMP_OUTPUTFILENAME, weightFactor);
	    		//TextFileWriter.writeFileOutput(pscoreBashLines,Paths.get(TEMP_BASHFILE));
	    		
	    		process = runtime.exec("bash " + TEMP_BASHFILE);  
	    	}
	
		    // IV. wait until PScoreassoc exits ( this will work for even when Pscoreassoc is invoked via the bash script, as that wont finish until the pscoreassoc is done!
		    try {
		      process.waitFor();  // wait for process to complete
		    } catch (InterruptedException e) { System.err.println(e);  return Double.NaN; }
		    System.out.println("Process done, exit status was " + process.exitValue()); // dont use System.err, as that would break the bluster
		    
		    
		    // V. once PscoreAssoc finished writing the file output, load it in and parse out the minus Log P value
		    minLogP = processPScoreAssocOutputFile(TEMP_OUTPUTFILENAME);
  
	    } catch (IOException e1) {e1.printStackTrace(); }

		return minLogP;
	}

	// helper function that parses a PscoreAssoc output file and finds the minus log p value that it returns
	private static double processPScoreAssocOutputFile(String pscoreAssocOutputFile)
	{
		Scanner s;
		String line;
		Double minLogP = Double.NaN;
		try {	
			s = new Scanner (new BufferedReader( new FileReader(pscoreAssocOutputFile)));
			//s.useDelimiter(System.getProperty("line.separator")); // OS independent line delimiter
			while(s.hasNext() ) // keep going through the file
			{
				line = s.nextLine(); 
				
		    	//System.out.println(line);
		    	// find the line that contains the logP value
		    	if(line.contains(MIN_LOGP_IDENTIFIED) == true)
		    	{	
		    		// need to disable regex for this split: http://stackoverflow.com/questions/6374050/string-split-not-on-regular-expression
		    		//String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].split(" "); // -log(p) =     1.45
		    		String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].trim().split(" ")[0];
		    		
		    		minLogP = HelperMethods.tryParseDouble(value); // the actual value has some whitespace around it by that is .trim()-ed away
		    		
		    		System.out.println("minLogP: " + minLogP + " from: " + value + " Array contains " + Arrays.toString(line.split(MIN_LOGP_IDENTIFIED)));

		    	}
			}
		} catch (FileNotFoundException e) {System.out.println(pscoreAssocOutputFile); e.printStackTrace(); }
		
		return minLogP;
	}
	
	
	
	

	
	
	
	
	public static Double callPscoreAssoc_testLS(String pscoreAssocFileLocation, String genotypesDatFile, String annotationFile, String weightsFile)
	{
		CommandLine cmdLine = new CommandLine("ls");

		DefaultExecuteResultHandler rh = new DefaultExecuteResultHandler();
		DefaultExecutor executor = new DefaultExecutor();
		//ExecuteWatchdog wd  = new ExecuteWatchdog( ExecuteWatchdog.INFINITE_TIMEOUT );
		//executor.setWatchdog( wd );
		//executor.setExitValue(1); // not needed?

		// handle output
		CollectingLogOutputStream  outputStream = new CollectingLogOutputStream ();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);
       

		// II. execute Pscoreassoc runtime
	    String line;
	    int exitValue;
	    Double minLogP = Double.NaN;
	    try 
	    { 
	    	exitValue = executor.execute(cmdLine);
	    	//executor.execute(cmdLine,rh);
	    
	    	
			System.out.println("TEST exec");
			List<String> lines = outputStream.getLines(); 
			System.out.println("got # lines: " + lines.size() + " _____________________________");
			
		    for (int i = 0; i < lines.size(); i++) System.out.println(lines.get(i));
		    System.out.println("______________________________________");
			
	    	try {
				rh.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    System.out.println("TEST EOF");
		
		   
		    //System.out.println("Process done, exit status was " + rh.getExitValue());
		    System.out.println("Process done, exit status was " +exitValue); // dont use System.err, as that would break the bluster
		    System.out.flush(); //  flushing your output after writing. The text could be stuck in a buffer and not getting to your java program.
		    
	    } catch (IOException e1) {e1.printStackTrace(); }


		return 1.0;
	}
	
	
	// version that uses Apache Commons exec to manage external processes (async)
	public static Double callPscoreAssoc_commons(String pscoreAssocFileLocation, String genotypesDatFile, String annotationFile, String weightsFile)
	{
		// I. produce arguments in a string array that follows the following format:
		// pscoreassoc.exe myOutput.dat --annotfile nod2.annot --weightfile nod2weights.txt
		CommandLine cmdLine = new CommandLine(pscoreAssocFileLocation);
		cmdLine.addArgument(genotypesDatFile);
		cmdLine.addArgument("--annotfile");
		cmdLine.addArgument(annotationFile);
		cmdLine.addArgument("--weightfile");
		cmdLine.addArgument( weightsFile);
		cmdLine.addArgument("--weightfactor");
		cmdLine.addArgument("10");
		
		
		// http://stackoverflow.com/questions/7340452/process-output-from-apache-commons-exec
		// http://commons.apache.org/proper/commons-exec/tutorial.html
		// what about WAITING????
		DefaultExecuteResultHandler rh = new DefaultExecuteResultHandler();
		DefaultExecutor executor = new DefaultExecutor();
		//ExecuteWatchdog wd  = new ExecuteWatchdog( ExecuteWatchdog.INFINITE_TIMEOUT );
		//executor.setWatchdog( wd );
		//executor.setExitValue(1); // not needed?

		// handle output
		CollectingLogOutputStream  outputStream = new CollectingLogOutputStream ();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);
       

		// II. execute Pscoreassoc runtime
	    String line;
	    int exitValue;
	    Double minLogP = Double.NaN;
	    try 
	    { 
	    	//exitValue = executor.execute(cmdLine);
	    	executor.execute(cmdLine,rh);

	    	
			System.out.println("PSCOREASSOC exec");
			List<String> lines = outputStream.getLines(); 

			 // III. parse output from ScoreAssoc
		    for (int i = 0; i < lines.size(); i++)
		    {
		    	line = lines.get(i);
		    	System.out.println(line);
		    	if(line.contains(MIN_LOGP_IDENTIFIED) == true)
		    	{	
		    		// need to disable regex for this split: http://stackoverflow.com/questions/6374050/string-split-not-on-regular-expression
		    		//String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].split(" "); // -log(p) =     1.45
		    		String value = line.split(Pattern.quote(MIN_LOGP_IDENTIFIED))[1].trim().split(" ")[0];
		    		
		    		minLogP = HelperMethods.tryParseDouble(value); // the actual value has some whitespace around it by that is .trim()-ed away
		    		
		    		System.out.println("minLogP: " + minLogP + " from: " + value + " Array contains " + Arrays.toString(line.split(MIN_LOGP_IDENTIFIED)));
		    		break;
		    	}
		    }
		   
		   
	    	try {
				rh.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    
		    System.out.println("PSCOREASSOC EOF");
		
		   
		    System.out.println("Process done, exit status was " + rh.getExitValue());
		    //System.out.println("Process done, exit status was " +exitValue); // dont use System.err, as that would break the bluster
		    System.out.flush(); //  flushing your output after writing. The text could be stuck in a buffer and not getting to your java program.
		    
	    } catch (IOException e1) {e1.printStackTrace(); }



		return minLogP;
	}*/
	

	
}


/*

try 
{
	String line;
	Process process = Runtime.getRuntime().exec("tasklist.exe");
	BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
	
	
    while ((line = input.readLine()) != null) 
    {
        System.out.println(line); //<!-- Parse data here.
    }
    input.close();
}

catch(Exception e)
{
	System.out.println(e.getMessage());
}
*/
