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

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import plotter.DateNumberFormat;
import plotter.DoubleData;
import plotter.Legend;
import plotter.LegendItem;
import plotter.xy.LinearXYAxis;
import plotter.xy.SimpleXYDataset;
import plotter.xy.XYAxis;

public class TailPlot {
    private List<DataFile> dataFiles = new ArrayList<DataFile>();

    private XYFormat slopeFormat;

    private XYFormat locationFormat;

    private XYPlotFrame frame;

    private XYAxis xAxis;

    private XYAxis yAxis;

    private XYAxis y2Axis;

    private Iterator<Color> colors;

    private MetaAxis metaX = new MetaAxis() {
        public List<DoubleData> getDatasets() {
            List<DoubleData> datasets = new ArrayList<DoubleData>();
            for(DataFile dataFile : dataFiles) {
                for(Field f : dataFile.getFields()) {
                    SimpleXYDataset dataset = f.getDataset();
                    if(dataset != null) {
                        datasets.add(dataset.getXData());
                    }
                }
            }
            return datasets;
        }


        @Override
        protected void logscaleUpdated(boolean logscale) {
            locationFormat.setLogX(logscale);
            slopeFormat.setLogX(logscale);
            frame.getPlot().repaint();
        }
    };

    private MetaAxis metaY = new MetaAxis() {
        @Override
        public List<DoubleData> getDatasets() {
            List<DoubleData> datasets = new ArrayList<DoubleData>();
            for(DataFile dataFile : dataFiles) {
                for(Field f : dataFile.getFields()) {
                    if(!f.isOnY2()) {
                        SimpleXYDataset dataset = f.getDataset();
                        if(dataset != null) {
                            datasets.add(dataset.getYData());
                        }
                    }
                }
            }
            return datasets;
        }


        @Override
        protected void logscaleUpdated(boolean logscale) {
            locationFormat.setLogY(logscale);
            slopeFormat.setLogY(logscale);
            frame.getPlot().repaint();
        }
    };

    private MetaAxis metaY2 = new MetaAxis() {
        @Override
        public List<DoubleData> getDatasets() {
            List<DoubleData> datasets = new ArrayList<DoubleData>();
            for(DataFile dataFile : dataFiles) {
                for(Field f : dataFile.getFields()) {
                    if(f.isOnY2()) {
                        SimpleXYDataset dataset = f.getDataset();
                        if(dataset != null) {
                            datasets.add(dataset.getYData());
                        }
                    }
                }
            }
            return datasets;
        }


        @Override
        protected void logscaleUpdated(boolean logscale) {
            frame.getPlot().repaint();
        }
    };

    private String title;

    private JCheckBox autorestartCheckbox;

    /** Editable table with the plotted fields listed. */
    private JTable linesTable;

    /** The plot lines. */
    private List<MultiplexingXYPlotLine> plotLines = new ArrayList<MultiplexingXYPlotLine>();

    /** Visible legend entries, keyed by plot line. */
    private Map<MultiplexingXYPlotLine, LegendItem> legendItems = new HashMap<MultiplexingXYPlotLine, LegendItem>();


    public static void main(String[] args) {
        try {
            new TailPlot().run(args);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    void usage(String msg) {
        if(msg != null) {
            System.err.println(msg);
        }
        System.err.println("Usage: TailPlot file [options] [ file [options] ... ]");
        System.err.println("Plots a file, displaying new data as it is generated (analogous to 'tail -f')");
        System.err.println("If no file is specified, standard input is read.");
        System.err.println("File-specific options must come after the relevant file name, although general options may appear anywhere.");
        System.err.println();
        System.err.println("General options:");
        System.err.println("      --x-format=FMT            display format of the X axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("      --y-format=FMT            display format of the Y axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("      --y2-format=FMT           display format of the Y2 axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("  -t, --title=TITLE             set the window title (defaults to the file name)");
        System.err.println("      --scroll-width=AMT        amount of data to keep on screen (in X axis units)");
        System.err.println("      --help                    display this message");
        System.err.println();
        System.err.println("File-specific options:");
        System.err.println("  -F, --field-separator=REGEX   set the field separator regex (default: [,\\t ]+)");
        System.err.println("  -f, --fields=FIELDS           field names (separated by the field separator)");
        System.err.println("  -s, --select=FIELDS           comma-separated list of field indices to plot (1-based)");        
        System.err.println("      --y2=FIELDS               comma-separated list of field indices to place on the Y2 axis (1-based)");
        System.err.println("  -x, --x=INDEX                 index of field to use as X value. Note that X values must be monotonically increasing. (1-based, default: line number is X value)");
        System.err.println("      --field-format=FIELD,FMT  input format of a field. Example: 1,time,YYY-MM-dd_HH:mm:ss to read field 1 as a timestamp (default: number)");
        System.err.println("  -h, --header-line             use the first line as a header line");
        System.err.println();
        System.err.println("Notes:");
        System.err.println("  If both --fields and --header-line are specified, the first line is skipped, and field names are taken from --fields.");
        System.err.println();
        System.err.println("  For compatibility with legacy scripts, if only one file is specified, the options may come before the file name, although this usage is discouraged.");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  Plot the first and third fields, with the third on the Y2 axis");
        System.err.println("    TailPlot file --select=1,3 --y2=3");
        System.err.println();
        System.err.println("  Plot the first field of fileA and the third field of fileB");
        System.err.println("    TailPlot fileA --select=1 fileB --select=3");
        System.exit(1);
    }


    private void parseArgsLegacy(String[] args) {
        DataFile dataFile = new DataFile(this);
        title = null;
        String scrollWidthString = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-F")) {
                dataFile.setFieldSeparator(Pattern.compile(args[++i]));
            } else if(args[i].startsWith("--field-separator=")) {
                dataFile.setFieldSeparator(Pattern.compile(args[i].substring("--field-separator=".length())));
            } else if(args[i].equals("-f")) {
                dataFile.setFieldString(args[++i]);
            } else if(args[i].startsWith("--fields=")) {
                dataFile.setFieldString(args[i].substring("--fields=".length()));
            } else if(args[i].equals("-s")) {
                dataFile.setSelection(parseIntList(args[++i]));
            } else if(args[i].startsWith("--select=")) {
                dataFile.setSelection(parseIntList(args[i].substring("--select=".length())));
            } else if(args[i].startsWith("--y2=")) {
                dataFile.setY2(parseIntList(args[i].substring("--y2=".length())));
            } else if(args[i].startsWith("-x")) {
                dataFile.setX(Integer.parseInt(args[++i]));
            } else if(args[i].startsWith("--x=")) {
                dataFile.setX(Integer.parseInt(args[i].substring("--x=".length())));
            } else if(args[i].startsWith("--field-format=")) {
                String s = args[i].substring("--field-format=".length());
                int ix = s.indexOf(",");
                int fieldIx = Integer.parseInt(s.substring(0, ix));
                String format = s.substring(ix + 1);
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                dataFile.addFieldFormat(fieldIx, fmt);
            } else if(args[i].startsWith("--x-format=")) {
                String format = args[i].substring("--x-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaX.setFormat(fmt);
            } else if(args[i].startsWith("--y-format=")) {
                String format = args[i].substring("--y-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaY.setFormat(fmt);
            } else if(args[i].startsWith("--y2-format=")) {
                String format = args[i].substring("--y2-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaY2.setFormat(fmt);
            } else if(args[i].equals("--header-line") || args[i].equals("-h")) {
                dataFile.setHeaderLine(true);
            } else if(args[i].equals("-t")) {
                title = args[++i];
            } else if(args[i].startsWith("--title=")) {
                title = args[i].substring("--title=".length());
            } else if(args[i].startsWith("--scroll-width=")) {
                scrollWidthString = args[i].substring("--scroll-width=".length());
            } else if(args[i].equals("--help") || args[i].equals("-h")) {
                usage(null);
            } else if(args[i].startsWith("-")) {
                usage("Unrecognized option: " + args[i]);
            } else {
                if(dataFile.getFile() != null) {
                    // Multiple files have been specified.  Use the new parser instead.
                    parseArgs(args);
                    return;
                }
                dataFile.setFile(new File(args[i]));
            }
        }
        dataFile.init();
        if(title == null) {
            if(dataFile.getFile() == null) {
                title = "<standard input>";
            } else {
                title = dataFile.getFile().getPath();
            }
        }
        if(scrollWidthString != null) {
            try {
                NumberFormat format = metaX.getFormat();
                metaX.setScrollWidth(format.parse(scrollWidthString).doubleValue());
            } catch(ParseException e) {
                System.err.println("Invalid X value for scroll width: " + scrollWidthString);
            }
        }
        dataFiles.add(dataFile);
    }


    private void parseArgs(String[] args) {
        title = null;
        String scrollWidthString = null;
        DataFile dataFile = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-F")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setFieldSeparator(Pattern.compile(args[++i]));
                }
            } else if(args[i].startsWith("--field-separator=")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setFieldSeparator(Pattern.compile(args[i].substring("--field-separator=".length())));
                }
            } else if(args[i].equals("-f")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setFieldString(args[++i]);
                }
            } else if(args[i].startsWith("--fields=")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setFieldString(args[i].substring("--fields=".length()));
                }
            } else if(args[i].equals("-s")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setSelection(parseIntList(args[++i]));
                }
            } else if(args[i].startsWith("--select=")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setSelection(parseIntList(args[i].substring("--select=".length())));
                }
            } else if(args[i].startsWith("--y2=")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setY2(parseIntList(args[i].substring("--y2=".length())));
                }
            } else if(args[i].startsWith("-x")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setX(Integer.parseInt(args[++i]));
                }
            } else if(args[i].startsWith("--x=")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setX(Integer.parseInt(args[i].substring("--x=".length())));
                }
            } else if(args[i].startsWith("--field-format=")) {
                String s = args[i].substring("--field-format=".length());
                int ix = s.indexOf(",");
                int fieldIx = Integer.parseInt(s.substring(0, ix));
                String format = s.substring(ix + 1);
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.addFieldFormat(fieldIx, fmt);
                }
            } else if(args[i].startsWith("--x-format=")) {
                String format = args[i].substring("--x-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaX.setFormat(fmt);
            } else if(args[i].startsWith("--y-format=")) {
                String format = args[i].substring("--y-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaY.setFormat(fmt);
            } else if(args[i].startsWith("--y2-format=")) {
                String format = args[i].substring("--y2-format=".length());
                NumberFormat fmt;
                try {
                    fmt = parseFormat(format);
                } catch(ParseException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    fmt = null;
                }
                metaY2.setFormat(fmt);
            } else if(args[i].equals("--header-line") || args[i].equals("-h")) {
                if(dataFile == null) {
                    usage(args[i] + " must be used after file argument");
                } else {
                    dataFile.setHeaderLine(true);
                }
            } else if(args[i].equals("-t")) {
                title = args[++i];
            } else if(args[i].startsWith("--title=")) {
                title = args[i].substring("--title=".length());
            } else if(args[i].startsWith("--scroll-width=")) {
                scrollWidthString = args[i].substring("--scroll-width=".length());
            } else if(args[i].equals("--help") || args[i].equals("-h")) {
                usage(null);
            } else if(args[i].startsWith("-")) {
                usage("Unrecognized option: " + args[i]);
            } else {
                dataFile = new DataFile(this);
                dataFile.setFile(new File(args[i]));
                dataFile.setIndex(dataFiles.size());
                dataFiles.add(dataFile);
            }
        }
        for(DataFile file : dataFiles) {
            file.init();
        }
        if(title == null) {
            StringBuilder b = new StringBuilder();
            for(DataFile file : dataFiles) {
                if(b.length() > 0) {
                    b.append(", ");
                }
                if(file.getFile() == null) {
                    b.append("<standard input>");
                } else {
                    b.append(file.getFile().getPath());
                }
            }
            title = b.toString();
        }
        if(scrollWidthString != null) {
            try {
                NumberFormat format = metaX.getFormat();
                metaX.setScrollWidth(format.parse(scrollWidthString).doubleValue());
            } catch(ParseException e) {
                System.err.println("Invalid X value for scroll width: " + scrollWidthString);
            }
        }
    }


    public void run(String[] args) throws IOException {
        // If multiple files are specified, the legacy parser will hand over to the new parser.
        parseArgsLegacy(args);

        boolean restartable = true;
        for(DataFile dataFile : dataFiles) {
            if(dataFile.getFile() == null) {
                restartable = false;
            }
        }

        frame = new XYPlotFrame();
        boolean useY2 = false;
        for(DataFile dataFile : dataFiles) {
            if(dataFile.isUseY2()) {
                useY2 = true;
            }
        }
        frame.setUseY2(useY2);
        frame.setUseLegend(true);
        JPanel content = new JPanel();
        JPanel settings = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridwidth = 1;
        labelConstraints.insets = new Insets(0, 0, 0, 5);
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        settings.add(metaX.createAutoscaleCheckbox("Auto-scale X axis"), constraints);
        settings.add(metaY.createAutoscaleCheckbox("Auto-scale Y axis"), constraints);
        if(useY2) {
            settings.add(metaY2.createAutoscaleCheckbox("Auto-scale Y2 axis"), constraints);
        }
        frame.addAxisListener(metaX);
        frame.addAxisListener(metaY);
        if(useY2) {
            frame.addAxisListener(metaY2);
        }
        settings.add(metaX.createLogscaleCheckbox("Logarithmic X axis"), constraints);
        settings.add(metaY.createLogscaleCheckbox("Logarithmic Y axis"), constraints);
        if(useY2) {
            settings.add(metaY2.createLogscaleCheckbox("Logarithmic Y2 axis"), constraints);
        }

        JLabel xminLabel = new JLabel("X minimum:");
        JFormattedTextField xminText = metaX.createMinTextField();
        xminLabel.setLabelFor(xminText);
        settings.add(xminLabel, labelConstraints);
        settings.add(xminText, constraints);

        JLabel xmaxLabel = new JLabel("X maximum:");
        JFormattedTextField xmaxText = metaX.createMaxTextField();
        xmaxLabel.setLabelFor(xmaxText);
        settings.add(xmaxLabel, labelConstraints);
        settings.add(xmaxText, constraints);

        JLabel yminLabel = new JLabel("Y minimum:");
        JFormattedTextField yminText = metaY.createMinTextField();
        yminLabel.setLabelFor(yminText);
        settings.add(yminLabel, labelConstraints);
        settings.add(yminText, constraints);

        JLabel ymaxLabel = new JLabel("Y maximum:");
        JFormattedTextField ymaxText = metaY.createMaxTextField();
        ymaxLabel.setLabelFor(ymaxText);
        settings.add(ymaxLabel, labelConstraints);
        settings.add(ymaxText, constraints);

        if(useY2) {
            JLabel y2minLabel = new JLabel("Y2 minimum:");
            JFormattedTextField y2minText = metaY2.createMinTextField();
            y2minLabel.setLabelFor(y2minText);
            settings.add(y2minLabel, labelConstraints);
            settings.add(y2minText, constraints);

            JLabel y2maxLabel = new JLabel("Y2 maximum:");
            JFormattedTextField y2maxText = metaY2.createMaxTextField();
            y2maxLabel.setLabelFor(y2maxText);
            settings.add(y2maxLabel, labelConstraints);
            settings.add(y2maxText, constraints);
        }

        autorestartCheckbox = new JCheckBox("Auto-restart if file shrinks");
        autorestartCheckbox.setSelected(restartable);
        autorestartCheckbox.setEnabled(restartable);
        autorestartCheckbox.setToolTipText("Reload data if a file shrinks");
        settings.add(autorestartCheckbox, constraints);

        final JCheckBox showLegendCheckbox = new JCheckBox("Show legend");
        showLegendCheckbox.setSelected(true);
        showLegendCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.getLegend().setVisible(showLegendCheckbox.isSelected());
            }
        });
        showLegendCheckbox.setToolTipText("Display the plot legend");
        settings.add(showLegendCheckbox, constraints);

        JLabel xMarginLabel = new JLabel("Bottom margin:");
        final JSpinner xMarginSpinner = new JSpinner();
        xMarginSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(xAxis != null) {
                    int value = (Integer) xMarginSpinner.getValue();
                    xAxis.setPreferredSize(new Dimension(xAxis.getPreferredSize().width, value));
                    xAxis.getParent().revalidate();
                    yAxis.setStartMargin(value);
                    yAxis.revalidate();
                    if(y2Axis != null) {
                        y2Axis.setStartMargin(value);
                        y2Axis.revalidate();
                    }
                }
            }
        });
        xMarginSpinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        xMarginSpinner.setToolTipText("Number of pixels below the main plot area");
        xMarginLabel.setLabelFor(xMarginSpinner);
        settings.add(xMarginLabel, labelConstraints);
        settings.add(xMarginSpinner, constraints);

        JLabel yMarginLabel = new JLabel("Left margin:");
        final JSpinner yMarginSpinner = new JSpinner();
        yMarginSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(yAxis != null) {
                    int value = (Integer) yMarginSpinner.getValue();
                    yAxis.setPreferredSize(new Dimension(value, yAxis.getPreferredSize().height));
                    yAxis.getParent().revalidate();
                    xAxis.setStartMargin(value);
                    xAxis.revalidate();
                }
            }
        });
        yMarginSpinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        yMarginSpinner.setToolTipText("Number of pixels to the left of the main plot area");
        yMarginLabel.setLabelFor(yMarginSpinner);
        settings.add(yMarginLabel, labelConstraints);
        settings.add(yMarginSpinner, constraints);

        final JSpinner y2MarginSpinner;
        if(!useY2) {
            y2MarginSpinner = null;
        } else {
            JLabel y2MarginLabel = new JLabel("Right margin:");
            y2MarginSpinner = new JSpinner();
            y2MarginSpinner.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if(y2Axis != null) {
                        int value = (Integer) y2MarginSpinner.getValue();
                        y2Axis.setPreferredSize(new Dimension(value, y2Axis.getPreferredSize().height));
                        y2Axis.getParent().revalidate();
                        xAxis.setEndMargin(value);
                        xAxis.revalidate();
                    }
                }
            });
            y2MarginSpinner.setModel(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
            y2MarginSpinner.setToolTipText("Number of pixels to the right of the main plot area");
            y2MarginLabel.setLabelFor(y2MarginSpinner);
            settings.add(y2MarginLabel, labelConstraints);
            settings.add(y2MarginSpinner, constraints);
        }

        final JButton restartButton = new JButton("Restart");
        restartButton.setEnabled(restartable);
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                for(DataFile dataFile : dataFiles) {
                    dataFile.restart();
                }
            }
        });
        restartButton.setToolTipText("Reload data from file(s)");
        settings.add(restartButton, constraints);

        JLabel linesTableLabel = new JLabel("Plot lines:");
        final DefaultTableModel tableModel = new DefaultTableModel(new String[] { "Name", "Visible" }, 0) {
            Class[] columnClasses = new Class[] { String.class, Boolean.class };


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnClasses[columnIndex];
            }
        };
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if(e.getType() == TableModelEvent.UPDATE) {
                    assert e.getFirstRow() == e.getLastRow();
                    int row = e.getFirstRow();
                    int col = e.getColumn();
                    MultiplexingXYPlotLine pline = plotLines.get(row);
                    Legend legend = frame.getLegend();
                    LegendItem item = legendItems.get(pline);
                    if(col == 0) {
                        String name = (String) tableModel.getValueAt(row, col);
                        item.setDescription(name);
                    } else {
                        assert col == 1;
                        boolean visible = (Boolean) tableModel.getValueAt(row, col);
                        pline.setVisible(visible);
                        if(visible) {
                            legend.add(item);
                        } else {
                            legend.remove(item);
                        }
                        legend.revalidate();
                        resetMinMax();
                    }
                }
            }
        });
        linesTable = new JTable(tableModel);
        linesTableLabel.setLabelFor(linesTable);
        settings.add(linesTableLabel, constraints); // note that we don't use labelConstraints, because it should be on its own line
        JTableHeader linesTableHeader = linesTable.getTableHeader();
        settings.add(linesTableHeader, constraints);
        settings.add(linesTable, constraints);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, settings, content);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(0);
        frame.setContentPane(splitPane);
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setup(content);

        xAxis = (LinearXYAxis) frame.getXAxis();
        yAxis = frame.getYAxis();
        y2Axis = frame.getY2Axis();
        xMarginSpinner.setValue(xAxis.getPreferredSize().height);
        yMarginSpinner.setValue(yAxis.getPreferredSize().width);
        if(useY2) {
            y2MarginSpinner.setValue(y2Axis.getPreferredSize().width);
        }
        metaX.setAxis(xAxis);
        metaY.setAxis(yAxis);
        metaY2.setAxis(y2Axis);
        String axisToolTipText = "Shift-drag to move/translate, ctrl-drag to scale, click and hold to display marker line";
        xAxis.setToolTipText(axisToolTipText);
        yAxis.setToolTipText(axisToolTipText);
        if(useY2) {
            y2Axis.setToolTipText(axisToolTipText);
        }

        MessageFormat innerSlopeFormat = new MessageFormat("<html><b>&Delta;x:</b> {0}  <b>&Delta;y:</b> {1}</html>");
        MessageFormat innerLocationFormat = new MessageFormat("<html><b>X:</b> {0} &nbsp; <b>Y:</b> {1}</html>");
        NumberFormat xFormat = metaX.getFormat();
        if(xFormat != null) {
            innerSlopeFormat.setFormatByArgumentIndex(0, xFormat);
            innerLocationFormat.setFormatByArgumentIndex(0, xFormat);
        }
        NumberFormat yFormat = metaY.getFormat();
        if(yFormat != null) {
            innerSlopeFormat.setFormatByArgumentIndex(1, yFormat);
            innerLocationFormat.setFormatByArgumentIndex(1, yFormat);
        }
        slopeFormat = new XYFormat(innerSlopeFormat);
        locationFormat = new XYFormat(innerLocationFormat);
        frame.getLocationDisplay().setFormat(locationFormat);
        frame.getSlopeLineDisplay().setTextFormat(slopeFormat);

        colors = new Iterator<Color>() {
            Color[] colors = new Color[] { Color.red, Color.green, Color.blue, Color.yellow, Color.orange, Color.cyan,
                    Color.magenta, Color.pink, Color.gray, Color.white };

            int ix;


            @Override
            public boolean hasNext() {
                return true;
            }


            @Override
            public Color next() {
                Color c = colors[ix];
                ix = (ix + 1) % colors.length;
                return c;
            }


            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

        Legend legend = frame.getLegend();
        frame.getToolkit().addAWTEventListener(
                new LegendDragListener(legend, frame.getContents(), frame.getPlot()),
                AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        legend.setToolTipText("Shift-drag to move legend");

        frame.setSize(400, 300);
        frame.setVisible(true);

        for(DataFile dataFile : dataFiles) {
            dataFile.start();
        }
    }


    private NumberFormat parseFormat(String format) throws ParseException {
        NumberFormat fmt;
        if(format.equals("date")) {
            DateFormat f = DateFormat.getDateInstance();
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            fmt = new DateNumberFormat(f);
        } else if(format.startsWith("date,")) {
            SimpleDateFormat f = new SimpleDateFormat(format.substring("date,".length()));
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            fmt = new DateNumberFormat(f);
        } else if(format.equals("time")) {
            DateFormat f = DateFormat.getTimeInstance();
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            fmt = new DateNumberFormat(f);
        } else if(format.startsWith("time,")) {
            SimpleDateFormat f = new SimpleDateFormat(format.substring("time,".length()));
            f.setTimeZone(TimeZone.getTimeZone("GMT"));
            fmt = new DateNumberFormat(f);
        } else if(format.equals("number")) {
            fmt = NumberFormat.getInstance();
        } else if(format.startsWith("number,")) {
            fmt = new DecimalFormat(format.substring("number,".length()));
        } else {
            throw new ParseException("Unrecognized number format: " + format, 0);
        }
        return fmt;
    }


    private int[] parseIntList(String s) {
        String[] data = s.split(",");
        int[] selection = new int[data.length];
        for(int i = 0; i < selection.length; i++) {
            selection[i] = Integer.parseInt(data[i].trim());
        }
        return selection;
    }


    void addPlotLine(String name, MultiplexingXYPlotLine pline, Stroke highlightStroke, Shape highlightPointFill,
            Shape highlightPointOutline) {
        assert SwingUtilities.isEventDispatchThread();
        plotLines.add(pline);

        LegendItem item = frame.addPlotLine(name, pline, highlightStroke, highlightPointFill, highlightPointOutline);
        legendItems.put(pline, item);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;

        ((DefaultTableModel) linesTable.getModel()).addRow(new Object[] { name, true });
    }


    public XYAxis getXAxis() {
        return xAxis;
    }


    public XYAxis getYAxis() {
        return yAxis;
    }


    public XYAxis getY2Axis() {
        return y2Axis;
    }


    public Color nextColor() {
        return colors.next();
    }


    public void resetMinMax() {
        metaY.resetMinMax();
        metaY2.resetMinMax();
        metaX.resetMinMax();
        for(DataFile dataFile : dataFiles) {
            for(Field field : dataFile.getFields()) {
                if(field.isVisible()) {
                    SimpleXYDataset dataset = field.getDataset();
                    if(dataset != null && dataset.getPointCount() > 0) {
                        // It's important to check for an empty data set because
                        // an empty one will return infinities for the min and max.
                        double minX = dataset.getMinX();
                        double maxX = dataset.getMaxX();
                        double minY = dataset.getMinY();
                        double maxY = dataset.getMaxY();
                        metaX.updateMinMax(minX);
                        metaX.updateMinMax(maxX);
                        MetaAxis y = field.isOnY2() ? metaY2 : metaY;
                        y.updateMinMax(minY);
                        y.updateMinMax(maxY);
                    }
                }
            }
        }
        metaX.commitMinMax();
        metaY.commitMinMax();
        metaY2.commitMinMax();
    }


    public void commitMinMax() {
        metaY.commitMinMax();
        metaY2.commitMinMax();
        metaX.commitMinMax();
    }


    public MetaAxis getMetaX() {
        return metaX;
    }


    public MetaAxis getMetaY() {
        return metaY;
    }


    public MetaAxis getMetaY2() {
        return metaY2;
    }


    public boolean isAutorestart() {
        return autorestartCheckbox.isSelected();
    }
}
