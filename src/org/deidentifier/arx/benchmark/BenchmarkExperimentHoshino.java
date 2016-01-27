package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.ARXSolverConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.io.CSVHierarchyInput;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness.PopulationUniquenessModel;

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
public abstract class BenchmarkExperimentHoshino {

    private static final double[][] SOLVER_START_VALUES = getSolverStartValues();

    private static final double POPULATION_TEXAS = 26.96 * Math.pow(10d, 6d);
    
    private static final double POPULATION_HOUSTON = 2.196 * Math.pow(10d, 6d);
    
    private static final double POPULATION_USA = 318.9 * Math.pow(10d, 6d);
    
    
    /**
     * Returns the data object for the test case.
     *
     * @param dataset
     * @return
     * @throws IOException
     */
    public static Data getDataObject(final String dataset) throws IOException {
        
        final Data data = Data.create("./data/"+dataset+".csv", ';');
        
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
                data.getDefinition().setAttributeType(attributeName, Hierarchy.create(hier.getHierarchy()));
            }
        }
        
        return data;
    }

    public static void main(String[] args) throws IOException {
    
        for (String dataset : new String[]{"ihis"}) { // "adult", "cup", "fars", "atus", "ihis" 

            for (double population : new double[]{POPULATION_USA, POPULATION_TEXAS, POPULATION_HOUSTON}) {
                System.out.println("DATA FOR POPULATION: " + population);
                analyze(dataset, population);
            }
        }
    }

    private static void analyze(String dataset, double population) throws IOException {

        Data data = getDataObject(dataset);

        System.out.println(dataset);

        double[] levels = new double[] { 0.0000001d, 0.000001d, 0.00001d, 0.0001d, 0.001d };

        System.out.println("risk;utility");

        for (double riskLevel : levels) {
            for (double risk : getRisksForLevel(riskLevel)) {
                analyze(data, risk, population);
            }
        }

        analyze(data, 0.01d, population);

    }

    private static void analyze(Data data, double risk, double population) throws IOException {

        ARXConfiguration config = ARXConfiguration.create();
        config.setMetric(Metric.createPrecomputedLossMetric(1.0d, 0.5d, AggregateFunction.GEOMETRIC_MEAN));
        config.setMaxOutliers(1d);
        config.addCriterion(new PopulationUniqueness(risk,
                                                     PopulationUniquenessModel.PITMAN,
                                                     new ARXPopulationModel(data.getHandle(), population), 
                                                     ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES)
                                                     .iterationsPerTry(15)));
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        double utility = 1d - Double.valueOf(result.getGlobalOptimum().getMaximumInformationLoss().toString());
        data.getHandle().release();
        System.out.print(risk * 100d);
        System.out.print(";");
        System.out.println(utility * 100d);
    }

    private static double[] getRisksForLevel(double riskLevel) {
        double[] risks = new double[9];
        for (int i=1; i<10; i++) {
            risks[i-1] = (double)i * riskLevel;
        }
        return risks;
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
}
