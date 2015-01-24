package plotter.tail;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

class MultiscaleNumberFormat extends NumberFormat {
    private NumberFormat plainFormat = new DecimalFormat("#.###");

    private NumberFormat exponentialFormat = new DecimalFormat("0.###E0");

    private double upperThreshold = 999.5;

    private double lowerThreshold = .01;


    public MultiscaleNumberFormat() {
    }


    public MultiscaleNumberFormat(NumberFormat plainFormat, NumberFormat exponentialFormat, double lowerThreshold,
            double upperThreshold) {
        this.plainFormat = plainFormat;
        this.exponentialFormat = exponentialFormat;
        this.lowerThreshold = lowerThreshold;
        this.upperThreshold = upperThreshold;
    }


    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        double mag = Math.abs(number);
        if(mag > upperThreshold || (mag < lowerThreshold && mag != 0)) {
            return exponentialFormat.format(number, toAppendTo, pos);
        } else {
            return plainFormat.format(number, toAppendTo, pos);
        }
    }


    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        long mag = Math.abs(number);
        if(mag > upperThreshold || (mag < lowerThreshold && mag != 0)) {
            return exponentialFormat.format(number, toAppendTo, pos);
        } else {
            return plainFormat.format(number, toAppendTo, pos);
        }
    }


    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        ParsePosition tmp = new ParsePosition(parsePosition.getIndex());
        tmp.setErrorIndex(parsePosition.getErrorIndex());
        Number n = plainFormat.parse(source, tmp);
        if(tmp.getErrorIndex() == -1 && tmp.getIndex() == source.length()) {
            parsePosition.setIndex(tmp.getIndex());
            parsePosition.setErrorIndex(tmp.getErrorIndex());
            return n;
        } else {
            tmp.setIndex(parsePosition.getIndex());
            tmp.setErrorIndex(parsePosition.getErrorIndex());
            n = exponentialFormat.parse(source, tmp);
            parsePosition.setIndex(tmp.getIndex());
            parsePosition.setErrorIndex(tmp.getErrorIndex());
            return n;
        }
    }
}
