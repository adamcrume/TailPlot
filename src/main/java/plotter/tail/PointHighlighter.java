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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import plotter.DoubleData;
import plotter.PlotLine;
import plotter.xy.XYAxis;
import plotter.xy.XYDimension;
import plotter.xy.XYPlot;
import plotter.xy.XYPlotContents;
import plotter.xy.XYPlotLine;

/**
 * Highlights data points the mouse cursor is near and displays their values.
 * @author Adam Crume
 */
public class PointHighlighter extends JComponent implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;

    /** Insets with zero values. */
    private static final Insets emptyInsets = new Insets(0, 0, 0, 0);

    /** Diameter of the circles highlighting data points. */
    private int diameter = 10;

    /** X coordinate of the mouse cursor, or -1 if the mouse is not over the plot. */
    private int x = -1;

    /** Y coordinate of the mouse cursor, or -1 if the mouse is not over the plot. */
    private int y = -1;

    /** Plot contents displaying the point highlighter, or null if the point highlighter is not displayed. */
    private XYPlotContents contents;

    /** Popover that displays the point values. */
    private JComponent display;

    /** Fields to highlight and display. */
    private List<Field> fields = new ArrayList<Field>();

    /** True if the X axis is log-scaled. */
    private boolean logX;

    /** True if the Y axis is log-scaled. */
    private boolean logY;

    /** True if the Y2 axis is log-scaled. */
    private boolean logY2;

    /** Distance between the mouse cursor and the popover. */
    private int distance = 20;

    /** Format for X values. */
    private NumberFormat xFormat = NumberFormat.getInstance();

    /** Format for Y values. */
    private NumberFormat yFormat = NumberFormat.getInstance();

    /** Format for Y2 values. */
    private NumberFormat y2Format = NumberFormat.getInstance();

    /** Preferred size for the line sample for each line. */
    private Dimension lineSamplePreferredSize = new Dimension(20, 10);

    /** Insets for the line sample. */
    private Insets lineSampleInsets = new Insets(0, 0, 0, 5);

    /** Insets for name and X value labels (but not Y label). */
    private Insets labelInsets = new Insets(0, 0, 0, 10);


    /**
     * Creates a point highlighter.
     */
    public PointHighlighter() {
        setLayout(null);
    }


    @Override
    protected void paintComponent(Graphics g) {
        if(contents != null && x != -1) {
            Color savedColor = g.getColor();
            try {
                g.setColor(getForeground());
                for(Field field : fields) {
                    if(field.isVisible()) {
                        XYPlotLine line = field.getPlotLine();
                        Point2D logical = getNearestDataPoint(line, this.x, this.y);
                        if(logical != null) {
                            XYAxis xAxis = line.getXAxis();
                            XYAxis yAxis = line.getYAxis();
                            int xx = (int) SwingUtilities
                                    .convertPoint(xAxis, xAxis.toPhysical(logical.getX()), 0, this).getX();
                            int yy = (int) SwingUtilities
                                    .convertPoint(yAxis, 0, yAxis.toPhysical(logical.getY()), this).getY();
                            g.drawOval(xx - diameter / 2, yy - diameter / 2, diameter, diameter);
                        }
                    }
                }
            } finally {
                g.setColor(savedColor);
            }
        }
    }


    /**
     * Result is *not* adjusted for log scale.
     * Returns null if there is no data.
     */
    private Point2D getNearestDataPoint(XYPlotLine line, int x, int y) {
        XYAxis xAxis = line.getXAxis();
        XYAxis yAxis = line.getYAxis();
        double logicalX = xAxis.toLogical((int) SwingUtilities.convertPoint(this, x, 0, xAxis).getX());
        double logicalY = yAxis.toLogical((int) SwingUtilities.convertPoint(this, 0, y, yAxis).getY());
        assert line.getIndependentDimension() == XYDimension.X;
        DoubleData xData = line.getXData();
        DoubleData yData = line.getYData();
        if(xData.getLength() == 0) {
            return null;
        }
        int ix = xData.binarySearch(logicalX);
        if(ix < 0) {
            ix = -ix - 1;
        }
        if(ix == xData.getLength()) {
            ix--;
        }
        if(ix > 0 && Math.abs(logicalX - xData.get(ix - 1)) < Math.abs(logicalX - xData.get(ix))) {
            ix--;
        }
        logicalX = xData.get(ix);
        logicalY = yData.get(ix);
        return new Point2D.Double(logicalX, logicalY);
    }


    @Override
    public void mouseClicked(MouseEvent e) {
        // ignore
    }


    @Override
    public void mouseEntered(MouseEvent e) {
        contents = (XYPlotContents) e.getSource();
        contents.add(this, 0);
        contents.revalidate();
        display = new JPanel();
        display.setLayout(new GridBagLayout());
        display.setBackground(Color.black);
        display.setOpaque(true);
        Border outsideBorder = BorderFactory.createLineBorder(Color.DARK_GRAY);
        Border insideBorder = BorderFactory.createLineBorder(Color.black, 5);
        Border border = BorderFactory.createCompoundBorder(outsideBorder, insideBorder);
        display.setBorder(border);
        add(display);
        revalidate();
        x = e.getX();
        y = e.getY();
        updateDisplay();
    }


    @Override
    public void mouseExited(MouseEvent e) {
        x = -1;
        y = -1;
        if(contents != null) {
            contents.remove(this);
            contents.revalidate();
            contents.repaint(); // TODO: Repaint only what's necessary
        }
        contents = null;
        remove(display);
        revalidate();
    }


    @Override
    public void mousePressed(MouseEvent e) {
        // ignore
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        // ignore
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        x = e.getX();
        y = e.getY();
        updateDisplay();
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        x = e.getX();
        y = e.getY();
        updateDisplay();
    }


    /**
     * Updates the display.
     */
    void updateDisplay() {
        if(x == -1) {
            return;
        }

        // TODO: optimize
        display.removeAll();
        Color foreground = getForeground();
        Font font = getFont();
        for(Field field : fields) {
            if(field.isVisible()) {
                XYPlotLine line = field.getPlotLine();
                Point2D logical = getNearestDataPoint(line, this.x, this.y);
                if(logical != null) {
                    double logicalX = logical.getX();
                    double logicalY = logical.getY();
                    if(logX) {
                        logicalX = Math.pow(10, logicalX);
                    }
                    if(field.isOnY2() ? logY2 : logY) {
                        logicalY = Math.pow(10, logicalY);
                    }
                    NumberFormat xFormat = this.xFormat;
                    NumberFormat yFormat = field.isOnY2() ? this.y2Format : this.yFormat;
                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.insets = lineSampleInsets;
                    constraints.anchor = GridBagConstraints.WEST;
                    constraints.fill = GridBagConstraints.BOTH;
                    LineSample sample = new LineSample(line);
                    sample.setPreferredSize(lineSamplePreferredSize);
                    display.add(sample, constraints);
                    JLabel nameLabel = new JLabel(field.getName());
                    nameLabel.setForeground(foreground);
                    nameLabel.setFont(font);
                    constraints.insets = labelInsets;
                    constraints.fill = GridBagConstraints.NONE;
                    display.add(nameLabel, constraints);
                    JLabel xLabel = new JLabel(xFormat.format(logicalX));
                    xLabel.setForeground(foreground);
                    xLabel.setFont(font);
                    display.add(xLabel, constraints);
                    JLabel yLabel = new JLabel(yFormat.format(logicalY));
                    yLabel.setForeground(foreground);
                    yLabel.setFont(font);
                    constraints.gridwidth = GridBagConstraints.REMAINDER;
                    constraints.insets = emptyInsets;
                    display.add(yLabel, constraints);
                }
            }
        }

        Dimension preferredSize = display.getPreferredSize();
        int px = x + distance;
        int py = y + distance;
        double dispWidth = preferredSize.getWidth();
        double dispHeight = preferredSize.getHeight();
        // keep the display from running off the right
        if(px + dispWidth > getWidth()) {
            px = (int) (getWidth() - dispWidth);
        }
        // keep the display from running off the bottom
        if(py + dispHeight > getHeight()) {
            py = (int) (getHeight() - dispHeight);
        }
        // keep the display out from under the mouse cursor
        if(x >= px && x <= px + dispWidth && y >= py && y <= py + dispHeight) {
            px = (int) (x - dispWidth - distance);
            py = (int) (y - dispHeight - distance);
        }
        display.setLocation(px, py);
        display.setSize(preferredSize);
        display.revalidate();
        repaint();
    }


    /**
     * Attaches this point highlighter to a plot.
     * @param plot plot to attach to
     */
    public void attach(XYPlot plot) {
        XYPlotContents contents = plot.getContents();
        if(contents == null) {
            throw new IllegalArgumentException("Plot does not contain an XYPlotContents component");
        }
        contents.addMouseListener(this);
        contents.addMouseMotionListener(this);
    }


    /**
     * Adds a field to highlight and display.
     * @param field new field to process
     */
    public void addField(Field field) {
        fields.add(field);
    }


    /**
     * Sets whether or not the X axis is log-scaled.
     * @param logscale true if the X axis is log-scaled
     */
    public void setLogX(boolean logscale) {
        this.logX = logscale;
    }


    /**
     * Sets whether or not the Y axis is log-scaled.
     * @param logscale true if the Y axis is log-scaled
     */
    public void setLogY(boolean logscale) {
        this.logY = logscale;
    }


    /**
     * Sets whether or not the Y2 axis is log-scaled.
     * @param logscale true if the Y2 axis is log-scaled
     */
    public void setLogY2(boolean logscale) {
        this.logY2 = logscale;
    }


    /**
     * Returns the distance between the mouse cursor and the popover.
     * @return the distance between the mouse cursor and the popover
     */
    public int getDistance() {
        return distance;
    }


    /**
     * Sets the distance between the mouse cursor and the popover.
     * @param distance the distance between the mouse cursor and the popover
     */
    public void setDistance(int distance) {
        this.distance = distance;
    }


    /**
     * Returns the diameter of the circles highlighting data points.
     * @return the diameter of the circles highlighting data points
     */
    public int getDiameter() {
        return diameter;
    }


    /**
     * Sets the diameter of the circles highlighting data points.
     * @param diameter the diameter of the circles highlighting data points
     */
    public void setDiameter(int diameter) {
        this.diameter = diameter;
    }


    /**
     * Returns the format for X values.
     * @return the format for X values
     */
    public NumberFormat getXFormat() {
        return xFormat;
    }


    /**
     * Sets the format for X values.
     * @param xFormat the format for X values
     */
    public void setXFormat(NumberFormat xFormat) {
        this.xFormat = xFormat;
    }


    /**
     * Returns the format for Y values.
     * @return the format for Y values
     */
    public NumberFormat getYFormat() {
        return yFormat;
    }


    /**
     * Sets the format for Y values.
     * @param yFormat the format for Y values
     */
    public void setYFormat(NumberFormat yFormat) {
        this.yFormat = yFormat;
    }


    /**
     * Returns the format for Y2 values.
     * @return the format for Y2 values
     */
    public NumberFormat getY2Format() {
        return y2Format;
    }


    /**
     * Sets the format for Y2 values.
     * @param y2Format the format for Y2 values
     */
    public void setY2Format(NumberFormat y2Format) {
        this.y2Format = y2Format;
    }


    // TODO: Merge with LegendItem.LineSample
    /**
     * Displays a sample of the plot line.
     */
    private static class LineSample extends JComponent {
        private static final long serialVersionUID = 1L;

        private final PlotLine line;


        public LineSample(PlotLine line) {
            this.line = line;
        }


        @Override
        public void paint(Graphics g) {
            Stroke stroke = line.getStroke();
            Graphics2D g2 = (Graphics2D)g;
            if(stroke != null) {
                g2.setStroke(stroke);
            }
            g.setColor(line.getForeground());
            int x = getWidth();
            int y = getHeight() / 2;
            g.drawLine(0, y, x, y);
            g.translate(x / 2, y);
            Shape pointOutline = line.getPointOutline();
            if(pointOutline != null) {
                 g2.draw(pointOutline);
            }
            Shape pointFill = line.getPointFill();
            if(pointFill != null) {
                 g2.fill(pointFill);
            }
        }
    }
}
