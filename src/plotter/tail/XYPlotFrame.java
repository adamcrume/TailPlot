package plotter.tail;
/*******************************************************************************
 * Mission Control Technologies, Copyright (c) 2009-2012, United States Government
 * as represented by the Administrator of the National Aeronautics and Space 
 * Administration. All rights reserved.
 *
 * The MCT platform is licensed under the Apache License, Version 2.0 (the 
 * "License"); you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations under 
 * the License.
 *
 * MCT includes source code licensed under additional open source licenses. See 
 * the MCT Open Source Licenses file included with this distribution or the About 
 * MCT Licenses dialog available at runtime from the MCT Help menu for additional 
 * information. 
 *******************************************************************************/


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.MessageFormat;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SpringLayout;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import plotter.xy.DefaultXYLayoutGenerator;
import plotter.xy.LinearXYAxis;
import plotter.xy.SlopeLine;
import plotter.xy.SlopeLineDisplay;
import plotter.xy.XYAxis;
import plotter.xy.XYDimension;
import plotter.xy.XYGrid;
import plotter.xy.XYLocationDisplay;
import plotter.xy.XYMarkerLine;
import plotter.xy.XYPlot;
import plotter.xy.XYPlotContents;
import plotter.xy.XYPlotLine;

public class XYPlotFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private XYPlot plot;

	private XYAxis xAxis;

	private XYAxis yAxis;

	private XYGrid grid;

	private XYPlotContents contents;

	private XYLocationDisplay locationDisplay;

	private SlopeLineDisplay slopeLineDisplay;

    private Legend legend;


	public void setup() {
	    setup(getContentPane());
	}


	public void setup(Container contentPane) {
		plot = new XYPlot();
		xAxis = createXAxis();
		yAxis = createYAxis();
		xAxis.setPreferredSize(new Dimension(1, 40));
		yAxis.setPreferredSize(new Dimension(40, 1));
		xAxis.setForeground(Color.white);
		yAxis.setForeground(Color.white);
		xAxis.setTextMargin(10);
		yAxis.setTextMargin(10);
		plot.add(xAxis);
		plot.add(yAxis);
		plot.setXAxis(xAxis);
		plot.setYAxis(yAxis);
		plot.setBackground(Color.darkGray);
		contents = new XYPlotContents();
		contents.setBackground(Color.black);
		plot.setBackground(Color.darkGray);
		grid = new XYGrid(xAxis, yAxis);
		grid.setForeground(Color.lightGray);
		contents.add(grid);
		plot.add(contents);
		plot.setPreferredSize(new Dimension(150, 100));

		contentPane.setBackground(Color.darkGray);

		locationDisplay = new XYLocationDisplay();
		// This is a hack to set the preferred height to the normal height so the component doesn't collapse to height 0 when the text is empty.
		// Note that mimimumSize does not work for some reason.
		locationDisplay.setText("Ag");
		Dimension size = locationDisplay.getPreferredSize();
		size.width = 100;
		locationDisplay.setText("");
		locationDisplay.setPreferredSize(size);
		// End hack
		locationDisplay.setForeground(Color.white);
		locationDisplay.setFont(new Font("Arial", 0, 12));
		locationDisplay.setFormat(new MessageFormat("<html><b>X:</b> {0} &nbsp; <b>Y:</b> {1}</html>"));
		locationDisplay.attach(plot);
		plot.add(locationDisplay);

		SlopeLine slopeLine = new SlopeLine();
		slopeLine.setForeground(Color.white);
		slopeLine.attach(plot);

		slopeLineDisplay = new SlopeLineDisplay();
		slopeLine.addListenerForPlot(plot, slopeLineDisplay);
		slopeLineDisplay.setFont(new Font("Arial", 0, 12));
		slopeLineDisplay.setForeground(Color.white);
		slopeLineDisplay.setFormat(new MessageFormat("<html><b>&Delta;x:</b> {0}  <b>&Delta;y:</b> {1}</html>"));
		plot.add(slopeLineDisplay);

        legend = new Legend();
        plot.add(legend, 0);

        new DefaultXYLayoutGenerator().generateLayout(plot);

		SpringLayout layout2 = (SpringLayout) plot.getLayout();
		layout2.putConstraint(SpringLayout.NORTH, locationDisplay, 0, SpringLayout.NORTH, plot);
		layout2.putConstraint(SpringLayout.WEST, locationDisplay, 0, SpringLayout.WEST, contents);
		layout2.putConstraint(SpringLayout.EAST, locationDisplay, 0, SpringLayout.HORIZONTAL_CENTER, contents);
		layout2.putConstraint(SpringLayout.NORTH, contents, 0, SpringLayout.SOUTH, locationDisplay);
		layout2.putConstraint(SpringLayout.NORTH, slopeLineDisplay, 0, SpringLayout.NORTH, plot);
		layout2.putConstraint(SpringLayout.WEST, slopeLineDisplay, 0, SpringLayout.HORIZONTAL_CENTER, contents);
		layout2.putConstraint(SpringLayout.EAST, slopeLineDisplay, 0, SpringLayout.EAST, contents);
        layout2.putConstraint(SpringLayout.NORTH, legend, 0, SpringLayout.NORTH, contents);
        layout2.putConstraint(SpringLayout.EAST, legend, 0, SpringLayout.EAST, contents);
		yAxis.setEndMargin((int) locationDisplay.getPreferredSize().getHeight());

		SpringLayout layout = new SpringLayout();
		contentPane.setLayout(layout);
		layout.putConstraint(SpringLayout.NORTH, plot, 0, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, contentPane, 0, SpringLayout.SOUTH, plot);
		layout.putConstraint(SpringLayout.EAST, contentPane, 0, SpringLayout.EAST, plot);
		contentPane.add(plot);

        new MarkerListener().attach(contents, xAxis);
        new MarkerListener().attach(contents, yAxis);
        new DragListener().attach(xAxis);
        new DragListener().attach(yAxis);
        new ScaleListener().attach(xAxis);
        new ScaleListener().attach(yAxis);
    }


	protected XYAxis createYAxis() {
		return new LinearXYAxis(XYDimension.Y);
	}


	protected XYAxis createXAxis() {
		return new LinearXYAxis(XYDimension.X);
	}


    public void addPlotLine(JComponent plotLine) {
        addPlotLine(null, (XYPlotLine) plotLine);
    }


    public void addPlotLine(String name, XYPlotLine plotLine) {
        contents.add(plotLine);
        contents.setComponentZOrder(grid, contents.getComponentCount() - 1);
        if(name != null) {
            legend.addLine(name, plotLine);
        }
    }


	public XYAxis getXAxis() {
		return xAxis;
	}


	public XYAxis getYAxis() {
		return yAxis;
	}


	public XYPlotContents getContents() {
		return contents;
	}


	public XYLocationDisplay getLocationDisplay() {
		return locationDisplay;
	}


	public SlopeLineDisplay getSlopeLineDisplay() {
		return slopeLineDisplay;
	}


	public XYPlot getPlot() {
		return plot;
	}


    private static class MarkerListener implements MouseListener, MouseMotionListener {
        private static final int MASK = InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
                | InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        XYPlotContents contents;

        XYAxis axis;

        XYMarkerLine markerLine;

        boolean isy;
        

        public void attach(XYPlotContents contents, XYAxis axis) {
            if(this.axis != null) {
                throw new IllegalStateException("Already attached");
            }
            this.contents = contents;
            this.axis = axis;
            isy = axis.getPlotDimension() == XYDimension.Y;
            axis.addMouseListener(this);
            axis.addMouseMotionListener(this);
        }


        @Override
        public void mouseClicked(MouseEvent e) {
        }


        @Override
        public void mouseEntered(MouseEvent e) {
        }


        @Override
        public void mouseExited(MouseEvent e) {
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            if(markerLine != null) {
                contents.remove(markerLine);
                contents.repaint(); // TODO: Only repaint the relevant portion
                markerLine = null;
            }
        }


        @Override
        public void mousePressed(MouseEvent e) {
            if((e.getModifiersEx() & MASK) == 0) {
                int physicalPos = isy ? e.getY() : e.getX();
                double logicalPos = axis.toLogical(physicalPos);
                markerLine = new XYMarkerLine(axis, logicalPos);
                markerLine.setForeground(Color.yellow);
                contents.add(markerLine);
                contents.revalidate();
            }
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            if(markerLine != null) {
                int physicalPos = isy ? e.getY() : e.getX();
                double logicalPos = axis.toLogical(physicalPos);
                markerLine.setValue(logicalPos);
            }
        }


        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }


    private static class DragListener implements MouseListener, MouseMotionListener {
        private static final int MASK = InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
                | InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        XYAxis axis;

        boolean isy;

        double logicalAnchor;

        boolean active;


        public void attach(XYAxis axis) {
            if(this.axis != null) {
                throw new IllegalStateException("Already attached");
            }
            this.axis = axis;
            isy = axis.getPlotDimension() == XYDimension.Y;
            axis.addMouseListener(this);
            axis.addMouseMotionListener(this);
        }


        @Override
        public void mouseClicked(MouseEvent e) {
        }


        @Override
        public void mouseEntered(MouseEvent e) {
        }


        @Override
        public void mouseExited(MouseEvent e) {
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            active = false;
        }


        @Override
        public void mousePressed(MouseEvent e) {
            if((e.getModifiersEx() & MASK) == InputEvent.SHIFT_DOWN_MASK) {
                int physicalPos = isy ? e.getY() : e.getX();
                logicalAnchor = axis.toLogical(physicalPos);
                active = true;
            }
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            if(active) {
                int physicalPos = isy ? e.getY() : e.getX();
                double logicalPos = axis.toLogical(physicalPos);
                axis.shift(logicalAnchor - logicalPos);
            }
        }


        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }


    private static class ScaleListener implements MouseListener, MouseMotionListener {
        private static final int MASK = InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
                | InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        XYAxis axis;

        boolean isy;

        double logicalAnchor;

        double midpoint;

        boolean active;


        public void attach(XYAxis axis) {
            if(this.axis != null) {
                throw new IllegalStateException("Already attached");
            }
            this.axis = axis;
            isy = axis.getPlotDimension() == XYDimension.Y;
            axis.addMouseListener(this);
            axis.addMouseMotionListener(this);
        }


        @Override
        public void mouseClicked(MouseEvent e) {
        }


        @Override
        public void mouseEntered(MouseEvent e) {
        }


        @Override
        public void mouseExited(MouseEvent e) {
        }


        @Override
        public void mouseReleased(MouseEvent e) {
            active = false;
        }


        @Override
        public void mousePressed(MouseEvent e) {
            if((e.getModifiersEx() & MASK) == InputEvent.CTRL_DOWN_MASK) {
                int physicalPos = isy ? e.getY() : e.getX();
                logicalAnchor = axis.toLogical(physicalPos);
                midpoint = (axis.getStart() + axis.getEnd()) / 2;
                active = true;
            }
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            if(active) {
                int physicalPos = isy ? e.getY() : e.getX();
                double logicalPos = axis.toLogical(physicalPos);
                double scale = (logicalAnchor - midpoint) / (logicalPos - midpoint);
                axis.setStart(scale * (axis.getStart() - midpoint) + midpoint);
                axis.setEnd(scale * (axis.getEnd() - midpoint) + midpoint);
            }
        }


        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }


    public static class Legend extends JComponent {
        public Legend() {
            setLayout(new GridBagLayout());
            setBackground(Color.black);
            setForeground(Color.white);
            setOpaque(true);
            setBorder(new LineBorder(Color.gray));
        }


        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }


        public void addLine(String name, XYPlotLine line) {
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(0, 2, 0, 2);
            LineSample sample = new LineSample(line);
            sample.setPreferredSize(new Dimension(20, 10));
            add(sample, c);

            c.insets = new Insets(0, 2, 0, 2);
            c.gridwidth = GridBagConstraints.REMAINDER;
            JLabel label = new JLabel(name);
            Font font = label.getFont();
            font = font.deriveFont(0);
            label.setFont(font);
            label.setForeground(getForeground());
            add(label, c);
        }


        private static class LineSample extends JComponent {
            final XYPlotLine line;


            public LineSample(XYPlotLine line) {
                this.line = line;
            }


            @Override
            public void paint(Graphics g) {
                g.setColor(line.getForeground());
                int x = getWidth();
                int y = getHeight() / 2;
                g.drawLine(0, y, x, y);
            }
        }
    }
}
