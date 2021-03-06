/*
 * Copyright (C) 2018 Brockmann Consult GmbH
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

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.util.NetCDFUtils;
import ucar.ma2.Array;

import java.io.IOException;

public class Read2dFrom1d extends WindowReader {

    private final String shortName;
    private final Dimension productSize;
    private ArrayCache arrayCache;
    private Array dataArray;
    private Number fillValue;
    private boolean needData = true;

    public Read2dFrom1d(ArrayCache arrayCache, String shortName, int defaultWidth, Number fillValue) {
        this.shortName = shortName;
        this.productSize = new Dimension("size", defaultWidth, 0);
        this.arrayCache = arrayCache;
        this.fillValue = fillValue;
    }

    @Override
    public Array read(int centerX, int centerY, Interval interval) throws IOException {
        if (needData) {
            initData();
        }
        return RawDataReader.read(centerX, centerY, interval, fillValue, dataArray, productSize);
    }

    private void initData() throws IOException {
        dataArray = arrayCache.get(shortName);
        if (fillValue == null) {
            fillValue = NetCDFUtils.getDefaultFillValue(dataArray);
        }
        needData = false;
    }
}
