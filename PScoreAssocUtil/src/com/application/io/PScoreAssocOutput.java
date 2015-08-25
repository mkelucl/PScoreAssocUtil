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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.application.data.gendat.*;

/**
 * Generates an output files, that can be fed to PSCOREASSOC,
 *
 */
public class PScoreAssocOutput {

	/**
	 * Produces a .dat file, that contains genotype infos that can be fed to SCOREASSOC.
	 * 
	 * more infos: format from: https://atgu.mgh.harvard.edu/plinkseq/view.shtml#var
	 * 
	 * @param allVariants: all the variants that need to be included in the annotation
	 * @param caseControls: Samples categorized as either case[0] or controls[1]
	 * @return: list of lines that can be written out as a textfile
	 */
	public static List<String> produceOutput(List<List<Sample>> caseControls, List<Variant> allVariants) {

		int i = 0;
		int j = 0;
		int z = 0;
		
		List<String> lines = new ArrayList<String>();
		Variant variant;
		List<Sample> samples;
		Sample sample;
		
		//StringBuilder stringBuilder; // some_appropriate_size

		byte[] sampleAlleles;
		
		// I. loop through all Variants
		for (i = 0; i < allVariants.size(); i++) 
		{
			variant = allVariants.get(i);
			
			// arent the '.' and '1' meant to be different??? 2504??
			// arent the '/' separator meant to be different?, also the 'PASS' meant to be 'space separated' ? should it be 
			//chr11:113281476  rs77930100 C/T   .     1     PASS
			lines.add("chr" + variant.CHROM+":"+variant.START + "\t"+ variant.ID +"\t" +variant.get_allele(0)+ "/" +variant.get_allele(1) + "\t.\t1\tPASS");
			//stringBuilder = new StringBuilder();
			
			// II. loop through both Cases/Controls
			for (z = 0; z < caseControls.size(); z++)
			{	
				samples = caseControls.get(z);
				for (j = 0; j < samples.size(); j++)
				{	
					sample = samples.get(j);
					//28279 1     CASE  C/C
					// are these always the same numbers???
					// Is it 'CONTROL' ???
					// what if Person has 3rd Allele??
					sampleAlleles = sample.get_AllelesAtPosition(variant.lociIndex); // eg: [0,1]
					
					//                                     array index = 0 -> CASE!!
					lines.add(sample.sampleName + "\t1\t" + ( z==0 ? "CASE" : "CONTROL" )+ "\t" +variant.get_allele(sampleAlleles[0]) + "/" + variant.get_allele(sampleAlleles[1]) );
					//stringBuilder.append("select id1, ");
				}
			}
			
			//lines.add(stringBuilder.toString());
		}

		return lines;
	}
	
	/**
	 * Produces an weights file, that can be fed to SCOREASSOC. The Actual weight values will need to be manually edited
	 * 
	 * @param allConsequenceTypes: all the difference consequence types like missense/non-synounymous AA change etc
	 * @return: list of lines that can be written out as a textfile
	 */
	public static List<String> produceOutput_WeightOutput(List<String> allConsequenceTypes) 
	{
		List<String> lines = new ArrayList<String>();
		int dummyWeight = 1; // may want to change this later...
		
		for (int i = 0; i < allConsequenceTypes.size(); i++) // go through all weights and write them in a tab separated list for 
			lines.add(allConsequenceTypes.get(i) + "\t" +dummyWeight);

		return lines;
	}
	
	
	/**
	 * Produces an .annot file, that can be fed to SCOREASSOC
	 * 
	 * @param allVariants: all the variants that need to be included in the annotation
	 * @return: list of lines that can be written out as a textfile
	 */
	public static List<String> produceOutput_AnnotFile(List<Variant> allVariants) 
	{
		List<String> lines = new ArrayList<String>();
		
		// add header as first line...
		lines.add("VAR" + "\t" +  "REF/ALT" + "\t" + "MINOR" + "\t" + "CNTA" + "\t" + "CNTU" + "\t" + "TOTA" + "\t" + "TOTU" + "\t" + "FUNC" + "\t" + "GENE");
		
		Variant variant;
		for (int i = 0; i < allVariants.size(); i++) // go through all variants
		{
			variant = allVariants.get(i);
			
			// add line 
			// chr11:113281476  C/T   T     1     2     5090  5090  silent      NM_000795,NM_016574
			// this structure follows PLINK/SEQ, but only loosely, only 2 columns matter, the 1st and the 8th, (the loci and the consequence) the rest are filled in with dummy values of the correct datatype
			lines.add("chr"+ variant.CHROM+":" +variant.START + "\t"+"X/X" + "\t"+"X" + "\t"+"1" + "\t"+"1" + "\t"+"5000" + "\t"+"5000" + "\t"+ variant.consequence + "\t"+"VALUE,VALUE" );
		}

		
		return lines;
	}
	
}
