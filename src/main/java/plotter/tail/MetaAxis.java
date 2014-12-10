package plotter.tail;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.JCheckBox;

import plotter.DateNumberFormat;
import plotter.DoubleData;
import plotter.ExpFormat;
import plotter.LinearTickMarkCalculator;
import plotter.LogTickMarkCalculator;
import plotter.TimeTickMarkCalculator;
import plotter.xy.XYAxis;

abstract class MetaAxis {
    private XYAxis axis;

    private NumberFormat format = new DefaultAxisFormat();

    // Only access from the Swing thread
    private double min;

    // Only access from the Swing thread
    private double max;

    // Only access from the Swing thread
    private JCheckBox autoScaleCheckBox;

    private boolean logscale;

    private double scrollWidth = Double.NaN;


    public abstract List<DoubleData> getDatasets();


    public JCheckBox createAutoscaleCheckbox(String label) {
        autoScaleCheckBox = new JCheckBox(label);
        autoScaleCheckBox.setSelected(true);
        return autoScaleCheckBox;
    }


    public JCheckBox createLogscaleCheckbox(String label) {
        final JCheckBox checkbox = new JCheckBox(label);
        checkbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setLogscale(checkbox.isSelected());
            }
        });
        return checkbox;
    }


    public XYAxis getAxis() {
        return axis;
    }


    public void setAxis(XYAxis axis) {
        this.axis = axis;
        updateFormat();
    }


    public NumberFormat getFormat() {
        return format;
    }


    public void setFormat(NumberFormat format) {
        this.format = format;
        updateFormat();
    }


    private void updateFormat() {
        if(axis != null) {
            if(format instanceof DateNumberFormat) {
                axis.setTickMarkCalculator(new TimeTickMarkCalculator());
            } else {
                axis.setTickMarkCalculator(new LinearTickMarkCalculator());
            }
            axis.setFormat(format);
        }
    }


    public double getMin() {
        return min;
    }


    public void setMin(double min) {
        this.min = min;
    }


    public double getMax() {
        return max;
    }


    public void setMax(double max) {
        this.max = max;
    }


    public void resetMinMax() {
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
        if(axis != null && isAutoscale()) {
            axis.setStart(0);
            axis.setEnd(1);
        }
    }


    public void updateMinMax(double val) {
        if(val < min) {
            min = val;
        }
        if(val > max) {
            max = val;
        }
    }


    public void commitMinMax() {
        if(min != Double.POSITIVE_INFINITY && isAutoscale()) {
            autoMinMax();
        }
    }


    private void autoMinMax() {
        double min = this.min;
        if(!Double.isNaN(scrollWidth)) {
            min = max - scrollWidth;
        }
        double margin = .1 * (max - min);
        axis.setStart(min - margin);
        axis.setEnd(max + margin);
    }


    public boolean isAutoscale() {
        return autoScaleCheckBox.isSelected();
    }


    public void setAutoscale(boolean autoscale) {
        autoScaleCheckBox.setSelected(autoscale);
    }


    private void makeLinear() {
        axis.setTickMarkCalculator(new LinearTickMarkCalculator());
        axis.setFormat(format);
        min = Math.pow(10, min);
        max = Math.pow(10, max);
        scrollWidth = Math.pow(10, scrollWidth);
        if(isAutoscale()) {
            autoMinMax();
        } else {
            axis.setStart(Math.pow(10, axis.getStart()));
            axis.setEnd(Math.pow(10, axis.getEnd()));
        }
        for(DoubleData data : getDatasets()) {
            int n = data.getLength();
            for(int i = 0; i < n; i++) {
                data.set(i, Math.pow(10, data.get(i)));
            }
        }
    }


    private void makeLog() {
        axis.setTickMarkCalculator(new LogTickMarkCalculator());
        axis.setFormat(new ExpFormat(format));
        min = Math.log10(min);
        max = Math.log10(max);
        scrollWidth = Math.log10(scrollWidth);
        if(isAutoscale()) {
            autoMinMax();
        } else {
            if(axis.getStart() <= 0) {
                axis.setStart(1e-100);
            } else {
                axis.setStart(Math.log10(axis.getStart()));
            }
            if(axis.getEnd() <= 0) {
                axis.setEnd(1e-100);
            } else {
                axis.setEnd(Math.log10(axis.getEnd()));
            }
        }
        for(DoubleData data : getDatasets()) {
            int n = data.getLength();
            for(int i = 0; i < n; i++) {
                data.set(i, Math.log10(data.get(i)));
            }
        }
    }


    public boolean isLogscale() {
        return logscale;
    }


    public void setLogscale(boolean logscale) {
        boolean oldlogscale = this.logscale;
        this.logscale = logscale;
        if(logscale != oldlogscale) {
            if(logscale) {
                makeLog();
            } else {
                makeLinear();
            }
            logscaleUpdated(logscale);
        }
    }


    protected abstract void logscaleUpdated(boolean logscale);


    public void setScrollWidth(double scrollWidth) {
        this.scrollWidth = scrollWidth;
    }
}
