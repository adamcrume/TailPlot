package plotter.tail;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.Stroke;
import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import plotter.xy.SimpleXYDataset;
import plotter.xy.XYDimension;

/**
 * Manages configuration and state for an input file.
 * @author Adam Crume
 */
class DataFile {
    /** The plot. */
    private final TailPlot tailPlot;

    /** File this manages, or null for standard input. */
    private File file;

    /** Fields being plotted. */
    private List<Field> fields = new ArrayList<Field>();

    /** Input formats for the fields. */
    private Map<Integer, NumberFormat> fieldFormats = new HashMap<Integer, NumberFormat>();

    /** Field separator. */
    private Pattern fieldSeparator = Pattern.compile("[,\t ]+");

    /** Indices of fields to plot. */
    private int[] selection;

    /** Indices of fields on the Y2 axis. */
    private int[] y2;

    /** Whether or not fields are on the Y2 axis, indexed the same as the return value of {@link #select(String[], int)}. */
    private BitSet y2PostSelection = new BitSet();

    /** Index of the field to use as the X value. */
    private int x = -1;

    /** Input format of the X field. */
    private NumberFormat xInputFormat = NumberFormat.getInstance();

    /** Number of fields needed for an input line to be usable. */
    private int minFieldCount;

    /** Number of points processed. */
    private int points;

    /** True if the file contains a header line. */
    private boolean headerLine;

    /** Names of the fields, separated by the field separator. */
    private String fieldString;

    /** True if the first line of the file has been read. */
    private boolean firstLineRead;

    /** Current processor for the file. */
    private FileProcessor processor;

    /** Index within the files being plotted. */
    private int index;


    /**
     * Creates an unconfigured DataFile.
     * @param tailPlot plot instance
     */
    DataFile(TailPlot tailPlot) {
        this.tailPlot = tailPlot;
    }


    /**
     * Processes a line of the file.
     * @param lineNumber current line number
     * @param line the line
     * @return data to plot
     */
    double[] processLine(int lineNumber, String line) {
        String trimmed = line.trim();
        if(trimmed.isEmpty() || trimmed.startsWith("#")) {
            return null;
        }
        String[] data = fieldSeparator.split(line);
        if(!firstLineRead) {
            firstLineRead = true;
            if(fields.isEmpty()) {
                String[] data2 = select(data, lineNumber);
                for(int i = 0; i < data2.length; i++) {
                    String name;
                    if(headerLine) {
                        name = data2[i];
                    } else if(index > 0) {
                        name = "Column " + (i + 1) + "(file " + (index + 1) + ")";
                    } else {
                        name = "Column " + (i + 1);
                    }
                    boolean onY2 = y2PostSelection.get(i);
                    if(y2 != null) {
                        name += " (" + (onY2 ? "Y2" : "Y1") + ")";
                    }
                    fields.add(new Field(name, onY2));
                }
            }

            if(fields.get(0).getFormat() == null) {
                assert fields.size() == selection.length;
                for(int i = 0; i < selection.length; i++) {
                    NumberFormat format = fieldFormats.get(selection[i]);
                    fields.get(i).setFormat(format == null ? NumberFormat.getInstance() : format);
                }
            }

            for(final Field f : fields) {
                final MultiplexingXYPlotLine pline = new MultiplexingXYPlotLine(tailPlot.getXAxis(),
                        f.isOnY2() ? tailPlot.getY2Axis() : tailPlot.getYAxis(), XYDimension.X);
                final Stroke highlightStroke = new BasicStroke(3);
                final Shape highlightPointFill = null;
                final Shape highlightPointOutline = null;
                pline.setForeground(tailPlot.nextColor());
                SimpleXYDataset dataset = new SimpleXYDataset(pline);
                dataset.setXData(pline.getXData());
                dataset.setYData(pline.getYData());
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        tailPlot.addPlotLine(f.getName(), pline, highlightStroke, highlightPointFill,
                                highlightPointOutline);
                    }
                });
                f.setDataset(dataset);
                f.setPlotLine(pline);
            }
            if(headerLine) {
                return null;
            }
        }

        String[] data2 = select(data, lineNumber);
        if(data2 == null) {
            return null;
        }

        final double[] ddata = new double[data2.length + 1];
        if(x == -1) {
            ddata[0] = points;
        } else {
            String xString = data[x - 1];
            try {
                ddata[0] = xInputFormat.parse(xString).doubleValue();
            } catch(ParseException e) {
                System.err.println("Invalid X value on line " + lineNumber + ": " + xString);
                ddata[0] = Double.NaN;
            }
        }
        for(int i = 0; i < data2.length; i++) {
            try {
                ddata[i + 1] = fields.get(i).getFormat().parse(data2[i]).doubleValue();
            } catch(ParseException e) {
                System.err.println("Invalid value on line " + lineNumber + " for \"" + fields.get(i).getName() + "\": "
                        + data2[i]);
                ddata[i + 1] = Double.NaN;
            }
        }
        points++;
        return ddata;
    }


    /**
     * Selects the relevant fields and discards the rest.
     * @param data all fields from the raw input
     * @param lineNumber current line number
     * @return fields we want to plot
     */
    private String[] select(String[] data, int lineNumber) {
        if(data.length < minFieldCount) {
            System.err.println("Expected at least " + minFieldCount + " fields, but saw " + data.length + " on line "
                    + lineNumber);
            return null;
        }
        if(selection == null) {
            if(x == -1) {
                selection = new int[data.length];
                for(int i = 0; i < selection.length; i++) {
                    selection[i] = i + 1;
                }
            } else {
                // Default not to plot the X value (it would just draw a diagonal)
                selection = new int[data.length - 1];
                for(int i = 0; i < x - 1; i++) {
                    selection[i] = i + 1;
                }
                for(int i = x - 1; i < selection.length; i++) {
                    selection[i] = i + 2;
                }
            }
        }

        String[] ret = new String[selection.length];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = data[selection[i] - 1];
        }
        return ret;
    }


    /**
     * Starts reading data from the file.
     * Should only be called once.
     */
    public void start() {
        if(processor != null) {
            throw new IllegalStateException("start() has already been called");
        }
        processor = new FileProcessor(this.tailPlot, this);
        new Thread(processor).start();
    }


    /**
     * Restarts processing for the file.
     * Should only be called after {@link #start()}.
     */
    public void restart() {
        if(processor == null) {
            throw new IllegalStateException("start() has not been called yet");
        }
        processor.stop();
        processor = null;
        start();
    }


    /**
     * Sets up internal data after configuration.
     */
    protected void init() {
        if(y2 != null) {
            for(int i = 0; i < y2.length; i++) {
                int ix = y2[i];
                if(selection != null) {
                    int ix2 = -1;
                    for(int j = 0; j < selection.length; j++) {
                        if(selection[j] == ix) {
                            ix2 = j;
                            break;
                        }
                    }
                    if(ix2 == -1) {
                        this.tailPlot.usage("Field specified in --y2 (" + ix + ") not present in --select");
                    }
                    ix = ix2;
                }
                y2PostSelection.set(ix);
            }
        }
        if(fieldString != null) {
            String[] names = fieldSeparator.split(fieldString);
            for(int i = 0; i < names.length; i++) {
                String name = names[i];
                boolean onY2 = y2PostSelection.get(i);
                if(y2 != null) {
                    name += " (" + (onY2 ? "Y2" : "Y1") + ")";
                }
                fields.add(new Field(name, onY2));
            }
            if(selection != null) {
                if(selection.length != fields.size()) {
                    this.tailPlot
                            .usage("Number of fields selected with --select does not match number of labels given with --fields");
                }
            }
        }
        if(selection != null) {
            for(int i : selection) {
                minFieldCount = Math.max(minFieldCount, i);
            }
        }
        if(x != -1) {
            NumberFormat format = fieldFormats.get(x);
            xInputFormat = format == null ? NumberFormat.getInstance() : format;
        }
    }


    /**
     * Returns the file being plotted, or null for standard input.
     * @return the file being plotted, or null for standard input
     */
    public File getFile() {
        return file;
    }


    /**
     * Sets the file to plot, or null for standard input.
     * @param file the file to plot, or null for standard input
     */
    public void setFile(File file) {
        this.file = file;
    }


    /**
     * Returns the fields.
     * @return the fields
     */
    public List<Field> getFields() {
        return fields;
    }


    /**
     * Sets the input format for a field.
     * @param fieldIx field index
     * @param format field format
     */
    public void addFieldFormat(int fieldIx, NumberFormat format) {
        fieldFormats.put(fieldIx, format);
    }


    /**
     * Sets the field separator.
     * @param fieldSeparator the field separator
     */
    public void setFieldSeparator(Pattern fieldSeparator) {
        this.fieldSeparator = fieldSeparator;
    }


    /**
     * Sets the indices of fields to plot.
     * @param selection the indices of fields to plot
     */
    public void setSelection(int[] selection) {
        this.selection = selection;
    }


    /**
     * Sets the indices of fields to plot on the Y2 axis.
     * Must be a subset of fields selected by {@link #setSelection(int[])}.
     * @param y2 the indices of fields to plot on the Y2 axis
     */
    public void setY2(int[] y2) {
        this.y2 = y2;
    }


    /**
     * Returns true if at least one field is plotted on the Y2 axis.
     * @return true if at least one field is plotted on the Y2 axis
     */
    public boolean isUseY2() {
        return y2 != null && y2.length > 0;
    }


    /**
     * Sets the index of the X field, or -1 to use the line number.
     * @param x the index of the X field, or -1 to use the line number
     */
    public void setX(int x) {
        this.x = x;
    }


    /**
     * Removes all data.
     */
    public void clearData() {
        for(Field f : fields) {
            SimpleXYDataset dataset = f.getDataset();
            if(dataset != null) {
                dataset.removeAllPoints();
            }
        }
        points = 0;
    }


    /**
     * Sets whether or not the file contains a header line.
     * @param headerLine true if the file contains a header line
     */
    public void setHeaderLine(boolean headerLine) {
        this.headerLine = headerLine;
    }


    /**
     * Sets the field string.
     * This is the names of the fields separated by the field separator.
     * @param fieldString the names of the fields separated by the field separator
     */
    public void setFieldString(String fieldString) {
        this.fieldString = fieldString;
    }


    /**
     * Sets the index within the files being plotted.
     * @param index index within the files being plotted
     */
    public void setIndex(int index) {
        this.index = index;
    }
}
