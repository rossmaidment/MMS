/*
 * Copyright (C) 2015 Brockmann Consult GmbH
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

package com.bc.fiduceo;

import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Point;
import com.google.common.geometry.S2LatLng;
import com.google.common.geometry.S2Point;
import com.vividsolutions.jts.geom.Coordinate;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestUtil {

    private static final String SYSTEM_TEMP_PROPETY = "java.io.tmpdir";
    private static final String TEST_DIRECTORY = "fiduceo_test";

    public static File getTestDataDirectory() throws IOException {
        final InputStream resourceStream = TestUtil.class.getResourceAsStream("dataDirectory.properties");
        final Properties properties = new Properties();
        properties.load(resourceStream);
        final String dataDirectoryProperty = properties.getProperty("dataDirectory");
        if (dataDirectoryProperty == null) {
            fail("Property 'dataDirectory' is not set.");
        }
        final File dataDirectory = new File(dataDirectoryProperty);
        if (!dataDirectory.isDirectory()) {
            fail("Property 'dataDirectory' supplied does not exist: '" + dataDirectoryProperty + "'");
        }
        return dataDirectory;
    }

    public static void assertCorrectUTCDate(int year, int month, int day, int hour, int minute, int second, Date utcDate) {
        final Calendar calendar = ProductData.UTC.createCalendar();
        calendar.setTime(utcDate);

        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month - 1, calendar.get(Calendar.MONTH));
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, calendar.get(Calendar.MINUTE));
        assertEquals(second, calendar.get(Calendar.SECOND));
    }

    public static void assertCorrectUTCDate(int year, int month, int day, int hour, int minute, int second, int millisecond, Date utcDate) {
        final Calendar calendar = ProductData.UTC.createCalendar();
        calendar.setTime(utcDate);

        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month - 1, calendar.get(Calendar.MONTH));
        assertEquals(day, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
        assertEquals(minute, calendar.get(Calendar.MINUTE));
        assertEquals(second, calendar.get(Calendar.SECOND));
        assertEquals(millisecond, calendar.get(Calendar.MILLISECOND));
    }

    public static File createTestDirectory() {
        final File testDir = getTestDir();
        if (!testDir.mkdirs()) {
            fail("unable to create test directory: " + testDir.getAbsolutePath());
        }
        return testDir;
    }

    public static void deleteTestDirectory() {
        final File testDir = getTestDir();
        if (testDir.isDirectory()) {
            final boolean deleted = FileUtils.deleteTree(testDir);
            if (!deleted) {
                fail("unable to delete test directory: " + testDir.getAbsolutePath());
            }
        }
    }

    public static File createFileInTestDir(String fileName) throws IOException {
        final File testDirectory = getTestDir();

        final File databaseConfigFile = new File(testDirectory, fileName);
        if (!databaseConfigFile.createNewFile()) {
            fail("Unable to create test file: " + databaseConfigFile.getAbsolutePath());
        }
        return databaseConfigFile;
    }

    private static File getTestDir() {
        final String tempDirPath = System.getProperty(SYSTEM_TEMP_PROPETY);
        return new File(tempDirPath, TEST_DIRECTORY);
    }

    public static com.bc.fiduceo.geometry.Polygon allBoundingPolygon(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude, NodeType nodeType, GeometryFactory.Type type) {
        final int[] shape = arrayLatitude.getShape();
        int width = shape[1] - 1;
        int height = shape[0] - 1;

        int intervalX = 50;
        int intervalY = 50;

        com.bc.fiduceo.geometry.GeometryFactory geometryFactory = new com.bc.fiduceo.geometry.GeometryFactory(type);
        List<Point> coordinates = new ArrayList<>();

        int[] timeAxisStart = new int[2];
        int[] timeAxisEnd = new int[2];
        if (nodeType == NodeType.ASCENDING) {
            for (int x = 0; x < width; x += intervalX) {
                final double lon = arrayLongitude.get(0, x);
                final double lat = arrayLatitude.get(0, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            timeAxisStart[0] = coordinates.size();
            timeAxisEnd[0] = timeAxisStart[0];
            for (int y = 0; y < height; y += intervalY) {
                final double lon = arrayLongitude.get(y, width);
                final double lat = arrayLatitude.get(y, width);
                coordinates.add(geometryFactory.createPoint(lon, lat));
                ++timeAxisEnd[0];
            }

            for (int x = width; x > 0; x -= intervalX) {
                final double lon = arrayLongitude.get(height, x);
                final double lat = arrayLatitude.get(height, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int y = height; y > 0; y -= intervalY) {
                final double lon = arrayLongitude.get(y, 0);
                final double lat = arrayLatitude.get(y, 0);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }
        } else {
            timeAxisStart[0] = 0;
            timeAxisEnd[0] = 0;
            for (int y = 0; y < height; y += intervalY) {
                final double lon = arrayLongitude.get(y, width);
                final double lat = arrayLatitude.get(y, width);
                coordinates.add(geometryFactory.createPoint(lon, lat));
                ++timeAxisEnd[0];
            }

            for (int x = width; x > 0; x -= intervalX) {
                final double lon = arrayLongitude.get(height, x);
                final double lat = arrayLatitude.get(height, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int y = height; y > 0; y -= intervalY) {
                final double lon = arrayLongitude.get(y, 0);
                final double lat = arrayLatitude.get(y, 0);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int x = 0; x < width; x += intervalX) {
                final double lon = arrayLongitude.get(0, x);
                final double lat = arrayLatitude.get(0, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }
        }
        if (GeometryFactory.Type.JTS == type) {
            coordinates.add(coordinates.get(0));
        }
        return geometryFactory.createPolygon(coordinates);
    }

    public static Coordinate[] getCoordinates(List<Point> points) {
        final Coordinate[] coordinates = new Coordinate[points.size()];
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            coordinates[i] = new Coordinate(point.getLon(), point.getLat());
        }
        return coordinates;
    }

    public static S2Point createS2Point(double lon, double lat) {
        return S2LatLng.fromDegrees(lat, lon).toPoint();
    }

    public static String plotPolygon(Point[] points) {
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("POLYGON((");

        for (int i = 0; i < points.length; i++) {
            Point coordinate = points[i];
            stringBuffer.append(coordinate.getLon());
            stringBuffer.append(" ");
            stringBuffer.append(coordinate.getLat());
            if (i < points.length - 1) {
                stringBuffer.append(",");
            }
        }
        stringBuffer.append(",");
        stringBuffer.append(points[0].getLon());
        stringBuffer.append(" ");
        stringBuffer.append(points[0].getLat());
        stringBuffer.append("))");

        System.out.println(stringBuffer.toString());
        return stringBuffer.toString();
    }

    public static ArrayDouble.D2 rescaleCoordinate(ArrayInt.D2 coodinate, double scale) {
        int[] coordinates = (int[]) coodinate.copyTo1DJavaArray();
        int[] shape = coodinate.getShape();
        ArrayDouble arrayDouble = new ArrayDouble(shape);

        for (int i = 0; i < coordinates.length; i++) {
            arrayDouble.setDouble(i, ((coordinates[i] * scale)));
        }
        return (ArrayDouble.D2) arrayDouble.copy();
    }

}
