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

package com.bc.fiduceo.geometry.jts;


import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JTSGeometryFactoryTest {

    private JtsGeometryFactory factory;
    private WKTReader wktReader;

    @Before
    public void setUp() {
        factory = new JtsGeometryFactory();
        wktReader = new WKTReader();
    }

    @Test
    public void testMapToGlobe_onlyPointsInGlobe() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((10 10, 20 10, 20 20, 10 20, 10 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(1, mappedPolygons.length);
        assertEquals("POLYGON ((10 10, 10 20, 20 20, 20 10, 10 10))", mappedPolygons[0].toString());
    }

    @Test
    public void testMapToGlobe_westShiftedOnlyGlobe() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((-200 10, -190 10, -190 20, -200 20, -200 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(1, mappedPolygons.length);
        assertEquals("POLYGON ((160 10, 160 20, 170 20, 170 10, 160 10))", mappedPolygons[0].toString());
    }

    @Test
    public void testMapToGlobe_westShiftedAndCentralGlobe() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((-200 10, -170 10, -170 20, -200 20, -200 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(2, mappedPolygons.length);
        assertEquals("POLYGON ((180 20, 180 10, 160 10, 160 20, 180 20))", mappedPolygons[0].toString());
        assertEquals("POLYGON ((-180 10, -180 20, -170 20, -170 10, -180 10))", mappedPolygons[1].toString());
    }

    @Test
    public void testMapToGlobe_eastShiftedAndCentralGlobe() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((170 10, 210 10, 210 20, 170 20, 170 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(2, mappedPolygons.length);
        assertEquals("POLYGON ((180 20, 180 10, 170 10, 170 20, 180 20))", mappedPolygons[0].toString());
        assertEquals("POLYGON ((-180 10, -180 20, -150 20, -150 10, -180 10))", mappedPolygons[1].toString());
    }

    @Test
    public void testMapToGlobe_eastShiftedOnlyGlobe() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((200 10, 210 10, 210 20, 200 20, 200 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(1, mappedPolygons.length);
        assertEquals("POLYGON ((-160 10, -160 20, -150 20, -150 10, -160 10))", mappedPolygons[0].toString());
    }

    @Test
    public void testMapToGlobe_allShiftsPresent() throws ParseException {
        final Polygon polygonInGlobe = (Polygon) wktReader.read("POLYGON((-200 10, 210 10, 210 20, -200 20, -200 10))");

        final Polygon[] mappedPolygons = factory.mapToGlobe(polygonInGlobe);
        assertEquals(3, mappedPolygons.length);
        assertEquals("POLYGON ((180 20, 180 10, 160 10, 160 20, 180 20))", mappedPolygons[0].toString());
        assertEquals("POLYGON ((-180 10, -180 20, 180 20, 180 10, -180 10))", mappedPolygons[1].toString());
        assertEquals("POLYGON ((-180 10, -180 20, -150 20, -150 10, -180 10))", mappedPolygons[2].toString());
    }
}
