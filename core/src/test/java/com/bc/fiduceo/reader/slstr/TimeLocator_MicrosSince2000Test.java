package com.bc.fiduceo.reader.slstr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeLocator_MicrosSince2000Test {

    @Test
    public void testGetTimes() {
        final long[] time_2000 = {592784694225744L, 592784694525730L, 592784694825716L, 592784695125701L, 592784695425687L, 592784695725673L};

        final TimeLocator_MicrosSince2000 timeLocator = new TimeLocator_MicrosSince2000(time_2000);

        assertEquals(1542147894226L, timeLocator.getTimeFor(0, 0));
        assertEquals(1542147894826L, timeLocator.getTimeFor(2, 1000));
        assertEquals(1542147895726L, timeLocator.getTimeFor(5, 2000));
    }

    @Test
    public void testConvertToUnixEpochMillis() {
        assertEquals(949363200000L, TimeLocator_MicrosSince2000.convertToUnixEpochMillis(0));
        assertEquals(949363286400L, TimeLocator_MicrosSince2000.convertToUnixEpochMillis(86400000));
        assertEquals(949373200000L, TimeLocator_MicrosSince2000.convertToUnixEpochMillis(10000000000L));
    }
}
