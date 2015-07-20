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

import java.io.File;
import java.io.IOException;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.metric.InformationLoss;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment3 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK      = new Benchmark(new String[] { "Dataset", "Privacy model", "Uniqueness"});

    /** Utility */
    public static final int        UTILITY        = BENCHMARK.addMeasure("Utility");

    /** Suppressed */
    public static final int        SUPPRESSED     = BENCHMARK.addMeasure("Suppressed");

    /** Transformation */
    public static final int        TRANSFORMATION = BENCHMARK.addMeasure("Transformation");

    /**
     * Returns all criteria relevant for this benchmark
     * @return
     */
    public static BenchmarkPrivacyModel[] getPrivacyModels() {
        return new BenchmarkPrivacyModel[] {
                BenchmarkPrivacyModel.UNIQUENESS_DANKAR,
                BenchmarkPrivacyModel.UNIQUENESS_PITMAN,
                BenchmarkPrivacyModel.UNIQUENESS_ZAYATZ,
                BenchmarkPrivacyModel.UNIQUENESS_SNB,
        };
    }
    
    /**
     * Returns all uniqueness parameters
     * @return
     */
    public static double[] getUniqueness() {
        return new double[]{    
                                0.001d,
                                0.002d,
                                0.003d,
                                0.004d,
                                0.005d,
                                0.006d,
                                0.007d,
                                0.008d,
                                0.009d,
                                0.01d
                                };
    }

    /**
     * Returns all datasets
     * @return
     */
    public static BenchmarkDataset[] getDatasets() {
        return new BenchmarkDataset[] {
                BenchmarkDataset.ADULT,
                BenchmarkDataset.CUP,
                BenchmarkDataset.FARS,
                BenchmarkDataset.ATUS,
                BenchmarkDataset.IHIS
        };
    }
    
    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(TRANSFORMATION, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : getDatasets()) {
            for (BenchmarkPrivacyModel privacy : getPrivacyModels()) {
                for (double uniqueness : getUniqueness()) {
                    
                    System.out.println(data + "/" + privacy + "/" + uniqueness);
                    
                    // New run
                    BENCHMARK.addRun(data.toString(), privacy.toString(), String.valueOf(uniqueness));
                    
                    anonymize(data, privacy, uniqueness);
                    
                    // Write after each experiment
                    BENCHMARK.getResults().write(new File("results/experiment3.csv"));
                }
            }
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset, BenchmarkPrivacyModel criterion, double uniqueness) throws IOException {
        Data data = BenchmarkSetup.getData(dataset, criterion);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, criterion, uniqueness);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        BENCHMARK.addValue(UTILITY, getRelativeLoss(result.getGlobalOptimum().getMinimumInformationLoss()));
        BENCHMARK.addValue(SUPPRESSED, ((double) getSuppressed(result.getOutput()) / (double) data.getHandle().getNumRows()) * 100d);
        BENCHMARK.addValue(TRANSFORMATION, getTransformation(result));
    }
    
    /**
     * Formats the result
     * @param result
     * @return
     */
    private static String getTransformation(ARXResult result) {
        
        String string = "(";
        int[] optimum = result.getGlobalOptimum().getTransformation();
        int[] top = result.getLattice().getTop().getTransformation();
        
        for (int i=0; i<optimum.length; i++) {
            int val = (int)Math.round((double)optimum[i] / (double)top[i] * 100d);
            string += val + "%";
            if (i<optimum.length-1) {
                string += ", ";
            }
        }
        string += ")";
        return string;
    }

    /**
     * Normalizes the loss utility measure
     * @param loss
     * @return
     */
    private static double getRelativeLoss(InformationLoss<?> loss) {
        return Double.valueOf(loss.toString()) * 100d;
    }
    
    /**
     * Returns the number of suppressed tuples
     * 
     * @param output
     * @return
     */
    private static int getSuppressed(DataHandle output) {
        int count = 0;
        for (int i = 0; i < output.getNumRows(); i++) {
            if (output.isOutlier(i)) {
                count++;
            }
        }
        return count;
    }
}
