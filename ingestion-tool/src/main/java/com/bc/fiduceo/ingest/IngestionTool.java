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

package com.bc.fiduceo.ingest;

import com.bc.fiduceo.core.SatelliteObservation;
import com.bc.fiduceo.core.Sensor;
import com.bc.fiduceo.core.ServicesUtils;
import com.bc.fiduceo.core.SystemConfig;
import com.bc.fiduceo.db.DatabaseConfig;
import com.bc.fiduceo.db.Storage;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.Reader;
import com.bc.fiduceo.util.TimeUtils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

class IngestionTool {

    static String VERSION = "1.0.0";
    private List<Calendar[]> daysIntervalYear;

    static Options getOptions() {
        final Options options = new Options();

        final Option helpOption = new Option("h", "help", false, "Prints the tool usage.");
        options.addOption(helpOption);

        final Option sensorOption = new Option("s", "sensor", true, "Defines the sensor to be ingested.");
        options.addOption(sensorOption);

        final Option configOption = new Option("c", "config", true, "Defines the configuration directory. Defaults to './config'.");
        options.addOption(configOption);

        final Option startOption = new Option("start", "start-time", true, "Define the starting time of products to inject.");
        startOption.setArgName("Date");
        options.addOption(startOption);

        final Option endOption = new Option("end", "end-time", true, "Define the ending time of products to inject.");
        endOption.setArgName("Date");
        options.addOption(endOption);

        final Option versionOption = new Option("v", "version", true, "Define the sensor version.");
        options.addOption(versionOption);

        return options;
    }

    void run(CommandLine commandLine) throws IOException, SQLException {

        final String configDirPath = commandLine.getOptionValue("config");
        final String sensorType = commandLine.getOptionValue("s");

        final String startTime = commandLine.getOptionValue("start");
        final String endTime = commandLine.getOptionValue("end");

        final String processingVersion = commandLine.getOptionValue("v");

        final Date startDate = TimeUtils.parseDOYBeginOfDay(startTime);
        final Date endDate = TimeUtils.parseDOYEndOfDay(endTime);


        final Path confDirPath = Paths.get(configDirPath);
        final DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.loadFrom(confDirPath.toFile());

        final SystemConfig systemConfig = new SystemConfig();
        systemConfig.loadFrom(confDirPath.toFile());

        // @todo 2 tb/tb parametrize geometry factory type 2015-12-16
        final GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        final Storage storage = Storage.create(databaseConfig.getDataSource(), geometryFactory);
        if (!storage.isInitialized()) {
            storage.initialize();
        }

        try {
            ingestMetadata(systemConfig, geometryFactory, storage, sensorType, processingVersion, startDate, endDate);
        } finally {
            storage.close();
        }
    }

    private void ingestMetadata(SystemConfig systemConfig, GeometryFactory geometryFactory,
                                Storage storage, String sensorType, String processingVersion,
                                Date startDate, Date endDate) throws SQLException, IOException {

        // @todo 2 tb/** the wildcard pattern should be supplied by the reader 2015-12-22
        // @todo 2 tb/** extend expression to run recursively through a file tree, write tests for this 2015-12-22

        final ServicesUtils servicesUtils = new ServicesUtils<>();
        final Reader reader = (Reader) servicesUtils.getServices(Reader.class, sensorType);
        final Path archiveRootPath = Paths.get(systemConfig.getArchiveRoot());

        final Archive archive = new Archive(archiveRootPath);
        final Path[] productPaths =  archive.get(startDate,endDate, processingVersion, sensorType);


        for (final Path file : productPaths) {
            reader.open(file.toFile());
            try {
                final AcquisitionInfo acquisitionInfo = reader.read();
                final Polygon polygon = geometryFactory.createPolygon(acquisitionInfo.getCoordinates());


                // build polygon from list of points
                // test if polygon is valid
                // if not
                // -- call reader.refineGeometry()
                // else
                // -- set up SatelliteObservation object and ingest

                final SatelliteObservation satelliteObservation = new SatelliteObservation();
                final Sensor sensor = new Sensor();
                sensor.setName(sensorType);
                satelliteObservation.setSensor(sensor);

                satelliteObservation.setStartTime(acquisitionInfo.getSensingStart());
                satelliteObservation.setStopTime(acquisitionInfo.getSensingStop());
                satelliteObservation.setDataFile(file.toFile().getAbsoluteFile());

                Geometry geometry;
                if (acquisitionInfo.getMultiPolygons() == null) {
                    //todo: mba to specify which Geometry library to use on each reader. 2016-19-02
                    geometry = new GeometryFactory(GeometryFactory.Type.JTS).createPolygon(acquisitionInfo.getCoordinates());
                } else {
                    if (acquisitionInfo.getMultiPolygons().size() > 0) {
                        geometry = geometryFactory.createMultiPolygon(acquisitionInfo.getMultiPolygons());
                    } else {
                        geometry = geometryFactory.createPolygon(acquisitionInfo.getCoordinates());
                    }
                }
                satelliteObservation.setGeoBounds(geometry);
                storage.insert(satelliteObservation);
            } finally {
                reader.close();
            }
        }
    }



    public Path getInputProductPath(CommandLine commandLine, SystemConfig systemConfig) {
        final String configValue = commandLine.getOptionValue("config");
        final String sensorType = commandLine.getOptionValue("s");

        final String startDateCommmandLineInput = commandLine.getOptionValue("start");
        final String endDateCommmandLineInput = commandLine.getOptionValue("end");

        final String version = commandLine.getOptionValue("v");
        final String concurrent = commandLine.getOptionValue("concurrent");

        Date startDate = TimeUtils.parseDOYBeginOfDay(startDateCommmandLineInput);
        Date endDate = TimeUtils.parseDOYBeginOfDay(endDateCommmandLineInput);

        Calendar instance = Calendar.getInstance();
        instance.setTime(startDate);

        String year = Integer.toString(instance.get(Calendar.YEAR));
        String s = systemConfig.getArchiveRoot() + FileSystems.getDefault().getSeparator() + year;
        return new File(s).toPath();
    }

    void printUsageTo(OutputStream outputStream) {
        final String ls = System.lineSeparator();
        final PrintWriter writer = new PrintWriter(outputStream);
        writer.write("ingestion-tool version " + VERSION);
        writer.write(ls + ls);

        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp(writer, 120, "ingestion-tool <options>", "Valid options are:", getOptions(), 3, 3, "");

        writer.flush();
    }

}
