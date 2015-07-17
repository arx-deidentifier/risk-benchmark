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

package org.deidentifier.arx.utility.util;

import java.util.Arrays;

/**
 * For hash tables
 * 
 * @author Fabian Prasser
 */
public class StringArray {

    /** Hash code */
    public final int      hashcode;
    /** Indices */
    public final String[] values;

    /**
     * Constructor
     * 
     * @param handle
     * @param row
     */
    public StringArray(String[] values) {
        this.values = values;
        this.hashcode = Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object other) {
        return Arrays.equals(((StringArray) other).values, this.values);
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}