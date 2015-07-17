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

import java.util.Arrays;

/**
 * Aggregate function for multi-dimensional utility measures
 * 
 * @author Fabian Prasser
 *
 * @param <T>
 */
public abstract class AggregateFunction<T> {
    
    public static final AggregateFunction<Double> ARITHMETIC_MEAN = new AggregateFunction<Double>(){
        @Override protected Utility<Double> aggregate(double[] values) {
            double result = 0d;
            for (int i = 0; i< values.length; i++) {
                result += values[i];
            }
            result /= values.length;
            return new UtilityDouble(result);
        }
    };
    public static final AggregateFunction<Double> GEOMETRIC_MEAN = new AggregateFunction<Double>(){
        @Override protected Utility<Double> aggregate(double[] values) {
            double result = 1d;
            for (int i = 0; i< values.length; i++) {
                result *= values[i] + 1d;
            }
            result = Math.pow(result, 1d / values.length) - 1d;
            return new UtilityDouble(result);
        }
    };
    public static final AggregateFunction<Double> SUM = new AggregateFunction<Double>(){
        @Override protected Utility<Double> aggregate(double[] values) {
            double result = 0d;
            for (int i = 0; i< values.length; i++) {
                result += values[i];
            }
            return new UtilityDouble(result);
        }
    };
    public static final AggregateFunction<Double> MAX = new AggregateFunction<Double>(){
        @Override protected Utility<Double> aggregate(double[] values) {
            double result = - Double.MAX_VALUE;
            for (int i = 0; i< values.length; i++) {
                result = Math.max(result, values[i]);
            }
            return new UtilityDouble(result);
        }
    };
    public static final AggregateFunction<double[]> RANK = new AggregateFunction<double[]>(){
        @Override protected Utility<double[]> aggregate(double[] values) {
            Arrays.sort(values);
            reverse(values);
            return new UtilityDoubleArray(values);
        }

        private void reverse(double[] utility) {
            double[] other = new double[utility.length];
            for (int i = 0; i < utility.length; i++) {
                other[i] = utility[utility.length - 1 - i];
            }
            for (int i = 0; i < utility.length; i++) {
                utility[i] = other[i];
            }
        }
    };

    protected abstract Utility<T> aggregate(double[] values);
}
