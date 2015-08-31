package com.bc.fiduceo.reader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class BoundingPolygonCreator {

    private final int intervalX;
    private final int intervalY;


    BoundingPolygonCreator(int intervalX, int intervalY) {
        this.intervalX = intervalX;
        this.intervalY = intervalY;
    }


    public Geometry createPolygonForAIRS(NetcdfFile netcdfFile) {
        List<Group> groups = netcdfFile.getRootGroup().getGroups().get(0).getGroups();
        ArrayDouble.D2 d2XCoordinate = null;
        ArrayDouble.D2 d2YCoordinate = null;
        for (Group group : groups) {
            try {
                if (group.getShortName().equals("Geolocation_Fields")) {
                    List<Variable> variables = group.getVariables();
                    for (Variable variable : variables) {
                        if (variable.getShortName().startsWith("Latitude")) {

                            d2XCoordinate = (ArrayDouble.D2) variable.read();

                            if (d2XCoordinate == null) {
                                throw new NullPointerException("The array is empty !!!");
                            }
                        }
                        if (variable.getShortName().startsWith("Longitude")) {
                            d2YCoordinate = (ArrayDouble.D2) variable.read();
                            if (d2XCoordinate == null) {
                                throw new NullPointerException("The array is empty !!!");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return polygonAIRS(d2XCoordinate, d2YCoordinate);
    }

    private Geometry polygonAIRS(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude) {
        int geoXTrack = arrayLatitude.getShape()[1] - 1;
        int geoTrack = arrayLatitude.getShape()[0] - 1;

        List<Coordinate> coordinates = new ArrayList<>();
        for (int x = 1; x < geoXTrack; x += intervalX) {
            coordinates.add(new Coordinate(arrayLongitude.get(0, x), arrayLatitude.get(0, x)));
        }
        for (int y = 0; y <= geoTrack; y += intervalY) {
            coordinates.add(new Coordinate(arrayLongitude.get(y, geoXTrack), arrayLatitude.get(y, geoXTrack)));
            if ((y + intervalY) > geoTrack) {
                coordinates.add(new Coordinate(arrayLongitude.get(geoTrack, geoXTrack), arrayLatitude.get(geoTrack, geoXTrack)));
            }
        }
        for (int x = geoXTrack - 1; x > 0; x -= intervalX) {
            coordinates.add(new Coordinate(arrayLongitude.get(geoTrack, x), arrayLatitude.get(geoTrack, x)));
        }
        for (int y = geoTrack; y >= 0; y -= intervalY) {
            coordinates.add(new Coordinate(arrayLongitude.get(y, 0), arrayLatitude.get(y, 0)));
        }
        coordinates.add(coordinates.get(0));
        return new GeometryFactory().createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
    }


    public Geometry createPolygonForEumetSat(NetcdfFile netcdfFile) {
        ArrayFloat.D2 arrayLatitude = null;
        ArrayFloat.D2 arrayLongitude = null;

        List<Variable> variables = netcdfFile.getVariables();
        try {
            for (Variable variable : variables) {
                if (variable.getShortName().equals("lat")) {
                    arrayLatitude = (ArrayFloat.D2) variable.read();
                }

                if (variable.getShortName().startsWith("lon")) {
                    arrayLongitude = (ArrayFloat.D2) variable.read();
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return polygonEumet(arrayLatitude, arrayLongitude);
    }


    private Geometry polygonEumet(ArrayFloat.D2 arrayLatitude, ArrayFloat.D2 arrayLongitude) {

        int geoXTrack = arrayLatitude.getShape()[1] - 1;
        int geoTrack = arrayLatitude.getShape()[0] - 1;
        List<Coordinate> coordinates = new ArrayList<>();
        coordinates.add(new Coordinate(arrayLongitude.get(0, 0), arrayLatitude.get(0, 0)));

        for (int x = 1; x < geoXTrack; x += intervalX) {
            coordinates.add(new Coordinate(arrayLongitude.get(0, x), arrayLatitude.get(0, x)));
        }

        for (int y = 0; y <= geoTrack; y += intervalY) {
            coordinates.add(new Coordinate(arrayLongitude.get(y, geoXTrack), arrayLatitude.get(y, geoXTrack)));
            if ((y + intervalY) > geoTrack) {
                coordinates.add(new Coordinate(arrayLongitude.get(geoTrack, geoXTrack), arrayLatitude.get(geoTrack, geoXTrack)));
            }
        }

        for (int x = geoXTrack - 1; x > 0; x -= intervalX) {
            coordinates.add(new Coordinate(arrayLongitude.get(geoTrack, x), arrayLatitude.get(geoTrack, x)));
        }

        for (int y = geoTrack; y >= 0; y -= intervalY) {
            coordinates.add(new Coordinate(arrayLongitude.get(y, 0), arrayLatitude.get(y, 0)));
        }
        coordinates.add(coordinates.get(0));
        return new GeometryFactory().createPolygon(coordinates.toArray(new Coordinate[coordinates.size()]));
    }
}
