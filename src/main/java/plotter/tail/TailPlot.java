package plotter.tail;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plotter.DateNumberFormat;
import plotter.DoubleData;
import plotter.xy.LinearXYAxis;
import plotter.xy.SimpleXYDataset;
import plotter.xy.XYAxis;
import plotter.xy.XYDimension;

public class TailPlot {
    private File file;

    private List<Field> fields = new ArrayList<Field>();

    private Map<Integer, NumberFormat> fieldFormats = new HashMap<Integer, NumberFormat>();

    private Pattern fieldSeparator = Pattern.compile("[,\t ]+");

    private int[] selection;

    private int[] y2;

    private BitSet y2PostSelection = new BitSet();

    private int x = -1;

    private NumberFormat xInputFormat = NumberFormat.getInstance();

    private XYFormat slopeFormat;

    private XYFormat locationFormat;

    private int minFieldCount;

    private XYPlotFrame frame;

    private XYAxis xAxis;

    private XYAxis yAxis;

    private XYAxis y2Axis;

    private Iterator<Color> colors;

    private int points;

    private MetaAxis metaX = new MetaAxis() {
        public List<DoubleData> getDatasets() {
            List<DoubleData> datasets = new ArrayList<DoubleData>();
            for(Field f : fields) {
                datasets.add(f.dataset.getXData());
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
            for(Field f : fields) {
                if(!f.onY2) {
                    datasets.add(f.dataset.getYData());
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
            for(Field f : fields) {
                if(!f.onY2) {
                    datasets.add(f.dataset.getYData());
                }
            }
            return datasets;
        }


        @Override
        protected void logscaleUpdated(boolean logscale) {
            frame.getPlot().repaint();
        }
    };

    private boolean firstLineRead;

    private boolean restart;


    public static void main(String[] args) {
        try {
            new TailPlot().run(args);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }


    private void usage(String msg) {
        if(msg != null) {
            System.err.println(msg);
        }
        System.err.println("Usage: TailPlot [OPTION]... [FILE]");
        System.err.println("Plots a file, displaying new data as it is generated (analogous to 'tail -f')");
        System.err.println("If no file is specified, standard input is read.");
        System.err.println();
        System.err.println("Options:");
        System.err.println("  -F, --field-separator=REGEX   set the field separator regex (default: [,\\t ]+)");
        System.err.println("  -f, --fields=FIELDS           field names (separated by the field separator)");
        System.err.println("  -s, --select=FIELDS           comma-separated list of field indices to plot (1-based)");        
        System.err.println("      --y2=FIELDS               comma-separated list of field indices to place on the Y2 axis (1-based)");
        System.err.println("  -x, --x=INDEX                 index of field to use as X value. Note that X values must be monotonically increasing. (1-based, default: line number is X value)");
        System.err.println("      --field-format=FIELD,FMT  input format of a field. Example: 1,time,YYY-MM-dd_HH:mm:ss to read field 1 as a timestamp (default: number)");
        System.err.println("      --x-format=FMT            display format of the X axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("      --y-format=FMT            display format of the Y axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("      --y2-format=FMT           display format of the Y2 axis. Example: time,YYY-MM-dd_HH:mm:ss to display as a timestamp (default: number)");
        System.err.println("  -h, --header-line             use the first line as a header line");
        System.err.println("  -t, --title=TITLE             set the window title (defaults to the file name)");
        System.err.println("      --scroll-width=AMT        amount of data to keep on screen (in X axis units)");
        System.err.println("      --help                    display this message");
        System.err.println();
        System.err.println("Notes:");
        System.err.println("  If both --fields and --header-line are specified, the first line is skipped, and field names are taken from --fields.");
        System.err.println();
        System.err.println("Examples:");
        System.err.println("  Plot the first and third fields, with the third on the Y2 axis");
        System.err.println("    TailPlot --select=1,3 --y2=3 file");
        System.exit(1);
    }


    public void run(String[] args) throws IOException {
        boolean headerLine = false;
        String fieldString = null;
        String title = null;
        String scrollWidthString = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-F")) {
                fieldSeparator = Pattern.compile(args[++i]);
            } else if(args[i].startsWith("--field-separator=")) {
                fieldSeparator = Pattern.compile(args[i].substring("--field-separator=".length()));
            } else if(args[i].equals("-f")) {
                fieldString = args[++i];
            } else if(args[i].startsWith("--fields=")) {
                fieldString = args[i].substring("--fields=".length());
            } else if(args[i].equals("-s")) {
                selection = parseIntList(args[++i]);
            } else if(args[i].startsWith("--select=")) {
                selection = parseIntList(args[i].substring("--select=".length()));
            } else if(args[i].startsWith("--y2=")) {
                y2 = parseIntList(args[i].substring("--y2=".length()));
            } else if(args[i].startsWith("-x")) {
                x = Integer.parseInt(args[++i]);
            } else if(args[i].startsWith("--x=")) {
                x = Integer.parseInt(args[i].substring("--x=".length()));
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
                fieldFormats.put(fieldIx, fmt);
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
                headerLine = true;
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
                if(file != null) {
                    usage("Only one file may be specified");
                }
                file = new File(args[i]);
            }
        }
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
                        usage("Field specified in --y2 (" + ix + ") not present in --select");
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
                    usage("Number of fields selected with --select does not match number of labels given with --fields");
                }
            }
        }
        if(selection != null) {
            for(int i : selection) {
                minFieldCount = Math.max(minFieldCount, i);
            }
        }
        if(title == null) {
            if(file == null) {
                title = "<standard input>";
            } else {
                title = file.getPath();
            }
        }
        if(x != -1) {
            NumberFormat format = fieldFormats.get(x);
            xInputFormat = format == null ? NumberFormat.getInstance() : format;
        }
        if(scrollWidthString != null) {
            try {
                metaX.setScrollWidth(xInputFormat.parse(scrollWidthString).doubleValue());
            } catch(ParseException e) {
                System.err.println("Invalid X value for scroll width: " + scrollWidthString);
            }
        }
        boolean restartable = file != null;

        frame = new XYPlotFrame();
        frame.setUseY2(y2 != null);
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
        if(y2 != null) {
            settings.add(metaY2.createAutoscaleCheckbox("Auto-scale Y2 axis"), constraints);
        }
        settings.add(metaX.createLogscaleCheckbox("Logarithmic X axis"), constraints);
        settings.add(metaY.createLogscaleCheckbox("Logarithmic Y axis"), constraints);
        if(y2 != null) {
            settings.add(metaY2.createLogscaleCheckbox("Logarithmic Y2 axis"), constraints);
        }
        final JCheckBox autorestartCheckbox = new JCheckBox("Auto-restart if file shrinks");
        autorestartCheckbox.setSelected(restartable);
        autorestartCheckbox.setEnabled(restartable);
        settings.add(autorestartCheckbox, constraints);

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
        yMarginLabel.setLabelFor(yMarginSpinner);
        settings.add(yMarginLabel, labelConstraints);
        settings.add(yMarginSpinner, constraints);

        final JSpinner y2MarginSpinner;
        if(y2 == null) {
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
            y2MarginLabel.setLabelFor(y2MarginSpinner);
            settings.add(y2MarginLabel, labelConstraints);
            settings.add(y2MarginSpinner, constraints);
        }

        final JButton restartButton = new JButton("Restart");
        restartButton.setEnabled(restartable);
        restartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                restart();
            }
        });
        settings.add(restartButton, constraints);
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
        if(y2 != null) {
            y2MarginSpinner.setValue(y2Axis.getPreferredSize().width);
        }
        metaX.setAxis(xAxis);
        metaY.setAxis(yAxis);
        metaY2.setAxis(y2Axis);

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

        frame.setSize(400, 300);
        frame.setVisible(true);

        while(true) {
            BufferedReader in;
            if(file == null) {
                in = new BufferedReader(new InputStreamReader(System.in));
            } else {
                in = new BufferedReader(new FileReader(file));
            }
            try {
                for(Field f : fields) {
                    if(f.dataset != null) {
                        f.dataset.removeAllPoints();
                    }
                }
                points = 0;
                metaY.resetMinMax();
                metaY2.resetMinMax();
                metaX.resetMinMax();

                int lineNumber = 0;
                final List<double[]> buffer = new ArrayList<double[]>();
                long oldFileSize = 0;
                while(true) {
                    if(file != null && autorestartCheckbox.isSelected()) {
                        long fileSize = file.length();
                        if(fileSize < oldFileSize) {
                            restart();
                        }
                        oldFileSize = fileSize;
                    }
                    synchronized(this) {
                        if(restart) {
                            restart = false;
                            break;
                        }
                    }
                    String line = in.readLine();
                    if(line == null) {
                        if(file == null) {
                            break;
                        }
                        try {
                            Thread.sleep(100);
                        } catch(InterruptedException e) {
                        }
                        continue;
                    }
                    lineNumber++;

                    final double[] ddata = processLine(headerLine, lineNumber, line);

                    if(ddata == null) {
                        continue;
                    }

                    synchronized(buffer) {
                        boolean empty = buffer.isEmpty();
                        buffer.add(ddata);
                        if(empty) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    synchronized(buffer) {
                                        for(double[] ddata : buffer) {
                                            double xVal = ddata[0];
                                            if(metaX.isLogscale()) {
                                                xVal = Math.log10(xVal);
                                            }
                                            for(int i = 1; i < ddata.length; i++) {
                                                double val = ddata[i];
                                                Field field = fields.get(i - 1);
                                                MetaAxis fieldY;
                                                if(field.onY2) {
                                                    fieldY = metaY2;
                                                } else {
                                                    fieldY = metaY;
                                                }
                                                if(fieldY.isLogscale()) {
                                                    val = Math.log10(val);
                                                }
                                                fieldY.updateMinMax(val);
                                                field.dataset.add(xVal, val);
                                            }
                                            metaX.updateMinMax(xVal);
                                        }
                                        buffer.clear();
                                    }
                                    metaY.commitMinMax();
                                    metaY2.commitMinMax();
                                    metaX.commitMinMax();
                                }
                            });
                        }
                    }
                }
            } finally {
                in.close();
            }
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


    protected void restart() {
        synchronized(this) {
            restart = true;
        }
    }


    private double[] processLine(boolean headerLine, int lineNumber, String line) {
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

            if(fields.get(0).format == null) {
                assert fields.size() == selection.length;
                for(int i = 0; i < selection.length; i++) {
                    NumberFormat format = fieldFormats.get(selection[i]);
                    fields.get(i).format = format == null ? NumberFormat.getInstance() : format;
                }
            }

            for(Field f : fields) {
                final MultiplexingXYPlotLine pline = new MultiplexingXYPlotLine(xAxis, f.onY2 ? y2Axis : yAxis, XYDimension.X);
                Stroke highlightStroke = new BasicStroke(3);
                Shape highlightPointFill = null;
                Shape highlightPointOutline = null;
                pline.setForeground(colors.next());
                SimpleXYDataset dataset = new SimpleXYDataset(pline);
                dataset.setXData(pline.getXData());
                dataset.setYData(pline.getYData());
                frame.addPlotLine(f.name, pline, highlightStroke, highlightPointFill, highlightPointOutline);
                f.dataset = dataset;
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
                ddata[i + 1] = fields.get(i).format.parse(data2[i]).doubleValue();
            } catch(ParseException e) {
                System.err.println("Invalid value on line " + lineNumber + " for \"" + fields.get(i).name
                        + "\": " + data2[i]);
                ddata[i + 1] = Double.NaN;
            }
        }
        points++;
        return ddata;
    }


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


    private int[] parseIntList(String s) {
        String[] data = s.split(",");
        int[] selection = new int[data.length];
        for(int i = 0; i < selection.length; i++) {
            selection[i] = Integer.parseInt(data[i].trim());
        }
        return selection;
    }


    private class Field {
        public SimpleXYDataset dataset;

        private String name;

        private boolean onY2;

        public NumberFormat format;


        public Field(String name, boolean onY2) {
            this.name = name;
            this.onY2 = onY2;
        }
    }
}
