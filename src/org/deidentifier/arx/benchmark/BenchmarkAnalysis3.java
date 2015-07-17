/*
 * Source code of the experiments for the entropy metric
 *      
 * Copyright (C) 2015 Fabian Prasser
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.deidentifier.arx.benchmark.BenchmarkSetup.BenchmarkDataset;

import de.linearbits.objectselector.Selector;
import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Labels;
import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotLinesClustered;
import de.linearbits.subframe.graph.Series3D;
import de.linearbits.subframe.io.CSVFile;
import de.linearbits.subframe.render.GnuPlotParams;
import de.linearbits.subframe.render.GnuPlotParams.KeyPos;
import de.linearbits.subframe.render.LaTeX;
import de.linearbits.subframe.render.PlotGroup;

/**
 * Example benchmark
 * @author Fabian Prasser
 */
public class BenchmarkAnalysis3 {

	/**
	 * Main
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
    public static void main(String[] args) throws IOException, ParseException {
        
        CSVFile file = new CSVFile(new File("results/experiment3.csv"));
        List<PlotGroup> groups = new ArrayList<PlotGroup>();

        for (BenchmarkDataset dataset : BenchmarkSetup.getDatasets()) {
            groups.add(analyze(file, dataset));
        }
        
        LaTeX.plot(groups, "results/experiment3");
        
    }
    
    /**
     * Performs the analysis
     * @param file
     * @param dataset
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyze(CSVFile file, BenchmarkDataset dataset) throws ParseException{

        Series3D series = getSeriesForLinesPlot(file, dataset);
            
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotLinesClustered("", 
                                         new Labels("Uniqueness", "Information loss [%]"),
                                         series));
        
        GnuPlotParams params = new GnuPlotParams();
        params.rotateXTicks = 0;
        params.keypos = KeyPos.OUTSIDE_TOP;
        params.size = 1.5d;
        params.minY = 0d;
        params.maxY = 20d;
        return new PlotGroup("Comparison of different uniqueness models: " + dataset.toString(), plots, params, 1.0d);
    }
    

    /**
     * Returns a series that can be clustered by size
     * @param file
     * @param method
     * @return
     * @throws ParseException
     */
    private static Series3D getSeriesForLinesPlot(CSVFile file, BenchmarkDataset dataset) throws ParseException {

        Selector<String[]> selector = file.getSelectorBuilder()
                                          .field("Dataset").equals(dataset.toString())
                                          .build();
                
        Series3D series = new Series3D(file, selector, 
                                       new Field("Uniqueness"),
                                       new Field("Privacy model"),
                                       new Field("Utility", Analyzer.VALUE));
                
        return series;
    }
}
