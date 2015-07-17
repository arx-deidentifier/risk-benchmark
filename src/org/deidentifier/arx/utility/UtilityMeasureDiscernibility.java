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
 * Implementation of the Discernibility measure, as proposed in:<br>
 * <br>
 * R. Bayardo, R. Agrawal, Data privacy through optimal k-anonymization, in: Proc Int Conf Data Engineering, 2005, pp. 217–228
 * 
 * @author Fabian Prasser
 */
public class UtilityMeasureDiscernibility extends UtilityMeasure<Double>{

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
        double sum = getPenalty(e, input.length);
        while (e.hasNext()) {
            e = e.next();
            sum += getPenalty(e, input.length);
        }
        return new UtilityDouble(sum);
    }

    /**
     * Returns the penalty for the given table
     * @param entry
     * @param rows
     * @return
     */
    private double getPenalty(HashGroupifyEntry<StringArray> entry, double rows) {

        if (isSuppressed(entry)) {
            return entry.getCount() * rows;
        } else {
            return entry.getCount() * entry.getCount();
        }
    }

    /**
     * We assume that an entry is suppressed, if all values are equal
     * @param entry
     * @return
     */
    private boolean isSuppressed(HashGroupifyEntry<StringArray> entry) {
        String[] array = entry.getElement().values;
        for (int i=1; i<array.length; i++) {
            if (!array[i-1].equals(array[i])) {
                return false;
            }
        }
        return true;
    }
}
