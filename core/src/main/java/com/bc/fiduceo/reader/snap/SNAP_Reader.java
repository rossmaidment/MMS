package com.bc.fiduceo.reader.snap;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.*;
import com.bc.fiduceo.util.NetCDFUtils;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static ucar.ma2.DataType.*;

public abstract class SNAP_Reader implements Reader {

    protected final GeometryFactory geometryFactory;

    protected Product product;
    protected PixelLocator pixelLocator;

    protected SNAP_Reader(ReaderContext readerContext) {
        this.geometryFactory = readerContext.getGeometryFactory();
    }

    protected void open(File file, String formatName) throws IOException {
        product = ProductIO.readProduct(file, formatName);
        if (product == null) {
            throw new IOException("Unable to read product of type '" + formatName + "`': " + file.getAbsolutePath());
        }
        pixelLocator = null;
    }

    @Override
    public void close() throws IOException {
        pixelLocator = null;
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    public AcquisitionInfo read(Interval interval, int numSplits) throws IOException {
        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();

        extractSensingTimes(acquisitionInfo);

        acquisitionInfo.setNodeType(NodeType.UNDEFINED);

        final Geometries geometries = calculateGeometries(interval, numSplits);
        acquisitionInfo.setBoundingGeometry(geometries.getBoundingGeometry());
        ReaderUtils.setTimeAxes(acquisitionInfo, geometries.getTimeAxesGeometry(), geometryFactory);

        return acquisitionInfo;
    }

    @Override
    public Dimension getProductSize() {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();

        return new Dimension("product_size", width, height);
    }

    @Override
    public PixelLocator getPixelLocator() {
        if (pixelLocator == null) {
            final GeoCoding geoCoding = product.getSceneGeoCoding();

            pixelLocator = new SNAP_PixelLocator(geoCoding);
        }
        return pixelLocator;
    }

    @Override
    public List<Variable> getVariables() {
        final List<Variable> result = new ArrayList<>();

        final Band[] bands = product.getBands();
        for (final Band band : bands) {
            final VariableProxy variableProxy = new VariableProxy(band);
            result.add(variableProxy);
        }

        final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
        for (final TiePointGrid tiePointGrid : tiePointGrids) {
            final VariableProxy variableProxy = new VariableProxy(tiePointGrid);
            result.add(variableProxy);
        }

        return result;
    }

    private void extractSensingTimes(AcquisitionInfo acquisitionInfo) {
        final ProductData.UTC startTime = product.getStartTime();
        acquisitionInfo.setSensingStart(startTime.getAsDate());

        final ProductData.UTC endTime = product.getEndTime();
        acquisitionInfo.setSensingStop(endTime.getAsDate());
    }

    protected RasterDataNode getRasterDataNode(String variableName) {
        final RasterDataNode dataNode;
        if (product.containsBand(variableName)) {
            dataNode = product.getBand(variableName);
        } else if (product.containsTiePointGrid(variableName)) {
            dataNode = product.getTiePointGrid(variableName);
        } else {
            dataNode = product.getMaskGroup().get(variableName);
        }
        if (dataNode == null) {
            throw new RuntimeException("Requested variable not contained in product: " + variableName);
        }
        return dataNode;
    }

    private Geometries calculateGeometries(Interval interval, int numSplits) throws IOException {
        final Geometries geometries = new Geometries();

        final TiePointGrid longitude = product.getTiePointGrid(getLongitudeVariableName());
        final TiePointGrid latitude = product.getTiePointGrid(getLatitudeVariableName());

        final int[] shape = new int[2];
        shape[0] = longitude.getGridHeight();
        shape[1] = longitude.getGridWidth();

        final DataType netcdfDataType = NetCDFUtils.getNetcdfDataType(longitude.getDataType());
        if (netcdfDataType == null) {
            throw new IOException("Unsupported data type: " + longitude.getDataType());
        }

        final ProductData longitudeGridData = longitude.getGridData();
        final ProductData latitudeGridData = latitude.getGridData();
        final Array lonArray = Array.factory(netcdfDataType, shape, longitudeGridData.getElems());
        final Array latArray = Array.factory(netcdfDataType, shape, latitudeGridData.getElems());

        Geometry timeAxisGeometry;
        final BoundingPolygonCreator boundingPolygonCreator = new BoundingPolygonCreator(interval, geometryFactory);
        Geometry boundingGeometry = boundingPolygonCreator.createBoundingGeometry(lonArray, latArray);
        if (!boundingGeometry.isValid()) {
            boundingGeometry = boundingPolygonCreator.createBoundingGeometrySplitted(lonArray, latArray, numSplits, false);
            if (!boundingGeometry.isValid()) {
                throw new RuntimeException("Invalid bounding geometry detected");
            }
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometrySplitted(lonArray, latArray, numSplits);
        } else {
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometry(lonArray, latArray);
        }

        geometries.setBoundingGeometry(boundingGeometry);
        geometries.setTimeAxesGeometry(timeAxisGeometry);

        return geometries;
    }

    protected static ProductData createProductData(DataType dataType, int rasterSize) {
        final ProductData productData;
        if (dataType == FLOAT) {
            productData = ProductData.createInstance(ProductData.TYPE_FLOAT32, rasterSize);
        } else if (dataType == INT) {
            productData = ProductData.createInstance(ProductData.TYPE_INT32, rasterSize);
        } else if (dataType == SHORT) {
            productData = ProductData.createInstance(ProductData.TYPE_INT16, rasterSize);
        } else if (dataType == BYTE) {
            productData = ProductData.createInstance(ProductData.TYPE_INT8, rasterSize);
        } else {
            throw new RuntimeException("Data type not supported");
        }
        return productData;
    }

    protected static double getGeophysicalNoDataValue(RasterDataNode dataNode) {
        if (dataNode.isNoDataValueUsed()) {
            return dataNode.getGeophysicalNoDataValue();
        } else {
            final int dataType = dataNode.getDataType();
            return ReaderUtils.getDefaultFillValue(dataType).doubleValue();
        }
    }

    protected static double getNoDataValue(RasterDataNode dataNode) {
        if (dataNode.isNoDataValueUsed()) {
            return dataNode.getNoDataValue();
        } else {
            final int dataType = dataNode.getDataType();
            return ReaderUtils.getDefaultFillValue(dataType).doubleValue();
        }
    }

    protected static Array createReadingArray(DataType targetDataType, int[] shape) {
        switch (targetDataType) {
            case FLOAT:
                return Array.factory(DataType.FLOAT, shape);
            case INT:
                return Array.factory(DataType.INT, shape);
            case SHORT:
                return Array.factory(DataType.INT, shape);
            case BYTE:
                return Array.factory(DataType.BYTE, shape);
            default:
                throw new RuntimeException("unsupported data type: " + targetDataType);
        }
    }

    protected static int[] getShape(Interval interval) {
        final int[] shape = new int[2];
        shape[0] = interval.getY();
        shape[1] = interval.getX();

        return shape;
    }
}