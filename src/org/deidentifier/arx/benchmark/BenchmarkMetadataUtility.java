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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataDefinition;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropyWithLowerBoundNormalized;

/**
 * Bounds on utility
 * 
 * @author Fabian Prasser
 */
public class BenchmarkMetadataUtility {

    /** Bound*/
    private Map<BenchmarkDataset, Map<BenchmarkUtilityMeasure, Double>> lower = new HashMap<BenchmarkDataset, Map<BenchmarkUtilityMeasure, Double>>();
    /** Bound*/
    private Map<BenchmarkDataset, Map<BenchmarkUtilityMeasure, Double>> upper = new HashMap<BenchmarkDataset, Map<BenchmarkUtilityMeasure, Double>>();
    
    /**
     * Creates a new instance
     * @throws IOException
     */
    public BenchmarkMetadataUtility() throws IOException{
        long time = System.currentTimeMillis();
        System.out.print("Preparing utility metadata...");
        for (BenchmarkDataset dataset : BenchmarkSetup.getDatasets()) {
            computeLowerBounds(dataset);
            computeUpperBounds(dataset);
        }
        System.out.println("Done in " + (System.currentTimeMillis() - time) + "[ms]");
    }
    

    /**
     * Returns the lower bound
     * @param dataset
     * @param measure
     * @return
     */
    public double getLowerBound(BenchmarkDataset dataset, BenchmarkUtilityMeasure measure) {
        return lower.get(dataset).get(measure);
    }

    /**
     * Returns the lower bound
     * @param dataset
     * @param measure
     * @return
     */
    public double getUpperBound(BenchmarkDataset dataset, BenchmarkUtilityMeasure measure) {
        return upper.get(dataset).get(measure);
    }
    
    /**
     * Computes the lower bounds
     * @param dataset
     * @throws IOException
     */
    private void computeLowerBounds(BenchmarkDataset dataset) throws IOException {
        // Prepare
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.K_ANONYMITY);
        DataDefinition definition = data.getDefinition();
        DataHandle inputHandle = data.getHandle();

        // Convert
        DataConverter converter = new DataConverter();
        String[][] input = converter.toArray(inputHandle);
        String[][] output = input;
        Map<String, String[][]> hierarchies = converter.toMap(definition);
        String[] header = converter.getHeader(inputHandle);
        int[] transformation = new int[definition.getQuasiIdentifyingAttributes().size()];
        Arrays.fill(transformation, 0);

        // Compute metrics
        double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output)
                                                                                                                 .getUtility();
        double outputEntropy = new UtilityMeasureNonUniformEntropyWithLowerBoundNormalized<Double>(header, input, hierarchies).evaluate(output, transformation).getUtility();

        // Store results
        if (!lower.containsKey(dataset)) {
            lower.put(dataset, new HashMap<BenchmarkUtilityMeasure, Double>());
        }
        lower.get(dataset).put(BenchmarkUtilityMeasure.LOSS, outputLoss);
        lower.get(dataset).put(BenchmarkUtilityMeasure.ENTROPY, outputEntropy);
    }

    /**
     * Computes the upper bounds
     * @param dataset
     * @throws IOException
     */
    private void computeUpperBounds(BenchmarkDataset dataset) throws IOException {
        // Prepare
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.K_ANONYMITY);
        DataDefinition definition = data.getDefinition();
        DataHandle inputHandle = data.getHandle();

        // Convert to completely suppressed output data
        DataConverter converter = new DataConverter();
        String[][] input = converter.toArray(inputHandle);
        String[][] output = new String[inputHandle.getNumRows()][inputHandle.getNumColumns()];
        for (int i = 0; i < inputHandle.getNumRows(); i++) {
            Arrays.fill(output[i], "*");
        }
        Map<String, String[][]> hierarchies = converter.toMap(definition);
        String[] header = converter.getHeader(inputHandle);
        int[] transformation = new int[definition.getQuasiIdentifyingAttributes().size()];
        for (String attr : definition.getQuasiIdentifyingAttributes()) {
            int maxLevel = definition.getHierarchy(attr)[0].length - 1;
            transformation[inputHandle.getColumnIndexOf(attr)] = maxLevel;
        }

        // Compute metrics
        double outputLoss = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
        double outputEntropy = new UtilityMeasureNonUniformEntropyWithLowerBoundNormalized<Double>(header, input, hierarchies).evaluate(output, transformation).getUtility();

        // Store results
        if (!upper.containsKey(dataset)) {
            upper.put(dataset, new HashMap<BenchmarkUtilityMeasure, Double>());
        }
        upper.get(dataset).put(BenchmarkUtilityMeasure.LOSS, outputLoss);
        upper.get(dataset).put(BenchmarkUtilityMeasure.ENTROPY, outputEntropy);
    }
}
