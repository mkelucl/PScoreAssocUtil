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


package com;

import java.util.ArrayList;

public class Commands extends CommandLineParser {
	public static String INPUT_VCF = "*inputvcf"; // eg: *inputvcf ALL.chrY.phase3_integrated_v1a.20130502.genotypes.vcf.gz
	public static String REGION = "*region"; // eg: *-region Y 2655180 2658789
	public static String SIMULATION = "*simulate"; // eg *simulate 1000 2000 (case/control)
	//public static String TEST_VARIANTS = "*variants";  // eg: *variants rs2066845:C:0.1 rs2066844:T:0.1
	public static String TEST_VARIANTS = "*variants";  // eg: *variants PSCORE_VAR.csv
	//public static String BASELINE_PROB = "*baselineProb";  // eg: *baselineProb 0.00322
	public static String SIM_OUTPUT = "*simoutput";  // eg: *simoutput myOutput.dat (myOutput.phe)
	public static String WEIGHTS_OUTPUT = "*weightsoutput";  // eg: *weightsoutput variants.annot weights.txt
	public static String VEPINPUT = "*vepinput"; // eg: *vepinput vepfile.txt
	
	public static String PSCOREASSOCPATH = "*pscorreassoc"; // eg: *pscorreassoc pscoreassoc.exe
	public static String NUM_SIMULATIONS = "*numsims"; // eg: *numsims 1000
	public static String PVAL_OUTPUT = "*pvaloutput"; // eg: *pvaloutput pvalues.txt
	public static String R_OUTPUT = "*routput"; // eg: *routput myScript.R
	public static String WEIGHTS_MAP = "*weightmap";  // eg: *weightmap weigthmap.csv
	public static String WEIGHT_FACTOR = "*weightfactor";  // eg: *weightfactor 10
	
	public static String UNIX_FORCED = "*forceunix";  // eg: -forceunix // IE if when writing output files we force a unix linebreak format
	public static String RANDOM_SEED = "*seed";  // eg: *seed 1
	

	

	
	
	@Override
	public void setupCommandparams()
	{
		super.setupCommandparams();
		
		noParamsErrorMsg = "usage: *inputvcf myVCF.vcf.gz *simulate 100 100 *region 1 123456 123457 *variants rs2066843:T:0.584 *baselineProb 0.00322 *simoutput genotype.dat phenotype.phe *vepinput VEPfile.csv *weightsoutput annotation.annot weights.txt (*seed 1) (*forceunix)";
		
		
		//            key                               if required    how many parameters are required(0 for any)      error message if required, or if not enough fields specified
		commands.put(REGION, new ArrayList<Object>() {{ add(true);                  add(3);                        add("optional param region must bespecified as as: *region chrom startPs endPos"); }} );
		commands.put(INPUT_VCF, new ArrayList<Object>() {{ add(true);                  add(1);                        add("must specify a target vcf as: *inputvcf yourVCF.vcf.gz"); }} );	
		commands.put(SIMULATION, new ArrayList<Object>() {{ add(true);                  add(2);                        add("simulations must specifiy number of cases and controls as: *simulate numCases numControls"); }} );	
		commands.put(TEST_VARIANTS, new ArrayList<Object>() {{ add(true);                  add(1);                        add("a test variants file must be specified as: *variants PSCORE_VAR.csv"); }} );
		//commands.put(BASELINE_PROB, new ArrayList<Object>() {{ add(true);                  add(1);                        add("you must specify a baseline Probability as: *baselineProb 0.00322"); }} );
		
		commands.put(SIM_OUTPUT, new ArrayList<Object>() {{ add(true);                  add(2);                        add("simulation output file must be specified as: *simoutput myOutput.dat (myOutput.phe)"); }} );
		commands.put(WEIGHTS_OUTPUT, new ArrayList<Object>() {{ add(true);                  add(2);                        add("weight output files must be specified as: *weightsoutput variants.annot weights.txt"); }} );
		commands.put(VEPINPUT, new ArrayList<Object>() {{ add(true);                  add(1);                        add("variant effect predictor file must be produced with options: 'RefSeq transcripts' + 'Filters / Restrict results: show most severe consequence'. The file must be then specified as: *vepinput vepfile.txt"); }} );
		
		commands.put(PSCOREASSOCPATH, new ArrayList<Object>() {{ add(true);                  add(1);                        add("The path to PSCOREASSOC executable must be then specified as: *pscorreassoc pscoreassoc.exe"); }} );
		commands.put(NUM_SIMULATIONS, new ArrayList<Object>() {{ add(true);                  add(1);                        add("The number of simulations must be then specified as: *numsims 1000"); }} );
		commands.put(PVAL_OUTPUT, new ArrayList<Object>() {{ add(true);                  add(1);                        add("The output file where the -log(P) values are saved must be then specified as: *pvaloutput pvalues.txt"); }} );
		commands.put(WEIGHTS_MAP, new ArrayList<Object>() {{ add(true);                  add(1);                        add("The input file for PSCOREASSOC of the weightmap: *weightmap weigthmap.csv"); }} );
		commands.put(WEIGHT_FACTOR, new ArrayList<Object>() {{ add(true);                  add(1);                        add("The weight factor for PSCOREASSOC is required as: *weightfactor 10"); }} );
		
		commands.put(R_OUTPUT, new ArrayList<Object>() {{ add(false);                  add(0);                        add("An optional R-script file must be then specified as: *routput"); }} );
		
		
		
		commands.put(UNIX_FORCED, new ArrayList<Object>() {{ add(false);                  add(0);                        add(""); }} );
		commands.put(RANDOM_SEED, new ArrayList<Object>() {{ add(false);                  add(1);                        add("optional parameter random seed must be specified as: *seed 1"); }} );
		
		// Handle if Test variant is a LIST or single object
		
		//commands.put(REGION, new ArrayList<Object>() {{  add(false); add(2);  add("String"); add("region must bespecified as as: -region chrom startPs endPos"); }} );
	}
	
	
	
	
}
