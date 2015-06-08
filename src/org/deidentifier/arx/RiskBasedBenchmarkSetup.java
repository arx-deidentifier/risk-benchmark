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

package org.deidentifier.arx;

import java.io.IOException;

import org.deidentifier.arx.ARXPopulationModel.Region;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.metric.Metric;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class RiskBasedBenchmarkSetup {
    
    public static enum BenchmarkCriterion {
        K_ANONYMITY {
            @Override
            public String toString() {
                return "k";
            }
        },
        RISK_BASED {
            @Override
            public String toString() {
                return "t";
            }
        },
    }
    
    public static enum BenchmarkDataset {
        ADULT {
            @Override
            public String toString() {
                return "Adult";
            }
        },
        CUP {
            @Override
            public String toString() {
                return "Cup";
            }
        },
        FARS {
            @Override
            public String toString() {
                return "Fars";
            }
        },
        ATUS {
            @Override
            public String toString() {
                return "Atus";
            }
        },
        IHIS {
            @Override
            public String toString() {
                return "Ihis";
            }
        },
    }
    
    /**
     * Returns a configuration for the ARX framework
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset, BenchmarkCriterion criterion) throws IOException {
        ARXConfiguration config = ARXConfiguration.create();
        config.setMetric(Metric.createLossMetric());
        config.setMaxOutliers(1.0d);
        
        switch (criterion) {
        case RISK_BASED:
            config.addCriterion(new PopulationUniqueness(0.01d, ARXPopulationModel.create(Region.USA)));
            break;
        case K_ANONYMITY:
            config.addCriterion(new KAnonymity(5));
            break;
        default:
            throw new RuntimeException("Invalid criterion");
        }
        return config;
    }
    
    public static BenchmarkCriterion[] getCriteria() {
        return new BenchmarkCriterion[] {
                BenchmarkCriterion.K_ANONYMITY,
                BenchmarkCriterion.RISK_BASED,
        };
    }
    
    /**
     * Configures and returns the dataset
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    
    public static Data getData(BenchmarkDataset dataset, BenchmarkCriterion criterion) throws IOException {
        Data data = null;
        switch (dataset) {
        case ADULT:
            data = Data.create("data/adult.csv", ';');
            break;
        case ATUS:
            data = Data.create("data/atus.csv", ';');
            break;
        case CUP:
            data = Data.create("data/cup.csv", ';');
            break;
        case FARS:
            data = Data.create("data/fars.csv", ';');
            break;
        case IHIS:
            data = Data.create("data/ihis.csv", ';');
            break;
        default:
            throw new RuntimeException("Invalid dataset");
        }
        
        for (String qi : getQuasiIdentifyingAttributes(dataset)) {
            data.getDefinition().setAttributeType(qi, getHierarchy(dataset, qi));
        }
        
        return data;
    }
    
    /**
     * Returns all datasets
     * @return
     */
    public static BenchmarkDataset[] getDatasets() {
        return new BenchmarkDataset[] {
                BenchmarkDataset.ADULT,
                BenchmarkDataset.CUP,
                BenchmarkDataset.FARS,
                BenchmarkDataset.ATUS,
                BenchmarkDataset.IHIS
        };
    }
    
    /**
     * Returns the generalization hierarchy for the dataset and attribute
     * @param dataset
     * @param attribute
     * @return
     * @throws IOException
     */
    public static Hierarchy getHierarchy(BenchmarkDataset dataset, String attribute) throws IOException {
        switch (dataset) {
        case ADULT:
            return Hierarchy.create("hierarchies/adult_hierarchy_" + attribute + ".csv", ';');
        case ATUS:
            return Hierarchy.create("hierarchies/atus_hierarchy_" + attribute + ".csv", ';');
        case CUP:
            return Hierarchy.create("hierarchies/cup_hierarchy_" + attribute + ".csv", ';');
        case FARS:
            return Hierarchy.create("hierarchies/fars_hierarchy_" + attribute + ".csv", ';');
        case IHIS:
            return Hierarchy.create("hierarchies/ihis_hierarchy_" + attribute + ".csv", ';');
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
    
    /**
     * Returns the quasi-identifiers for the dataset
     * @param dataset
     * @return
     */
    public static String[] getQuasiIdentifyingAttributes(BenchmarkDataset dataset) {
        switch (dataset) {
        case ADULT:
            return new String[] {   "age",
                                    "education",
                                    "marital-status",
                                    "native-country",
                                    "race",
                                    "salary-class",
                                    "sex",
                                    "workclass",
                                    "occupation" };
        case ATUS:
            return new String[] {   "Age",
                                    "Birthplace",
                                    "Citizenship status",
                                    "Labor force status",
                                    "Marital status",
                                    "Race",
                                    "Region",
                                    "Sex",
                                    "Highest level of school completed" };
        case CUP:
            return new String[] {   "AGE",
                                    "GENDER",
                                    "INCOME",
                                    "MINRAMNT",
                                    "NGIFTALL",
                                    "STATE",
                                    "ZIP",
                                    "RAMNTALL" };
        case FARS:
            return new String[] {   "iage",
                                    "ideathday",
                                    "ideathmon",
                                    "ihispanic",
                                    "iinjury",
                                    "irace",
                                    "isex",
                                    "istatenum" };
        case IHIS:
            return new String[] {   "AGE",
                                    "MARSTAT",
                                    "PERNUM",
                                    "QUARTER",
                                    "RACEA",
                                    "REGION",
                                    "SEX",
                                    "YEAR",
                                    "EDUC" };
        default:
            throw new RuntimeException("Invalid dataset");
        }
    }
}
