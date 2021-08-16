/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.startupargs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jdk.jfr.FlightRecorder;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.dcmd.CommandExecutor;
import jdk.test.lib.dcmd.PidJcmdExecutor;
import jdk.test.lib.process.OutputAnalyzer;



public class StartupHelper {

    public static Recording getRecording(String name) {
        for (Recording r : FlightRecorder.getFlightRecorder().getRecordings()) {
            System.out.println("Recording=" + r.getName());
            if (name.equals(r.getName())) {
                return r;
            }
        }
        Asserts.fail("No recording with name " + name);
        return null;
    }

    public static List<Path> listFilesInDir(Path root) throws IOException {
        List<Path> paths = new ArrayList<>();
        List<Path> searchPaths = new ArrayList<>();
        searchPaths.add(root);

        while (!searchPaths.isEmpty()) {
            Path currRoot = searchPaths.remove(searchPaths.size() - 1);
            DirectoryStream<Path> contents = Files.newDirectoryStream(currRoot);
            for (Path path : contents) {
                paths.add(path);
                if (Files.isDirectory(path)) {
                    searchPaths.add(path);
                }
            }
        }

        System.out.println("Files in '" + root + "':");
        for (Path path : paths) {
            System.out.println("  path: " + path);
        }
        return paths;
    }

    public static Path findRecordingFileInRepo(Path baseLocation) throws IOException {
        // Check that repo contains a file ending in "jfr" or "part"
        for (Path path : listFilesInDir(baseLocation)) {
            if (Files.isRegularFile(path)) {
                String filename = path.toString();
                if (filename.endsWith("jfr") || filename.endsWith("part")) {
                    return path;
                }
            }
        }
        return null;
    }

    public static OutputAnalyzer jcmd(String... args) {
        String argsString = Arrays.stream(args).collect(Collectors.joining(" "));
        CommandExecutor executor = new PidJcmdExecutor();
        OutputAnalyzer oa = executor.execute(argsString);
        oa.shouldHaveExitValue(0);
        return oa;
    }

    public static OutputAnalyzer jcmdCheck(String recordingName) {
        return jcmd("JFR.check", "name=" + recordingName, "verbose=true");
    }
}
