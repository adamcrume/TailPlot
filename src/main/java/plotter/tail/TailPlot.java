package plotter.tail;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import plotter.xy.LinearXYAxis;
import plotter.xy.LinearXYPlotLine;
import plotter.xy.SimpleXYDataset;
import plotter.xy.XYAxis;
import plotter.xy.XYDimension;

public class TailPlot {
    private File file;

    private List<Field> fields = new ArrayList<Field>();

    private Pattern fieldSeparator = Pattern.compile("[,\t ]+");

    private int[] selection;

    private int[] y2;

    private BitSet y2PostSelection = new BitSet();

    private int minFieldCount;

    private XYPlotFrame frame;

    private XYAxis xAxis;

    private XYAxis yAxis;

    private XYAxis y2Axis;

    private Iterator<Color> colors;

    // Only access from the Swing thread
    private int points;

    // Only access from the Swing thread
    private double min;

    // Only access from the Swing thread
    private double max;

    // Only access from the Swing thread
    private double min2;

    // Only access from the Swing thread
    private double max2;

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
        System.err.println("  -h, --header-line             use the first line as a header line");
        System.err.println("  -t, --title=TITLE             set the window title (defaults to the file name)");
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
            } else if(args[i].equals("--header-line") || args[i].equals("-h")) {
                headerLine = true;
            } else if(args[i].equals("-t")) {
                title = args[++i];
            } else if(args[i].startsWith("--title=")) {
                title = args[i].substring("--title=".length());
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
        boolean restartable = file != null;

        frame = new XYPlotFrame();
        frame.setUseY2(y2 != null);
        frame.setUseLegend(true);
        JPanel content = new JPanel();
        JPanel settings = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        final JCheckBox autoScaleX = new JCheckBox("Auto-scale X axis");
        autoScaleX.setSelected(true);
        settings.add(autoScaleX, constraints);
        final JCheckBox autoScaleY = new JCheckBox("Auto-scale Y axis");
        autoScaleY.setSelected(true);
        settings.add(autoScaleY, constraints);
        final JCheckBox autoScaleY2;
        if(y2 != null) {
            autoScaleY2 = new JCheckBox("Auto-scale Y2 axis");
            autoScaleY2.setSelected(true);
            settings.add(autoScaleY2, constraints);
        } else {
            autoScaleY2 = null;
        }
        final JCheckBox autorestartCheckbox = new JCheckBox("Auto-restart if file shrinks");
        autorestartCheckbox.setSelected(restartable);
        autorestartCheckbox.setEnabled(restartable);
        settings.add(autorestartCheckbox, constraints);
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
                if(autoScaleY.isSelected()) {
                    yAxis.setStart(0);
                    yAxis.setEnd(1);
                }
                if(autoScaleX.isSelected()) {
                    xAxis.setStart(0);
                    xAxis.setEnd(1);
                }
                if(y2Axis != null && autoScaleY2.isSelected()) {
                    y2Axis.setStart(0);
                    y2Axis.setEnd(1);
                }

                min = Double.POSITIVE_INFINITY;
                max = Double.NEGATIVE_INFINITY;
                min2 = Double.POSITIVE_INFINITY;
                max2 = Double.NEGATIVE_INFINITY;

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
                                            for(int i = 0; i < ddata.length; i++) {
                                                double val = ddata[i];
                                                Field field = fields.get(i);
                                                if(field.onY2) {
                                                    if(val < min2) {
                                                        min2 = val;
                                                    }
                                                    if(val > max2) {
                                                        max2 = val;
                                                    }
                                                } else {
                                                    if(val < min) {
                                                        min = val;
                                                    }
                                                    if(val > max) {
                                                        max = val;
                                                    }
                                                }
                                                field.dataset.add(points, val);
                                            }
                                            points++;
                                        }
                                        buffer.clear();
                                    }
                                    if(min != Double.POSITIVE_INFINITY && autoScaleY.isSelected()) {
                                        double margin = .1 * (max - min);
                                        yAxis.setStart(min - margin);
                                        yAxis.setEnd(max + margin);
                                    }
                                    if(min2 != Double.POSITIVE_INFINITY && autoScaleY2.isSelected()) {
                                        double margin = .1 * (max2 - min2);
                                        y2Axis.setStart(min2 - margin);
                                        y2Axis.setEnd(max2 + margin);
                                    }
                                    if(autoScaleX.isSelected()) {
                                        xAxis.setEnd(points);
                                    }
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
                    } else if(selection != null) {
                        name = "Column " + selection[i];
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
            for(Field f : fields) {
                final LinearXYPlotLine pline = new LinearXYPlotLine(xAxis, f.onY2 ? y2Axis : yAxis, XYDimension.X);
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

        data = select(data, lineNumber);
        if(data == null) {
            return null;
        }

        final double[] ddata = new double[data.length];
        for(int i = 0; i < data.length; i++) {
            try {
                ddata[i] = Double.parseDouble(data[i]);
            } catch(NumberFormatException e) {
                System.err.println("Invalid value on line " + lineNumber + " for \"" + fields.get(i).name
                        + "\": " + data[i]);
                ddata[i] = Double.NaN;
            }
        }
        return ddata;
    }


    private String[] select(String[] data, int lineNumber) {
        if(data.length < minFieldCount) {
            System.err.println("Expected at least " + minFieldCount + " fields, but saw " + data.length + " on line "
                    + lineNumber);
            return null;
        }
        if(selection == null) {
            return data;
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


        public Field(String name, boolean onY2) {
            this.name = name;
            this.onY2 = onY2;
        }
    }
}
