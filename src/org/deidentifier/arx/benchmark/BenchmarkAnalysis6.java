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

import de.linearbits.objectselector.Selector;
import de.linearbits.subframe.analyzer.Analyzer;
import de.linearbits.subframe.graph.Field;
import de.linearbits.subframe.graph.Function;
import de.linearbits.subframe.graph.Labels;
import de.linearbits.subframe.graph.Plot;
import de.linearbits.subframe.graph.PlotHistogramClustered;
import de.linearbits.subframe.graph.Point3D;
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
public class BenchmarkAnalysis6 {

	/**
	 * Main
	 * @param args
	 * @throws IOException
	 * @throws ParseException
	 */
    public static void main(String[] args) throws IOException, ParseException {
        
        CSVFile file = new CSVFile(new File("results/experiment6.csv"));
        List<PlotGroup> groups = new ArrayList<PlotGroup>();
        groups.add(analyze(file));
        LaTeX.plot(groups, "results/experiment6");
        
    }
    
    /**
     * Performs the analysis
     * @param file
     * @return
     * @throws ParseException
     */
    private static PlotGroup analyze(CSVFile file) throws ParseException{

        // Selects all rows
        Selector<String[]> selector = file.getSelectorBuilder().field("LowerBound").equals("true").or().equals("false").build();
                
        Series3D series = new Series3D(file, selector, 
                                       new Field("Dataset"),
                                       new Field("LowerBound"),
                                       new Field("Total", Analyzer.VALUE));
        
        series.transform(new Function<Point3D>(){
            @Override
            public Point3D apply(Point3D arg0) {
                return new Point3D(arg0.x,
                                   arg0.y.equals("true") ? "With lower bound" : "Without lower bound",
                                   arg0.z);
            }
        });
        
        List<Plot<?>> plots = new ArrayList<Plot<?>>();
        plots.add(new PlotHistogramClustered("", 
                                         new Labels("Dataset", "Total time [ms]"),
                                         series));
        
        GnuPlotParams params = new GnuPlotParams();
        params.rotateXTicks = 0;
        params.keypos = KeyPos.TOP_LEFT;
        params.size = 1.0d;
        params.ratio = 0.5d;
        return new PlotGroup("Comparison of execution times of using the Pitman model with and without the lower bound optimization", plots, params, 1.0d);
    }
}
