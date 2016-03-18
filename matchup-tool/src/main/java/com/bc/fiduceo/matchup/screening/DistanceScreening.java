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

package com.bc.fiduceo.matchup.screening;

import com.bc.fiduceo.matchup.MatchupCollection;
import com.bc.fiduceo.matchup.MatchupSet;
import com.bc.fiduceo.matchup.Sample;
import com.bc.fiduceo.matchup.SampleSet;
import org.esa.snap.core.util.math.RsMathUtils;
import org.esa.snap.core.util.math.SphericalDistance;

import java.util.ArrayList;
import java.util.List;

public class DistanceScreening implements Screening {

    private final double MEAN_EARTH_RADIUS_IN_KM = RsMathUtils.MEAN_EARTH_RADIUS / 1000d;
    private final double maxDeltaInKm;

    public DistanceScreening(double maxDeltaInKm) {
        this.maxDeltaInKm = maxDeltaInKm;
    }

    @Override
    public MatchupCollection screen(MatchupCollection matchupCollection) {
        final List<MatchupSet> matchupSets = matchupCollection.getSets();
        for (final MatchupSet matchupSet : matchupSets) {
            final List<SampleSet> sourceSamples = matchupSet.getSampleSets();
            final List<SampleSet> targetSamples = new ArrayList<>();
            for (final SampleSet sampleSet : sourceSamples) {
                final Sample primary = sampleSet.getPrimary();
                final Sample secondary = sampleSet.getSecondary();
                final SphericalDistance sphericalDistance = new SphericalDistance(primary.lon, primary.lat);
                final double radDistance = sphericalDistance.distance(secondary.lon, secondary.lat);
                final double kmDistance = radDistance * MEAN_EARTH_RADIUS_IN_KM;
                if (kmDistance <= maxDeltaInKm) {
                    targetSamples.add(sampleSet);
                }
            }
            matchupSet.setSampleSets(targetSamples);
            sourceSamples.clear();
        }
        return matchupCollection;
    }
}