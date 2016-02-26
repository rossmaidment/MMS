
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

package com.bc.fiduceo.math;


import com.bc.fiduceo.core.SatelliteGeometry;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.TimeAxis;

import java.util.ArrayList;
import java.util.Date;

public class GeometryIntersector {

    // @todo 1 tb/tb extend to support multiple time axes and Multi-Polygon geometries
    public static TimeInfo getIntersectingInterval(SatelliteGeometry satGeometry_1, SatelliteGeometry satGeometry_2) {
        final Geometry geometry_1 = satGeometry_1.getGeometry();
        final Geometry geometry_2 = satGeometry_2.getGeometry();
        final TimeInfo timeInfo = new TimeInfo();

        final Geometry intersection = geometry_1.getIntersection(geometry_2);
        if (intersection.isEmpty()) {
            return timeInfo;
        }

        final TimeAxis[] timeAxes_1 = satGeometry_1.getTimeAxes();
        final TimeAxis[] timeAxes_2 = satGeometry_2.getTimeAxes();

        final Point[] coordinates = intersection.getCoordinates();
        final ArrayList<Date> sensor_1_dates = new ArrayList<>(coordinates.length);
        final ArrayList<Date> sensor_2_dates = new ArrayList<>(coordinates.length);
        for (int i = 0; i < coordinates.length - 1; i++) {
            final Point coordinate = coordinates[i];
            Date time = timeAxes_1[0].getTime(coordinate);
            if (time != null) {
                sensor_1_dates.add(time);
            }

            time = timeAxes_2[0].getTime(coordinate);
            if (time != null) {
                sensor_2_dates.add(time);
            }
        }

        final TimeInterval interval_1 = TimeInterval.create(sensor_1_dates);
        final TimeInterval interval_2 = TimeInterval.create(sensor_2_dates);


        final TimeInterval overlapInterval = interval_1.intersect(interval_2);
        timeInfo.setOverlapInterval(overlapInterval);

        if (overlapInterval == null) {
            final int timeDelta = calculateTimeDelta(interval_1, interval_2);
            timeInfo.setMinimalTimeDelta(timeDelta);
        } else {
            timeInfo.setMinimalTimeDelta(0);
        }
        return timeInfo;
    }

    // package access for testing only tb 2015-09-04
    static int calculateTimeDelta(TimeInterval interval_1, TimeInterval interval_2) {
        TimeInterval earlier;
        TimeInterval later;
        if (interval_1.getStartTime().before(interval_2.getStartTime())) {
            earlier = interval_1;
            later = interval_2;
        } else {
            earlier = interval_2;
            later = interval_1;
        }

        return (int) (later.getStartTime().getTime() - earlier.getStopTime().getTime());
    }
}
