package org.deidentifier.arx.benchmark;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deidentifier.arx.ARXAnonymizer;
import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXResult;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.io.CSVHierarchyInput;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;

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
public abstract class BenchmarkExperimentKAnonymity {

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
    
        for (String dataset : new String[]{"adult", "cup", "fars", "atus", "ihis"}) {
            analyze(dataset);
        }
    }

    private static void analyze(String dataset) throws IOException {

        Data data = getDataObject(dataset);

        System.out.println(dataset);
        
        System.out.println("risk;utility");
        
        for (int k : new int[]{2,5,10}) {
            analyze(data, k);
        }
    }

    private static void analyze(Data data, int k) throws IOException {

        ARXConfiguration config = ARXConfiguration.create();
        config.setMetric(Metric.createPrecomputedLossMetric(1.0d, 0.5d, AggregateFunction.GEOMETRIC_MEAN));
        config.setMaxOutliers(1d);
        config.addCriterion(new KAnonymity(k));
        
        ARXAnonymizer anonymizer = new ARXAnonymizer();
        ARXResult result = anonymizer.anonymize(data, config);
        double utility = 1d - Double.valueOf(result.getGlobalOptimum().getMaximumInformationLoss().toString());
        data.getHandle().release();
        System.out.print(k);
        System.out.print(";");
        System.out.println(utility * 100d);
    }
}
