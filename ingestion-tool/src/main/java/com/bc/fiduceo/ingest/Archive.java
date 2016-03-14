package com.bc.fiduceo.ingest;

import com.bc.fiduceo.log.FiduceoLogger;
import com.bc.fiduceo.util.TimeUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.logging.Logger;

class Archive {

    private Path rootPath;
    private final Logger log;

    public Archive(Path rootPath) {
        this.rootPath = rootPath;
        log = FiduceoLogger.getLogger();
    }

    public Path[] get(Date startDate, Date endDate, String processingVersion, String sensorType) throws IOException {
        final ArrayList<Path> pathArrayList = new ArrayList<>();
        final Calendar instance = TimeUtils.getUTCCalendar();
        instance.setTime(startDate);

        while (instance.getTime().compareTo(endDate) <= 0) {
            final int year = instance.get(Calendar.YEAR);
            final int month = instance.get(Calendar.MONTH) + 1;
            final int day = instance.get(Calendar.DAY_OF_MONTH);
            final Path productsDir = createValidProductPath(processingVersion, sensorType, year, month, day);

            if (Files.exists(productsDir)) {
                log.info("The product directory :" + productsDir.toString());
                final Iterator<Path> iterator = Files.list(productsDir).iterator();
                while (iterator.hasNext()) {
                    pathArrayList.add(iterator.next());
                }
            } else {
                System.err.println("The directory doest not exist: " + productsDir.toString());
            }
            instance.add(Calendar.DAY_OF_MONTH, 1);
        }
        return pathArrayList.toArray(new Path[0]);
    }

    Path createValidProductPath(String processingVersion, String sensorType, int year, int month, int day) {
        return rootPath.resolve(sensorType)
                .resolve(processingVersion)
                .resolve("" + year)
                .resolve(String.format("%02d", month))
                .resolve(String.format("%02d", day));
    }
}
