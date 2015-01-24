package plotter.tail;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JUnitMultiscaleNumberFormat {
    @Test
    public void testFormatDouble() {
        MultiscaleNumberFormat format = new MultiscaleNumberFormat();
        assertEquals("1", format.format(1.0));
        assertEquals("1E6", format.format(1e6));
        assertEquals("1.1E6", format.format(1.1e6));
        assertEquals("1E-6", format.format(1e-6));
        assertEquals("-1", format.format(-1.0));
        assertEquals("-1E6", format.format(-1e6));
        assertEquals("-1.1E6", format.format(-1.1e6));
        assertEquals("-1E-6", format.format(-1e-6));
    }


    @Test
    public void testFormatLong() {
        MultiscaleNumberFormat format = new MultiscaleNumberFormat();
        assertEquals("1", format.format(1L));
        assertEquals("1E6", format.format(1000000L));
        assertEquals("1.1E6", format.format(1100000L));
        assertEquals("-1", format.format(-1L));
        assertEquals("-1E6", format.format(-1000000L));
        assertEquals("-1.1E6", format.format(-1100000L));
    }
}
