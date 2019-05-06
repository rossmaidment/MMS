package com.bc.fiduceo.reader.fiduceo_fcdr;

import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.location.PixelLocatorFactory;
import com.bc.fiduceo.reader.BoundingPolygonCreator;
import com.bc.fiduceo.reader.Geometries;
import com.bc.fiduceo.reader.ReaderContext;
import com.bc.fiduceo.reader.netcdf.NetCDFReader;
import com.bc.fiduceo.util.NetCDFUtils;
import com.bc.fiduceo.util.TimeUtils;
import ucar.ma2.*;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

abstract class FCDR_Reader extends NetCDFReader {

    private static final int NUM_SPLITS = 2;

    private static final String LONGITUDE_VAR_NAME = "longitude";
    private static final String LATITUDE_VAR_NAME = "latitude";

    private BoundingPolygonCreator boundingPolygonCreator;

    protected final GeometryFactory geometryFactory;
    protected File file;
    protected PixelLocator pixelLocator;

    FCDR_Reader(ReaderContext readerContext) {
        this.geometryFactory = readerContext.getGeometryFactory();
    }

    @Override
    public void open(File file) throws IOException {
        super.open(file);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        boundingPolygonCreator = null;
        pixelLocator = null;
        file = null;

        super.close();
    }

    @Override
    public int[] extractYearMonthDayFromFilename(String fileName) {
        final Date date = FCDRUtils.parseStartDate(fileName);
        final Calendar utcCalendar = TimeUtils.getUTCCalendar();
        utcCalendar.setTime(date);
        final int[] ymd = new int[3];
        ymd[0] = utcCalendar.get(Calendar.YEAR);
        ymd[1] = utcCalendar.get(Calendar.MONTH) + 1;
        ymd[2] = utcCalendar.get(Calendar.DAY_OF_MONTH);
        return ymd;
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        if (pixelLocator == null) {
            final ArrayDouble lonStorage = (ArrayDouble) arrayCache.getScaled(LONGITUDE_VAR_NAME, "scale_factor", "add_offset");
            final ArrayDouble latStorage = (ArrayDouble) arrayCache.getScaled(LATITUDE_VAR_NAME, "scale_factor", "add_offset");
            final int[] shape = lonStorage.getShape();
            final int width = shape[1];
            final int height = shape[0];
            pixelLocator = PixelLocatorFactory.getSwathPixelLocator(lonStorage, latStorage, width, height);
        }
        return pixelLocator;
    }

    @Override
    public String getLongitudeVariableName() {
        return LONGITUDE_VAR_NAME;
    }

    @Override
    public String getLatitudeVariableName() {
        return LATITUDE_VAR_NAME;
    }

    Geometries calculateGeometries(boolean clockwise, Interval interval) throws IOException {
        final BoundingPolygonCreator boundingPolygonCreator = getBoundingPolygonCreator(interval);
        final Geometries geometries = new Geometries();

        final Array longitudes = arrayCache.getScaled(LONGITUDE_VAR_NAME, "scale_factor", "add_offset");
        final Array latitudes = arrayCache.getScaled(LATITUDE_VAR_NAME, "scale_factor", "add_offset");
        Geometry timeAxisGeometry;
        Geometry boundingGeometry = boundingPolygonCreator.createBoundingGeometry(longitudes, latitudes);
        if (!boundingGeometry.isValid()) {
            boundingGeometry = boundingPolygonCreator.createBoundingGeometrySplitted(longitudes, latitudes, NUM_SPLITS, clockwise);
            if (!boundingGeometry.isValid()) {
                throw new RuntimeException("Invalid bounding geometry detected");
            }
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometrySplitted(longitudes, latitudes, NUM_SPLITS);
        } else {
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometry(longitudes, latitudes);
        }

        geometries.setBoundingGeometry(boundingGeometry);
        geometries.setTimeAxesGeometry(timeAxisGeometry);
        return geometries;
    }

    PixelLocator getSubScenePixelLocator(Polygon sceneGeometry, Interval stepInterval) throws IOException {
        final Array longitudes = arrayCache.get(LONGITUDE_VAR_NAME);
        final int[] shape = longitudes.getShape();
        final int height = shape[0];
        final int width = shape[1];
        final int subsetHeight = getBoundingPolygonCreator(stepInterval).getSubsetHeight(height, NUM_SPLITS);
        final PixelLocator pixelLocator = getPixelLocator();

        return PixelLocatorFactory.getSubScenePixelLocator(sceneGeometry, width, height, subsetHeight, pixelLocator);
    }

    protected ArrayInt.D2 readAcquisitionTime(int x, int y, Interval interval, String timeVariableName) throws IOException, InvalidRangeException {
        final Array rawTimeArray = readRaw(x, y, interval, timeVariableName);

        final Number fillValue = getFillValue(timeVariableName);
        final int[] shape = rawTimeArray.getShape();
        int height = shape[0];
        int width = shape[1];
        final ArrayInt.D2 integerTimeArray = new ArrayInt.D2(height, width);
        final int targetFillValue = (int) NetCDFUtils.getDefaultFillValue(DataType.INT, false);
        final Index index = rawTimeArray.getIndex();
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                index.set(i, j);
                final double rawTime = rawTimeArray.getDouble(index);
                if (!fillValue.equals(rawTime)) {
                    integerTimeArray.set(i, j, (int) Math.round(rawTime));
                } else {
                    integerTimeArray.set(i, j, targetFillValue);
                }
            }
        }

        return integerTimeArray;
    }

    private BoundingPolygonCreator getBoundingPolygonCreator(Interval interval) {
        if (boundingPolygonCreator == null) {
            boundingPolygonCreator = new BoundingPolygonCreator(interval, geometryFactory);
        }

        return boundingPolygonCreator;
    }
}