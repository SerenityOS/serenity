/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8176537
 * @summary Check JDK modules have no qualified export to any upgradeable module
 * @modules java.base/jdk.internal.module
 * @run main JdkQualifiedExportTest
 */

import jdk.internal.module.ModuleHashes;
import jdk.internal.module.ModuleInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class JdkQualifiedExportTest {
    public static void main(String... args) {
        // check all system modules
        ModuleFinder.ofSystem().findAll()
                    .stream()
                    .map(ModuleReference::descriptor)
                    .sorted(Comparator.comparing(ModuleDescriptor::name))
                    .forEach(JdkQualifiedExportTest::check);
    }

    static void check(ModuleDescriptor md) {
        // skip checking if this is an upgradeable module
        if (!HashedModules.contains(md.name())) {
            return;
        }

        checkExports(md);
        checkOpens(md);
    }

    static Set<String> KNOWN_EXCEPTIONS =
        Set.of("jdk.internal.vm.ci/jdk.vm.ci.services",
               "jdk.internal.vm.ci/jdk.vm.ci.runtime",
               "jdk.internal.vm.ci/jdk.vm.ci.hotspot",
               "jdk.internal.vm.ci/jdk.vm.ci.meta",
               "jdk.internal.vm.ci/jdk.vm.ci.code",
               "java.base/jdk.internal.javac");

    static void checkExports(ModuleDescriptor md) {
        // build a map of upgradeable module to Exports that are qualified to it
        // skip the qualified exports
        Map<String, Set<Exports>> targetToExports = new HashMap<>();
        md.exports().stream()
          .filter(Exports::isQualified)
          .forEach(e -> e.targets().stream()
                         .filter(mn -> accept(md, mn))
                         .forEach(t -> targetToExports.computeIfAbsent(t, _k -> new HashSet<>())
                                                      .add(e)));

        if (targetToExports.size() > 0) {
            String mn = md.name();

            System.err.println(mn);
            targetToExports.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    e.getValue().stream()
                     .forEach(exp -> System.err.format("    exports %s to %s%n",
                                                       exp.source(), e.getKey()));
                });

            // no qualified exports to upgradeable modules are expected
            // except the known exception cases
            if (targetToExports.entrySet().stream()
                    .flatMap(e -> e.getValue().stream())
                    .anyMatch(e -> !KNOWN_EXCEPTIONS.contains(mn + "/" + e.source()))) {
                throw new RuntimeException(mn + " can't export package to upgradeable modules");
            }
        }
    }

    static void checkOpens(ModuleDescriptor md) {
        // build a map of upgradeable module to Exports that are qualified to it
        // skip the qualified exports
        Map<String, Set<Opens>> targetToOpens = new HashMap<>();
        md.opens().stream()
            .filter(Opens::isQualified)
            .forEach(e -> e.targets().stream()
                           .filter(mn -> accept(md, mn))
                           .forEach(t -> targetToOpens.computeIfAbsent(t, _k -> new HashSet<>())
                                                      .add(e)));

        if (targetToOpens.size() > 0) {
            String mn = md.name();

            System.err.println(mn);
            targetToOpens.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    e.getValue().stream()
                     .forEach(exp -> System.err.format("    opens %s to %s%n",
                                                       exp.source(), e.getKey()));
                });

            throw new RuntimeException(mn + " can't open package to upgradeable modules");
        }
    }

    /**
     * Returns true if target is an upgradeable module but not required
     * by the source module directly and indirectly.
     */
    private static boolean accept(ModuleDescriptor source, String target) {
        if (HashedModules.contains(target))
            return false;

        if (!ModuleFinder.ofSystem().find(target).isPresent())
            return false;

        Configuration cf = Configuration.empty().resolve(ModuleFinder.of(),
                                                         ModuleFinder.ofSystem(),
                                                         Set.of(source.name()));
        return !cf.findModule(target).isPresent();
    }

    private static class HashedModules {
        static Set<String> HASHED_MODULES = hashedModules();

        static Set<String> hashedModules() {
            Module javaBase = Object.class.getModule();
            try (InputStream in = javaBase.getResourceAsStream("module-info.class")) {
                ModuleInfo.Attributes attrs = ModuleInfo.read(in, null);
                ModuleHashes hashes = attrs.recordedHashes();
                if (hashes == null)
                    return Collections.emptySet();

                Set<String> names = new HashSet<>(hashes.names());
                names.add(javaBase.getName());
                return names;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /*
         * Returns true if the named module is tied with java.base,
         * i.e. non-upgradeable
         */
        static boolean contains(String mn) {
            return HASHED_MODULES.contains(mn);
        }
    }
}
