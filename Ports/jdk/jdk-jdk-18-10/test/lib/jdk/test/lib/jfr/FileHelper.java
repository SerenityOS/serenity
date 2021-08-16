/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.test.lib.jfr;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import jdk.test.lib.Asserts;

/**
 * Common helper class.
 */
public class FileHelper {

    public static Path getDest(String subPath) throws IOException {
        Path path = Paths.get(subPath + "/test.jfr");
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("No parent cound be found for path " + subPath);
        }
        Files.createDirectories(parent);
        return path;
    }

    public static Path createLongDir(Path root) throws IOException {
        final int minPathLength = 400;
        StringBuilder buff = new StringBuilder();
        buff.append(root.toString());
        while (buff.length() < minPathLength) {
            buff.append("/veryLongPath012345678901234567890123456789");
        }
        Path path = Paths.get(buff.toString());
        System.out.println("long dir=" + path);
        Files.createDirectories(path);
        return path;
    }

    public static Path getDestReadOnly(String subPath) throws IOException {
        final Path path = getDest(subPath);
        Path parent = path.getParent();
        if (parent == null) {
            throw new IOException("No parent cound be found for path " + subPath);
        }
        parent.toFile().setReadOnly();
        return path;
    }

    public static Path createReadOnlyFile(Path path) throws IOException {
        final Path createdPath = Files.createFile(path);
        createdPath.toFile().setReadOnly();
        return createdPath;
    }

    public static Path createReadOnlyDir(Path path) throws IOException {
        final Path createdPath = Files.createDirectories(path);
        createdPath.toFile().setReadOnly();
        return createdPath;
    }

    public static Path getDestNotExist() {
        return Paths.get(".", "thisDirDoesNotExist/test.jfr");
    }

    public static boolean isReadOnlyPath(Path path) throws IOException {
        // Files.isWritable(path) can not really be trusted. At least not on Windows.
        // If path is a directory, we try to create a new file in it.
        if (Files.isDirectory(path)) {
            try {
                Path f = Files.createFile(Paths.get(path.toString(), "dummyFileToCheckReadOnly"));
                System.out.printf("Dir is not read-only, created %s, exists=%b%n", f, Files.exists(f));
                return false;
            } catch (AccessDeniedException e) {
                System.out.printf("'%s' verified read-only by %s%n", path, e.toString());
                return true;
            }
        } else {
            boolean isReadOnly = !Files.isWritable(path);
            System.out.format("isReadOnly '%s': %b%n", path, isReadOnly);
            return isReadOnly;
        }
    }

    public static void verifyRecording(File file) throws Exception {
        Asserts.assertTrue(file.exists(), file.getAbsolutePath() + " does not exist");
        Asserts.assertTrue(file.isFile(), file.getAbsolutePath() + " is not a file");
        Asserts.assertGreaterThan(file.length(), 0L, "Size of recording is 0.");
        List<RecordedEvent> events = RecordingFile.readAllEvents(file.toPath());
        for (RecordedEvent event : events) {
            System.out.printf("First event in recording '%s':%n%s", file.getName(), event);
            return;
        }
        Asserts.fail("No events in file " + file.getName());
    }

}
