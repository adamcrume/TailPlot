package plotter.tail;

import java.awt.Component;

import javax.swing.Spring;

/**
 * This is similar to {@link Spring#width(Component)}, except that the minimum, preferred,
 * and maximum values are always the current width of the component.

 * @author Adam Crume
 * @see HeightSpring
 */
class WidthSpring extends Spring {
    private int value = UNSET;

    private Component component;


    public WidthSpring(Component component) {
        this.component = component;
    }


    @Override
    public int getMinimumValue() {
        return getPreferredValue();
    }


    @Override
    public int getPreferredValue() {
        return component.getWidth();
    }


    @Override
    public int getMaximumValue() {
        return getPreferredValue();
    }


    @Override
    public int getValue() {
        if(value == UNSET) {
            return getPreferredValue();
        } else {
            return value;
        }
    }


    @Override
    public void setValue(int value) {
        this.value = value;
    }
}
