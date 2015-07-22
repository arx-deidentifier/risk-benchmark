/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
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

package org.deidentifier.arx.utility;

import java.util.Map;

/**
 * Implementation of a normalized version of the Non-Uniform Entropy measure with a lower bound. Inspired by:<br>
 * <br>
 * A. Gionis, T. Tassa, k-Anonymization with minimal loss of information, Trans Knowl Data Engineering 21 (2) (2009) 206–219.
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureNonUniformEntropyWithLowerBoundNormalized<T> extends UtilityMeasureNonUniformEntropyWithLowerBound<T>{
    
    private final double[] upper;
    
    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    @SuppressWarnings("unchecked")
    public UtilityMeasureNonUniformEntropyWithLowerBoundNormalized(String[] header, String[][] input, Map<String, String[][]> hierarchies) {
        super(header, input, hierarchies, (AggregateFunction<T>)AggregateFunction.SUM);
        this.upper = getUpperBound();
    }
        
    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    public UtilityMeasureNonUniformEntropyWithLowerBoundNormalized(String[] header, String[][] input, Map<String, String[][]> hierarchies, AggregateFunction<T> function) {
        super(header, input, hierarchies, function);
        this.upper = getUpperBound();
    }

    /**
     * Evaluates the upper bound
     * @return
     */
    protected double[] getUpperBound() {
        
        double rows = input.length;
        double[] result = new double[input[0].length];
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < result.length; col++) {
                result[col] += log2(frequencyInput.get(header[col]).get(input[row][col]) / rows);
            }
        }
        
        for (int i=0; i<result.length; i++) {
            result[i] *= -1;
        }
        return result;
    }

    @Override
    protected double[] evaluateAggregatable(String[][] output, int[] transformation) {
        
        double[] result = super.evaluateAggregatable(output, transformation);
        for (int i=0; i<result.length; i++) {
            result[i] /= upper[i];
        }
        return result;
    }
}
