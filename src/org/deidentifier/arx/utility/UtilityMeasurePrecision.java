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
 * Implementation of the Precision measure, as proposed in:<br>
 * <br>
 * L. Sweeney, Achieving k-anonymity privacy protection using generalization and suppression, J Uncertain Fuzz Knowl Sys 10 (5) (2002) 571–588.
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasurePrecision<T> extends UtilityMeasureAggregatable<T>{

    /** Precision */
    private final Map<String, Map<String, Double>> precision;
    /** Header */
    private final String[]                         header;

    /**
     * Creates a new instance
     * @param hierarchies
     */
    @SuppressWarnings("unchecked")
    public UtilityMeasurePrecision(String[] header, Map<String, String[][]> hierarchies) {
        this(header, hierarchies, (AggregateFunction<T>)AggregateFunction.ARITHMETIC_MEAN);
    }
        
    /**
     * Creates a new instance
     * @param hierarchies
     */
    public UtilityMeasurePrecision(String[] header, Map<String, String[][]> hierarchies, AggregateFunction<T> function) {
        super(function);
        this.header = header;
        this.precision = new HashMap<String, Map<String, Double>>();
        for (String attr : hierarchies.keySet()) {
            this.precision.put(attr, getPrecision(hierarchies.get(attr)));
        }
    }

    /**
     * Evaluates the utility measure
     * @param output
     * @param transformation
     * @return
     */
    protected double[] evaluateAggregatable(String[][] input, int[] transformation) {
        
        double[] result = new double[input[0].length];
        
        for (String[] row : input) {
            for (int i=0; i<result.length; i++) {
                result[i] += getPrecision(header[i], row[i]);
            }
        }
        
        for (int i=0; i<result.length; i++) {
            result[i] /= input.length;
        }
        return result;
    }

    /**
     * Returns the precision
     * @param attribute
     * @param value
     * @return
     */
    private double getPrecision(String attribute, String value) {
        Double precision = this.precision.get(attribute).get(value);
        return precision != null ? precision : 1d;
    }

    /**
     * Builds the precision map
     * @param hierarchy
     * @return
     */
    private Map<String, Double> getPrecision(String[][] hierarchy) {
        
        Map<String, Double> precision = new HashMap<String, Double>();
        for (int col = 0; col < hierarchy[0].length; col++) {
            for (int row = 0; row < hierarchy.length; row++) {
                String value = hierarchy[row][col];
                if (!precision.containsKey(value)) {
                    precision.put(value, (double)col / ((double)hierarchy[0].length - 1d));
                }
            }
        }
        return precision;
    }
}
