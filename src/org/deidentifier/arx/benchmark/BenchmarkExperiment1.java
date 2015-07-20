/*
 * Benchmark of risk-based anonymization in ARX 3.0.0
 * Copyright 2015 - Fabian Prasser
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

package org.deidentifier.arx.benchmark;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.metric.InformationLoss;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment1 {
    
    /** Repetitions */
    private static final int REPETITIONS = 1;

    /**
     * Returns all criteria relevant for this benchmark
     * @return
     */
    public static BenchmarkPrivacyModel[] getCriteria() {
        return new BenchmarkPrivacyModel[] {
                BenchmarkPrivacyModel.K_ANONYMITY,
                BenchmarkPrivacyModel.UNIQUENESS_DANKAR,
        };
    }
    
    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (int i = 0; i < REPETITIONS; i++) {
                     anonymize(data, criterion);
                }
            }
        }
    }
    
    /**
     * Returns whether all entries are equal
     * 
     * @param tuple
     * @return
     */
    private static boolean allEqual(String[] tuple) {
        String value = tuple[0];
        for (int i = 1; i < tuple.length; i++) {
            if (!tuple[i].equals(value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset, BenchmarkPrivacyModel criterion) throws IOException {
        Data data = BenchmarkSetup.getData(dataset, criterion);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, criterion, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        long time = System.currentTimeMillis();
        ARXResult result = anonymizer.anonymize(data, config);
        time = System.currentTimeMillis() - time;
        int searchSpaceSize = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            searchSpaceSize *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        Iterator<String[]> iter = result.getOutput().iterator();
        System.out.println(dataset);
        System.out.println(" - Criterion     : " + criterion.name());
        System.out.println(" - Time          : " + time + " [ms]");
        System.out.println(" - QIs           : " + data.getDefinition().getQuasiIdentifyingAttributes().size());
        System.out.println(" - Search space  : " + searchSpaceSize);
        int checkedTransformations = getCheckedTransformations(result);
        System.out.println(" - Checked       : " + checkedTransformations + " (pruned: " + (100 - (((double) checkedTransformations / (double) searchSpaceSize) * 100d)) + "%)");
        System.out.println(" - Header        : " + Arrays.toString(iter.next()));
        System.out.println(" - Tuple         : " + Arrays.toString(getTuple(iter)));
        int suppressed = getSuppressed(result.getOutput());
        System.out.println(" - Suppressed    : " + suppressed + " (" + ((double) suppressed / (double) data.getHandle().getNumRows()) * 100d + "%)");
        System.out.println(" - Transformation: " + Arrays.toString(result.getGlobalOptimum().getTransformation()));
        System.out.println(" - Heights       : " + Arrays.toString(result.getLattice().getTop().getTransformation()));
        System.out.println(" - Total         : " + data.getHandle().getNumRows());
        System.out.println(" - Infoloss      : " + result.getGlobalOptimum().getMinimumInformationLoss().toString());
        System.out.println(" - Relative      : " + getRelativeLoss(result.getGlobalOptimum().getMinimumInformationLoss()));
    }
    

    /**
     * Normalizes the loss utility measure
     * @param loss
     * @return
     */
    private static double getRelativeLoss(InformationLoss<?> loss) {
        return Double.valueOf(loss.toString()) * 100d;
    }
    
    /**
     * Returns the number of checked transformations
     * @param result
     * @return
     */
    private static int getCheckedTransformations(ARXResult result) {
        int count = 0;
        for (ARXNode[] level : result.getLattice().getLevels()) {
            for (ARXNode node : level) {
                if (node.isChecked()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    /**
     * Returns the number of suppressed tuples
     * 
     * @param output
     * @return
     */
    private static int getSuppressed(DataHandle output) {
        int count = 0;
        for (int i = 0; i < output.getNumRows(); i++) {
            if (output.isOutlier(i)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Returns the first tuple that is not suppressed
     * 
     * @param iter
     * @return
     */
    private static String[] getTuple(Iterator<String[]> iter) {
        String[] tuple = iter.next();
        while (allEqual(tuple)) {
            tuple = iter.next();
        }
        return tuple;
    }
}
