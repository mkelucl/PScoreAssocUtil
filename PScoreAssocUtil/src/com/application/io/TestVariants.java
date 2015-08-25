package com.application.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import com.application.data.gendat.GeneticDataBase;
import com.application.data.gendat.Sample;
import com.application.data.gendat.Variant;
import com.application.utils.HelperMethods;

public class TestVariants {

	private static String baselineProb = "baselineProb";
	
	
	public List<Variant> effectVariants = new ArrayList<Variant>();
	public List<Variant> effectAlleles_Inserted = new ArrayList<Variant>(); // a sublist of the above of variants that were NOT found in the original dataset, but had to be inserted
	
	public double baselineProbaBility = 0.0;
	
	public List<Integer> traitVariantIndeces = new ArrayList<Integer>();
	public List<Double> alleleEffectSizes = new ArrayList<Double>();
	public List<Byte> alleles = new ArrayList<Byte>();

	public TestVariants()
	{
		
	}
	
	public List<Variant> DEBUG_ALREADYFOUNDRARE = new ArrayList<Variant>();
	
	/**
	 * Finds the variant if it already existed in the GeneticDatabase object OR, if it didn't then it creates it from scratch, inserts it into the genDB as well as 
	 * it adds it onto the Samples, at the given MAF 
	 * 
	 * @param chrom
	 * @param pos
	 * @param id
	 * @param effectAllele
	 * @param logOR: the effect size in Log Odds ratio
	 * @param frequency: the MAF
	 * @param genDB
	 * @param randomGen
	 */
	private void findOrCreateVariant(String chrom, int pos, String id, String effectAllele, double logOR, double frequency, GeneticDataBase genDB, Random randomGen  )
	{
		int i;
		Variant variant;
		String variantLookup = chrom+":" + pos;
		if(genDB.variantsByChromPos.containsKey(variantLookup) ) // attempt to find Variant in DB
		{
			variant = genDB.variantsByChromPos.get(variantLookup);
			
			if(id.equalsIgnoreCase("RARE")) { System.out.println("Rare variant: " + variantLookup + " was already in genDB, its MAF was " +variant.alleleFrequences_DoublePrecision[1]+  " vs TEST FREQ: "+ frequency); DEBUG_ALREADYFOUNDRARE.add(variant);  }
		}
		
		else // if Variant was not in the DB, we have to create it and add it to genDB
		{ System.out.println("we cannot find Variant for RSID: " + id + " / loci: " + variantLookup); 
			
			String rareRefAllele = "REF";
			int numAlts = 0;
			int numRefs = 0;
			
			// add it to all samples
			int j;
			Sample sample;
			List<Byte> chromosome;
			byte allele;
			Double currentProbability;
			for( i = 0; i< genDB.allSamples.size(); i++)
			{
				sample = genDB.allSamples.get(i);
				
				
				// Add the variants with given MAF onto samples
				for(j = 0; j < sample.get_ploidy(); j++)
				{
					chromosome = sample.chromosomes.get(j);
					
					// get a random value between 0-100%: eg 50%
					currentProbability = randomGen.nextDouble();

				   // if this random chance is less than probability then allele should be an ALT
				   if( currentProbability <= frequency ) { allele = 1; numAlts++;}
				   else { allele = 0;  numRefs++;}// otherwise a REF
				   
				   chromosome.add(allele);
				}
			}
			
			
			
			// use the frequency as the MAF
			int[] allele_counts =  { numRefs, numAlts};
			String[] variantAlleles = {rareRefAllele, effectAllele};
			
			                                        
			variant = new Variant("rare", chrom, pos, pos, id, variantAlleles, rareRefAllele,allele_counts , genDB.allVariants.size());
			
			// add it to genDB's lookups
			genDB.allVariants.add(variant);
			genDB.variantsByChromPos.put(variantLookup, variant);
			effectAlleles_Inserted.add(variant);
			
			

			
		}
		
		
		// add them to lookups
		effectVariants.add(variant);
		traitVariantIndeces.add(variant.lociIndex);
		alleleEffectSizes.add(logOR);
		alleles.add(variant.get_alleleIndex( effectAllele ));

	}
	
	
	/**
	 * @param filepath: path to the PSCORE assoc test variant input file
	 * @param genDB: the Genetic Database of the base dataset ( this may be modified!)
	 * @param randomSeed: seed for the random generator
	 * @return a TestVariants object that contains all the results
	 */
	public static TestVariants parseTestVariants(String filepath, GeneticDataBase genDB, int randomSeed  )
	{
		Random randomGen = new Random(randomSeed);
		
		List<String> allConsequenceTypes = null;
		LinkedHashMap<String, Variant> variantsByChromPos = genDB.variantsByChromPos;
		// 1. Open the file
		try 
		{
			Scanner s = new Scanner (new BufferedReader( new FileReader(filepath)));
			String nextItem;
			allConsequenceTypes = new ArrayList<String>(); // only init this if we could open...
			String[] processedLine;
			String chrom;
			String RSID;
			String consequence;
			Variant variant;
			int counter = 0;
			
			int fileNumLines = BasicFileUtils.getNumLines(filepath);
			
			
			TestVariants testVariants = new TestVariants();
			while(s.hasNext() ) // if there is anything else left...
			{
				nextItem = s.nextLine();//  s.next(); // next should refer to correct datatype
				processedLine = nextItem.split("\t");
				
				// this looks like this:
				// #chrom	pos	ID	EffectAllele	LOGOR	frequency
				// 16	50745199	rs2066843	T	0.584	-1

				if(nextItem.startsWith("#")) // if its a header line
				{
					// the first headerline should have the overall baseline probability for the test trait
					if(nextItem.contains(baselineProb)) testVariants.baselineProbaBility = HelperMethods.tryParseDouble(processedLine[1]);
				}
				else 	testVariants.findOrCreateVariant(processedLine[0], HelperMethods.tryParseInt(processedLine[1]), processedLine[2] , processedLine[3] ,HelperMethods.tryParseDouble(processedLine[4]), HelperMethods.tryParseDouble(processedLine[5]), genDB, randomGen);


				
				counter++;
			}
			
			return testVariants;
			
		} catch (FileNotFoundException e) { System.out.println(filepath); e.printStackTrace(); }
		
		return null;
	}
	
	
	
}
