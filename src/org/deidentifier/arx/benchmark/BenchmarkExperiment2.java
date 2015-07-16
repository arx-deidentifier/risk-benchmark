/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
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

import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.metric.InformationLoss;
import org.deidentifier.arx.metric.v2.ILMultiDimensionalRank;
import org.deidentifier.arx.risk.ModelPitman;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment2 {
    
    /** Repetitions */
    private static final int REPETITIONS = 5;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
            anonymize(data);
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset) throws IOException {
        
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.UNIQUENESS_PITMAN);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, BenchmarkPrivacyModel.UNIQUENESS_PITMAN, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        
        // Warmup
        System.out.println("Dataset: " + dataset);
        System.out.println(" - Warmup-1");
        ModelPitman.hookUsePolygamma(true);
        ARXResult result = anonymizer.anonymize(data, config);
        System.out.println(" - Warmup-2");
        ModelPitman.hookUsePolygamma(false);
        data.getHandle().release();
        result = anonymizer.anonymize(data, config);
        
        // Benchmark
        ModelPitman.hookUsePolygamma(true);
        long timePolygamma = System.currentTimeMillis();
        for (int i = 0; i < REPETITIONS; i++) {
            System.out.println(" - Run-1 " + i + " of " + REPETITIONS);
            data.getHandle().release();
            result = anonymizer.anonymize(data, config);
        }
        timePolygamma = System.currentTimeMillis() - timePolygamma;
        long checksPolygamma = getNumChecks(result);
        double utilityPolygamma = getRelativeLoss(result.getGlobalOptimum().getMaximumInformationLoss());

        // Benchmark
        ModelPitman.hookUsePolygamma(false);
        long timeWithoutPolygamma = System.currentTimeMillis();
        for (int i = 0; i < REPETITIONS; i++) {
            System.out.println(" - Run-2 " + i + " of " + REPETITIONS);
            data.getHandle().release();
            result = anonymizer.anonymize(data, config);
        }
        timeWithoutPolygamma = System.currentTimeMillis() - timeWithoutPolygamma;
        long checksWithoutPolygamma = getNumChecks(result);
        double utilityWithoutPolygamma = getRelativeLoss(result.getGlobalOptimum().getMaximumInformationLoss());
        
        System.out.println("Dataset: " + dataset);
        System.out.println(" - Time with polygamma total: " + timePolygamma + " [ms]");
        System.out.println(" - Time with polygamma per check: " + (int)((double)timePolygamma / (double)checksPolygamma) + " [ms]");
        System.out.println(" - Num checks with polygamma: " + checksPolygamma);
        System.out.println(" - Utility with polygamma: " + utilityPolygamma + " [%]");
        
        System.out.println(" - Time without polygamma total: " + timeWithoutPolygamma + " [ms]");
        System.out.println(" - Time without polygamma per check: " + (int)((double)timeWithoutPolygamma / (double)checksWithoutPolygamma)+ " [ms]");
        System.out.println(" - Num checks without polygamma: " + checksWithoutPolygamma);
        System.out.println(" - Utility without polygamma: " + utilityWithoutPolygamma + " [%]");
    }
    
    /**
     * Returns the number of checks
     * @param result
     * @return
     */
    private static int getNumChecks(ARXResult result) {
        int checks = 0;
        ARXLattice lattice = result.getLattice();
        for (ARXNode[] level : lattice.getLevels()) {
            for (ARXNode node : level) {
                checks += node.isChecked() ? 1 : 0;
            }
        }
        return checks;
    }

    /**
     * Normalizes the loss utility measure
     * @param loss
     * @return
     */
    private static double getRelativeLoss(InformationLoss<?> loss) {
        double[] values = ((ILMultiDimensionalRank) loss).getValue();
        double result = 1.0d;
        for (int i = 0; i < values.length; i++) {
            result *= Math.pow(values[i] + 1d, 1.0d / (double) values.length);
        }
        result -= 1d;
        result *= 100d;
        return result;
    }
}
