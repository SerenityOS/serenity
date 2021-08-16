/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;

import jdk.jfr.internal.SecuritySupport.SafePath;
import jdk.jfr.internal.management.ChunkFilename;

public final class Repository {

    private static final int MAX_REPO_CREATION_RETRIES = 1000;
    private static final JVM jvm = JVM.getJVM();
    private static final Repository instance = new Repository();

    private static final String JFR_REPOSITORY_LOCATION_PROPERTY = "jdk.jfr.repository";

    private final Set<SafePath> cleanupDirectories = new HashSet<>();
    private SafePath baseLocation;
    private SafePath repository;
    private ChunkFilename chunkFilename;

    private Repository() {
    }

    public static Repository getRepository() {
        return instance;
    }

    public synchronized void setBasePath(SafePath baseLocation) throws IOException {
        if(baseLocation.equals(this.baseLocation)) {
            Logger.log(LogTag.JFR, LogLevel.INFO, "Same base repository path " + baseLocation.toString() + " is set");
            return;
        }
        // Probe to see if repository can be created, needed for fail fast
        // during JVM startup or JFR.configure
        this.repository = createRepository(baseLocation);
        this.chunkFilename = null;
        try {
            // Remove so we don't "leak" repositories, if JFR is never started
            // and shutdown hook not added.
            SecuritySupport.delete(repository);
        } catch (IOException ioe) {
            Logger.log(LogTag.JFR, LogLevel.INFO, "Could not delete disk repository " + repository);
        }
        this.baseLocation = baseLocation;
    }

    public synchronized void ensureRepository() throws IOException {
        if (baseLocation == null) {
            setBasePath(SecuritySupport.JAVA_IO_TMPDIR);
        }
    }

    synchronized RepositoryChunk newChunk() {
        ZonedDateTime timestamp = ZonedDateTime.now();
        try {
            if (!SecuritySupport.existDirectory(repository)) {
                this.repository = createRepository(baseLocation);
                jvm.setRepositoryLocation(repository.toString());
                SecuritySupport.setProperty(JFR_REPOSITORY_LOCATION_PROPERTY, repository.toString());
                cleanupDirectories.add(repository);
                chunkFilename = null;
            }
            if (chunkFilename == null) {
                chunkFilename = ChunkFilename.newPriviliged(repository.toPath());
            }
            String filename = chunkFilename.next(timestamp.toLocalDateTime());
            return new RepositoryChunk(new SafePath(filename));
        } catch (Exception e) {
            String errorMsg = String.format("Could not create chunk in repository %s, %s: %s", repository, e.getClass(), e.getMessage());
            Logger.log(LogTag.JFR, LogLevel.ERROR, errorMsg);
            jvm.abort(errorMsg);
            throw new InternalError("Could not abort after JFR disk creation error");
        }
    }

    private static SafePath createRepository(SafePath basePath) throws IOException {
        SafePath canonicalBaseRepositoryPath = createRealBasePath(basePath);
        SafePath f = null;

        String basename = Utils.formatDateTime(LocalDateTime.now()) + "_" + JVM.getJVM().getPid();
        String name = basename;

        int i = 0;
        for (; i < MAX_REPO_CREATION_RETRIES; i++) {
            f = new SafePath(canonicalBaseRepositoryPath.toPath().resolve(name));
            if (tryToUseAsRepository(f)) {
                break;
            }
            name = basename + "_" + i;
        }

        if (i == MAX_REPO_CREATION_RETRIES) {
            throw new IOException("Unable to create JFR repository directory using base location (" + basePath + ")");
        }
        return SecuritySupport.toRealPath(f);
    }

    private static SafePath createRealBasePath(SafePath safePath) throws IOException {
        if (SecuritySupport.exists(safePath)) {
            if (!SecuritySupport.isWritable(safePath)) {
                throw new IOException("JFR repository directory (" + safePath.toString() + ") exists, but isn't writable");
            }
            return SecuritySupport.toRealPath(safePath);
        }
        SafePath p = SecuritySupport.createDirectories(safePath);
        return SecuritySupport.toRealPath(p);
    }

    private static boolean tryToUseAsRepository(final SafePath path) {
        Path parent = path.toPath().getParent();
        if (parent == null) {
            return false;
        }
        try {
            try {
                SecuritySupport.createDirectories(path);
            } catch (Exception e) {
                // file already existed or some other problem occurred
            }
            if (!SecuritySupport.exists(path)) {
                return false;
            }
            if (!SecuritySupport.isDirectory(path)) {
                return false;
            }
            return true;
        } catch (IOException io) {
            return false;
        }
    }

    synchronized void clear() {
        for (SafePath p : cleanupDirectories) {
            try {
                SecuritySupport.clearDirectory(p);
                Logger.log(LogTag.JFR, LogLevel.INFO, "Removed repository " + p);
            } catch (IOException e) {
                Logger.log(LogTag.JFR, LogLevel.INFO, "Repository " + p + " could not be removed at shutdown: " + e.getMessage());
            }
        }
    }

    public synchronized SafePath getRepositoryPath() {
        return repository;
    }

    public synchronized SafePath getBaseLocation() {
        return baseLocation;
    }

}
