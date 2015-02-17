/*******************************************************************************
Copyright 2015 Adam Crume

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*******************************************************************************/

package plotter.tail;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Shape;
import java.awt.Stroke;

import javax.swing.Icon;

import plotter.DoubleData;
import plotter.xy.LinearXYPlotLine;
import plotter.xy.ScatterXYPlotLine;
import plotter.xy.XYAxis;
import plotter.xy.XYDimension;
import plotter.xy.XYPlotLine;

public class MultiplexingXYPlotLine extends XYPlotLine {
    private XYAxis xAxis;

    private XYAxis yAxis;

    private XYPlotLine delegate;

    private boolean switched;


    public MultiplexingXYPlotLine(XYAxis xAxis, XYAxis yAxis, XYDimension independentDimension) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        setLayout(new GridLayout(1, 1));
        delegate = new LinearXYPlotLine(xAxis, yAxis, independentDimension);
        add(delegate);
    }


    @Override
    public DoubleData getXData() {
        return delegate.getXData();
    }


    @Override
    public DoubleData getYData() {
        return delegate.getYData();
    }


    @Override
    public void add(double x, double y) {
        delegate.add(x, y);
        DoubleData xData = delegate.getXData();
        int length = xData.getLength();
        if(length > 1) {
            double prevX = xData.get(length - 2);
            if(x < prevX) {
                scatter();
            }
        }
    }


    public void scatter() {
        if(!switched) {
            ScatterXYPlotLine scatter = new ScatterXYPlotLine(xAxis, yAxis);
            scatter.setXData(delegate.getXData());
            scatter.setYData(delegate.getYData());
            scatter.setForeground(getForeground());
            scatter.setStroke(getStroke());
            scatter.setPointFill(getPointFill());
            scatter.setPointIcon(getPointIcon());
            scatter.setPointOutline(getPointOutline());
            remove(delegate);
            add(scatter);
            delegate = scatter;
            switched = true;
        }
    }


    @Override
    public XYDimension getIndependentDimension() {
        return delegate.getIndependentDimension();
    }


    @Override
    public void prepend(DoubleData x, DoubleData y) {
        delegate.prepend(x, y);
    }


    @Override
    public void prepend(double[] x, int xoff, double[] y, int yoff, int len) {
        delegate.prepend(x, xoff, y, yoff, len);
    }


    @Override
    public void repaintData(int index) {
        delegate.repaintData(index);
    }


    @Override
    public void repaintData(int index, int count) {
        delegate.repaintData(index, count);
    }


    @Override
    public void removeFirst(int removeCount) {
        delegate.removeFirst(removeCount);
    }


    @Override
    public void removeLast(int removeCount) {
        delegate.removeLast(removeCount);
    }


    @Override
    public void removeAllPoints() {
        delegate.removeAllPoints();
    }


    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        delegate.setForeground(fg);
    }


    @Override
    public void repaint() {
        // TODO: Do we need to manually repaint the delegate?
        super.repaint();
    }


    @Override
    public void setStroke(Stroke stroke) {
        super.setStroke(stroke);
        delegate.setStroke(stroke);
    }


    @Override
    public void setPointFill(Shape pointFill) {
        super.setPointFill(pointFill);
        delegate.setPointFill(pointFill);
    }


    @Override
    public void setPointIcon(Icon pointIcon) {
        super.setPointIcon(pointIcon);
        delegate.setPointIcon(pointIcon);
    }


    @Override
    public void setPointOutline(Shape pointOutline) {
        super.setPointOutline(pointOutline);
        delegate.setPointOutline(pointOutline);
    }
}
