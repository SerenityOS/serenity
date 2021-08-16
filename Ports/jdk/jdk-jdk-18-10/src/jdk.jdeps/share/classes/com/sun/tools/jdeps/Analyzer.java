/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Dependency.Location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.module.ModuleDescriptor;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Dependency Analyzer.
 */
public class Analyzer {
    /**
     * Type of the dependency analysis.  Appropriate level of data
     * will be stored.
     */
    public enum Type {
        SUMMARY,
        MODULE,  // equivalent to summary in addition, print module descriptor
        PACKAGE,
        CLASS,
        VERBOSE
    }

    /**
     * Filter to be applied when analyzing the dependencies from the given archives.
     * Only the accepted dependencies are recorded.
     */
    interface Filter {
        boolean accepts(Location origin, Archive originArchive,
                        Location target, Archive targetArchive);
    }

    protected final JdepsConfiguration configuration;
    protected final Type type;
    protected final Filter filter;
    protected final Map<Archive, Dependences> results = new HashMap<>();
    protected final Map<Location, Archive> locationToArchive = new HashMap<>();
    static final Archive NOT_FOUND
        = new Archive(JdepsTask.getMessage("artifact.not.found"));
    static final Predicate<Archive> ANY = a -> true;

    /**
     * Constructs an Analyzer instance.
     *
     * @param type Type of the dependency analysis
     * @param filter
     */
    Analyzer(JdepsConfiguration config, Type type, Filter filter) {
        this.configuration = config;
        this.type = type;
        this.filter = filter;
    }

    /**
     * Performs the dependency analysis on the given archives.
     */
    boolean run(Iterable<? extends Archive> archives,
                Map<Location, Archive> locationMap)
    {
        this.locationToArchive.putAll(locationMap);

        // traverse and analyze all dependencies
        for (Archive archive : archives) {
            Dependences deps = new Dependences(archive, type);
            archive.visitDependences(deps);
            results.put(archive, deps);
        }
        return true;
    }

    /**
     * Returns the analyzed archives
     */
    Set<Archive> archives() {
        return results.keySet();
    }

    /**
     * Returns true if the given archive has dependences.
     */
    boolean hasDependences(Archive archive) {
        if (results.containsKey(archive)) {
            return results.get(archive).dependencies().size() > 0;
        }
        return false;
    }

    /**
     * Returns the dependences, either class name or package name
     * as specified in the given verbose level, from the given source.
     */
    Set<String> dependences(Archive source) {
        if (!results.containsKey(source)) {
            return Collections.emptySet();
        }

        return results.get(source).dependencies()
                      .stream()
                      .map(Dep::target)
                      .collect(Collectors.toSet());
    }

    /**
     * Returns the direct dependences of the given source
     */
    Stream<Archive> requires(Archive source) {
        if (!results.containsKey(source)) {
            return Stream.empty();
        }
        return results.get(source).requires()
                      .stream();
    }

    interface Visitor {
        /**
         * Visits a recorded dependency from origin to target which can be
         * a fully-qualified classname, a package name, a module or
         * archive name depending on the Analyzer's type.
         */
        public void visitDependence(String origin, Archive originArchive,
                                    String target, Archive targetArchive);
    }

    /**
     * Visit the dependencies of the given source.
     * If the requested level is SUMMARY, it will visit the required archives list.
     */
    void visitDependences(Archive source, Visitor v, Type level, Predicate<Archive> targetFilter) {
        if (level == Type.SUMMARY) {
            final Dependences result = results.get(source);
            final Set<Archive> reqs = result.requires();
            Stream<Archive> stream = reqs.stream();
            if (reqs.isEmpty()) {
                if (hasDependences(source)) {
                    // If reqs.isEmpty() and we have dependences, then it means
                    // that the dependences are from 'source' onto itself.
                    stream = Stream.of(source);
                }
            }
            stream.sorted(Comparator.comparing(Archive::getName))
                  .forEach(archive -> {
                      Profile profile = result.getTargetProfile(archive);
                      v.visitDependence(source.getName(), source,
                                        profile != null ? profile.profileName()
                                                        : archive.getName(), archive);
                  });
        } else {
            Dependences result = results.get(source);
            if (level != type) {
                // requesting different level of analysis
                result = new Dependences(source, level, targetFilter);
                source.visitDependences(result);
            }
            result.dependencies().stream()
                  .sorted(Comparator.comparing(Dep::origin)
                                    .thenComparing(Dep::target))
                  .forEach(d -> v.visitDependence(d.origin(), d.originArchive(),
                                                  d.target(), d.targetArchive()));
        }
    }

    void visitDependences(Archive source, Visitor v) {
        visitDependences(source, v, type, ANY);
    }

    void visitDependences(Archive source, Visitor v, Type level) {
        visitDependences(source, v, level, ANY);
    }

    /**
     * Dependences contains the dependencies for an Archive that can have one or
     * more classes.
     */
    class Dependences implements Archive.Visitor {
        protected final Archive archive;
        protected final Set<Archive> requires;
        protected final Set<Dep> deps;
        protected final Type level;
        protected final Predicate<Archive> targetFilter;
        private Profile profile;
        Dependences(Archive archive, Type level) {
            this(archive, level, ANY);
        }
        Dependences(Archive archive, Type level, Predicate<Archive> targetFilter) {
            this.archive = archive;
            this.deps = new HashSet<>();
            this.requires = new HashSet<>();
            this.level = level;
            this.targetFilter = targetFilter;
        }

        Set<Dep> dependencies() {
            return deps;
        }

        Set<Archive> requires() {
            return requires;
        }

        Profile getTargetProfile(Archive target) {
            if (target.getModule().isJDK()) {
                return Profile.getProfile((Module) target);
            } else {
                return null;
            }
        }

        /*
         * Returns the archive that contains the given location.
         */
        Archive findArchive(Location t) {
            // local in this archive
            if (archive.getClasses().contains(t))
                return archive;

            Archive target;
            if (locationToArchive.containsKey(t)) {
                target = locationToArchive.get(t);
            } else {
                // special case JDK removed API
                target = configuration.findClass(t)
                    .orElseGet(() -> REMOVED_JDK_INTERNALS.contains(t)
                                        ? REMOVED_JDK_INTERNALS
                                        : NOT_FOUND);
            }
            return locationToArchive.computeIfAbsent(t, _k -> target);
        }

        // return classname or package name depending on the level
        private String getLocationName(Location o) {
            if (level == Type.CLASS || level == Type.VERBOSE) {
                return VersionHelper.get(o.getClassName());
            } else {
                String pkg = o.getPackageName();
                return pkg.isEmpty() ? "<unnamed>" : pkg;
            }
        }

        @Override
        public void visit(Location o, Location t) {
            Archive targetArchive = findArchive(t);
            if (filter.accepts(o, archive, t, targetArchive) && targetFilter.test(targetArchive)) {
                addDep(o, t);
                if (archive != targetArchive && !requires.contains(targetArchive)) {
                    requires.add(targetArchive);
                }
            }
            if (targetArchive.getModule().isNamed()) {
                Profile p = Profile.getProfile(t.getPackageName());
                if (profile == null || (p != null && p.compareTo(profile) > 0)) {
                    profile = p;
                }
            }
        }

        private Dep curDep;
        protected Dep addDep(Location o, Location t) {
            String origin = getLocationName(o);
            String target = getLocationName(t);
            Archive targetArchive = findArchive(t);
            if (curDep != null &&
                    curDep.origin().equals(origin) &&
                    curDep.originArchive() == archive &&
                    curDep.target().equals(target) &&
                    curDep.targetArchive() == targetArchive) {
                return curDep;
            }

            Dep e = new Dep(origin, archive, target, targetArchive);
            if (deps.contains(e)) {
                for (Dep e1 : deps) {
                    if (e.equals(e1)) {
                        curDep = e1;
                    }
                }
            } else {
                deps.add(e);
                curDep = e;
            }
            return curDep;
        }
    }

    /*
     * Class-level or package-level dependency
     */
    class Dep {
        final String origin;
        final Archive originArchive;
        final String target;
        final Archive targetArchive;

        Dep(String origin, Archive originArchive, String target, Archive targetArchive) {
            this.origin = origin;
            this.originArchive = originArchive;
            this.target = target;
            this.targetArchive = targetArchive;
        }

        String origin() {
            return origin;
        }

        Archive originArchive() {
            return originArchive;
        }

        String target() {
            return target;
        }

        Archive targetArchive() {
            return targetArchive;
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (o instanceof Dep) {
                Dep d = (Dep) o;
                return this.origin.equals(d.origin) &&
                        this.originArchive == d.originArchive &&
                        this.target.equals(d.target) &&
                        this.targetArchive == d.targetArchive;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.origin,
                                this.originArchive,
                                this.target,
                                this.targetArchive);
        }

        public String toString() {
            return String.format("%s (%s) -> %s (%s)%n",
                    origin, originArchive.getName(),
                    target, targetArchive.getName());
        }
    }

    /*
     * Returns true if the given archive represents not found.
     */
    static boolean notFound(Archive archive) {
        return archive == NOT_FOUND || archive == REMOVED_JDK_INTERNALS;
    }

    static final Jdk8Internals REMOVED_JDK_INTERNALS = new Jdk8Internals();

    static class Jdk8Internals extends Module {
        private static final String NAME = "JDK removed internal API";
        private static final String JDK8_INTERNALS = "/com/sun/tools/jdeps/resources/jdk8_internals.txt";
        private final Set<String> jdk8Internals;
        private Jdk8Internals() {
            super(NAME, ModuleDescriptor.newModule("jdk8internals").build(), true);
            try (InputStream in = JdepsTask.class.getResourceAsStream(JDK8_INTERNALS);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                this.jdk8Internals = reader.lines()
                                          .filter(ln -> !ln.startsWith("#"))
                                          .collect(Collectors.toSet());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /*
         * Ignore the module name which should not be shown in the output
         */
        @Override
        public String name() {
            return getName();
        }

        public boolean contains(Location location) {
            String cn = location.getClassName();
            int i = cn.lastIndexOf('.');
            String pn = i > 0 ? cn.substring(0, i) : "";

            return jdk8Internals.contains(pn);
        }

        @Override
        public boolean isJDK() {
            return true;
        }

        @Override
        public boolean isExported(String pn) {
            return false;
        }
    }
}
