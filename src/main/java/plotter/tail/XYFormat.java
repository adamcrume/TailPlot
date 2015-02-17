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

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

class XYFormat extends Format {
    private final Format baseFormat;

    private boolean logY;

    private boolean logX;


    public XYFormat(Format baseFormat) {
        this.baseFormat = baseFormat;
    }


    @Override
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        Object[] data = (Object[]) obj;
        data = data.clone();
        double x = ((Number) data[0]).doubleValue();
        double y = ((Number) data[1]).doubleValue();
        if(logX) {
            x = Math.pow(10, x);
        }
        if(logY) {
            y = Math.pow(10, y);
        }
        data[0] = x;
        data[1] = y;
        return baseFormat.format(data, toAppendTo, pos);
    }


    @Override
    public Object parseObject(String source, ParsePosition pos) {
        Object[] data = (Object[]) baseFormat.parseObject(source, pos);
        double x = ((Number) data[0]).doubleValue();
        double y = ((Number) data[1]).doubleValue();
        if(logX) {
            x = Math.log10(x);
        }
        if(logY) {
            y = Math.log10(y);
        }
        data[0] = x;
        data[1] = y;
        return data;
    }


    public boolean isLogY() {
        return logY;
    }


    public void setLogY(boolean logY) {
        this.logY = logY;
    }


    public boolean isLogX() {
        return logX;
    }


    public void setLogX(boolean logX) {
        this.logX = logX;
    }
}
