package plotter.tail;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

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

    private Pattern fieldSeparator = Pattern.compile("[,\t]");

    private int[] selection;

    private int minFieldCount;


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
        System.err.println("If no file is specified, standard input is read.");
        System.err.println("Options:");
        System.err.println("  -F, --field-separator=REGEX   set the field separator regex");
        System.err.println("      --fields=FIELDS           field names (separated by the field separator)");
        System.err.println("  -s, --select=FIELDS           comma-separated list of field indices to plot (1-based)");
        System.err.println("  -h, --header-line             use the first line as a header line");
        System.err.println("      --help                    display this message");
        System.exit(1);
    }


    public void run(String[] args) throws IOException {
        boolean headerLine = false;
        String fieldString = null;
        for(int i = 0; i < args.length; i++) {
            if(args[i].equals("-F")) {
                fieldSeparator = Pattern.compile(args[++i]);
            } else if(args[i].startsWith("--field-separator=")) {
                fieldSeparator = Pattern.compile(args[i].substring("--field-separator=".length()));
            } else if(args[i].startsWith("--fields=")) {
                fieldString = args[i].substring("--fields=".length());
            } else if(args[i].equals("-s")) {
                selection = parseIntList(args[++i]);
            } else if(args[i].startsWith("--select=")) {
                selection = parseIntList(args[i].substring("--select=".length()));
            } else if(args[i].equals("--header-line") || args[i].equals("-h")) {
                headerLine = true;
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
        if(headerLine && fieldString != null) {
            usage("Cannot specify both --header-line and --field-separator");
        }
        if(fieldString != null) {
            for(String name : fieldSeparator.split(fieldString)) {
                fields.add(new Field(name));
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

        XYPlotFrame frame = new XYPlotFrame();
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
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, settings, content);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(0);
        frame.setContentPane(splitPane);
        if(file == null) {
            frame.setTitle("<standard input>");
        } else {
            frame.setTitle(file.getPath());
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setup(content);

        final XYAxis xAxis = (LinearXYAxis) frame.getXAxis();
        final XYAxis yAxis = frame.getYAxis();

        Iterator<Color> colors = new Iterator<Color>() {
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

        BufferedReader in;
        if(file == null) {
            in = new BufferedReader(new InputStreamReader(System.in));
        } else {
            in = new BufferedReader(new FileReader(file));
        }
        try {
            yAxis.setStart(0);
            yAxis.setEnd(1);
            xAxis.setStart(0);
            xAxis.setEnd(1);

            frame.setSize(400, 300);
            frame.setVisible(true);

            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            int lineNumber = 0;
            int points = 0;
            while(true) {
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

                String[] data = fieldSeparator.split(line);
                if(lineNumber == 1) {
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
                            fields.add(new Field(name));
                        }
                    }
                    for(Field f : fields) {
                        final LinearXYPlotLine pline = new LinearXYPlotLine(xAxis, yAxis, XYDimension.X);
                        pline.setForeground(colors.next());
                        SimpleXYDataset dataset = new SimpleXYDataset(pline);
                        dataset.setXData(pline.getXData());
                        dataset.setYData(pline.getYData());
                        frame.addPlotLine(f.name, pline);
                        f.dataset = dataset;
                    }
                    if(headerLine) {
                        continue;
                    }
                }

                data = select(data, lineNumber);
                if(data == null) {
                    continue;
                }

                final double[] ddata = new double[data.length];
                for(int i = 0; i < data.length; i++) {
                    Field f = fields.get(i);
                    try {
                        double val = Double.parseDouble(data[i]);
                        if(val < min) {
                            min = val;
                        }
                        if(val > max) {
                            max = val;
                        }
                        ddata[i] = val;
                    } catch(NumberFormatException e) {
                        System.err.println("Invalid value on line " + lineNumber + " for \"" + fields.get(i).name
                                + "\": " + data[i]);
                        ddata[i] = Double.NaN;
                    }
                }

                final int _points = points;
                final double _min = min;
                final double _max = max;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        for(int i = 0; i < ddata.length; i++) {
                            fields.get(i).dataset.add(_points, ddata[i]);
                        }
                        if(_min != Double.POSITIVE_INFINITY && autoScaleY.isSelected()) {
                            double margin = .1 * (_max - _min);
                            yAxis.setStart(_min - margin);
                            yAxis.setEnd(_max + margin);
                        }
                        if(autoScaleX.isSelected()) {
                            xAxis.setEnd(_points);
                        }
                    }
                });
                points++;
            }
        } finally {
            in.close();
        }
    }


    private String[] select(String[] data, int lineNumber) {
        if(data.length < minFieldCount) {
            System.err.println("Expected at least " + minFieldCount + " fields, but saw " + data.length + " on line "
                    + lineNumber);
            return null;
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


        public Field(String name) {
            this.name = name;
        }
    }
}
