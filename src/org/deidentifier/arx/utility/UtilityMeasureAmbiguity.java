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
 * Implementation of the Ambiguity measure, as described in:<br>
 * <br>
 * "Goldberger, Tassa: Efficient Anonymizations with Enhanced Utility
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureAmbiguity extends UtilityMeasure<Double>{

    /** Map of losses */
    private final Map<String, Map<String, Double>> loss;
    /** Header */
    private final String[]                         header;
    /** Domain sizes */
    private final double[]                         domainSize;

    /**
     * Creates a new instance
     * @param hierarchies
     */
    public UtilityMeasureAmbiguity(String[] header, Map<String, String[][]> hierarchies) {
        this.loss = new HashMap<String, Map<String, Double>>();
        this.header = header;
        this.domainSize = getDomainSize(header, hierarchies);
        for (String attr : hierarchies.keySet()) {
            this.loss.put(attr, getLoss(hierarchies.get(attr)));
        }
    }

    @Override
    public Utility<Double> evaluate(String[][] input, int[] transformation) {

        double result = 0d;
        for (String[] row : input) {
            double resultRow = 1d;
            for (int i = 0; i < row.length; i++) {
                resultRow *= getLoss(header[i], row[i]) * domainSize[i];
            }
            result += resultRow;
        }
        return new UtilityDouble(result);
    }

    /**
     * Returns the domain sizes
     * @param header
     * @param hierarchies
     * @return
     */
    private double[] getDomainSize(String[] header, Map<String, String[][]> hierarchies) {
        double[] result = new double[header.length];
        for (int i = 0; i < header.length; i++) {
            result[i] = hierarchies.get(header[i]).length;
        }
        return result;
    }
    
    /**
     * Build loss
     * @param attribute
     * @param value
     * @return
     */
    private double getLoss(String attribute, String value) {
        Double loss = this.loss.get(attribute).get(value);
        return loss != null ? loss : 1d;
    }


    /**
     * Build loss
     * @param hierarchy
     * @return
     */
    private Map<String, Double> getLoss(String[][] hierarchy) {
        
        Map<String, Double> loss = new HashMap<String, Double>();
        
        // Prepare map:
        // Level -> Value on level + 1 -> Count of values on level that are generalized to this value
        Map<Integer, Map<String, Integer>> map = new HashMap<Integer, Map<String, Integer>>();
        for (int level = 0; level < hierarchy[0].length - 1; level++) {
            for (int row = 0; row < hierarchy.length; row++) {
                
                // Obtain map per level
                Map<String, Integer> levelMap = map.get(level);
                if (levelMap == null) {
                    levelMap = new HashMap<String, Integer>();
                    map.put(level, levelMap);
                }
                
                // Count
                String value = hierarchy[row][level + 1];
                Integer count = levelMap.get(value);
                count = count == null ? 1 : count + 1;
                levelMap.put(value, count);
            }
        }
        
        // Level 0
        for (int row = 0; row < hierarchy.length; row++) {
            String value = hierarchy[row][0];
            if (!loss.containsKey(value)) {
                loss.put(value, 1d / (double)hierarchy.length);
            }
        }
        
        // Level > 1
        for (int col = 1; col < hierarchy[0].length; col++) {
            for (int row = 0; row < hierarchy.length; row++) {
                String value = hierarchy[row][col];
                if (!loss.containsKey(value)) {
                    double count = map.get(col - 1).get(value);
                    loss.put(value, (double) count / (double) hierarchy.length);
                }
            }
        }
        
        return loss;
    }
}

