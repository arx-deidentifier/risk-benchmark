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

import java.io.File;
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
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkUtilityMeasure;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment1 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "Utility", "Privacy" });

    /** TIME */
    public static final int TIME = BENCHMARK.addMeasure("Time");

    /** QIS */
    public static final int QIS = BENCHMARK.addMeasure("Qis");

    /** SEARCH_SPACE */
    public static final int SEARCH_SPACE = BENCHMARK.addMeasure("Search space");

    /** CHECKED */
    public static final int CHECKED = BENCHMARK.addMeasure("Checked");

    /** HEADER */
    public static final int HEADER = BENCHMARK.addMeasure("Header");

    /** TUPLE */
    public static final int TUPLE = BENCHMARK.addMeasure("Tuple");

    /** SUPPRESSED */
    public static final int SUPPRESSED = BENCHMARK.addMeasure("Suppressed");

    /** TRANSFORMATION */
    public static final int TRANSFORMATION = BENCHMARK.addMeasure("Transformation");

    /** HEIGHTS */
    public static final int HEIGHTS = BENCHMARK.addMeasure("Heights");

    /** TOTAL */
    public static final int TOTAL = BENCHMARK.addMeasure("Total");

    /** UTILITY */
    public static final int UTILITY = BENCHMARK.addMeasure("Utility");

    /** RELATIVE_UTILITY */
    public static final int RELATIVE_UTILITY = BENCHMARK.addMeasure("Relative utility");

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

        // Init
        BENCHMARK.addAnalyzer(TIME, new ValueBuffer());
        BENCHMARK.addAnalyzer(QIS, new ValueBuffer());
        BENCHMARK.addAnalyzer(SEARCH_SPACE, new ValueBuffer());
        BENCHMARK.addAnalyzer(CHECKED, new ValueBuffer());
        BENCHMARK.addAnalyzer(HEADER, new ValueBuffer());
        BENCHMARK.addAnalyzer(TUPLE, new ValueBuffer());
        BENCHMARK.addAnalyzer(SUPPRESSED, new ValueBuffer());
        BENCHMARK.addAnalyzer(TRANSFORMATION, new ValueBuffer());
        BENCHMARK.addAnalyzer(HEIGHTS, new ValueBuffer());
        BENCHMARK.addAnalyzer(TOTAL, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(RELATIVE_UTILITY, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {
            for (BenchmarkPrivacyModel criterion : getCriteria()) {
                for (BenchmarkUtilityMeasure measure : BenchmarkSetup.getUtilityMeasures()) {
                    BENCHMARK.addRun(data.toString(), measure.toString(), criterion.toString());
                    anonymize(data, measure, criterion);
                    BENCHMARK.getResults().write(new File("results/experiment1.csv"));
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
     * @param measure 
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset, BenchmarkUtilityMeasure measure, BenchmarkPrivacyModel criterion) throws IOException {
        Data data = BenchmarkSetup.getData(dataset, criterion);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, measure, criterion, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        long time = System.currentTimeMillis();
        ARXResult result = anonymizer.anonymize(data, config);
        time = System.currentTimeMillis() - time;
        int searchSpaceSize = 1;
        for (String qi : data.getDefinition().getQuasiIdentifyingAttributes()) {
            searchSpaceSize *= data.getDefinition().getHierarchy(qi)[0].length;
        }
        Iterator<String[]> iter = result.getOutput().iterator();
        BENCHMARK.addValue(TIME, time);
        BENCHMARK.addValue(QIS, data.getDefinition().getQuasiIdentifyingAttributes().size());
        BENCHMARK.addValue(SEARCH_SPACE, searchSpaceSize);
        BENCHMARK.addValue(CHECKED, (double) getCheckedTransformations(result) / (double) searchSpaceSize * 100d);
        BENCHMARK.addValue(HEADER, Arrays.toString(iter.next()));
        BENCHMARK.addValue(TUPLE, Arrays.toString(getTuple(iter)));
        BENCHMARK.addValue(SUPPRESSED, (double) getSuppressed(result.getOutput()) / (double) data.getHandle().getNumRows() * 100d);
        BENCHMARK.addValue(TRANSFORMATION, Arrays.toString(result.getGlobalOptimum().getTransformation()));
        BENCHMARK.addValue(HEIGHTS, Arrays.toString(result.getLattice().getTop().getTransformation()));
        BENCHMARK.addValue(TOTAL, data.getHandle().getNumRows());
        BENCHMARK.addValue(UTILITY, result.getGlobalOptimum().getMinimumInformationLoss().toString());
        BENCHMARK.addValue(RELATIVE_UTILITY, BenchmarkMetadata.getRelativeLoss(data.getHandle(),
                                                                               result.getOutput(),
                                                                               result.getGlobalOptimum().getTransformation(),
                                                                               dataset,
                                                                               measure));
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
