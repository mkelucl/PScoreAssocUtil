package com.application.io;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BashInterface {
	
	

	
	/**
	 * @return: produces a ready to be written Bash script that will execute PScoreAssoc
	 */
	public static List<String> prepareBashOutput(String pscoreAssocPath, String genotypeIntput, String annotIntput, String weightIntput, String outputFile, int weightFactor)
	{
		List<String> lines = new ArrayList<String>();
	
		lines.add("#!/bin/bash");
		lines.add("#$ -S /bin/bash");
		lines.add("#$ -cwd");
		lines.add("#$ -pe smp 1");
		lines.add("#$ -l scr=0G");
		lines.add("#$ -l tmem=1G,h_vmem=1G");
		lines.add("#$ -l h_rt=20:0:0");
		lines.add("#$ -V");
		lines.add("#$ -R y");
		
		lines.add("");
		lines.add("date");
		lines.add(pscoreAssocPath + " " +genotypeIntput + " --annotfile " + annotIntput + " --weightfile "+weightIntput+" --weightfactor "+weightFactor+" --outfile "+outputFile+" > ps.out 2> ps.err");
		lines.add("date");

		return lines;
	}
}
