/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.classfile.Dependencies;
import com.sun.tools.classfile.Dependency;
import com.sun.tools.classfile.Dependency.Location;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/*
 * Filter configured based on the input jdeps option
 * 1. -p and -regex to match target dependencies
 * 2. -filter:package to filter out same-package dependencies
 *    This filter is applied when jdeps parses the class files
 *    and filtered dependencies are not stored in the Analyzer.
 * 3. --require specifies to match target dependence from the given module
 *    This gets expanded into package lists to be filtered.
 * 4. -filter:archive to filter out same-archive dependencies
 *    This filter is applied later in the Analyzer as the
 *    containing archive of a target class may not be known until
 *    the entire archive
 */
public class JdepsFilter implements Dependency.Filter, Analyzer.Filter {

    public static final JdepsFilter DEFAULT_FILTER =
        new JdepsFilter.Builder().filter(true, true).build();

    private final Dependency.Filter filter;
    private final Pattern filterPattern;
    private final boolean filterSamePackage;
    private final boolean filterSameArchive;
    private final boolean findJDKInternals;
    private final boolean findMissingDeps;
    private final Pattern includePattern;

    private final Set<String> requires;

    private JdepsFilter(Dependency.Filter filter,
                        Pattern filterPattern,
                        boolean filterSamePackage,
                        boolean filterSameArchive,
                        boolean findJDKInternals,
                        boolean findMissingDeps,
                        Pattern includePattern,
                        Set<String> requires) {
        this.filter = filter;
        this.filterPattern = filterPattern;
        this.filterSamePackage = filterSamePackage;
        this.filterSameArchive = filterSameArchive;
        this.findJDKInternals = findJDKInternals;
        this.findMissingDeps = findMissingDeps;
        this.includePattern = includePattern;
        this.requires = requires;
    }

    /**
     * Tests if the given class matches the pattern given in the -include option
     *
     * @param cn fully-qualified name
     */
    public boolean matches(String cn) {
        if (includePattern == null)
            return true;

        if (includePattern != null)
            return includePattern.matcher(cn).matches();

        return false;
    }

    /**
     * Tests if the given source includes classes specified in -include option
     *
     * This method can be used to determine if the given source should eagerly
     * be processed.
     */
    public boolean matches(Archive source) {
        if (includePattern != null) {
            return source.reader().entries().stream()
                    .map(name -> name.replace('/', '.'))
                    .filter(name -> !name.equals("module-info.class"))
                    .anyMatch(this::matches);
        }
        return hasTargetFilter();
    }

    public boolean hasIncludePattern() {
        return includePattern != null;
    }

    public boolean hasTargetFilter() {
        return filter != null;
    }

    public Set<String> requiresFilter() {
        return requires;
    }

    // ----- Dependency.Filter -----

    @Override
    public boolean accepts(Dependency d) {
        if (d.getOrigin().equals(d.getTarget()))
            return false;

        // filter same package dependency
        String pn = d.getTarget().getPackageName();
        if (filterSamePackage && d.getOrigin().getPackageName().equals(pn)) {
            return false;
        }

        // filter if the target package matches the given filter
        if (filterPattern != null && filterPattern.matcher(pn).matches()) {
            return false;
        }

        // filter if the target matches the given filtered package name or regex
        return filter != null ? filter.accepts(d) : true;
    }

    // ----- Analyzer.Filter ------

    /**
     * Filter depending on the containing archive or module
     */
    @Override
    public boolean accepts(Location origin, Archive originArchive,
                           Location target, Archive targetArchive) {
        if (findJDKInternals) {
            // accepts target that is JDK class but not exported
            Module module = targetArchive.getModule();
            return originArchive != targetArchive &&
                    isJDKInternalPackage(module, target.getPackageName());
        } else if (findMissingDeps) {
            return Analyzer.notFound(targetArchive);
        } else if (filterSameArchive) {
            // accepts origin and target that from different archive
            return originArchive != targetArchive;
        }
        return true;
    }

    /**
     * Tests if the package is an internal package of the given module.
     */
    public boolean isJDKInternalPackage(Module module, String pn) {
        if (module.isJDKUnsupported()) {
            // its exported APIs are unsupported
            return true;
        }

        return module.isJDK() && !module.isExported(pn);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("include pattern: ").append(includePattern).append("\n");
        sb.append("filter same archive: ").append(filterSameArchive).append("\n");
        sb.append("filter same package: ").append(filterSamePackage).append("\n");
        sb.append("requires: ").append(requires).append("\n");
        return sb.toString();
    }

    public static class Builder {
        Pattern filterPattern;
        Pattern regex;
        boolean filterSamePackage;
        boolean filterSameArchive;
        boolean findJDKInterals;
        boolean findMissingDeps;
        // source filters
        Pattern includePattern;
        Set<String> requires = new HashSet<>();
        Set<String> targetPackages = new HashSet<>();

        public Builder() {};

        public Builder packages(Set<String> packageNames) {
            this.targetPackages.addAll(packageNames);
            return this;
        }
        public Builder regex(Pattern regex) {
            this.regex = regex;
            return this;
        }
        public Builder filter(Pattern regex) {
            this.filterPattern = regex;
            return this;
        }
        public Builder filter(boolean samePackage, boolean sameArchive) {
            this.filterSamePackage = samePackage;
            this.filterSameArchive = sameArchive;
            return this;
        }
        public Builder requires(String name, Set<String> packageNames) {
            this.requires.add(name);
            this.targetPackages.addAll(packageNames);
            return this;
        }
        public Builder findJDKInternals(boolean value) {
            this.findJDKInterals = value;
            return this;
        }
        public Builder findMissingDeps(boolean value) {
            this.findMissingDeps = value;
            return this;
        }
        public Builder includePattern(Pattern regex) {
            this.includePattern = regex;
            return this;
        }

        public JdepsFilter build() {
            Dependency.Filter filter = null;
            if (regex != null)
                filter = Dependencies.getRegexFilter(regex);
            else if (!targetPackages.isEmpty()) {
                filter = Dependencies.getPackageFilter(targetPackages, false);
            }
            return new JdepsFilter(filter,
                                   filterPattern,
                                   filterSamePackage,
                                   filterSameArchive,
                                   findJDKInterals,
                                   findMissingDeps,
                                   includePattern,
                                   requires);
        }

    }
}
