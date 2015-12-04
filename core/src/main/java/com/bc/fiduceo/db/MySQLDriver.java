
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

package com.bc.fiduceo.db;


import com.bc.fiduceo.core.SatelliteObservation;
import com.bc.fiduceo.core.Sensor;
import com.bc.fiduceo.core.NodeType;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLDriver extends AbstractDriver {

    private final WKBWriter wkbWriter;
    private final WKBReader wkbReader;

    public MySQLDriver() {
        wkbWriter = new WKBWriter();
        wkbReader = new WKBReader();
    }

    @Override
    public String getUrlPattern() {
        return "jdbc:mysql";
    }

    @Override
    public void clear() throws SQLException {
        Statement statement = connection.createStatement();
        statement.execute("DROP TABLE SATELLITE_OBSERVATION");

        connection.createStatement();
        statement.execute("DROP TABLE SENSOR");
    }

    @Override
    public void insert(SatelliteObservation observation) throws SQLException {
        final Sensor sensor = observation.getSensor();
        Integer sensorId = getSensorId(sensor.getName());
        if (sensorId == null) {
            sensorId = insert(sensor);
        }

        final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO SATELLITE_OBSERVATION VALUES(default, ?, ?, ?, GeomFromWKB(?), ?, ?, ?, ?)");
        preparedStatement.setTimestamp(1, toTimeStamp(observation.getStartTime()));
        preparedStatement.setTimestamp(2, toTimeStamp(observation.getStopTime()));
        preparedStatement.setByte(3, (byte) observation.getNodeType().toId());
        preparedStatement.setObject(4, wkbWriter.write(observation.getGeoBounds()));
        preparedStatement.setInt(5, sensorId);
        preparedStatement.setString(6, observation.getDataFile().getAbsolutePath());
        preparedStatement.setInt(7, observation.getTimeAxisStartIndex());
        preparedStatement.setInt(8, observation.getTimeAxisEndIndex());

        preparedStatement.executeUpdate();
    }

    @Override
    public List<SatelliteObservation> get() throws SQLException {
        final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        final ResultSet resultSet = statement.executeQuery("SELECT StartDate, StopDate,NodeType, AsWKB(GeoBounds), SensorId, DataFile, TimeAxisStartIndex, TimeAxisEndIndex FROM SATELLITE_OBSERVATION");
        resultSet.last();
        final int numValues = resultSet.getRow();
        resultSet.beforeFirst();

        final List<SatelliteObservation> resultList = new ArrayList<>(numValues);
        try {
            while (resultSet.next()) {
                final SatelliteObservation observation = new SatelliteObservation();

                final Timestamp startDate = resultSet.getTimestamp("StartDate");
                observation.setStartTime(toDate(startDate));

                final Timestamp stopDate = resultSet.getTimestamp("StopDate");
                observation.setStopTime(toDate(stopDate));

                final int nodeTypeId = resultSet.getInt("NodeType");
                observation.setNodeType(NodeType.fromId(nodeTypeId));

                final byte[] geoBoundsBytes = resultSet.getBytes("AsWKB(GeoBounds)");
                observation.setGeoBounds(wkbReader.read(geoBoundsBytes));

                final int sensorId = resultSet.getInt("SensorId");
                final Sensor sensor = getSensor(sensorId);
                observation.setSensor(sensor);

                final String dataFile = resultSet.getString("DataFile");
                observation.setDataFile(new File(dataFile));

                final int timeAxisStartIndex = resultSet.getInt("TimeAxisStartIndex");
                observation.setTimeAxisStartIndex(timeAxisStartIndex);

                final int timeAxisEndIndex = resultSet.getInt("TimeAxisEndIndex");
                observation.setTimeAxisEndIndex(timeAxisEndIndex);

                resultList.add(observation);
            }
        } catch (ParseException e) {
            throw new SQLException(e.getMessage());
        }

        return resultList;
    }
}