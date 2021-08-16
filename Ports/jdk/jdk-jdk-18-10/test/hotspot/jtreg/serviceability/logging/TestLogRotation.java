/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test TestLogRotation.java
 * @summary test flags for log rotation
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run driver/timeout=600 TestLogRotation
 *
 */
import jdk.test.lib.process.ProcessTools;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;

class GCLoggingGenerator {

    public static void main(String[] args) throws Exception {

        long sizeOfLog = Long.parseLong(args[0]);
        long lines = sizeOfLog / 70;
        // full.GC generates ad least 1-line which is not shorter then 70 chars
        // for some GC 2 shorter lines are generated
        for (long i = 0; i < lines; i++) {
            System.gc();
        }
    }
}

public class TestLogRotation {

    static final File currentDirectory = new File(".");
    static final String logFileName = "test.log";
    static final int logFileSizeK = 16;
    static FilenameFilter logFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(logFileName);
        }
    };

    public static void cleanLogs() {
        for (File log : currentDirectory.listFiles(logFilter)) {
            if (!log.delete()) {
                throw new Error("Unable to delete " + log.getAbsolutePath());
            }
        }
    }

    public static void runTest(int numberOfFiles) throws Exception {
        ProcessBuilder pb = ProcessTools.createTestJvm(
                "-cp", System.getProperty("java.class.path"),
                "-Xlog:gc=debug:" + logFileName
                        + "::filesize=" + logFileSizeK + "k"
                        + ",filecount=" + numberOfFiles,
                "-XX:-DisableExplicitGC", // to ensure that System.gc() works
                "-Xmx128M",
                GCLoggingGenerator.class.getName(),
                String.valueOf(numberOfFiles * logFileSizeK * 1024));
        pb.redirectErrorStream(true);
        pb.redirectOutput(new File(GCLoggingGenerator.class.getName() + ".log"));
        Process process = pb.start();
        int result = process.waitFor();
        if (result != 0) {
            throw new Error("Unexpected exit code = " + result);
        }
        File[] logs = currentDirectory.listFiles(logFilter);
        int smallFilesNumber = 0;
        for (File log : logs) {
            if (log.length() < logFileSizeK * 1024) {
                smallFilesNumber++;
            }
        }
        // Expect one more log file since the number-of-files doesn't include the active log file
        int expectedNumberOfFiles = numberOfFiles + 1;
        if (logs.length != expectedNumberOfFiles) {
            throw new Error("There are " + logs.length + " logs instead of the expected " + expectedNumberOfFiles);
        }
        if (smallFilesNumber > 1) {
            throw new Error("There should maximum one log with size < " + logFileSizeK + "K");
        }
    }

    public static void main(String[] args) throws Exception {
        cleanLogs();
        runTest(1);
        cleanLogs();
        runTest(3);
        cleanLogs();
    }
}
