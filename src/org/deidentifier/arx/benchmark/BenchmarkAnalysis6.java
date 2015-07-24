/*
 * Source code of the experiments for the entropy metric
 *      
 * Copyright (C) 2015 Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;

import de.linearbits.objectselector.Selector;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.io.CSVLine;

/**
 * Example benchmark
 * @author Fabian Prasser
 */
public class BenchmarkAnalysis6 {
    
    private static class BenchmarkResult {
        public double numChecksWith;
        public double timeTotalWith;
        public double numChecksWithout;
        public double timeTotalWithout;
        public double solutionSpaceSize;
    }

	/**
	 * Main
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
    public static void main(String[] args) throws IOException, ParseException {
        
        CSVFile file = new CSVFile(new File("results/experiment6.csv"));
        analyze(file);
        
    }
    
    /**
     * Performs the analysis
     * @param file
     * @return
     * @throws ParseException
     * @throws IOException 
     */
    private static void analyze(CSVFile file) throws ParseException, IOException{

        // Selects all rows
        Selector<String[]> selectorWith = file.getSelectorBuilder().field("LowerBound").equals("true").build();
        Selector<String[]> selectorWithout = file.getSelectorBuilder().field("LowerBound").equals("false").build();
        
        Map<String, BenchmarkResult> results = new HashMap<String, BenchmarkResult>();

        for (Iterator<CSVLine> iter = file.iterator(); iter.hasNext();) {
            
            CSVLine line = iter.next();

            String dataset = line.get("", "Dataset");
            if (!results.containsKey(dataset)) {
                results.put(dataset, new BenchmarkResult());
            }
            
            if (selectorWith.isSelected(line.getData())) {
                results.get(dataset).numChecksWith =  Double.valueOf(line.get("Total", "Value")) / Double.valueOf(line.get("Check", "Value"));
                results.get(dataset).timeTotalWith = Double.valueOf(line.get("Total", "Value"));
                results.get(dataset).solutionSpaceSize = getSolutionSpaceSize(getDataset(dataset));
                
            } else if (selectorWithout.isSelected(line.getData())) {
                
                results.get(dataset).numChecksWithout = Double.valueOf(line.get("Total", "Value")) / Double.valueOf(line.get("Check", "Value"));
                results.get(dataset).timeTotalWithout = Double.valueOf(line.get("Total", "Value"));
                
            } else {
                throw new RuntimeException("Illegal state");
            }
        }
        
        for (String dataset : results.keySet()) {
            BenchmarkResult result = results.get(dataset);
            System.out.println("Dataset: " + dataset);
            System.out.println(" - Total w/o optimization  : " + result.timeTotalWithout);
            System.out.println(" - Total with optimization : " + result.timeTotalWith + " (" + getRelative(result.timeTotalWith, result.timeTotalWithout)+"%)");
            System.out.println(" - Checks w/o optimization : " + (int)result.numChecksWithout);
            System.out.println(" - Checks with optimization: " + (int)result.numChecksWith + " (" + getRelative(result.numChecksWith, result.numChecksWithout)+"%)");
            System.out.println(" - Solution space size     : " + result.solutionSpaceSize);
        }
    }
    
    /**
     * Computes a relative value
     * @param first
     * @param second
     * @return
     */
    private static double getRelative(double first, double second) {
        return first / second * 100d;
    }

    /**
     * Returns the dataset
     * @param dataset
     * @return
     */
    private static BenchmarkDataset getDataset(String dataset) {
        if (dataset.equals(BenchmarkDataset.ADULT.toString())) {
            return BenchmarkDataset.ADULT;
        }
        if (dataset.equals(BenchmarkDataset.CUP.toString())) {
            return BenchmarkDataset.CUP;
        }
        if (dataset.equals(BenchmarkDataset.FARS.toString())) {
            return BenchmarkDataset.FARS;
        }
        if (dataset.equals(BenchmarkDataset.ATUS.toString())) {
            return BenchmarkDataset.ATUS;
        }
        if (dataset.equals(BenchmarkDataset.IHIS.toString())) {
            return BenchmarkDataset.IHIS;
        }
        throw new RuntimeException("Illegal state");
    }

    /**
     * Returns the size of the solution space
     * @return
     * @throws IOException 
     */
    private static int getSolutionSpaceSize(BenchmarkDataset dataset) throws IOException {
        int size = 1;
        DataDefinition definition = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.K_ANONYMITY).getDefinition();
        for (String qi : definition.getQuasiIdentifiersWithGeneralization()) {
            size *= definition.getHierarchy(qi)[0].length;
        }
        return size;
    }
}
