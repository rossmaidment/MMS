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

package com.bc.fiduceo.post;

import com.bc.fiduceo.FiduceoConstants;
import com.bc.fiduceo.IOTestRunner;
import com.bc.fiduceo.NCTestUtils;
import com.bc.fiduceo.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(IOTestRunner.class)
public class PostProcessingToolIntegrationTest_Era5 {

    private File configDir;
    private File testDirectory;

    @Before
    public void setUp() throws IOException {
        testDirectory = TestUtil.createTestDirectory();
        configDir = new File(testDirectory, "config");
        if (!configDir.mkdir()) {
            fail("unable to create test directory: " + configDir.getAbsolutePath());
        }

        TestUtil.writeSystemConfig(configDir);
    }

    @After
    public void tearDown() {
        TestUtil.deleteTestDirectory();
    }

    @Test
    public void testAddEra5Variables() throws IOException, InvalidRangeException {
        final File inputDir = getInputDirectory();

        writeConfiguration();

        final String[] args = new String[]{"-c", configDir.getAbsolutePath(), "-start", "2008-149", "-end", "2008-155",
                "-i", inputDir.getAbsolutePath(), "-j", "post-processing-config.xml"};

        PostProcessingToolMain.main(args);

        final File targetFile = new File(testDirectory, "mmd15_sst_drifter-sst_amsre-aq_caliop_vfm-cal_2008-149_2008-155.nc");
        assertTrue(targetFile.isFile());

        try (NetcdfFile mmd = NetcdfFiles.open(targetFile.getAbsolutePath())) {
            NCTestUtils.assertGlobalAttribute(mmd, "era5-collection", "ERA-5");

            Variable variable = NCTestUtils.getVariable("amsre\\.Geostationary_Reflection_Latitude", mmd, false);
            NCTestUtils.assert3DValueDouble(0, 0, 0, 4105, variable);
            NCTestUtils.assert3DValueDouble(1, 0, 0, 4087, variable);

            NCTestUtils.assertDimension(FiduceoConstants.MATCHUP_COUNT, 7, mmd);

            // satellite fields
            NCTestUtils.assertDimension("left", 5, mmd);
            NCTestUtils.assertDimension("right", 7, mmd);
            NCTestUtils.assertDimension("up", 23, mmd);

            variable = NCTestUtils.getVariable("nwp_q", mmd);
            NCTestUtils.assertAttribute(variable, "units", "kg kg**-1");
            NCTestUtils.assert4DVariable(variable.getFullName(), 2, 0, 0, 0, 2.067875129796448E-6, mmd);
            NCTestUtils.assert4DVariable(variable.getFullName(), 2, 0, 10, 0, 4.002843979833415E-6, mmd);
            NCTestUtils.assert4DVariable(variable.getFullName(), 2, 0, 20, 0, 3.6158501188765513E-6, mmd);

            variable = NCTestUtils.getVariable("nwp_lnsp", mmd);
            NCTestUtils.assertAttribute(variable, "long_name", "Logarithm of surface pressure");
            NCTestUtils.assert3DValueDouble(3, 1, 1, 11.514025688171387, variable);
            NCTestUtils.assert3DValueDouble(3, 2, 1, 11.513952255249023, variable);
            NCTestUtils.assert3DValueDouble(3, 3, 1, 11.513876914978027, variable);

            variable = NCTestUtils.getVariable("nwp_v10", mmd);
            assertNull(variable.findAttribute("standard_name"));
            NCTestUtils.assert3DValueDouble(4, 2, 2, 3.7464919090270996, variable);
            NCTestUtils.assert3DValueDouble(4, 3, 2, 3.842674493789673, variable);
            NCTestUtils.assert3DValueDouble(4, 4, 2, 3.5886740684509277, variable);

            variable = NCTestUtils.getVariable("nwp_sst", mmd);
            NCTestUtils.assertAttribute(variable, "_FillValue", "9.969209968386869E36");
            NCTestUtils.assert3DValueDouble(0, 3, 3, 271.46014404296875, variable);
            NCTestUtils.assert3DValueDouble(0, 4, 3, 271.46014404296875, variable);
            NCTestUtils.assert3DValueDouble(0, 5, 3, 271.46014404296875, variable);

            variable = NCTestUtils.getVariable("era5-time", mmd);
            NCTestUtils.assertAttribute(variable, "units", "seconds since 1970-01-01");
            NCTestUtils.assert1DValueLong(2, 1212400800, variable);
            NCTestUtils.assert1DValueLong(6, 1212145200, variable);

            // matchup fields
            NCTestUtils.assertDimension("the_time", 54, mmd);

            variable = NCTestUtils.getVariable("era5-mu-time", mmd);
            NCTestUtils.assertAttribute(variable, "units", "seconds since 1970-01-01");
            NCTestUtils.assert2DValueInt(1, 1, 959796000, variable);
            NCTestUtils.assert2DValueInt(2, 2, 959803200, variable);
            NCTestUtils.assert2DValueInt(3, 2, 959806800, variable);

            variable = NCTestUtils.getVariable("nwp_mu_u10", mmd);
            NCTestUtils.assertAttribute(variable, "units", "m s**-1");
            NCTestUtils.assert2DValueFloat(4, 3, -2.598637819290161f, variable);
            NCTestUtils.assert2DValueFloat(5, 3, -2.281101942062378f, variable);
            NCTestUtils.assert2DValueFloat(6, 3, -2.125869035720825f, variable);

            variable = NCTestUtils.getVariable("nwp_mu_sst", mmd);
            NCTestUtils.assertAttribute(variable, "long_name", "Sea surface temperature");
            NCTestUtils.assert2DValueFloat(7, 4, 271.46014404296875f, variable);
            NCTestUtils.assert2DValueFloat(8, 4, 271.4603576660156f, variable);
            NCTestUtils.assert2DValueFloat(9, 4, 271.4601745605469f, variable);

            variable = NCTestUtils.getVariable("nwp_mu_mslhf", mmd);
            assertNull(variable.findAttribute("standard_name"));
            NCTestUtils.assert2DValueFloat(10, 5, -26.741840362548828f, variable);
            NCTestUtils.assert2DValueFloat(11, 5, -21.49241065979004f, variable);
            NCTestUtils.assert2DValueFloat(12, 5, -17.586181640625f, variable);

            variable = NCTestUtils.getVariable("nwp_mu_msshf", mmd);
            NCTestUtils.assertAttribute(variable, "_FillValue", "9.969209968386869E36");
            NCTestUtils.assert2DValueFloat(13, 6, 1.9936094284057617f, variable);
            NCTestUtils.assert2DValueFloat(14, 6, 2.673461437225342f, variable);
            NCTestUtils.assert2DValueFloat(15, 6, 3.422379732131958f, variable);
        }
    }

    private void writeConfiguration() throws IOException {
        final File testDataDirectory = TestUtil.getTestDataDirectory();
        final File era5Dir = new File(testDataDirectory, "era-5/v1");
        final String postProcessingConfig = "<post-processing-config>\n" +
                "    <create-new-files>\n" +
                "        <output-directory>\n" +
                testDirectory.getAbsolutePath() +
                "        </output-directory>\n" +
                "    </create-new-files>\n" +
                "    <post-processings>\n" +
                "        <era5>\n" +
                "            <nwp-aux-dir>\n" +
                era5Dir.getAbsolutePath() +
                "            </nwp-aux-dir>\n" +
                "            <satellite-fields>" +
                "                <x_dim name='left' length='5' />" +
                "                <y_dim name='right' length='7' />" +
                "                <z_dim name='up' length='23' />" +
                "                <era5_time_variable>era5-time</era5_time_variable>" +
                "                <time_variable>amsre.acquisition_time</time_variable>" +
                "                <longitude_variable>amsre.longitude</longitude_variable>" +
                "                <latitude_variable>amsre.latitude</latitude_variable>" +
                "            </satellite-fields>" +
                "            <matchup-fields>" +
                "                <time_steps_past>41</time_steps_past>" +
                "                <time_steps_future>12</time_steps_future>" +
                "                <time_dim_name>the_time</time_dim_name>" +
                "                <era5_time_variable>era5-mu-time</era5_time_variable>" +
                "                <time_variable>drifter-sst.insitu.time</time_variable>" +
                "                <longitude_variable>drifter-sst.insitu.lon</longitude_variable>" +
                "                <latitude_variable>drifter-sst.insitu.lat</latitude_variable>" +
                "            </matchup-fields>" +
                "        </era5>\n" +
                "    </post-processings>\n" +
                "</post-processing-config>";

        final File postProcessingConfigFile = new File(configDir, "post-processing-config.xml");
        if (!postProcessingConfigFile.createNewFile()) {
            fail("unable to create test file");
        }
        TestUtil.writeStringTo(postProcessingConfigFile, postProcessingConfig);
    }

    private File getInputDirectory() throws IOException {
        final File testDataDirectory = TestUtil.getTestDataDirectory();
        return new File(testDataDirectory, "post-processing/mmd15sst");
    }
}