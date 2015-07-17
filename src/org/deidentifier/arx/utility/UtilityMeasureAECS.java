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

import org.deidentifier.arx.utility.util.HashGroupify;
import org.deidentifier.arx.utility.util.HashGroupifyEntry;
import org.deidentifier.arx.utility.util.StringArray;

/**
 * Implementation of the AECS measure, as proposed in:<br>
 * <br>
 * K. LeFevre, D. DeWitt, R. Ramakrishnan, Mondrian multidimensional k-anonymity, in: Proc Int Conf Data Engineering, 2006.
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureAECS extends UtilityMeasure<Double>{

    /**
     * Evaluates the utility measure
     * @param input
     * @param transformation
     * @return
     */
    public Utility<Double> evaluate(String[][] input, int[] transformation) {
        
        HashGroupify<StringArray> table = new HashGroupify<StringArray>(10);
        for (String[] row : input) {
            table.add(new StringArray(row));
        }
        
        HashGroupifyEntry<StringArray> e = table.first();
        double count = 1;
        double sum = e.getCount();
        while (e.hasNext()) {
            e = e.next();
            count++;
            sum += e.getCount();
        }
        return new UtilityDouble(sum / count);
    }
}
