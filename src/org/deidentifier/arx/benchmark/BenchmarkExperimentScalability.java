package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.ARXSolverConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.AverageReidentificationRisk;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.io.CSVHierarchyInput;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness.PopulationUniquenessModel;

import de.linearbits.subframe.Benchmark;
import de.linearbits.subframe.analyzer.ValueBuffer;

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



/**
 * Test for data transformations.
 *
 * @author Fabian Prasser
 * @author Florian Kohlmayer
 */
public abstract class BenchmarkExperimentScalability {


    /** The benchmark instance */
    private static final Benchmark BENCHMARK   = new Benchmark(new String[] { "Dataset", "Rows", "Columns"});

    /** TOTAL */
    public static final int         TIME_UNIQUENESS     = BENCHMARK.addMeasure("time-(0.01)-uniqueness");
    /** TOTAL */
    public static final int         TIME_STRICT_AVERAGE = BENCHMARK.addMeasure("time-(3,5)-strict-average-risk");
    /** TOTAL */
    public static final int         TIME_ANONYMITY      = BENCHMARK.addMeasure("time-(5)-anonymity");

    /** TOTAL */
    public static final int         UTILITY_UNIQUENESS     = BENCHMARK.addMeasure("utility-(0.01)-uniqueness");
    /** TOTAL */
    public static final int         UTILITY_STRICT_AVERAGE = BENCHMARK.addMeasure("utility-(3,5)-strict-average-risk");
    /** TOTAL */
    public static final int         UTILITY_ANONYMITY      = BENCHMARK.addMeasure("utility-(5)-anonymity");

    /** VALUE */
    private static final double[][] SOLVER_START_VALUES    = getSolverStartValues();
    /** VALUE */
    private static final double     POPULATION_USA         = 318.9 * Math.pow(10d, 6d);
    /** VALUE */
    private static final int        REPETITIONS            = 5;
    /** START_INDEX */
    private static int              START_INDEX            = 0;

    public static void main(String[] args) throws IOException {
        
        // Parse commandline
        if (args != null && args.length != 0) {
            
            int index = -1;
            try {
                index = Integer.parseInt(args[0]);
            } catch (Exception e) {
                index = -1;
            }
            if (index != -1) {
                START_INDEX = index;
            } else {
                START_INDEX = 0;
            }
        }

        // Init
        BENCHMARK.addAnalyzer(TIME_UNIQUENESS, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_STRICT_AVERAGE, new ValueBuffer());
        BENCHMARK.addAnalyzer(TIME_ANONYMITY, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY_UNIQUENESS, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY_STRICT_AVERAGE, new ValueBuffer());
        BENCHMARK.addAnalyzer(UTILITY_ANONYMITY, new ValueBuffer());

        // Perform
        String[] datasets = new String[] { "adult", "cup", "fars", "atus", "ihis" };
        for (int i = START_INDEX; i < datasets.length; i++) {
            analyze(datasets[i]);
        }
    }

    private static void analyze(String dataset) throws IOException {

        Data data = getDataObject(dataset);
        int allColumns = data.getHandle().getNumColumns();
        int allRows = data.getHandle().getNumRows();

        System.out.println("Running: " + dataset + " - column scaling benchmark");
        
        // Foreach set of columns
        for (int columns = 3; columns <= allColumns; columns++) {
            
            System.out.println(" - Columns: " + columns + "/" + allColumns);

            // Run & Store
            BENCHMARK.addRun(dataset, allRows, columns);
            analyze(dataset, allRows, columns);     
            BENCHMARK.getResults().write(new File("results/scalability.csv"));
        }
        
        System.out.println("Running: " + dataset + " - row scaling benchmark");

        // Foreach set of rows
        int offset = allRows / 10;
        for (int index = 1; index <= 10; index++) {

            System.out.println(" - Step: " + index + "/" + 10);
            
            // Compute rows
            int rows = index * offset;
            if (index == 10) rows = allRows;

            // Run & Store
            BENCHMARK.addRun(dataset, rows, allColumns);
            analyze(dataset, rows, allColumns);     
            BENCHMARK.getResults().write(new File("results/scalability.csv"));
        }

    }
    
    private static void analyze(String dataset, int rows, int columns) throws IOException {
        
        Data data = getDataObject(dataset, rows, columns);
        
        // Uniqueness
        ARXConfiguration config = ARXConfiguration.create();
        config.setMetric(Metric.createPrecomputedLossMetric(1.0d, 0.5d, AggregateFunction.GEOMETRIC_MEAN));
        config.setMaxOutliers(1d);
        config.addCriterion(new PopulationUniqueness(0.01d,
                                                     PopulationUniquenessModel.PITMAN,
                                                     new ARXPopulationModel(data.getHandle(), POPULATION_USA), 
                                                     ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES)
                                                     .iterationsPerTry(15)));
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();

        // Warmup
        ARXResult result = anonymizer.anonymize(data, config);
        double utility = 1d - Double.valueOf(result.getGlobalOptimum().getMaximumInformationLoss().toString());
        data.getHandle().release();

        long time = System.currentTimeMillis();
        for (int i=0; i<REPETITIONS; i++) {
            anonymizer.anonymize(data, config);
            data.getHandle().release();
        }
        double timeUniqueness = (double)(System.currentTimeMillis() - time) / (double)REPETITIONS;
        double utilityUniqueness = utility;
        
        // Strict average
        config = ARXConfiguration.create();
        config.setMetric(Metric.createPrecomputedLossMetric(1.0d, 0.5d, AggregateFunction.GEOMETRIC_MEAN));
        config.setMaxOutliers(1d);
        config.addCriterion(new KAnonymity(3));
        config.addCriterion(new AverageReidentificationRisk(0.2d));

        // Warmup
        result = anonymizer.anonymize(data, config);
        utility = 1d - Double.valueOf(result.getGlobalOptimum().getMaximumInformationLoss().toString());
        data.getHandle().release();

        time = System.currentTimeMillis();
        for (int i=0; i<REPETITIONS; i++) {
            anonymizer.anonymize(data, config);
            data.getHandle().release();
        }
        double timeStrictAverage = (double)(System.currentTimeMillis() - time) / (double)REPETITIONS;
        double utilityStrictAverage = utility;
        
        // K-anonymity
        config = ARXConfiguration.create();
        config.setMetric(Metric.createPrecomputedLossMetric(1.0d, 0.5d, AggregateFunction.GEOMETRIC_MEAN));
        config.setMaxOutliers(1d);
        config.addCriterion(new KAnonymity(5));

        // Warmup
        result = anonymizer.anonymize(data, config);
        utility = 1d - Double.valueOf(result.getGlobalOptimum().getMaximumInformationLoss().toString());
        data.getHandle().release();

        time = System.currentTimeMillis();
        for (int i=0; i<REPETITIONS; i++) {
            anonymizer.anonymize(data, config);
            data.getHandle().release();
        }
        double timeAnonymity = (double)(System.currentTimeMillis() - time) / (double)REPETITIONS;
        double utilityAnonymity = utility;
        
        BENCHMARK.addValue(TIME_UNIQUENESS, timeUniqueness);
        BENCHMARK.addValue(TIME_STRICT_AVERAGE, timeStrictAverage);
        BENCHMARK.addValue(TIME_ANONYMITY, timeAnonymity);
        BENCHMARK.addValue(UTILITY_UNIQUENESS, utilityUniqueness);
        BENCHMARK.addValue(UTILITY_STRICT_AVERAGE, utilityStrictAverage);
        BENCHMARK.addValue(UTILITY_ANONYMITY, utilityAnonymity);
    }

    /**
     * Returns the data object for the test case.
     *
     * @param dataset
     * @return
     * @throws IOException
     */
    private static Data getDataObject(final String dataset) throws IOException {
        
        // Load dataset
        final Data data = Data.create("./data/"+dataset+".csv", ';');
        
        // Load hierarchies
        prepareDataObject(dataset, data, Integer.MAX_VALUE);
        return data;
    }

    /**
     * Returns the data object for the test case.
     *
     * @param dataset
     * @param rows
     * @param columns
     * @return
     * @throws IOException
     */
    private static Data getDataObject(final String dataset, int rows, int columns) throws IOException {
        
        // Load dataset
        Data data = Data.create("./data/"+dataset+".csv", ';');
        
        // Select rows
        Iterator<String[]> iter = data.getHandle().iterator();
        List<String[]> selection = new ArrayList<String[]>();
        
        // Add header
        selection.add(iter.next());
        
        // Add payload
        for (int i=0; i<rows; i++) {
            String[] row = iter.next();
            selection.add(row);
        }
        
        // Create data object
        data = Data.create(selection);
        
        // Load hierarchies and project
        prepareDataObject(dataset, data, columns);
        return data;
    }

    /**
     * Creates start values for the solver
     * @return
     */
    private static double[][] getSolverStartValues() {
        double[][] result = new double[100][];
        int index = 0;
        for (double d1 = 10d; d1 <=100d; d1 += 10d) {
            for (double d2 = 1000000d; d2 <= 10000000d; d2 += 1000000d) {
                result[index++] = new double[] { d1, d2 };
            }
        }
        return result;
    }

    /**
     * Loads hierarchies
     * @param dataset
     * @param data
     * @param columns
     * @return
     * @throws IOException
     */
    private static void prepareDataObject(final String dataset, final Data data, int columns) throws IOException {
        
        // Read generalization hierachies
        final FilenameFilter hierarchyFilter = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                if (name.matches(dataset+"_hierarchy_(.)+.csv")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        
        final File testDir = new File("./hierarchies");
        final File[] genHierFiles = testDir.listFiles(hierarchyFilter);
        final Pattern pattern = Pattern.compile("_hierarchy_(.*?).csv");
        
        for (final File file : genHierFiles) {
            final Matcher matcher = pattern.matcher(file.getName());
            if (matcher.find()) {
                final CSVHierarchyInput hier = new CSVHierarchyInput(file, ';');
                final String attributeName = matcher.group(1);
                if (data.getHandle().getColumnIndexOf(attributeName) < columns) {
                    data.getDefinition().setAttributeType(attributeName, Hierarchy.create(hier.getHierarchy()));
                }
            }
        }
    }
}
