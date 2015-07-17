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

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the Non-Uniform Entropy measure, as proposed in:<br>
 * <br>
 * A. Gionis, T. Tassa, k-Anonymization with minimal loss of information, Trans Knowl Data Engineering 21 (2) (2009) 206–219.
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureNonUniformEntropy<T> extends UtilityMeasureAggregatable<T>{

    /** Log */
    private static final double                    LOG2 = Math.log(2);
    /** Input frequencies */
    private final Map<String, Map<String, Double>> frequencyInput;
    /** Input */
    private final String[][]                       input;
    /** Header */
    private final String[]                         header;

    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    @SuppressWarnings("unchecked")
    public UtilityMeasureNonUniformEntropy(String[] header, String[][] input) {
        this(header, input, (AggregateFunction<T>)AggregateFunction.SUM);
    }
        
    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    public UtilityMeasureNonUniformEntropy(String[] header, String[][] input, AggregateFunction<T> function) {
        super(function);
        this.frequencyInput = getFrequency(header, input);
        this.input = input;
        this.header = header;
    }

    /**
     * Evaluates the utility measure
     * @param output
     * @param transformation
     * @return
     */
    protected double[] evaluateAggregatable(String[][] output, int[] transformation) {
        
        Map<String, Map<String, Double>> frequencyOutput = getFrequency(header, output);

        double[] result = new double[input[0].length];
        
        for (int row = 0; row < output.length; row++) {
            for (int col = 0; col < header.length; col++) {
                result[col] += log2(frequencyInput.get(header[col]).get(input[row][col]) / 
                                    frequencyOutput.get(header[col]).get(output[row][col]));
            }
        }

        for (int i=0; i<result.length; i++) {
            result[i] *= -1;
        }
        return result;
    }

    /**
     * Build frequencies
     * @param header
     * @param input
     * @return
     */
    private Map<String, Map<String, Double>> getFrequency(String[] header, String[][] input) {
        Map<String, Map<String, Double>> entropy = new HashMap<String, Map<String, Double>>();
        for (int i=0; i<header.length; i++) {
            entropy.put(header[i], getFrequency(input, i));
        }
        return entropy;
    }

    /**
     * Build frequencies
     * @param input
     * @param column
     * @return
     */
    private Map<String, Double> getFrequency(String[][] input, int column) {
        Map<String, Double> counts = new HashMap<String, Double>();
        for (int row = 0; row < input.length; row++) {
            String value = input[row][column];
            if (!counts.containsKey(value)) {
                counts.put(value, 1d);
            } else {
                counts.put(value, counts.get(value) + 1d);
            }
        }
        return counts;
    }

    /**
     * Log base-2
     * @param d
     * @return
     */
    private double log2(double d) {
        return Math.log(d) / LOG2;
    }
}
