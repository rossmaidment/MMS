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
 */

package com.bc.fiduceo.reader.insitu;

import static org.junit.Assert.*;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.TimeLocator;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.*;
import org.opengis.geometry.Geometry;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sabine on 27.10.2016.
 */
public class InsituReaderTest {

    public static final Interval _3x3 = new Interval(3, 3);
    private InsituReader ir;

    @Before
    public void setUp() throws Exception {
        ir = new InsituReader();
        ir.open(Paths.get("D:\\testData\\sst-cci\\insitu\\insitu_0_WMOID_51993_20040402_20060207.nc").toFile());
//        ir.open(Paths.get("D:\\testData\\sst-cci\\insitu\\insitu_3_WMOID_13001_20060608_20131126.nc").toFile());
    }

    @After
    public void tearDown() throws Exception {
        ir.close();
    }

    @Test
    public void testReadAcquisitionInfo() throws Exception {
        final AcquisitionInfo info = ir.read();

        assertNotNull(info);
        final DateFormat utc = ProductData.UTC.createDateFormat();
        assertEquals("02-Apr-2004 18:43:47", utc.format(info.getSensingStart()));
        assertEquals("07-Feb-2006 05:17:59", utc.format(info.getSensingStop()));
    }

    @Test
    public void testGetVariables() throws Exception {
        final List<Variable> variables = ir.getVariables();

        assertNotNull(variables);
        assertEquals(9, variables.size());
        assertEquals("insitu.time", variables.get(0).getShortName());
        assertEquals("insitu.lat", variables.get(1).getShortName());
        assertEquals("insitu.lon", variables.get(2).getShortName());
        assertEquals("insitu.sea_surface_temperature", variables.get(3).getShortName());
        assertEquals("insitu.sst_uncertainty", variables.get(4).getShortName());
        assertEquals("insitu.sst_depth", variables.get(5).getShortName());
        assertEquals("insitu.sst_qc_flag", variables.get(6).getShortName());
        assertEquals("insitu.sst_track_flag", variables.get(7).getShortName());
        assertEquals("insitu.mohc_id", variables.get(8).getShortName());

    }

    @Test
    public void testInsituType() throws Exception {
        assertEquals("drifter", ir.getInsituType());
    }

    @Test
    public void testGetTime() throws Exception {
        final int numObservations = ir.getNumObservations();

        assertEquals(8969, numObservations);
        assertEquals(828470627, ir.getTime(0));
        assertEquals(828701820, ir.getTime(24));
        assertEquals(886828679, ir.getTime(numObservations - 1));
        try {
            ir.getTime(numObservations);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException expected) {
        }
    }

    @Test
    public void testReadRaw_0() throws Exception {
        final Array array = ir.readRaw(0, 0, new Interval(1, 1), "insitu.lat");

        assertNotNull(array);
        assertEquals(2, array.getShape().length);
        assertArrayEquals(new int[]{1, 1}, array.getShape());
        assertEquals(1, array.getSize());
        assertEquals(DataType.FLOAT, array.getDataType());
        assertEquals("3.61", array.getObject(0).toString());
        assertEquals(3.61f, array.getFloat(0), 0);
    }

    @Test
    public void testReadRaw_1() throws Exception {
        final Array array = ir.readRaw(0, 1, new Interval(1, 1), "insitu.lat");

        assertNotNull(array);
        assertEquals(2, array.getShape().length);
        assertArrayEquals(new int[]{1, 1}, array.getShape());
        assertEquals(1, array.getSize());
        assertEquals(DataType.FLOAT, array.getDataType());
        assertEquals("3.67", array.getObject(0).toString());
        assertEquals(3.67f, array.getFloat(0), 0);
    }

    @Test
    public void testReadRaw_1_3x3() throws Exception {
        final Array array = ir.readRaw(0, 1, _3x3, "insitu.lat");

        assertNotNull(array);
        assertEquals(2, array.getShape().length);
        assertArrayEquals(new int[]{3, 3}, array.getShape());
        assertEquals(9, array.getSize());
        assertEquals(DataType.FLOAT, array.getDataType());
        assertEquals("-32768.0", array.getObject(0).toString());
        assertEquals("-32768.0", array.getObject(1).toString());
        assertEquals("-32768.0", array.getObject(2).toString());
        assertEquals("-32768.0", array.getObject(3).toString());
        assertEquals("3.67", array.getObject(4).toString());
        assertEquals("-32768.0", array.getObject(5).toString());
        assertEquals("-32768.0", array.getObject(6).toString());
        assertEquals("-32768.0", array.getObject(7).toString());
        assertEquals("-32768.0", array.getObject(8).toString());
    }

    @Test
    public void testReadScaled_1_3x3() throws Exception {
        final Array array = ir.readScaled(0, 24, _3x3, "insitu.sea_surface_temperature");

        assertNotNull(array);
        assertEquals(2, array.getShape().length);
        assertArrayEquals(new int[]{3, 3}, array.getShape());
        assertEquals(9, array.getSize());
        assertEquals(DataType.FLOAT, array.getDataType());
        assertEquals("-32768.0", array.getObject(0).toString());
        assertEquals("-32768.0", array.getObject(1).toString());
        assertEquals("-32768.0", array.getObject(2).toString());
        assertEquals("-32768.0", array.getObject(3).toString());
        assertEquals("27.4", array.getObject(4).toString());
        assertEquals("-32768.0", array.getObject(5).toString());
        assertEquals("-32768.0", array.getObject(6).toString());
        assertEquals("-32768.0", array.getObject(7).toString());
        assertEquals("-32768.0", array.getObject(8).toString());
    }

    @Test
    public void testReadAcquisitionTime() throws Exception {
        final ArrayInt.D2 array = ir.readAcquisitionTime(0, 3, _3x3);

        assertNotNull(array);
        assertEquals(2, array.getShape().length);
        assertArrayEquals(new int[]{3, 3}, array.getShape());
        assertEquals(9, array.getSize());
        assertEquals(DataType.INT, array.getDataType());
        assertEquals("-32768", array.getObject(0).toString());
        assertEquals("-32768", array.getObject(1).toString());
        assertEquals("-32768", array.getObject(2).toString());
        assertEquals("-32768", array.getObject(3).toString());
        assertEquals("828485100", array.getObject(4).toString());
        assertEquals("-32768", array.getObject(5).toString());
        assertEquals("-32768", array.getObject(6).toString());
        assertEquals("-32768", array.getObject(7).toString());
        assertEquals("-32768", array.getObject(8).toString());
    }

    @Test
    public void testGetProductSize() throws Exception {
        final Dimension productSize = ir.getProductSize();

        assertNotNull(productSize);
        assertEquals("product_size", productSize.getName());
        assertEquals(1, productSize.getNx());
        assertEquals(8969, productSize.getNy());
    }

    @Test
    public void testGetTimeLocator() throws Exception {
        final TimeLocator timeLocator = ir.getTimeLocator();

        assertNotNull(timeLocator);
        assertEquals(1080943451000L, timeLocator.getTimeFor(2, 2));
    }

    @Test
    public void testGetPixelLocator() throws Exception {
        try {
            ir.getPixelLocator();
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }

    }

    @Test
    public void testGetSubScenePixelLocator() throws Exception {
        final GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        final Polygon polygon = geometryFactory.createPolygon(Arrays.asList(
                    geometryFactory.createPoint(4, 5),
                    geometryFactory.createPoint(5, 6),
                    geometryFactory.createPoint(6, 5)
        ));

        try {
            ir.getSubScenePixelLocator(polygon);
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void testGetRegEx() {
        final String expected = "insitu_[0-9][0-9]?_WMOID_[^_]+_[12][09]\\d{2}[01]\\d[0123]\\d_[12][09]\\d{2}[01]\\d[0123]\\d.nc";

        assertEquals(expected, ir.getRegEx());
        final Pattern pattern = java.util.regex.Pattern.compile(expected);

        Matcher matcher = pattern.matcher("insitu_0_WMOID_51993_20040402_20060207.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_2_WMOID_ZXCS_19890623_19890626.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_3_WMOID_13001_20060608_20131126.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_3_WMOID_51019_19910722_20120610.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_4_WMOID_LeSuroit_20070110_20070218.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_5_WMOID_5901880_20100514_20100627.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_9_WMOID_14456569_19980913_19981123.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_10_WMOID_9733500_19840123_19840404.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_11_WMOID_370055_19810820_19810901.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_12_WMOID_Q9900579_20130401_20130812.nc");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("insitu_13_WMOID_1983407_19880404_19880406.nc");
        assertTrue(matcher.matches());


        matcher = pattern.matcher("AMSR_E_L2A_BrightnessTemperatures_V12_200502170536_D.hdf");
        assertFalse(matcher.matches());

        matcher = pattern.matcher("NSS.HIRX.TN.D79287.S1623.E1807.B0516566.GC.nc");
        assertFalse(matcher.matches());
    }

}