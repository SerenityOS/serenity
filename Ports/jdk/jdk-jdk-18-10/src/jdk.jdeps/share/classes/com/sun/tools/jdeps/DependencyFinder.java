/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tools.jdeps;

import static com.sun.tools.jdeps.Module.*;
import static com.sun.tools.jdeps.Analyzer.NOT_FOUND;
import static java.util.stream.Collectors.*;

import com.sun.tools.classfile.AccessFlags;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Dependencies;
import com.sun.tools.classfile.Dependencies.ClassFileError;
import com.sun.tools.classfile.Dependency;
import com.sun.tools.classfile.Dependency.Location;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Stream;

/**
 * Parses class files and finds dependences
 */
class DependencyFinder {
    private static Finder API_FINDER = new Finder(true);
    private static Finder CLASS_FINDER = new Finder(false);

    private final JdepsConfiguration configuration;
    private final JdepsFilter filter;

    private final Map<Finder, Deque<Archive>> parsedArchives = new ConcurrentHashMap<>();
    private final Map<Location, Archive> parsedClasses = new ConcurrentHashMap<>();

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Deque<FutureTask<Set<Location>>> tasks = new ConcurrentLinkedDeque<>();

    DependencyFinder(JdepsConfiguration configuration,
                     JdepsFilter filter) {
        this.configuration = configuration;
        this.filter = filter;
        this.parsedArchives.put(API_FINDER, new ConcurrentLinkedDeque<>());
        this.parsedArchives.put(CLASS_FINDER, new ConcurrentLinkedDeque<>());
    }

    Map<Location, Archive> locationToArchive() {
        return parsedClasses;
    }

    /**
     * Returns the modules of all dependencies found
     */
    Stream<Archive> getDependences(Archive source) {
        return source.getDependencies()
                     .map(this::locationToArchive)
                     .filter(a -> a != source);
    }

    /**
     * Returns the location to archive map; or NOT_FOUND.
     *
     * Location represents a parsed class.
     */
    Archive locationToArchive(Location location) {
        return parsedClasses.containsKey(location)
            ? parsedClasses.get(location)
            : configuration.findClass(location).orElse(NOT_FOUND);
    }

    /**
     * Returns a map from an archive to its required archives
     */
    Map<Archive, Set<Archive>> dependences() {
        Map<Archive, Set<Archive>> map = new HashMap<>();
        parsedArchives.values().stream()
            .flatMap(Deque::stream)
            .filter(a -> !a.isEmpty())
            .forEach(source -> {
                Set<Archive> deps = getDependences(source).collect(toSet());
                if (!deps.isEmpty()) {
                    map.put(source, deps);
                }
        });
        return map;
    }

    boolean isParsed(Location location) {
        return parsedClasses.containsKey(location);
    }

    /**
     * Parses all class files from the given archive stream and returns
     * all target locations.
     */
    public Set<Location> parse(Stream<? extends Archive> archiveStream) {
        archiveStream.forEach(archive -> parse(archive, CLASS_FINDER));
        return waitForTasksCompleted();
    }

    /**
     * Parses the exported API class files from the given archive stream and
     * returns all target locations.
     */
    public Set<Location> parseExportedAPIs(Stream<? extends Archive> archiveStream) {
        archiveStream.forEach(archive -> parse(archive, API_FINDER));
        return waitForTasksCompleted();
    }

    /**
     * Parses the named class from the given archive and
     * returns all target locations the named class references.
     */
    public Set<Location> parse(Archive archive, String name) {
        try {
            return parse(archive, CLASS_FINDER, name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Parses the exported API of the named class from the given archive and
     * returns all target locations the named class references.
     */
    public Set<Location> parseExportedAPIs(Archive archive, String name)
    {
        try {
            return parse(archive, API_FINDER, name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Optional<FutureTask<Set<Location>>> parse(Archive archive, Finder finder) {
        if (parsedArchives.get(finder).contains(archive))
            return Optional.empty();

        parsedArchives.get(finder).add(archive);

        trace("parsing %s %s%n", archive.getName(), archive.getPathName());
        FutureTask<Set<Location>> task = new FutureTask<>(() -> {
            Set<Location> targets = new HashSet<>();
            for (ClassFile cf : archive.reader().getClassFiles()) {
                if (cf.access_flags.is(AccessFlags.ACC_MODULE))
                    continue;

                String classFileName;
                try {
                    classFileName = cf.getName();
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }

                // filter source class/archive
                String cn = classFileName.replace('/', '.');
                if (!finder.accept(archive, cn, cf.access_flags))
                    continue;

                // tests if this class matches the -include
                if (!filter.matches(cn))
                    continue;

                for (Dependency d : finder.findDependencies(cf)) {
                    if (filter.accepts(d)) {
                        archive.addClass(d.getOrigin(), d.getTarget());
                        targets.add(d.getTarget());
                    } else {
                        // ensure that the parsed class is added the archive
                        archive.addClass(d.getOrigin());
                    }
                    parsedClasses.putIfAbsent(d.getOrigin(), archive);
                }
            }
            return targets;
        });
        tasks.add(task);
        pool.submit(task);
        return Optional.of(task);
    }

    private Set<Location> parse(Archive archive, Finder finder, String name)
        throws IOException
    {
        ClassFile cf = archive.reader().getClassFile(name);
        if (cf == null) {
            throw new IllegalArgumentException(archive.getName() +
                " does not contain " + name);
        }

        if (cf.access_flags.is(AccessFlags.ACC_MODULE))
            return Collections.emptySet();

        Set<Location> targets = new HashSet<>();
        String cn;
        try {
            cn =  cf.getName().replace('/', '.');
        } catch (ConstantPoolException e) {
            throw new Dependencies.ClassFileError(e);
        }

        if (!finder.accept(archive, cn, cf.access_flags))
            return targets;

        // tests if this class matches the -include
        if (!filter.matches(cn))
            return targets;

        // skip checking filter.matches
        for (Dependency d : finder.findDependencies(cf)) {
            if (filter.accepts(d)) {
                targets.add(d.getTarget());
                archive.addClass(d.getOrigin(), d.getTarget());
            } else {
                // ensure that the parsed class is added the archive
                archive.addClass(d.getOrigin());
            }
            parsedClasses.putIfAbsent(d.getOrigin(), archive);
        }
        return targets;
    }

    /*
     * Waits until all submitted tasks are completed.
     */
    private Set<Location> waitForTasksCompleted() {
        try {
            Set<Location> targets = new HashSet<>();
            FutureTask<Set<Location>> task;
            while ((task = tasks.poll()) != null) {
                // wait for completion
                targets.addAll(task.get());
            }
            return targets;
        } catch (InterruptedException|ExecutionException e) {
            throw new Error(e);
        }
    }

    /*
     * Shutdown the executor service.
     */
    void shutdown() {
        pool.shutdown();
    }

    private interface SourceFilter {
        boolean accept(Archive archive, String cn, AccessFlags accessFlags);
    }

    private static class Finder implements Dependency.Finder, SourceFilter {
        private final Dependency.Finder finder;
        private final boolean apiOnly;
        Finder(boolean apiOnly) {
            this.apiOnly = apiOnly;
            this.finder = apiOnly
                ? Dependencies.getAPIFinder(AccessFlags.ACC_PROTECTED)
                : Dependencies.getClassDependencyFinder();

        }

        @Override
        public boolean accept(Archive archive, String cn, AccessFlags accessFlags) {
            int i = cn.lastIndexOf('.');
            String pn = i > 0 ? cn.substring(0, i) : "";

            // if -apionly is specified, analyze only exported and public types
            // All packages are exported in unnamed module.
            return apiOnly ? archive.getModule().isExported(pn) &&
                                 accessFlags.is(AccessFlags.ACC_PUBLIC)
                           : true;
        }

        @Override
        public Iterable<? extends Dependency> findDependencies(ClassFile classfile) {
            return finder.findDependencies(classfile);
        }
    }
}
