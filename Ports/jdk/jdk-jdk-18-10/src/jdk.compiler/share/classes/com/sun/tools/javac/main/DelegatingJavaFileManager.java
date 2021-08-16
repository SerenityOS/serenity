/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.javac.util.Context;

/**
 * A JavaFileManager that delegates to one of two delegate ClassLoaders.
 */
public class DelegatingJavaFileManager implements JavaFileManager {

    public static void installReleaseFileManager(Context context,
                                                 JavaFileManager releaseFM,
                                                 JavaFileManager originalFM) {
        context.put(JavaFileManager.class, (JavaFileManager) null);
        JavaFileManager nue = originalFM instanceof StandardJavaFileManager standardJavaFileManager
                ? new DelegatingSJFM(releaseFM, standardJavaFileManager)
                : new DelegatingJavaFileManager(releaseFM, originalFM);
        context.put(JavaFileManager.class, nue);
    }

    private final JavaFileManager releaseFM;
    private final JavaFileManager baseFM;

    private DelegatingJavaFileManager(JavaFileManager releaseFM, JavaFileManager baseFM) {
        this.releaseFM = releaseFM;
        this.baseFM = baseFM;
    }

    private JavaFileManager delegate(Location location) {
        if (releaseFM.hasLocation(location)) {
            return releaseFM;
        }
        return baseFM;
    }

    @Override
    public ClassLoader getClassLoader(Location location) {
        return delegate(location).getClassLoader(location);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName,
                                         Set<Kind> kinds, boolean recurse) throws IOException {
        return delegate(location).list(location, packageName, kinds, recurse);
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        return delegate(location).inferBinaryName(location, file);
    }

    @Override
    public boolean isSameFile(FileObject a, FileObject b) {
        return baseFM.isSameFile(a, b);
    }

    @Override
    public boolean handleOption(String current, Iterator<String> remaining) {
        return baseFM.handleOption(current, remaining);
    }

    @Override
    public boolean hasLocation(Location location) {
        return releaseFM.hasLocation(location) || baseFM.hasLocation(location);
    }

    @Override
    public JavaFileObject getJavaFileForInput(Location location, String className,
                                              Kind kind) throws IOException {
        return delegate(location).getJavaFileForInput(location, className, kind);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind,
                                               FileObject sibling) throws IOException {
        return delegate(location).getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public FileObject getFileForInput(Location location, String packageName,
                                      String relativeName) throws IOException {
        return delegate(location).getFileForInput(location, packageName, relativeName);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName,
                                       FileObject sibling) throws IOException {
        return delegate(location).getFileForOutput(location, packageName, relativeName, sibling);
    }

    @Override
    public void flush() throws IOException {
        releaseFM.flush();
        baseFM.flush();
    }

    @Override
    public void close() throws IOException {
        releaseFM.close();
        baseFM.close();
    }

    @Override
    public Location getLocationForModule(Location location,
                                         String moduleName) throws IOException {
        return delegate(location).getLocationForModule(location, moduleName);
    }

    @Override
    public Location getLocationForModule(Location location,
                                         JavaFileObject fo) throws IOException {
        return delegate(location).getLocationForModule(location, fo);
    }

    @Override
    public <S> ServiceLoader<S> getServiceLoader(Location location,
                                                 Class<S> service) throws IOException {
        return delegate(location).getServiceLoader(location, service);
    }

    @Override
    public String inferModuleName(Location location) throws IOException {
        return delegate(location).inferModuleName(location);
    }

    @Override
    public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException {
        return delegate(location).listLocationsForModules(location);
    }

    @Override
    public boolean contains(Location location, FileObject fo) throws IOException {
        return delegate(location).contains(location, fo);
    }

    @Override
    public int isSupportedOption(String option) {
        return baseFM.isSupportedOption(option);
    }

    public JavaFileManager getBaseFileManager() {
        return baseFM;
    }

    private static final class DelegatingSJFM extends DelegatingJavaFileManager
                                              implements StandardJavaFileManager {

        private final StandardJavaFileManager baseSJFM;

        private DelegatingSJFM(JavaFileManager releaseFM,
                                                  StandardJavaFileManager baseSJFM) {
            super(releaseFM, baseSJFM);
            this.baseSJFM = baseSJFM;
        }

        @Override
        public boolean isSameFile(FileObject a, FileObject b) {
            return baseSJFM.isSameFile(a, b);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles
                                                  (Iterable<? extends File> files) {
            return baseSJFM.getJavaFileObjectsFromFiles(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths
                                                  (Collection<? extends Path> paths) {
            return baseSJFM.getJavaFileObjectsFromPaths(paths);
        }

        @Deprecated(since = "13")
        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths
                                                  (Iterable<? extends Path> paths) {
            return baseSJFM.getJavaFileObjectsFromPaths(paths);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files) {
            return baseSJFM.getJavaFileObjects(files);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths) {
            return baseSJFM.getJavaFileObjects(paths);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings
                                                  (Iterable<String> names) {
            return baseSJFM.getJavaFileObjectsFromStrings(names);
        }

        @Override
        public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names) {
            return baseSJFM.getJavaFileObjects(names);
        }

        @Override
        public void setLocation(Location location,
                                Iterable<? extends File> files) throws IOException {
            baseSJFM.setLocation(location, files);
        }

        @Override
        public void setLocationFromPaths(Location location,
                                         Collection<? extends Path> paths) throws IOException {
            baseSJFM.setLocationFromPaths(location, paths);
        }

        @Override
        public void setLocationForModule(Location location, String moduleName,
                                         Collection<? extends Path> paths) throws IOException {
            baseSJFM.setLocationForModule(location, moduleName, paths);
        }

        @Override
        public Iterable<? extends File> getLocation(Location location) {
            return baseSJFM.getLocation(location);
        }

        @Override
        public Iterable<? extends Path> getLocationAsPaths(Location location) {
            return baseSJFM.getLocationAsPaths(location);
        }

        @Override
        public Path asPath(FileObject file) {
            return baseSJFM.asPath(file);
        }

        @Override
        public void setPathFactory(PathFactory f) {
            baseSJFM.setPathFactory(f);
        }

    }
}
