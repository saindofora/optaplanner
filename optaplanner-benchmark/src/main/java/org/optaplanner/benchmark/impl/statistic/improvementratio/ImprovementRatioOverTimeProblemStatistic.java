/*
 * Copyright 2011 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.benchmark.impl.statistic.improvementratio;

import java.awt.BasicStroke;
import java.io.File;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.optaplanner.benchmark.impl.DefaultPlannerBenchmark;
import org.optaplanner.benchmark.impl.ProblemBenchmark;
import org.optaplanner.benchmark.impl.SingleBenchmark;
import org.optaplanner.benchmark.impl.statistic.AbstractProblemStatistic;
import org.optaplanner.benchmark.impl.statistic.MillisecondsSpendNumberFormat;
import org.optaplanner.benchmark.impl.statistic.ProblemStatisticType;
import org.optaplanner.benchmark.impl.statistic.SingleStatistic;
import org.optaplanner.core.impl.move.Move;

public class ImprovementRatioOverTimeProblemStatistic extends AbstractProblemStatistic {

    protected Map<String, File> graphStatisticFiles = null;

    public ImprovementRatioOverTimeProblemStatistic(ProblemBenchmark problemBenchmark) {
        super(problemBenchmark, ProblemStatisticType.IMPROVEMENT_RATIO_OVER_TIME);
    }

    @Override
    public SingleStatistic createSingleStatistic() {
        return new ImprovementRatioOverTimeSingleStatistic();
    }

    /**
     * @return never null, each path is relative to the {@link DefaultPlannerBenchmark#benchmarkReportDirectory} (not
     *  {@link ProblemBenchmark#problemReportDirectory})
     */
    public Map<String, String> getGraphFilePaths() {
        Map<String, String> graphFilePaths = new HashMap<String, String>(graphStatisticFiles.size());
        for (Map.Entry<String, File> entry : graphStatisticFiles.entrySet()) {
            graphFilePaths.put(entry.getKey(), toFilePath(entry.getValue()));
        }
        return graphFilePaths;
    }

    // ************************************************************************
    // Write methods
    // ************************************************************************

    @Override
    protected void writeCsvStatistic() {
        // TODO FIXME Planner doesn't support multiple CSV statistics per benchmark
    }

    @Override
    protected void writeGraphStatistic() {
        Map<Class<? extends Move>, XYPlot> plots = new HashMap<Class<? extends Move>, XYPlot>();
        int seriesIndex = 0;
        for (SingleBenchmark singleBenchmark : problemBenchmark.getSingleBenchmarkList()) {
            Map<Class<? extends Move>, XYSeries> seriesMap = new HashMap<Class<? extends Move>, XYSeries>();
            // No direct ascending lines between 2 points, but a stepping line instead
            XYItemRenderer renderer = new XYStepRenderer();
            if (singleBenchmark.isSuccess()) {
                ImprovementRatioOverTimeSingleStatistic singleStatistic = (ImprovementRatioOverTimeSingleStatistic)
                        singleBenchmark.getSingleStatistic(problemStatisticType);
                for (Map.Entry<Class<? extends Move>, List<ImprovementRatioOverTimeSingleStatisticPoint>> entry : singleStatistic.getPointLists().entrySet()) {
                    Class<? extends Move> type = entry.getKey();
                    if (!seriesMap.containsKey(type)) {
                        seriesMap.put(type, new XYSeries(singleBenchmark.getSolverBenchmark().getNameWithFavoriteSuffix()));
                    }
                    XYSeries series = seriesMap.get(type);
                    for (ImprovementRatioOverTimeSingleStatisticPoint point : entry.getValue()) {
                        long timeMillisSpend = point.getTimeMillisSpend();
                        long ratio = point.getRatio();
                        series.add(timeMillisSpend, ratio);
                    }
                }
            }
            if (singleBenchmark.getSolverBenchmark().isFavorite()) {
                // Make the favorite more obvious
                renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            }
            for (Map.Entry<Class<? extends Move>, XYSeries> entry : seriesMap.entrySet()) {
                Class<? extends Move> moveClass = entry.getKey();
                if (!plots.containsKey(moveClass)) {
                    plots.put(moveClass, createPlot(moveClass));
                }
                plots.get(moveClass).setDataset(seriesIndex, new XYSeriesCollection(entry.getValue()));
                plots.get(moveClass).setRenderer(seriesIndex, renderer);
            }
            for (int i = 0; i < seriesMap.size(); i++) {
            }
            seriesIndex++;
        }
        graphStatisticFiles = new HashMap<String, File>(plots.size());
        for (Map.Entry<Class<? extends Move>, XYPlot> entry : plots.entrySet()) {
            Class<? extends Move> moveClass = entry.getKey();
            String id = moveClass.getCanonicalName();
            String htmlSafeId = id.replace('.', '_');
            JFreeChart chart = new JFreeChart(
                    problemBenchmark.getName() + " improvement ratio over time statistic, move moveClass " + id,
                    JFreeChart.DEFAULT_TITLE_FONT, entry.getValue(), true);
            graphStatisticFiles.put(htmlSafeId, writeChartToImageFile(chart,
                    problemBenchmark.getName() + "ImprovementRatioOverTimeStatistic-" + id));
        }
    }

    private XYPlot createPlot(Class<? extends Move> moveClass) {
        Locale locale = problemBenchmark.getPlannerBenchmark().getBenchmarkReport().getLocale();
        NumberAxis xAxis = new NumberAxis("Time spend");
        xAxis.setNumberFormatOverride(new MillisecondsSpendNumberFormat(locale));
        NumberAxis yAxis = new NumberAxis("% score-improving");
        yAxis.setNumberFormatOverride(NumberFormat.getInstance(locale));
        yAxis.setAutoRangeIncludesZero(false);
        XYPlot plot = new XYPlot(null, xAxis, yAxis, null);
        plot.setOrientation(PlotOrientation.VERTICAL);
        return plot;
    }

}