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

import java.awt.Component;

import javax.swing.Spring;

/**
 * This is similar to {@link Spring#height(Component)}, except that the minimum, preferred,
 * and maximum values are always the current height of the component.

 * @author Adam Crume
 * @see WidthSpring
 */
class HeightSpring extends Spring {
    private int value = UNSET;

    private Component component;


    public HeightSpring(Component component) {
        this.component = component;
    }


    @Override
    public int getMinimumValue() {
        return getPreferredValue();
    }


    @Override
    public int getPreferredValue() {
        return component.getHeight();
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
