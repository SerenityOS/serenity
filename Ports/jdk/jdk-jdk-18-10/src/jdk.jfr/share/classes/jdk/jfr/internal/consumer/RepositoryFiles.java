/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal.consumer;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jdk.jfr.internal.LogLevel;
import jdk.jfr.internal.LogTag;
import jdk.jfr.internal.Logger;
import jdk.jfr.internal.Repository;
import jdk.jfr.internal.SecuritySupport.SafePath;

public final class RepositoryFiles {
    private static final Object WAIT_OBJECT = new Object();
    private static final String DIRECTORY_PATTERN = "DDDD_DD_DD_DD_DD_DD_";
    public static void notifyNewFile() {
        synchronized (WAIT_OBJECT) {
            WAIT_OBJECT.notifyAll();
        }
    }

    private final FileAccess fileAccess;
    private final NavigableMap<Long, Path> pathSet = new TreeMap<>();
    private final Map<Path, Long> pathLookup = new HashMap<>();
    private final Object waitObject;
    private boolean allowSubDirectory;
    private volatile boolean closed;
    private Path repository;

    public RepositoryFiles(FileAccess fileAccess, Path repository, boolean allowSubDirectory) {
        this.repository = repository;
        this.fileAccess = fileAccess;
        this.waitObject = repository == null ? WAIT_OBJECT : new Object();
        this.allowSubDirectory = allowSubDirectory;
    }

    long getTimestamp(Path p) {
        return pathLookup.get(p);
    }

    public Path lastPath(boolean wait) {
        if (updatePaths(wait)) {
            return pathSet.lastEntry().getValue();
        }
        return null; // closed
    }

    public Path firstPath(long startTimeNanos, boolean wait) {
        if (updatePaths(wait)) {
            // Pick closest chunk before timestamp
            Long time = pathSet.floorKey(startTimeNanos);
            if (time != null) {
                startTimeNanos = time;
            }
            return path(startTimeNanos, wait);
        }
        return null; // closed
    }

    private boolean updatePaths(boolean wait) {
        int beforeSize = pathLookup.size();
        while (!closed) {
            try {
                if (updatePaths()) {
                    break;
                }
            } catch (IOException e) {
                Logger.log(LogTag.JFR_SYSTEM_STREAMING, LogLevel.DEBUG, "IOException during repository file scan " + e.getMessage());
                // This can happen if a chunk is being removed
                // between the file was discovered and an instance
                // was accessed, or if new file has been written yet
                // Just ignore, and retry later.
            }
            if (wait) {
                nap();
            } else {
                return pathLookup.size() > beforeSize;
            }
        }
        return !closed;
    }

    public Path nextPath(long startTimeNanos, boolean wait) {
        if (closed) {
            return null;
        }
        // Try to get the 'exact' path first
        // to avoid skipping files if repository
        // is updated while DirectoryStream
        // is traversing it
        Path path = pathSet.get(startTimeNanos);
        if (path != null) {
            return path;
        }
        // Update paths
        try {
            updatePaths();
        } catch (IOException e) {
            // ignore
        }
        // try to get the next file
        return path(startTimeNanos, wait);
    }

    private Path path(long timestamp, boolean wait) {
        if (closed) {
            return null;
        }
        while (true) {
            SortedMap<Long, Path> after = pathSet.tailMap(timestamp);
            if (!after.isEmpty()) {
                Path path = after.get(after.firstKey());
                if (Logger.shouldLog(LogTag.JFR_SYSTEM_STREAMING, LogLevel.TRACE)) {
                    Logger.log(LogTag.JFR_SYSTEM_STREAMING, LogLevel.TRACE, "Return path " + path + " for start time nanos " + timestamp);
                }
                return path;
            }
            if (!updatePaths(wait)) {
                return null; // closed
            }
        }
    }

    private void nap() {
        try {
            synchronized (waitObject) {
                waitObject.wait(1000);
            }
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private boolean updatePaths() throws IOException {
        boolean foundNew = false;
        Path repoPath = repository;

        if (allowSubDirectory) {
            Path subDirectory = findSubDirectory(repoPath);
            if (subDirectory != null) {
                repoPath = subDirectory;
            }
        }

        if (repoPath == null) {
            // Always get the latest repository if 'jcmd JFR.configure
            // repositorypath=...' has been executed
            SafePath sf = Repository.getRepository().getRepositoryPath();
            if (sf == null) {
                return false; // not initialized
            }
            repoPath = sf.toPath();
        }

        try (DirectoryStream<Path> dirStream = fileAccess.newDirectoryStream(repoPath)) {
            List<Path> added = new ArrayList<>();
            Set<Path> current = new HashSet<>();
            for (Path p : dirStream) {
                if (!pathLookup.containsKey(p)) {
                    String s = p.toString();
                    if (s.endsWith(".jfr")) {
                        added.add(p);
                        Logger.log(LogTag.JFR_SYSTEM_STREAMING, LogLevel.DEBUG, "New file found: " + p.toAbsolutePath());
                    }
                    current.add(p);
                }
            }
            List<Path> removed = new ArrayList<>();
            for (Path p : pathLookup.keySet()) {
                if (!current.contains(p)) {
                    removed.add(p);
                }
            }

            for (Path remove : removed) {
                Long time = pathLookup.get(remove);
                pathSet.remove(time);
                pathLookup.remove(remove);
            }
            Collections.sort(added, (p1, p2) -> p1.compareTo(p2));
            for (Path p : added) {
                // Only add files that have a complete header
                // as the JVM may be in progress writing the file
                long size = fileAccess.fileSize(p);
                if (size >= ChunkHeader.headerSize()) {
                    long startNanos = readStartTime(p);
                    if (startNanos != -1) {
                        pathSet.put(startNanos, p);
                        pathLookup.put(p, startNanos);
                        foundNew = true;
                    }
                }
            }
            if (allowSubDirectory && foundNew) {
                // Found a valid file, possibly in a subdirectory.
                // Use the same (sub)directory from now on.
                repository = repoPath;
                allowSubDirectory = false;
            }

            return foundNew;
        }
    }

    private Path findSubDirectory(Path repoPath) {
        FileTime latestTimestamp = null;
        Path latestPath = null;
        try (DirectoryStream<Path> dirStream = fileAccess.newDirectoryStream(repoPath)) {
            for (Path p : dirStream) {
                String filename = p.getFileName().toString();
                if (isRepository(filename) && fileAccess.isDirectory(p)) {
                    FileTime timestamp = getLastModified(p);
                    if (timestamp != null) {
                        if (latestPath == null || latestTimestamp.compareTo(timestamp) <= 0) {
                            latestPath = p;
                            latestTimestamp = timestamp;
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return latestPath;
    }

    private FileTime getLastModified(Path p) {
        try {
            return fileAccess.getLastModified(p);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean isRepository(String filename) {
        if (filename.length() < DIRECTORY_PATTERN.length()) {
            return false;
        }
        for (int i = 0; i < DIRECTORY_PATTERN.length(); i++) {
            char expected = DIRECTORY_PATTERN.charAt(i);
            char c = filename.charAt(i);
            if (expected == 'D' && !Character.isDigit(c)) {
                return false;
            }
            if (expected == '_' && c != '_') {
                return false;
            }
        }
        return true;
    }

    private long readStartTime(Path p) {
        try (RecordingInput in = new RecordingInput(p.toFile(), fileAccess, 100)) {
            Logger.log(LogTag.JFR_SYSTEM_PARSER, LogLevel.INFO, "Parsing header for chunk start time");
            ChunkHeader c = new ChunkHeader(in);
            return c.getStartNanos();
        } catch (IOException ioe) {
            return -1;
        }
    }

    void close() {
        synchronized (waitObject) {
            this.closed = true;
            waitObject.notify();
        }
    }

    public boolean hasFixedPath() {
        return repository != null;
    }
}
