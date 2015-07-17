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

import cern.colt.Arrays;

/**
 * Implementation of the Non-Uniform Entropy measure with a lower bound. Inspired by:<br>
 * <br>
 * A. Gionis, T. Tassa, k-Anonymization with minimal loss of information, Trans Knowl Data Engineering 21 (2) (2009) 206–219.
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureNonUniformEntropyWithLowerBound<T> extends UtilityMeasureAggregatable<T>{

    /** Log */
    private static final double                                  LOG2 = Math.log(2);
    /** Input frequencies */
    private final Map<String, Map<String, Double>>               frequencyInput;
    /** Input */
    private final String[][]                                     input;
    /** Header */
    private final String[]                                       header;
    /** Hierarchies */
    private final Map<String, Map<Integer, Map<String, String>>> hierarchies;

    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    @SuppressWarnings("unchecked")
    public UtilityMeasureNonUniformEntropyWithLowerBound(String[] header, String[][] input, Map<String, String[][]> hierarchies) {
        this(header, input, hierarchies, (AggregateFunction<T>)AggregateFunction.SUM);
    }
        
    /**
     * Creates a new instance
     * @param header
     * @param input
     */
    public UtilityMeasureNonUniformEntropyWithLowerBound(String[] header, String[][] input, Map<String, String[][]> hierarchies, AggregateFunction<T> function) {
        super(function);
        this.frequencyInput = getFrequency(header, input);
        this.input = input;
        this.header = header;
        
        // Build hierarchies
        Map<String, Map<Integer, Map<String, String>>> map = new HashMap<String, Map<Integer, Map<String, String>>>();
        for (String attribute : header) {
            Map<Integer, Map<String, String>> amap = new HashMap<Integer, Map<String, String>>();
            map.put(attribute, amap);
            String[][] hierarchy = hierarchies.get(attribute);
            for (int level=0; level<hierarchy[0].length; level++) {
                Map<String, String> lmap = new HashMap<String, String>();
                amap.put(level, lmap);
                for (int value=0; value<hierarchy.length; value++) {
                    lmap.put(hierarchy[value][0], hierarchy[value][level]);
                }
            }
        }
        this.hierarchies = map;
    }


    /**
     * Evaluates the utility measure
     * @param output
     * @param transformation
     * @return
     */
    protected double[] evaluateAggregatable(String[][] output, int[] transformation) {
        
        Map<String, Map<String, Double>> frequencyOutput = getFrequency(header, output);
        Map<String, Map<String, Double>> frequencyTransformed = getFrequency(header, transformation);
        Map<String, Map<String, Double>> frequencyTransformedSuppressed = getFrequency(header, transformation, output);

        double[] result = new double[input[0].length];
        
        for (int row = 0; row < output.length; row++) {
            for (int col = 0; col < header.length; col++) {
                Map<String, String> hierarchy = hierarchies.get(header[col]).get(transformation[col]);
                String generalized = hierarchy.get(input[row][col]);
                result[col] += log2(frequencyInput.get(header[col]).get(input[row][col]) / 
                                    frequencyTransformed.get(header[col]).get(generalized));
                
                // Suppressed
                if (!generalized.equals(output[row][col])) {
                    
                    if (!output[row][col].equals("*")) {

                        System.out.println("Transformation: " + Arrays.toString(transformation));
                        System.out.println("Input: " + Arrays.toString(input[row]));
                        System.out.println("Output: " + Arrays.toString(output[row]));
                        System.out.println("Problem: " + generalized +"!=" + output[row][col] );
                        throw new IllegalStateException("Values are not matching, but output is not suppressed");
                    }
                    
                    result[col] += log2(frequencyTransformedSuppressed.get(header[col]).get(generalized) / 
                                         frequencyOutput.get(header[col]).get(output[row][col]));
                }
            }
        }
        
        for (int i=0; i<result.length; i++) {
            result[i] *= -1;
        }
        return result;
    }

    /**
     * Returns frequency distributions for the input dataset generalized to the given transformation
     * @param header
     * @param transformation
     * @return
     */
    private Map<String, Map<String, Double>> getFrequency(String[] header, int[] transformation) {
        Map<String, Map<String, Double>> entropy = new HashMap<String, Map<String, Double>>();
        for (int i=0; i<header.length; i++) {
            entropy.put(header[i], getFrequency(transformation, i));
        }
        return entropy;
    }

    /**
     * Returns frequency distributions for the input dataset generalized to the given transformation, only
     * including those tuples which are different from the actual output (indicates suppressed tuples)
     * @param header
     * @param transformation
     * @param output
     * @return
     */
    private Map<String, Map<String, Double>> getFrequency(String[] header, int[] transformation, String[][] output) {
        Map<String, Map<String, Double>> entropy = new HashMap<String, Map<String, Double>>();
        for (int i=0; i<header.length; i++) {
            entropy.put(header[i], getFrequency(transformation, i, output));
        }
        return entropy;
    }

    /**
     * Returns frequency distributions for the given dataset
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
     * Returns frequency distributions for the input dataset generalized to the given transformation
     * @param transformation
     * @param column
     * @return
     */
    private Map<String, Double> getFrequency(int[] transformation, int column) {
        
        Map<String, Double> counts = new HashMap<String, Double>();
        for (int row = 0; row < input.length; row++) {
            String value = hierarchies.get(header[column]).get(transformation[column]).get(input[row][column]);
            if (!counts.containsKey(value)) {
                counts.put(value, 1d);
            } else {
                counts.put(value, counts.get(value) + 1d);
            }
        }
        return counts;
    }

    /**
     * Returns frequency distributions for the input dataset generalized to the given transformation, only
     * including those tuples which are different from the actual output (indicates suppressed tuples)
     * @param transformation
     * @param column
     * @param output
     * @return
     */
    private Map<String, Double> getFrequency(int[] transformation, int column, String[][] output) {
        
        Map<String, Double> counts = new HashMap<String, Double>();
        for (int row = 0; row < input.length; row++) {
            String value = hierarchies.get(header[column]).get(transformation[column]).get(input[row][column]);
            if (!value.equals(output[row][column])) {
                if (!counts.containsKey(value)) {
                    counts.put(value, 1d);
                } else {
                    counts.put(value, counts.get(value) + 1d);
                }
            }
        }
        return counts;
    }

    /**
     * Returns frequency distributions for the given dataset
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
