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

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXLattice;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.ARXLattice.ARXNode;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkPrivacyModel;
import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;
import org.deidentifier.arx.metric.InformationLoss;
import org.deidentifier.arx.metric.v2.ILMultiDimensionalRank;
import org.deidentifier.arx.risk.ModelPitman;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

/**
 * Main benchmark class.
 * 
 * @author Fabian Prasser
 */
public class BenchmarkExperiment2 {

    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "Polygamma" });

    /** TOTAL */
    public static final int        TOTAL       = BENCHMARK.addMeasure("Total");

    /** CHECK */
    public static final int        CHECK       = BENCHMARK.addMeasure("Check");

    /** UTILITY */
    public static final int        UTILITY     = BENCHMARK.addMeasure("Utility");

    /** Repetitions */
    private static final int       REPETITIONS = 5;

    /**
     * Main entry point
     * 
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        // Init
        BENCHMARK.addAnalyzer(UTILITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(TOTAL, new ValueBuffer());
        BENCHMARK.addAnalyzer(CHECK, new ValueBuffer());
        
        // Repeat for each data set
        for (BenchmarkDataset data : BenchmarkSetup.getDatasets()) {

            // New run
            BENCHMARK.addRun(data.toString(), "true");
            anonymize(data, true);

            // New run
            BENCHMARK.addRun(data.toString(), "false");
            anonymize(data, false);

            // Write after each experiment
            BENCHMARK.getResults().write(new File("results/experiment2.csv"));
        }
    }
    
    /**
     * Performs the experiments
     * 
     * @param dataset
     * @throws IOException
     */
    private static void anonymize(BenchmarkDataset dataset, boolean usePolygamma) throws IOException {
        
        Data data = BenchmarkSetup.getData(dataset, BenchmarkPrivacyModel.UNIQUENESS_PITMAN);
        ARXConfiguration config = BenchmarkSetup.getConfiguration(dataset, BenchmarkPrivacyModel.UNIQUENESS_PITMAN, 0.01d);
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        
        // Warmup
        ModelPitman.hookUsePolygamma(usePolygamma);
        ARXResult result = anonymizer.anonymize(data, config);
        data.getHandle().release();
        
        // Benchmark
        ModelPitman.hookUsePolygamma(usePolygamma);
        long time = System.currentTimeMillis();
        for (int i = 0; i < REPETITIONS; i++) {
            System.out.println(" - Run-1 " + (i + 1) + " of " + REPETITIONS);
            data.getHandle().release();
            result = anonymizer.anonymize(data, config);
        }
        time = System.currentTimeMillis() - time;
        BENCHMARK.addValue(UTILITY, getRelativeLoss(result.getGlobalOptimum().getMaximumInformationLoss()));
        BENCHMARK.addValue(TOTAL, (int) time);
        BENCHMARK.addValue(CHECK, (int) ((double) time / (double) getNumChecks(result)));
    }
    
    /**
     * Returns the number of checks
     * @param result
     * @return
     */
    private static int getNumChecks(ARXResult result) {
        int checks = 0;
        ARXLattice lattice = result.getLattice();
        for (ARXNode[] level : lattice.getLevels()) {
            for (ARXNode node : level) {
                checks += node.isChecked() ? 1 : 0;
            }
        }
        return checks;
    }

    /**
     * Normalizes the loss utility measure
     * @param loss
     * @return
     */
    private static double getRelativeLoss(InformationLoss<?> loss) {
        double[] values = ((ILMultiDimensionalRank) loss).getValue();
        double result = 1.0d;
        for (int i = 0; i < values.length; i++) {
            result *= Math.pow(values[i] + 1d, 1.0d / (double) values.length);
        }
        result -= 1d;
        result *= 100d;
        return result;
    }
}
