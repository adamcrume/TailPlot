package plotter.tail;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Spring;
import javax.swing.SpringLayout;
import javax.swing.SpringLayout.Constraints;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;

import plotter.Legend;

/**
 * Allows the user to move the legend by dragging it.
 * Handles snapping to other components and border changes when snapped.
 *
 * To be use, listen in on all mouse and key events with {@link Toolkit}:
 * <pre>
 * frame.getToolkit().addAWTEventListener(
 *   new LegendDragListener(legend),
 *   AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
 * </pre>
 * This is necessary because events may be triggered by the legend's children,
 * and we need those events, too.
 *
 * The legend's parent <b>must</b> use {@link SpringLayout}.
 *
 * @author Adam Crume
 */
class LegendDragListener implements AWTEventListener {
    /** Legend we want to be able to drag around. */
    private Legend legend;

    /** Components we want to snap to, if within {@link #snapDist} pixels. */
    private Component[] snapTo;

    /** X coordinate of the mouse when dragging began. */
    private int screenX;

    /** Y coordinate of the mouse when dragging began. */
    private int screenY;

    /** X coordinate of the legend (in parent coordinates) when dragging began. */
    private int myX;

    /** Y coordinate of the legend (in parent coordinates) when dragging began. */
    private int myY;

    /** Pixel distance for snapping. */
    private int snapDist = 10;


    /**
     * Creates the listener.
     * @param legend legend that should be draggable
     * @param snapTo components to snap to
     */
    LegendDragListener(Legend legend, Component... snapTo) {
        this.legend = legend;
        this.snapTo = snapTo;
    }


    /**
     * Called when an event occurs.
     * Anything not related to dragging the legend is ignored.
     * @param event the event
     */
    public void eventDispatched(AWTEvent event) {
        if(event instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) event;
            if(SwingUtilities.isDescendingFrom(me.getComponent(), legend)) {
                int id = me.getID();
                if(id == MouseEvent.MOUSE_PRESSED) {
                    mousePressed(me);
                } else if(id == MouseEvent.MOUSE_DRAGGED && me.isShiftDown()) {
                    mouseDragged(me);
                }
            }
        } else if(event instanceof KeyEvent) {
            KeyEvent ke = (KeyEvent) event;
            int id = ke.getID();
            int keyCode = ke.getKeyCode();
            if(id == KeyEvent.KEY_PRESSED && keyCode == KeyEvent.VK_SHIFT) {
                legend.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            } else if(id == KeyEvent.KEY_RELEASED && keyCode == KeyEvent.VK_SHIFT) {
                legend.setCursor(null);
            }
        }
    }


    /**
     * Called when the mouse is pressed.
     * @param event the event
     */
    private void mousePressed(MouseEvent event) {
        screenX = event.getXOnScreen();
        screenY = event.getYOnScreen();
        myX = legend.getX();
        myY = legend.getY();
    }


    /**
     * Called when the mouse is dragged and the shift key is down.
     * @param event the event
     */
    private void mouseDragged(MouseEvent event) {
        // How much we've moved from the original location
        int deltaX = event.getXOnScreen() - screenX;
        int deltaY = event.getYOnScreen() - screenY;

        Container parent = legend.getParent();
        SpringLayout layout = (SpringLayout) parent.getLayout();
        layout.removeLayoutComponent(legend);

        // The new location, in parent coordinates
        int x = myX + deltaX;
        int y = myY + deltaY;

        // See if we snap to anything.
        boolean snapLeft = false;
        boolean snapRight = false;
        boolean snapTop = false;
        boolean snapBottom = false;
        int legendWidth = legend.getWidth();
        int legendHeight = legend.getHeight();
        for(Component snapTo : this.snapTo) {
            // Current location of the top left corner of the legend, in snapTo coordinates.
            Point p = SwingUtilities.convertPoint(parent, x, y, snapTo);
            if(!snapLeft && !snapRight) {
                if(Math.abs(p.x) <= snapDist) {
                    snapLeft = true;
                    layout.putConstraint(SpringLayout.WEST, legend, 0, SpringLayout.WEST, snapTo);
                    x = SwingUtilities.convertPoint(snapTo, 0, 0, parent).x;
                } else if(Math.abs(p.x + legendWidth - snapTo.getWidth()) <= snapDist) {
                    snapRight = true;
                    layout.putConstraint(SpringLayout.EAST, legend, 0, SpringLayout.EAST, snapTo);
                    x = SwingUtilities.convertPoint(snapTo, snapTo.getWidth() - legendWidth, 0, parent).x;
                }
            }
            if(!snapTop && !snapBottom) {
                if(Math.abs(p.y) <= snapDist) {
                    snapTop = true;
                    layout.putConstraint(SpringLayout.NORTH, legend, 0, SpringLayout.NORTH, snapTo);
                    y = SwingUtilities.convertPoint(snapTo, 0, 0, parent).y;
                } else if(Math.abs(p.y + legendHeight - snapTo.getHeight()) <= snapDist) {
                    snapBottom = true;
                    layout.putConstraint(SpringLayout.SOUTH, legend, 0, SpringLayout.SOUTH, snapTo);
                    y = SwingUtilities.convertPoint(snapTo, 0, snapTo.getHeight() - legendHeight, parent).y;
                }
            }
        }

        // Set the location directly for immediate effect
        legend.setLocation(x, y);

        // Set the location in the layout so it stays where it was, percentagewise.
        // In other words, if the center of the legend was 33% of the way from the left to the right of the parent,
        // it will remain 33% of the way from the left to the right of the parent even if the parent is resized.
        Constraints constraints = layout.getConstraints(legend);
        if(!snapTop && !snapBottom) {
            float py = (y + legendHeight * .5f) / (float) parent.getHeight();
            constraints.setConstraint(SpringLayout.VERTICAL_CENTER, Spring.scale(new HeightSpring(parent), py));
        }
        if(!snapLeft && !snapRight) {
            float px = (x + legendWidth * .5f) / (float) parent.getWidth();
            constraints.setConstraint(SpringLayout.HORIZONTAL_CENTER, Spring.scale(new WidthSpring(parent), px));
        }

        // Hide borders on the sides that have been snapped to.
        Border outsideBorder = BorderFactory.createMatteBorder(snapTop ? 0 : 1, snapLeft ? 0 : 1, snapBottom ? 0 : 1,
                snapRight ? 0 : 1, Color.darkGray);
        Border insideBorder = ((CompoundBorder) legend.getBorder()).getInsideBorder();
        legend.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));
    }


    /**
     * Returns the snap distance.
     * @return the snap distance, in pixels
     */
    public int getSnapDist() {
        return snapDist;
    }


    /**
     * Sets the snap distance.
     * @param snapDist the snap distance, in pixels
     */
    public void setSnapDist(int snapDist) {
        this.snapDist = snapDist;
    }
}
