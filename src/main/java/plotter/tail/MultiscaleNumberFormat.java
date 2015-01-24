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
        throw new IllegalArgumentException("not implemented");
    }
}
