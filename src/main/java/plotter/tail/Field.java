package plotter.tail;

import java.text.NumberFormat;

import plotter.xy.SimpleXYDataset;

class Field {
    private SimpleXYDataset dataset;

    private String name;

    private boolean onY2;

    private NumberFormat format;


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
}
