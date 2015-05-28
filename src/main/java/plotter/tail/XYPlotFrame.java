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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import plotter.Legend;
import plotter.LegendItem;
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

	private XYAxis x2Axis;

	private XYAxis y2Axis;

	private XYGrid grid;

	private XYGrid grid2;

	private XYPlotContents contents;

	private XYLocationDisplay locationDisplay;

	private SlopeLineDisplay slopeLineDisplay;

	private Legend legend;

	private boolean useLegend;

	private boolean useX2;

	private boolean useY2;

	private boolean useGrid2;

	/** Listeners to be notified when an axis is panned or zoomed. */
	private List<AxisListener> axisListeners = new ArrayList<AxisListener>();


	public void setup() {
		setup(getContentPane());
	}


	public void setup(Container contentPane) {
		plot = new XYPlot();
		xAxis = createXAxis();
		yAxis = createYAxis();
		xAxis.setPreferredSize(new Dimension(1, 30));
		yAxis.setPreferredSize(new Dimension(50, 1));
		xAxis.setForeground(Color.white);
		yAxis.setForeground(Color.white);
		xAxis.setTextMargin(10);
		yAxis.setTextMargin(10);
		plot.add(xAxis);
		plot.add(yAxis);
		plot.setXAxis(xAxis);
		plot.setYAxis(yAxis);
		if(useX2) {
			x2Axis = createX2Axis();
			x2Axis.setMirrored(true);
			x2Axis.setPreferredSize(new Dimension(1, 30));
			x2Axis.setForeground(Color.white);
			x2Axis.setTextMargin(10);
			plot.add(x2Axis);
			plot.setX2Axis(x2Axis);
		}
		if(useY2) {
			y2Axis = createY2Axis();
			y2Axis.setMirrored(true);
			y2Axis.setPreferredSize(new Dimension(50, 1));
			y2Axis.setForeground(Color.white);
			y2Axis.setTextMargin(10);
			plot.add(y2Axis);
			plot.setY2Axis(y2Axis);
		}
		plot.setBackground(Color.darkGray);
		contents = new XYPlotContents();
		contents.setBackground(Color.black);
		plot.setBackground(Color.darkGray);

		grid = new XYGrid(xAxis, yAxis);
		grid.setForeground(Color.lightGray);
		contents.add(grid);

		if(useGrid2) {
			if(!useX2 || !useY2) {
				throw new IllegalStateException("If useGrid2 is enabled, then useX2 and useY2 must be enabled");
			}
			grid2 = new XYGrid(x2Axis, y2Axis);
			grid2.setForeground(Color.lightGray);
			contents.add(grid2);
		}

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

		if(useLegend) {
			legend = new Legend();
			legend.setForeground(Color.white);
			legend.setBackground(Color.black);
			legend.setFont(new Font("Arial", 0, 12));
			Border outsideBorder = BorderFactory.createMatteBorder(0, 1, 1, 0, Color.darkGray);
			Border insideBorder = BorderFactory.createLineBorder(Color.black, 5);
			legend.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
			plot.add(legend, 0);
		}

		new DefaultXYLayoutGenerator().generateLayout(plot);

		SpringLayout layout = new SpringLayout();
		contentPane.setLayout(layout);
		layout.putConstraint(SpringLayout.NORTH, plot, 0, SpringLayout.NORTH, contentPane);
		layout.putConstraint(SpringLayout.SOUTH, contentPane, 0, SpringLayout.SOUTH, plot);
		layout.putConstraint(SpringLayout.EAST, contentPane, 0, SpringLayout.EAST, plot);
		contentPane.add(plot);

		new MarkerListener().attach(contents, xAxis);
		new MarkerListener().attach(contents, yAxis);
		if(useY2) {
			new MarkerListener().attach(contents, y2Axis);
			new DragListener().attach(contents, y2Axis);
			new ScaleListener().attach(contents, y2Axis);
		}
		new DragListener().attach(contents, xAxis);
		new DragListener().attach(contents, yAxis);
		new ScaleListener().attach(contents, xAxis);
		new ScaleListener().attach(contents, yAxis);
	}


	protected XYAxis createYAxis() {
		return new LinearXYAxis(XYDimension.Y);
	}


	protected XYAxis createXAxis() {
		return new LinearXYAxis(XYDimension.X);
	}


	protected XYAxis createY2Axis() {
		return new LinearXYAxis(XYDimension.Y);
	}


	protected XYAxis createX2Axis() {
		return new LinearXYAxis(XYDimension.X);
	}


	public LegendItem addPlotLine(Field field, final Stroke highlightStroke, final Shape highlightPointFill, final Shape highlightPointOutline) {
		String description = field.getName();
		final XYPlotLine plotLine = field.getPlotLine();
		contents.add(plotLine);
		contents.setComponentZOrder(grid, contents.getComponentCount() - 1);
		final LegendItem item;
		if(description != null && legend != null) {
			item = new LegendItem(description, plotLine, null, SwingConstants.LEFT);
			item.addMouseListener(new MouseAdapter() {
				Stroke originalStroke;
				Shape originalPointFill;
				Shape originalPointOutline;

				@Override
				public void mouseEntered(MouseEvent e) {
					originalStroke = plotLine.getStroke();
					originalPointFill = plotLine.getPointFill();
					originalPointOutline = plotLine.getPointOutline();
					plotLine.setStroke(highlightStroke);
					plotLine.setPointFill(highlightPointFill);
					plotLine.setPointOutline(highlightPointOutline);
					plotLine.repaint();
					item.repaint();
				}
				@Override
				public void mouseExited(MouseEvent e) {
					plotLine.setStroke(originalStroke);
					plotLine.setPointFill(originalPointFill);
					plotLine.setPointOutline(originalPointOutline);
					plotLine.repaint();
					item.repaint();
				}
			});
			legend.add(item);
		} else {
		    item = null;
		}
		if(grid2 != null) {
			contents.setComponentZOrder(grid2, contents.getComponentCount() - 1);
		}
		return item;
	}


	public XYAxis getXAxis() {
		return xAxis;
	}


	public XYAxis getYAxis() {
		return yAxis;
	}


	public XYAxis getX2Axis() {
		return x2Axis;
	}


	public XYAxis getY2Axis() {
		return y2Axis;
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


	public boolean isUseLegend() {
		return useLegend;
	}


	public void setUseLegend(boolean useLegend) {
		this.useLegend = useLegend;
	}

	public Legend getLegend() {
		return legend;
	}


	public boolean isUseX2() {
		return useX2;
	}


    public void setUseX2(boolean useX2) {
        this.useX2 = useX2;
    }


    public boolean isUseY2() {
        return useY2;
    }


    public void setUseY2(boolean useY2) {
        this.useY2 = useY2;
    }


    public boolean isUseGrid2() {
        return useGrid2;
    }


    public void setUseGrid2(boolean useGrid2) {
        this.useGrid2 = useGrid2;
    }


    public XYGrid getGrid() {
        return grid;
    }


    public XYGrid getGrid2() {
        return grid2;
    }


    /**
     * Adds a listener for axis manipulation events.
     * @param listener listener to notify when the axis is panned or zoomed
     */
    public void addAxisListener(AxisListener listener) {
        axisListeners.add(listener);
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


    private class DragListener implements MouseListener, MouseMotionListener {
        private static final int MASK = InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
                | InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        XYPlotContents contents;

        XYAxis axis;

        boolean isy;

        double logicalAnchor;

        boolean active;


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
                contents.repaint(); // TODO: Should the Axis.shift method handle this?
                for(AxisListener listener : axisListeners) {
                    listener.axisPanned(axis);
                }
            }
        }


        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }


    private class ScaleListener implements MouseListener, MouseMotionListener {
        private static final int MASK = InputEvent.ALT_DOWN_MASK | InputEvent.ALT_GRAPH_DOWN_MASK
                | InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;

        XYPlotContents contents;

        XYAxis axis;

        boolean isy;

        double logicalAnchor;

        double midpoint;

        boolean active;


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
                contents.repaint(); // TODO: Should the Axis.setStart and Axis.setEnd methods handle this?
                for(AxisListener listener : axisListeners) {
                    listener.axisZoomed(axis);
                }
            }
        }


        @Override
        public void mouseMoved(MouseEvent e) {
        }
    }


    /**
     * Gets notified when an axis is manipulated.
     */
    static interface AxisListener {
        /**
         * Invoked when the axis is panned.
         * @param axis the axis that was panned
         */
        void axisPanned(XYAxis axis);


        /**
         * Invoked when the axis is zoomed.
         * @param axis the axis that was zoomed
         */
        void axisZoomed(XYAxis axis);
    }
}
