package plotter.tail;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;

import plotter.DateNumberFormat;
import plotter.DoubleData;
import plotter.ExpFormat;
import plotter.LinearTickMarkCalculator;
import plotter.LogTickMarkCalculator;
import plotter.TimeTickMarkCalculator;
import plotter.tail.XYPlotFrame.AxisListener;
import plotter.xy.XYAxis;

abstract class MetaAxis implements AxisListener {
    private XYAxis axis;

    private NumberFormat format = new MultiscaleNumberFormat();

    // Only access from the Swing thread
    private double min;

    // Only access from the Swing thread
    private double max;

    // Only access from the Swing thread
    private JCheckBox autoScaleCheckBox;

    private boolean logscale;

    private double scrollWidth = Double.NaN;

    // These *AutomaticallyUpdated fields are set to true before we change the value,
    // and set to false afterward (so the PropertyChangeListeners know to ignore the
    // update).  We can't rely on the PropertyChangeListeners on the text fields to
    // set these back to false, because they may not fire (if the new value is equal
    // to the old value).
    protected boolean minTextAutomaticallyUpdated;

    protected boolean maxTextAutomaticallyUpdated;

    protected JFormattedTextField minText;

    protected JFormattedTextField maxText;

    private NumberFormat minmaxTextFormat;
    {
        NumberFormat plainFormat = new DecimalFormat("#.#########");
        NumberFormat exponentialFormat = new DecimalFormat("0.#########E0");
        double upperThreshold = 999.5;
        double lowerThreshold = .01;
        final NumberFormat baseFormat = new MultiscaleNumberFormat(plainFormat, exponentialFormat, lowerThreshold,
                upperThreshold);
        // DecimalFormat refuses to accept both 'e' and 'E' for the exponent separator,
        // so manually fix the case before parsing.
        minmaxTextFormat = new NumberFormat() {
            @Override
            public Number parse(String source, ParsePosition parsePosition) {
                return baseFormat.parse(source.toUpperCase(), parsePosition);
            }


            @Override
            public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
                return baseFormat.format(number, toAppendTo, pos);
            }


            @Override
            public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
                return baseFormat.format(number, toAppendTo, pos);
            }
        };
    }


    public abstract List<DoubleData> getDatasets();


    public JCheckBox createAutoscaleCheckbox(String label) {
        autoScaleCheckBox = new JCheckBox(label);
        autoScaleCheckBox.setSelected(true);
        autoScaleCheckBox.setToolTipText("Automatically scale axis to fit data");
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
        checkbox.setToolTipText("Use logarithmic scaling");
        return checkbox;
    }


    public JFormattedTextField createMinTextField() {
        minText = new JFormattedTextField(minmaxTextFormat);
        minText.setValue(new Double(0));
        minText.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(!minTextAutomaticallyUpdated) {
                    double value = ((Number)evt.getNewValue()).doubleValue();
                    if(logscale) {
                        value = Math.log10(value);
                    }
                    axis.setStart(value);
                    autoScaleCheckBox.setSelected(false);
                }
            }
        });
        return minText;
    }


    public JFormattedTextField createMaxTextField() {
        maxText = new JFormattedTextField(minmaxTextFormat);
        maxText.setValue(new Double(1));
        maxText.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(!maxTextAutomaticallyUpdated) {
                    double value = ((Number)evt.getNewValue()).doubleValue();
                    if(logscale) {
                        value = Math.log10(value);
                    }
                    axis.setEnd(value);
                    autoScaleCheckBox.setSelected(false);
                }
            }
        });
        return maxText;
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
        minTextAutomaticallyUpdated = true;
        maxTextAutomaticallyUpdated = true;
        if(logscale) {
            minText.setValue(Math.pow(10, min - margin));
            maxText.setValue(Math.pow(10, max + margin));
        } else {
            minText.setValue(min - margin);
            maxText.setValue(max + margin);
        }
        minTextAutomaticallyUpdated = false;
        maxTextAutomaticallyUpdated = false;
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


    private void axisManipulated() {
        minTextAutomaticallyUpdated = true;
        maxTextAutomaticallyUpdated = true;
        if(logscale) {
            minText.setValue(Math.pow(10, axis.getStart()));
            maxText.setValue(Math.pow(10, axis.getEnd()));
        } else {
            minText.setValue(axis.getStart());
            maxText.setValue(axis.getEnd());
        }
        minTextAutomaticallyUpdated = false;
        maxTextAutomaticallyUpdated = false;
        autoScaleCheckBox.setSelected(false);
    }


    @Override
    public void axisZoomed(XYAxis axis) {
        if(axis == this.axis) {
            axisManipulated();
        }
    }


    @Override
    public void axisPanned(XYAxis axis) {
        if(axis == this.axis) {
            axisManipulated();
        }
    }
}
