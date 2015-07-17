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
 * Double value as a result of a utility measure
 * 
 * @author Fabian Prasser
 */
public class UtilityDouble extends Utility<Double> {

    /**
     * Constructor
     * @param utility
     */
    public UtilityDouble(double utility) {
        super(utility);
    }

    @Override
    public String toString() {
        return getUtility().toString();
    }

    @Override
    public int compareTo(Utility<Double> o) {
        return this.getUtility().compareTo(o.getUtility());
    }
}
