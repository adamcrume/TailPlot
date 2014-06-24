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
