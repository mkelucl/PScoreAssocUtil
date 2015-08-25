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


import java.awt.EventQueue;

import com.PScoreAssocUtilApp;

public class PScoreAssocUtil {

	
	private PScoreAssocUtilApp _application;
	
	public static void main(String[] args) {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PScoreAssocUtil window = new PScoreAssocUtil( args);
					//window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}

	
	/**
	 * Create the application.
	 */
	public PScoreAssocUtil(String[] args) {
		//System.out.println("main");
		_application = new PScoreAssocUtilApp();
		
		if(PScoreAssocUtilApp.DEBUG)
		{ // NOD2 coordinates: "50731049", "50766987"
			
			// HOME PC
			String[] debugArguments = {"*inputvcf", "g:/!!!0_EclipseWorkspace/ALL.chr16.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz", "*simulate", "100", "100", "*region", "16", "50731049", "50766987", "*variants", "BarchRare/PSCORE_VAR.csv", "*seed", "1", "*simoutput", "myOutput.dat", "myOutput.phe",  "*vepinput", "BarchRare/VEP_NODRARE_COMBINED.csv", "*weightsoutput", "nod2.annot", "nod2weights.txt", "*numsims", "1", "*pscorreassoc", "pscoreassoc.exe", "*pvaloutput", "logPValues.txt","*routput", "*weightmap" , "weigthmap.csv", "*weightfactor", "10" }; 
			

			// LAPTOP
			//String[] debugArguments = {"*inputvcf", "c:/!!!0_EcliposeWorkspace/ALL.chr16.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz", "*simulate", "300", "300", "*region", "16", "50731049", "50766987", "*variants", "BarchRare/PSCORE_VAR.csv", "*seed", "1", "*simoutput", "myOutput.dat", "pheOutput.phe",  "*vepinput", "BarchRare/VEP_NODRARE_COMBINED.csv", "*weightsoutput", "nod2.annot", "nod2weights.txt", "*numsims", "1", "*pscorreassoc", "pscoreassoc.exe", "*pvaloutput", "logPValues.txt","*routput", "*weightmap" , "weigthmap.csv", "*weightfactor", "10" };
		

			_application.init(debugArguments);
		}
		
		else _application.init(args);	
	}

}
