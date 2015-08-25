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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import com.application.data.gendat.*;
import com.application.io.PScoreAssocBridge;
import com.application.io.PScoreAssocOutput;
import com.application.io.RInterface;
import com.application.io.TestVariants;
import com.application.io.TextFileWriter;
import com.application.io.phenotype.PheIO;
import com.application.io.vcf.VCFParser;
import com.application.io.vep.VEPParser;
import com.application.simulation.GenerateSimulationData;
import com.application.utils.DebugUtil;
import com.application.utils.HelperMethods;

public class PScoreAssocUtilApp {
	
	// Master variable to control Debug 
	public static Boolean DEBUG = false; // master switch to enable debugging
	public static Boolean DEBUG_CONSOLE = false; // if the console should be enabled
	public static Boolean DEBUG_PERFORMANCE = true; // switch to enable Debutil's performance output
	
	// Commands from commandline
	Commands commandlineparser = new Commands();
	
	
	// main vars
	protected GeneticDataBase genDB;
	
	
	// Variants that app tests / Simulation data
	// public Variant[] foundVariants; // sparse array, with matching indices to the below Lists
	// must use array datatype as we dont know if the VCF really has all the variants we are looking for
	// as a result, to make sure that the indices for the actually found variants keep referring to the below Lists it has to be 'sparse'
	//public List<String> selectedVariant_RSIDs = new ArrayList<String>();
	//public List<String> selectedVariant_Alleles = new ArrayList<String>();
	//public List<Double> selectedVariant_EffectSizes = new ArrayList<Double>();
	private List<List<Sample>> simulatedCaseControls ;
	private LinkedHashMap<String, Object> parameters;

	// output values
	private List<String> outputLines;
	private Double[] minLogP;
	
	// Debug
	private DebugUtil debugger;

	
	public PScoreAssocUtilApp()
	{
		debugger = new DebugUtil();
	}
	

	

	
	/**
	 * Inits app, parses parameters from command line
	 * and depending on instructions the app is executed
	 */
	public void init(String[] args)
	{ if(DEBUG_PERFORMANCE) DebugUtil.instance.initDebugLogic();
		
		// get commands
		parameters = commandlineparser.getParameters(args);
		
		// stop if there are errors from the command line
		if(parameters.containsKey(CommandLineParser.ERROR) ) return;

	
		// respect unix writer format
		if(parameters.containsKey(Commands.UNIX_FORCED)) TextFileWriter.FORCE_UNIX_FORMAT = true;
		
		
		// I. get the region from the VCF file that we want to process
		loadGenDB();
		
		
		// III. try to find test Variants
		initTestVariants();   if(DEBUG_PERFORMANCE) DebugUtil.instance.processVCFDatas();
		
		
		// II attach VEP to Variants, and export weights/annot file for PSCOREASSOC
		loadVEP_outputWeights();
		

		
		// IV run simulation the specified number of times, this fills up the minLogP array
		runAllSimulations();

		
		// V. output the P values into a file
		Path outputFilePath = Paths.get(commandlineparser.getString(Commands.PVAL_OUTPUT,0));
		TextFileWriter.writeFileOutput(outputLines, outputFilePath);
		// WILL THIS OVERWRITE the existing file ?? or just keep appending it???
		
		// VI. if an R output was requested do that
		if(parameters.containsKey(Commands.R_OUTPUT)) RInterface.produceROutput();
		
		if(DEBUG_PERFORMANCE) DebugUtil.instance.processNext("SIMULATIONS:");
		if(DEBUG_PERFORMANCE) System.out.println("finished: " + DebugUtil.instance.SPEEDTEST_RESULTS);
	}

	
	
	private void loadGenDB()
	{
		List<String> region = (ArrayList<String>) parameters.get(Commands.REGION);
		String chromosome =region.get(0);
		int startPos = HelperMethods.tryParseInt(region.get(1));
		int endPos = HelperMethods.tryParseInt(region.get(2));
		List<Object> commands = (ArrayList<Object>)(parameters.get(Commands.INPUT_VCF));
		String vcfFilePath = commandlineparser.getString(Commands.INPUT_VCF,0);
		
		genDB = VCFParser.processInput(vcfFilePath,chromosome,startPos,endPos);	
	}
	
	
	private void loadVEP_outputWeights()
	{
		List<String> textFileLines;
		String outputFileLocation;
		Path outputFilePath;

		// parse VEP consequences, and attach them to Variants
		List<String> consequences = VEPParser.parseVEP(commandlineparser.getString(Commands.VEPINPUT,0), genDB.variantsByChromPos);
		
		// write weights file output
		textFileLines = PScoreAssocOutput.produceOutput_WeightOutput(consequences);
		outputFileLocation = commandlineparser.getString(Commands.WEIGHTS_OUTPUT,1);
		outputFilePath = Paths.get(outputFileLocation);
		TextFileWriter.writeFileOutput(textFileLines, outputFilePath);

		// write .annot file output
		outputFileLocation = commandlineparser.getString(Commands.WEIGHTS_OUTPUT,0);
		outputFilePath = Paths.get(outputFileLocation);
		textFileLines = PScoreAssocOutput.produceOutput_AnnotFile(genDB.allVariants);
		TextFileWriter.writeFileOutput(textFileLines, outputFilePath);
	}
	
	
	
	public TestVariants testVariants;
	private void initTestVariants()
	{
		int i;
		Variant currentVariant;
		int randomSeed = 0;
		if(parameters.containsKey(Commands.RANDOM_SEED)) randomSeed = commandlineparser.getInt(Commands.RANDOM_SEED,0);
		else randomSeed = new Random().nextInt();
				
		testVariants = TestVariants.parseTestVariants(commandlineparser.getString(Commands.TEST_VARIANTS,0), genDB,randomSeed );
		
		
		
		//List<String> variantList = (List<String>) parameters.get(Commands.TEST_VARIANTS);
		//String variantString;
		//String[] variantArray;
		
		// finish processing command line
/*		for(i = 0; i < testVariants.effectVariants.size(); i++)
		{
			variantString = testVariants.effectVariants.get(i); // this gets rs2066845:C:1.861
			variantArray = variantString.split(":"); // [rs2066845,C,1.861]
			

			selectedVariant_RSIDs.add(variantArray[0]);
			selectedVariant_Alleles.add(variantArray[1]);
			selectedVariant_EffectSizes.add(HelperMethods.tryParseDouble(variantArray[2]));
		}*/
		
		//selectedVariant_EffectSizes = testVariants.alleleEffectSizes;
		// foundVariants = testVariants.effectVariants.toArray(new Variant[testVariants.effectVariants.size()]);
		
		//System.out.println("variantList: " + variantList.toString());
		//System.out.println("selectedVariant_RSIDs: " + selectedVariant_RSIDs.toString());
		//System.out.println("selectedVariant_Alleles: " + selectedVariant_Alleles.toString());
		//System.out.println("selectedVariant_EffectSizes: " + selectedVariant_EffectSizes.toString());
		
		// go through all variants and find variants that we need
/*		for(i = 0; i < genDB.allVariants.size(); i++)
		{
			currentVariant = genDB.allVariants.get(i);
			if(selectedVariant_RSIDs.indexOf(currentVariant.ID) !=-1) // if variant is found
			{
				foundVariants[selectedVariant_RSIDs.indexOf(currentVariant.ID)] = currentVariant; // need to add the variant at the exact same index, as all 3 lists work on the same indices
				
				//System.out.println("Found Variant: " + currentVariant.ID);
			}
		}	
		*/
		
	}
	
	
	private void runAllSimulations()
	{
		int numSimulations = commandlineparser.getInt(Commands.NUM_SIMULATIONS,0);
		int randomSeed;
		int i;
		outputLines = new ArrayList<String>();
		minLogP = new Double[numSimulations]; // the minus Log(P) values output by scoreassoc
		
		
		// if there was a random seed specified, get the initial random seed
		Random simRandom;
		if(parameters.containsKey(Commands.RANDOM_SEED)) simRandom = new Random(commandlineparser.getInt(Commands.RANDOM_SEED,0));
		else simRandom = new Random();	
		
		for(i  = 0; i < numSimulations; i++)
		{ System.out.println("Running simulation: " + (i+1));
			runIndividualSimulation(simRandom.nextInt());

			// write simulation output data into file
			outputSimData();
			
			
			
			int weightFactor = commandlineparser.getInt(Commands.WEIGHT_FACTOR,0);
			
			// call Pscoreassoc, and fill up the P values
			minLogP[i] = PScoreAssocBridge.callPscoreAssoc(commandlineparser.getString(Commands.PSCOREASSOCPATH,0),commandlineparser.getString(Commands.SIM_OUTPUT,0),commandlineparser.getString(Commands.WEIGHTS_OUTPUT,0),commandlineparser.getString(Commands.WEIGHTS_MAP,0),weightFactor);
			
			
			outputLines.add(String.valueOf(minLogP[i]));
			if(DEBUG_PERFORMANCE) DebugUtil.instance.processNext("PSCORE ASSOC TOOK:");
		}
	}
	

	private void runIndividualSimulation(int randomSeed)
	{
		// get baseline probability
		//Double baseline = commandlineparser.getDouble(Commands.BASELINE_PROB,0);
		int numSimCases = commandlineparser.getInt(Commands.SIMULATION,0);
		int numSimControls = commandlineparser.getInt(Commands.SIMULATION,1);

		simulatedCaseControls = GenerateSimulationData.generateSimulationData(genDB.allSamples,numSimCases, numSimControls, testVariants.traitVariantIndeces, testVariants.alleles, testVariants.alleleEffectSizes, testVariants.baselineProbaBility, randomSeed);
		if(DEBUG_PERFORMANCE) DebugUtil.instance.processNext("INDIVIDUAL SIM:");
	}
	
	
	private void outputSimData()
	{
		List<String> simulOutputFileLines = PScoreAssocOutput.produceOutput(simulatedCaseControls,genDB.allVariants);
		String outputFileLocation;
		Path outputFilePath;

		
		outputFileLocation = commandlineparser.getString(Commands.SIM_OUTPUT,0);
		outputFilePath = Paths.get(outputFileLocation);
		
		TextFileWriter.writeFileOutput(simulOutputFileLines, outputFilePath);
		
		List<Object> simOutputList = (ArrayList<Object>)commandlineparser.parameters.get(Commands.SIM_OUTPUT);
		
		if(simOutputList.size() > 1) // if the optional argument for the .phe output file was specified
		{
			outputFileLocation = commandlineparser.getString(Commands.SIM_OUTPUT,1);
			outputFilePath = Paths.get(outputFileLocation);
			
			simulOutputFileLines = PheIO.producePheOutput(simulatedCaseControls,0);
			TextFileWriter.writeFileOutput(simulOutputFileLines, outputFilePath);
		}	
	}
	
}




/*

private void initTestVariants_OLD()
{
	int i;
	Variant currentVariant;
	
	
	
	
	
	List<String> variantList = (List<String>) parameters.get(Commands.TEST_VARIANTS);
	String variantString;
	String[] variantArray;
	
	// finish processing command line
	for(i = 0; i < variantList.size(); i++)
	{
		variantString = variantList.get(i); // this gets rs2066845:C:1.861
		variantArray = variantString.split(":"); // [rs2066845,C,1.861]
		

		selectedVariant_RSIDs.add(variantArray[0]);
		selectedVariant_Alleles.add(variantArray[1]);
		selectedVariant_EffectSizes.add(HelperMethods.tryParseDouble(variantArray[2]));
	}
	
	foundVariants = new Variant[selectedVariant_RSIDs.size()]; // must be same size, so we can matching indices later
	
	//System.out.println("variantList: " + variantList.toString());
	//System.out.println("selectedVariant_RSIDs: " + selectedVariant_RSIDs.toString());
	//System.out.println("selectedVariant_Alleles: " + selectedVariant_Alleles.toString());
	//System.out.println("selectedVariant_EffectSizes: " + selectedVariant_EffectSizes.toString());
	
	// go through all variants and find variants that we need
	for(i = 0; i < genDB.allVariants.size(); i++)
	{
		currentVariant = genDB.allVariants.get(i);
		if(selectedVariant_RSIDs.indexOf(currentVariant.ID) !=-1) // if variant is found
		{
			foundVariants[selectedVariant_RSIDs.indexOf(currentVariant.ID)] = currentVariant; // need to add the variant at the exact same index, as all 3 lists work on the same indices
			
			//System.out.println("Found Variant: " + currentVariant.ID);
		}
	}	
	
	
}*/
