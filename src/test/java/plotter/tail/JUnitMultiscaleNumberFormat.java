package plotter.tail;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;

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


    @Test
    public void testParse() throws ParseException {
        MultiscaleNumberFormat format = new MultiscaleNumberFormat();
        assertEquals(1, format.parse("1").doubleValue(), 1e-9);
        assertEquals(1e6, format.parse("1E6").doubleValue(), 1e-3);
        assertEquals(1.1e6, format.parse("1.1E6").doubleValue(), 1e-3);
        assertEquals(1e-6, format.parse("1E-6").doubleValue(), 1e-15);
        assertEquals(-1, format.parse("-1").doubleValue(), 1e-9);
        assertEquals(-1e6, format.parse("-1E6").doubleValue(), 1e-3);
        assertEquals(-1.1e6, format.parse("-1.1E6").doubleValue(), 1e-3);
        assertEquals(-1e-6, format.parse("-1E-6").doubleValue(), 1e-15);
    }
}
