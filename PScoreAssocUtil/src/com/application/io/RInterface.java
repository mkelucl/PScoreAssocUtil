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

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class RInterface {
	
	
	
	public static void produceROutput()
	{
		TextFileWriter.writeFileOutput(prepareROutput(),Paths.get("PvalueGraph.R"));
		//System.out.println("location is: " + getExecutionPath());
		
		 
		//System.out.println("get R path: " + getRPath());
	
		try {
			Process process = Runtime.getRuntime().exec( getRPath() + " "+ "PvalueGraph.R");
			System.out.println("executing R");
			
			
		    // wait until process exits -> this is a must otherwise on a server the process may exit before R is even started up so the plot would never get done
		    try {
		      process.waitFor();  // wait for process to complete
		    } catch (InterruptedException e) { System.err.println("R caused error: " + e);  }
		    
		    
		    System.out.println("R done, exit status was " + process.exitValue());
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	/**
	 * @return: produces a ready to be written R script that will produce a plot and save it as a .png
	 */
	private static List<String> prepareROutput()
	{
		List<String> lines = new ArrayList<String>();
		
		lines.add("currentDir = '"+BasicFileUtils.getExecutionPath()+"';# this gets set externally");
		lines.add("setwd(currentDir) ");

		lines.add("signed_log_P_values = NULL");
		lines.add("signed_log_P_values = scan('logPValues.txt') ## signed_log_P_values = rnorm(50, mean = 0, sd = 1);");

		lines.add("png(filename='pscoreassoc.png');");
		lines.add("hist(signed_log_P_values,breaks='FD', col = 'red') ");
		lines.add("abline(a = -log(0.05), b =0, col = 'green',lwd=3,lty='dotted');");
		lines.add("dev.off();");
		
		return lines;
	}
	
	

	/**
	 * @return the full path to the Rscript executible (should work for both win and linux) 
	 */
	public static String getRPath()
	{
		String rScripExecutable = "Rscript";
		// I determine on what OS we are on.
		// if its is NOT windows, then Rscript should just work
		if( System.getProperty("os.name").toLowerCase().contains("win") == false)
		{
			// under linux/Mac simply calling Rscript will execute it
			return rScripExecutable;
		}
		else // System.getenv("ProgramFiles(X86)")
		{
			// in windows, we need to manually lookup the full Rscript  executable path under the Program files
			String fullRExecPath = null;
			rScripExecutable+= ".exe"; // on windows executables have this extension
			
			// first, get the program files folder, as R will be installed there
			String programFilesFolder = System.getenv("ProgramFiles") + "\\R\\R-3.1.1\\bin"; // this will either get   'C:\Program Files (x86)' or  'C:\Program Files'
			// add + "/R" as that is the default R install folder, and this will be MUCH quicker
			
			// It is possible that a Java 32bit-JVM is installed on a 64 bit OS, in that case, the above would return 'C:\Program Files (x86)', but R may be installed under 'C:\Program Files' 
			String programFilesFolder_Forced64Bit = System.getenv("ProgramFiles").replace(" (x86)", "") + "\\R\\R-3.1.1\\bin"; 
			

			//System.out.println("programFilesFolder: " + programFilesFolder);
			//System.out.println("programFilesFolder_Forced32Bit: " + programFilesFolder_Forced64Bit);

			
			// look for R install folder, under program files
			fullRExecPath = BasicFileUtils.findFilePath(rScripExecutable,new File(programFilesFolder));
			if(fullRExecPath != null) return fullRExecPath;
			
			// if we couldn't find it under 64 bit program files folder, but we are under a 64 bit OS, then check the 32 bit proramfiles folder
			if(fullRExecPath == null && programFilesFolder.equals(programFilesFolder_Forced64Bit) == false) return BasicFileUtils.findFilePath(rScripExecutable,new File(programFilesFolder_Forced64Bit));
			
			return null;
		}
	}
}
