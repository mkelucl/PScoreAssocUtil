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


package com.application.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.application.data.*;
import com.application.data.gendat.*;


/**
 * @author Marton
 *
 */
public class GenerateSimulationData {
	
	
	private static Random randomGenerator;
	
	
	// list of Variant indices that are potentially rare variants
	// list of Frequencies for these to be occurring
	
	// will need to insert the rare variants into the genDB BEFORE, IE all rate variants would ba added onto the END
	// of the allVariants.. This is needed as 
	// IE if after parsing, we got instruction for rare variants, then add +53 variants to the end of the list
	// ( will need this as these will store if the allele is a A or a T when we output the genotype data)
	// ( also add it to the 'variantsByChrom pos ?? )
	// but Do NOT add them to the samples, as this is how we will know where 'rare variants start' 

	
	
	/**
	 * Takes in a list of original samples, 
	 * splits their chromosomes apart, pools them
	 * then randomly picks 2 to generate simulated individuals
	 * these are then phenotyped based on a specified list of Variants that have an established effect size
	 * @return: a 2D array of freshly generated VCF samples where [0] = cases, [1] = controls
	 */
	public static List<List<Sample>> generateSimulationData(List<Sample> origSamples, int requiredSimulatedCases, int requiredSimulatedControls, List<Integer> traitVariantIndeces, List<Byte> alleles, List<Double> alleleEffectSizes, Double baseLine, int randomSeed)
	{ System.out.println("generateSimulationData with cases: " + requiredSimulatedCases + " / controls: " + requiredSimulatedControls + " / on variants: " + traitVariantIndeces.toString() +  " / alleles: " + alleles.toString() + " / baseline Probability: " + baseLine + " /with random seed: " + randomSeed);
		int ploidy = origSamples.get(0).get_ploidy();
		int i =0;
		int j =0;

		randomGenerator = new Random(randomSeed);
		
		List<List<Sample>> caseControl_simulatedResults = new ArrayList<List<Sample>>();// Sample[2][numSimulatedResults]; // case control is an array with a length of 2
		caseControl_simulatedResults.add(new ArrayList<Sample>()); caseControl_simulatedResults.add(new ArrayList<Sample>()); // add the 2 slots for case/control

		// I. Pool all chromosomes into a single 1D array ( at this point these can be just copied via reference )
		List<List<Byte>> allOrigChromosomes = new ArrayList<List<Byte>>();
		for(i =0; i < origSamples.size(); i++)
		{
			for(j =0; j < origSamples.get(i).chromosomes.size(); j++) allOrigChromosomes.add(origSamples.get(i).chromosomes.get(j));
		}
		

		int totalGenotypesSimulated = 0;
		int totalCasesSimulatedSimulated = 0;
		int totalControlSimulatedSimulated = 0;
		
		// II. populate the Case/Control arrays
		int simulatedCases = 0;
		int simulatedControls = 0;
		Boolean isCase = false;
		Boolean foundUnique = false;
		List<Byte> chromosome;
		List<List<Byte>> targetChromosomes; // = new byte[origSamples[0].chromosomes.length][]; // the target chromosomes will be 
		
		while(simulatedCases < requiredSimulatedCases || simulatedControls< requiredSimulatedControls) // keep doing simulations until both case/controls have enough results
		{
			// 1. pick 2 chromosomes randomly out of the pool. These mustn't be the same for 1 individual (want to avoid homs )
			targetChromosomes = new ArrayList<List<Byte>>(); // need to recreate these as these will be passed into the Sample constructor as reference
			for(j =0; j < ploidy; j++) 
			{
				foundUnique = false;
				while(!foundUnique) // while the chromosome we've picked isnt unique
				{
					chromosome = allOrigChromosomes.get(randomGenerator.nextInt(allOrigChromosomes.size())); // keep picking a random chromosome from the pool
					if(targetChromosomes.indexOf(chromosome) == -1) // if its not already picked 
						// (Note: in JAVA, arrays with primitive types appear to be tested if they are identical on a per element basis, rather than on the Array instance on a reference basis
						// but that is even better, as we just want to make sure that the exact same haplotype does not get paired to itself twice
						// (maybe it is just runtime caching... but in any case the above is OK)
					{
						targetChromosomes.add(chromosome);
						foundUnique = true;
					}
				}
			}
			
			
			// 3. infer Phenotype of prospective simulated sample, to decide if its a case or control: 
			isCase = inferPhenotype(targetChromosomes, traitVariantIndeces,  alleles, alleleEffectSizes, baseLine);
			
			if(isCase) totalCasesSimulatedSimulated++; else  totalControlSimulatedSimulated++;
			// if result is a simulated case, and we havent found all the needed cases yet
			if(isCase && simulatedCases < requiredSimulatedCases)             caseControl_simulatedResults.get(0).add(new Sample("CASE"+simulatedCases++   ,targetChromosomes,new ArrayList<Double>() {{ add(1.0);}} ));
			else if(!isCase && simulatedControls < requiredSimulatedControls) caseControl_simulatedResults.get(1).add(new Sample("CONTROL"+simulatedControls++,targetChromosomes,new ArrayList<Double>() {{ add(0.0);}}));
			
			// if neither of the above conditions are met then the simulated sample is discarded (IE when we already have all the controls, and we got another control)
			totalGenotypesSimulated++;
		}

		System.out.println("Simulated genotypes: cases: " + totalCasesSimulatedSimulated +" / controls: " + totalControlSimulatedSimulated  + " / total: " + totalGenotypesSimulated);
		return caseControl_simulatedResults;
	}
	
	
	/**
	 * return: true if the phenotype should be a case
	 * targetChromosomes: refers to the (2) passed in haplotypes
	 * traitVariantIndices: = the list of indices that refer to Variants /loci in the 2 chromosome arrays, that we need to check to determine phenotype
	 * alleles: the alleles that result in an effect size, indices match to the relevantVariantIndices (ie these will be like [1,1,0]  (referring to [A,A,T])
	 * alleleEffectSizes: given as Log Odds Ratios, indices match to the relevantIndices
	 * Baseline: is in PROBABILITY!, needs to be converted to log Odds
	 */
	   public static Boolean inferPhenotype(List<List<Byte>> targetChromosomes, List<Integer> traitVariantIndices, List<Byte> allele, List<Double>  alleleEffectSizes, Double baseLine)
       {
		   // get the total Genetic Effect size for each loci, based on both chromosomes
		   
		   // with no allele, the effect size will be the pop baseline
		   Double baselineLogOdds = Math.log( baseLine / (1-baseLine) ); // the baseline probability is given as PROBABILITY, must be converted to Log Odds
		   Double allLogEffectSize = baselineLogOdds;
		   int j;
		   for(int i =0; i < traitVariantIndices.size(); i++) // go through each variant
		   {
			   // get the variant on individuals chromosome, check  if its  the allele that is associated with the increased effect size
			   // then add the matching effect size to the cumulative total
			   for (j = 0; j < targetChromosomes.size(); j++)
			   {
				   if(targetChromosomes.get(j).get(traitVariantIndices.get(i)) == allele.get(i)) 
					   {
					   	allLogEffectSize += alleleEffectSizes.get(i);
					   }
			   }
		   }
		   
		   // Antilog it, as multiplicative genetic effect can only be summed on log scale
		   Double totalAntiLogEffect = Math.exp(allLogEffectSize);
		    
		   
		   // total Probability is converted back from the total Odds: eg: 70%, IE he has 70% of being a case with his given genotype
		   Double totalCaseProbability = totalAntiLogEffect / (1 + totalAntiLogEffect);
		   
		   // get a random value between 0-100%: eg 50%
		   Double currentProbability = randomGenerator.nextDouble();
		   
		   // if this random chance is less than the total Probability then Sample should be a case ( IE up to 0-70%, individuals should be a case )
		   if(currentProbability <= totalCaseProbability)  return true;
		   else return false;
       }
	   
}
