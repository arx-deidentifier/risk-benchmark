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


/**
 * Implementation of multi-dimensional utility measures
 * 
 * @author Fabian Prasser
 */
public abstract class UtilityMeasureAggregatable<T> extends UtilityMeasure<T>{

    private final AggregateFunction<T> function;
    
    public UtilityMeasureAggregatable(AggregateFunction<T> function) {
        this.function = function;
    }

    @Override
    public Utility<T> evaluate(String[][] input, int[] transformation) {
        return function.aggregate(evaluateAggregatable(input, transformation));
    }

    protected abstract double[] evaluateAggregatable(String[][] input, int[] transformation);
}
