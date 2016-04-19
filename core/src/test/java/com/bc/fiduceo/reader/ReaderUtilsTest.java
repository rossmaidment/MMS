/*
 * Copyright (C) 2016 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * A copy of the GNU General Public License should have been supplied along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.reader;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReaderUtilsTest {

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeDouble() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.DOUBLE);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Double.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeFloat() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.FLOAT);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Float.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeLong() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.LONG);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Long.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeInt() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.INT);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Integer.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeShort() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.SHORT);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Short.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forDataTypeByte() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.BYTE);

        final Number value = ReaderUtils.getDefaultFillValue(mock);

        assertEquals(Byte.MIN_VALUE, value);
    }

    @Test
    public void testFetchingTheDefaultFillValue_forUnknownType() throws Exception {
        final Variable mock = mock(Variable.class);
        when(mock.getDataType()).thenReturn(DataType.STRUCTURE);

        try {
            ReaderUtils.getDefaultFillValue(mock);
            fail("RuntimeException expected");
        } catch (NullPointerException notExpected) {
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }
}