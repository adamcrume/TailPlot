package plotter.tail;

import java.text.NumberFormat;

import plotter.xy.SimpleXYDataset;
import plotter.xy.XYPlotLine;

class Field {
    private SimpleXYDataset dataset;

    private String name;

    private boolean onY2;

    private NumberFormat format;

    /** The plot line for this field. */
    private XYPlotLine plotLine;


    public Field(String name, boolean onY2) {
        this.name = name;
        this.onY2 = onY2;
    }


    public SimpleXYDataset getDataset() {
        return dataset;
    }


    public void setDataset(SimpleXYDataset dataset) {
        this.dataset = dataset;
    }


    public NumberFormat getFormat() {
        return format;
    }


    public void setFormat(NumberFormat format) {
        this.format = format;
    }


    public String getName() {
        return name;
    }


    public boolean isOnY2() {
        return onY2;
    }


    /**
     * Returns true if the field is being plotted.
     * @return true if the field is visible
     */
    public boolean isVisible() {
        return plotLine == null ? true : plotLine.isVisible();
    }


    /**
     * Returns this field's plot line.
     * @return this field's plot line
     */
    public XYPlotLine getPlotLine() {
        return plotLine;
    }


    /**
     * Sets this field's plot line.
     * @param line new plot line
     */
    public void setPlotLine(XYPlotLine line) {
        this.plotLine = line;
    }
}
