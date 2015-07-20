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
import java.util.Arrays;
import java.util.Map;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropy;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropyWithLowerBound;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment4 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK        = new Benchmark(new String[] { "Dataset", "Generalization", "Suppression" });

    /** Label for ENTROPY */
    public static final int        ENTROPY          = BENCHMARK.addMeasure("Entropy");

    /** Label for ENTROPYWITHBOUND */
    public static final int        ENTROPYWITHBOUND = BENCHMARK.addMeasure("EntropyWithBound");

    /**
     * Datasets
     * @return
     */
    public static final BenchmarkDataset[] getDatasets() {
        return new BenchmarkDataset[]{
          BenchmarkDataset.ADULT,
          BenchmarkDataset.CUP,
          BenchmarkDataset.FARS,
          BenchmarkDataset.ATUS,
          BenchmarkDataset.IHIS
        };
    }
    
    /**
     * Generalization degrees
     * @return
     */
    public static final double[] getGeneralizations() {
        return new double[]{0d, 0.25d, 0.5d, 0.75d, 1d};
    }

    /**
     * Main
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(ENTROPY, new ValueBuffer());
        BENCHMARK.addAnalyzer(ENTROPYWITHBOUND, new ValueBuffer());
        
        for (BenchmarkDataset dataset : getDatasets()) {
            
            // Perform
            System.out.println("Running: " + dataset);
            performBenchmark(dataset);

            // Write after each experiment
            BENCHMARK.getResults().write(new File("results/experiment4.csv"));
        }
    }

    /**
     * Returns the dataset for the given transformation level
     * @param result
     * @param generalization
     * @return
     */
    private static DataHandle getOutput(ARXResult result, double generalization) {
        
        int[] transformation = getTransformation(result, generalization);
        for (ARXNode[] level : result.getLattice().getLevels()) {
            for (ARXNode node : level) {
                if (Arrays.equals(transformation, node.getTransformation()))  {
                    return result.getOutput(node);
                }
            }
        }
        return null;
    }

    /**
     * Returns the transformation for the given transformation level
     * @param result
     * @param generalization
     * @return
     */
    private static int[] getTransformation(ARXResult result, double generalization) {

        int[] transformation = result.getGlobalOptimum().getTransformation().clone();
        for (int i=0; i<transformation.length; i++) {
            transformation[i] = (int)Math.round(result.getLattice().getTop().getTransformation()[i] * generalization);
        }
        return transformation;
    }

    /**
     * Performs one run
     * @param dataset
     * @throws IOException
     */
    private static void performBenchmark(BenchmarkDataset dataset) throws IOException {
        
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, BenchmarkUtilityMeasure.ENTROPY, BenchmarkPrivacyModel.K_ANONYMITY, 0.01d);
        config.removeCriterion(config.getCriterion(KAnonymity.class));
        config.addCriterion(new KAnonymity(1));
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.K_ANONYMITY);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);

        // Foreach generalization level
        for (double generalization : getGeneralizations()) {
            
            System.out.println(" - Generalization: " + generalization);
            
            // Prepare
            DataHandle outputHandle = getOutput(result, generalization);
            int[] transformation = getTransformation(result, generalization);
            DataConverter converter = new DataConverter();
            String[][] input = converter.toArray(data.getHandle());
            String[][] output =  converter.toArray(outputHandle);
            Map<String, String[][]> hierarchies = converter.toMap(data.getDefinition());
            String[] header = data.getHandle().iterator().next().clone();
            
            // Foreach suppression level
            int stepping = (int)(data.getHandle().getNumRows() / 10d);
            for (int i=0; i<11; i++) {
            
                // Prepare
                 String[][] suppressed = new String[output.length][];
                 for (int j=0; j<output.length; j++) {
                     suppressed[j] = output[j].clone();
                 }
                 for (int j=0; j<i*stepping; j++) {
                     if (j<suppressed.length) {
                         Arrays.fill(suppressed[j], "*");
                     }
                 }

                 // Instantiate
                 UtilityMeasureNonUniformEntropy<Double> metricEntropy = new UtilityMeasureNonUniformEntropy<Double>(header, input);
                 UtilityMeasureNonUniformEntropyWithLowerBound<Double> metricEntropyWithBound = new UtilityMeasureNonUniformEntropyWithLowerBound<Double>(header, input, hierarchies);
                 
                 // Evaluate
                 double entropy = metricEntropy.evaluate(suppressed).getUtility();
                 double entropyWithBound = metricEntropyWithBound.evaluate(suppressed, transformation).getUtility();

                 // Write
                 BENCHMARK.addRun(dataset.toString(), String.valueOf(generalization), String.valueOf(i / 10d));
                 BENCHMARK.addValue(ENTROPY, entropy);
                 BENCHMARK.addValue(ENTROPYWITHBOUND, entropyWithBound);
            }
        }
    }
}
