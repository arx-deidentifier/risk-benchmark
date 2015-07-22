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
import java.util.Map;

import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;
import org.deidentifier.arx.utility.AggregateFunction;
import org.deidentifier.arx.utility.DataConverter;
import org.deidentifier.arx.utility.UtilityMeasureLoss;
import org.deidentifier.arx.utility.UtilityMeasureNonUniformEntropyWithLowerBoundNormalized;

public class BenchmarkMetadata {
    
    private static BenchmarkMetadataUtility metadata = null;

    /**
     * Normalizes the utility measure
     * @param inputHandle
     * @param outputHandle
     * @param transformation
     * @param dataset
     * @param measure
     * @return
     */
    public static double getRelativeLoss(DataHandle inputHandle,
                                         DataHandle outputHandle,
                                         int[] transformation,
                                         BenchmarkDataset dataset,
                                         BenchmarkUtilityMeasure measure) {
        if (metadata == null) {
            try {
                metadata = new BenchmarkMetadataUtility();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        DataConverter converter = new DataConverter();
        String[][] input = converter.toArray(inputHandle);
        String[][] output = converter.toArray(outputHandle, outputHandle.getView());
        Map<String, String[][]> hierarchies = converter.toMap(inputHandle.getDefinition());
        String[] header = converter.getHeader(inputHandle);


        // Evaluate
        double result;
        switch (measure) {
        case ENTROPY: 
            result = new UtilityMeasureNonUniformEntropyWithLowerBoundNormalized<Double>(header, input, hierarchies, AggregateFunction.SUM).evaluate(output, transformation).getUtility();
            break;
        case LOSS: 
            result = new UtilityMeasureLoss<Double>(header, hierarchies, AggregateFunction.GEOMETRIC_MEAN).evaluate(output).getUtility();
            break;
        default:
            throw new IllegalArgumentException("");
        }

        // Normalize
        result = result - metadata.getLowerBound(dataset, measure);
        result /= (metadata.getUpperBound(dataset, measure) - metadata.getLowerBound(dataset, measure));
        return result;
    }

}
