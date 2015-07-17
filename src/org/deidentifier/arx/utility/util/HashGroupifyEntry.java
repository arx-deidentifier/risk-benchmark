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

/**
 * Entry
 * 
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 * 
 * @param <U>
 */
public class HashGroupifyEntry<U> {

    /** Var */
    private final int hashcode;
    /** Var */
    private final U   element;
    /** Var */
    private int       count       = 0;
    /** Var */
    private HashGroupifyEntry<U>  next        = null;
    /** Var */
    private HashGroupifyEntry<U>  nextInOrder = null;

    /**
     * Creates a new instance
     * 
     * @param element
     * @param hashCode
     */
    public HashGroupifyEntry(U element, int hashCode) {
        this.hashcode = hashCode;
        this.element = element;
    }

    /**
     * The count
     */
    public int getCount() {
        return count;
    }

    /**
     * The element
     */
    public U getElement() {
        return element;
    }

    /**
     * Getter
     * @return
     */
    public int getHashcode() {
        return hashcode;
    }

    /**
     * Getter
     * @return
     */
    public HashGroupifyEntry<U> getNext() {
        return next;
    }

    /**
     * Getter
     * @return
     */
    public HashGroupifyEntry<U> getNextInOrder() {
        return nextInOrder;
    }

    /**
     * Returns whether a next entry exists
     * 
     * @return
     */
    public boolean hasNext() {
        return nextInOrder != null;
    }

    /**
     * Inc counter
     */
    public void inc() {
        count++;
    }

    /**
     * Returns the next entry, null if this is the last entry
     * 
     * @return
     */
    public HashGroupifyEntry<U> next() {
        return nextInOrder;
    }

    /**
     * Setter
     * @param next
     */
    void setNext(HashGroupifyEntry<U> next) {
        this.next = next;
    }

    /**
     * Setter
     * @param next
     */
    void setNextInOrder(HashGroupifyEntry<U> next) {
        this.nextInOrder = next;
    }
}
