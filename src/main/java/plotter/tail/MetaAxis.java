package plotter.tail;

import java.text.NumberFormat;

import javax.swing.JCheckBox;

import plotter.DateNumberFormat;
import plotter.LinearTickMarkCalculator;
import plotter.TimeTickMarkCalculator;
import plotter.xy.XYAxis;

class MetaAxis {
    private XYAxis axis;

    private NumberFormat format = new DefaultAxisFormat();

    // Only access from the Swing thread
    private double min;

    // Only access from the Swing thread
    private double max;

    // Only access from the Swing thread
    private JCheckBox autoScaleCheckBox;


    public JCheckBox createAutoscaleCheckbox(String label) {
        autoScaleCheckBox = new JCheckBox(label);
        autoScaleCheckBox.setSelected(true);
        return autoScaleCheckBox;
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
            double margin = .1 * (max - min);
            axis.setStart(min - margin);
            axis.setEnd(max + margin);
        }
    }


    public boolean isAutoscale() {
        return autoScaleCheckBox.isSelected();
    }


    public void setAutoscale(boolean autoscale) {
        autoScaleCheckBox.setSelected(autoscale);
    }
}
