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

import org.deidentifier.arx.ARXConfiguration;
import org.deidentifier.arx.ARXPopulationModel;
import org.deidentifier.arx.ARXPopulationModel.Region;
import org.deidentifier.arx.ARXSolverConfiguration;
import org.deidentifier.arx.AttributeType.Hierarchy;
import org.deidentifier.arx.Data;
import org.deidentifier.arx.criteria.KAnonymity;
import org.deidentifier.arx.criteria.PopulationUniqueness;
import org.deidentifier.arx.criteria.SampleUniqueness;
import org.deidentifier.arx.metric.Metric;
import org.deidentifier.arx.metric.Metric.AggregateFunction;
import org.deidentifier.arx.risk.RiskModelPopulationUniqueness.PopulationUniquenessModel;

/**
 * This class encapsulates most of the parameters of a benchmark run
 * @author Fabian Prasser
 */
public class BenchmarkSetup {
    
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
    
    public static enum BenchmarkPrivacyModel {
        K_ANONYMITY {
            @Override
            public String toString() {
                return "k-anonymity";
            }
        },
        UNIQUENESS_DANKAR {
            @Override
            public String toString() {
                return "p-uniqueness (dankar)";
            }
        },
        UNIQUENESS_PITMAN {
            @Override
            public String toString() {
                return "p-uniqueness (pitman)";
            }
        },
        UNIQUENESS_SNB {
            @Override
            public String toString() {
                return "p-uniqueness (snb)";
            }
        },
        UNIQUENESS_ZAYATZ {
            @Override
            public String toString() {
                return "p-uniqueness (zayatz)";
            }
        }, 
        UNIQUENESS_SAMPLE {
            @Override
            public String toString() {
                return "p-sample-uniqueness";
            }
        },
    }
    
    public static enum BenchmarkUtilityMeasure {
        ENTROPY {
            @Override
            public String toString() {
                return "Entropy";
            }
        },
        LOSS {
            @Override
            public String toString() {
                return "Loss";
            }
        },
    }
    
    private static final double[][] SOLVER_START_VALUES = getSolverStartValues();
    
    /**
     * Returns a configuration for the ARX framework
     * @param dataset
     * @param criteria
     * @param uniqueness
     * @return
     * @throws IOException
     */
    public static ARXConfiguration getConfiguration(BenchmarkDataset dataset, 
                                                    BenchmarkUtilityMeasure utility, 
                                                    BenchmarkPrivacyModel criterion, 
                                                    double uniqueness) throws IOException {
        
        ARXConfiguration config = ARXConfiguration.create();
        switch (utility) {
        case ENTROPY:
            config.setMetric(Metric.createPrecomputedNormalizedEntropyMetric(1.0d, AggregateFunction.SUM));
            break;
        case LOSS:
            config.setMetric(Metric.createPrecomputedLossMetric(1.0d, AggregateFunction.GEOMETRIC_MEAN));
            break;
        default:
            throw new IllegalArgumentException("");
        }
        
        config.setMaxOutliers(1.0d);
        
        switch (criterion) {
        case UNIQUENESS_DANKAR:
            config.addCriterion(new PopulationUniqueness(uniqueness, 
                                                         PopulationUniquenessModel.DANKAR,
                                                         ARXPopulationModel.create(Region.USA),
                                                         ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES).iterationsPerTry(15)));
            break;
        case UNIQUENESS_SNB:
            config.addCriterion(new PopulationUniqueness(uniqueness, 
                                                         PopulationUniquenessModel.SNB,
                                                         ARXPopulationModel.create(Region.USA),
                                                         ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES).iterationsPerTry(15)));
            break;
        case UNIQUENESS_PITMAN:
            config.addCriterion(new PopulationUniqueness(uniqueness, 
                                                         PopulationUniquenessModel.PITMAN,
                                                         ARXPopulationModel.create(Region.USA),
                                                         ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES).iterationsPerTry(15)));
            break;
        case UNIQUENESS_ZAYATZ:
            config.addCriterion(new PopulationUniqueness(uniqueness, 
                                                         PopulationUniquenessModel.ZAYATZ,
                                                         ARXPopulationModel.create(Region.USA),
                                                         ARXSolverConfiguration.create().preparedStartValues(SOLVER_START_VALUES).iterationsPerTry(15)));
            break;
        case UNIQUENESS_SAMPLE:
            config.addCriterion(new SampleUniqueness(uniqueness));
            break;
        case K_ANONYMITY:
            config.addCriterion(new KAnonymity(getK(uniqueness)));
            break;
        default:
            throw new RuntimeException("Invalid criterion");
        }
        return config;
    }
    
    /**
     * Configures and returns the dataset
     * @param dataset
     * @param criteria
     * @return
     * @throws IOException
     */
    
    public static Data getData(BenchmarkDataset dataset) throws IOException {
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
    
    /**
     * Returns a set of utility measures
     * @return
     */
    public static BenchmarkUtilityMeasure[] getUtilityMeasures() {
        return new BenchmarkUtilityMeasure[]{BenchmarkUtilityMeasure.ENTROPY,
                                             BenchmarkUtilityMeasure.LOSS};
    }

    public static int getK(double uniqueness) {
        if (uniqueness == 0.001d) {
            return 3;
        } else if (uniqueness == 0.002d) {
            return 4;
        } else if (uniqueness == 0.003d) {
            return 5;
        } else if (uniqueness == 0.004d) {
            return 10;
        } else if (uniqueness == 0.005d) {
            return 15;
        } else if (uniqueness == 0.006d) {
            return 20;
        } else if (uniqueness == 0.007d) {
            return 25;
        } else if (uniqueness == 0.008d) {
            return 50;
        } else if (uniqueness == 0.009d) {
            return 75;
        } else if (uniqueness == 0.01d) {
            return 100;
        } else {
            throw new IllegalArgumentException("Unknown uniqueness parameter");
        }
    }

    /**
     * Creates start values for the solver
     * @return
     */
    private static double[][] getSolverStartValues() {
        double[][] result = new double[16][];
        int index = 0;
        for (double d1 = 0d; d1 < 1d; d1 += 0.33d) {
            for (double d2 = 0d; d2 < 1d; d2 += 0.33d) {
                result[index++] = new double[] { d1, d2 };
            }
        }
        return result;
    }
}
