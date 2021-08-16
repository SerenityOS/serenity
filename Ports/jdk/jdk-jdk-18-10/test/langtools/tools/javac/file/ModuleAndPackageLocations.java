/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8171005 8175560
 * @summary Verify behavior of JavaFileManager methods w.r.t. module/package oriented locations
 * @library /tools/lib
 * @modules java.compiler
 * @build toolbox.TestRunner ModuleAndPackageLocations
 * @run main ModuleAndPackageLocations
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.TestRunner;
import toolbox.TestRunner.Test;

public class ModuleAndPackageLocations extends TestRunner {

    public static void main(String... args) throws Exception {
        new ModuleAndPackageLocations().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    public ModuleAndPackageLocations() {
        super(System.err);
    }

    @Test
    public void testListLocations(Path outerBase) throws Exception {
        doRunTest(outerBase, (base, fm) -> {
            assertLocations(fm.listLocationsForModules(StandardLocation.MODULE_SOURCE_PATH),
                            toSet("MODULE_SOURCE_PATH[a]:false:false",
                                  "MODULE_SOURCE_PATH[b]:false:false",
                                  "MODULE_SOURCE_PATH[c]:false:false"));
            assertLocations(fm.listLocationsForModules(StandardLocation.MODULE_PATH),
                            toSet("MODULE_PATH[0.X,a]:false:false",
                                  "MODULE_PATH[0.X,b]:false:false"),
                            toSet("MODULE_PATH[1.X,c]:false:false",
                                  "MODULE_PATH[1.X,b]:false:false"));
            assertLocations(fm.listLocationsForModules(StandardLocation.SOURCE_OUTPUT),
                            toSet("SOURCE_OUTPUT[a]:false:true",
                                  "SOURCE_OUTPUT[b]:false:true"));

            fm.getLocationForModule(StandardLocation.SOURCE_OUTPUT, "c");

            assertLocations(fm.listLocationsForModules(StandardLocation.SOURCE_OUTPUT),
                            toSet("SOURCE_OUTPUT[a]:false:true",
                                  "SOURCE_OUTPUT[b]:false:true",
                                  "SOURCE_OUTPUT[c]:false:true"));
        });
    }

    @Test
    public void testGetModuleForPath(Path outerBase) throws Exception {
        doRunTest(outerBase, (base, fm) -> {
            Location cOutput = fm.getLocationForModule(StandardLocation.SOURCE_OUTPUT, "c");
            JavaFileObject testFO = fm.getJavaFileForOutput(cOutput, "test.Test", Kind.CLASS, null);
            testFO.openOutputStream().close();
            Location cOutput2 = fm.getLocationForModule(StandardLocation.SOURCE_OUTPUT, testFO);

            if (cOutput != cOutput2) {
                throw new AssertionError("Unexpected location: " + cOutput2 + ", expected: " +cOutput);
            }
        });
    }

    @Test
    public void testRejects(Path outerBase) throws Exception {
        doRunTest(outerBase, (base, fm) -> {
            assertRefused(() -> fm.getClassLoader(StandardLocation.MODULE_SOURCE_PATH));
            assertRefused(() -> fm.getFileForInput(StandardLocation.MODULE_SOURCE_PATH, "", ""));
            assertRefused(() -> fm.getFileForOutput(StandardLocation.MODULE_SOURCE_PATH, "", "", null));
            assertRefused(() -> fm.getJavaFileForInput(StandardLocation.MODULE_SOURCE_PATH, "", Kind.SOURCE));
            assertRefused(() -> fm.getJavaFileForOutput(StandardLocation.MODULE_SOURCE_PATH, "", Kind.SOURCE, null));
            assertRefused(() -> fm.getLocationForModule(StandardLocation.SOURCE_PATH, "test"));
            JavaFileObject out = fm.getJavaFileForInput(StandardLocation.CLASS_OUTPUT, "test.Test", Kind.CLASS);
            assertRefused(() -> fm.inferBinaryName(StandardLocation.MODULE_PATH, out));
            assertRefused(() -> fm.inferModuleName(StandardLocation.MODULE_SOURCE_PATH));
            assertRefused(() -> fm.list(StandardLocation.MODULE_SOURCE_PATH, "test", EnumSet.allOf(Kind.class), false));
            assertRefused(() -> fm.listLocationsForModules(StandardLocation.SOURCE_PATH));
        });
    }

    void doRunTest(Path base, TestExec test) throws Exception {
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Path msp  = base.resolve("msp");
            Path msp1 = msp.resolve("1");
            Path msp2 = msp.resolve("2");

            touch(msp1.resolve("a/module-info.java"));
            Files.createDirectories(msp1.resolve("b"));
            touch(msp2.resolve("b/module-info.java"));
            touch(msp2.resolve("c/module-info.java"));

            Path mp  = base.resolve("mp");
            Path mp1 = mp.resolve("1");
            Path mp2 = mp.resolve("2");

            touch(mp1.resolve("a/module-info.class"),
                  mp1.resolve("b/module-info.class"),
                  mp2.resolve("b/module-info.class"),
                  mp2.resolve("c/module-info.class"));

            Path so  = base.resolve("so");

            Files.createDirectories(so.resolve("a"));
            Files.createDirectories(so.resolve("b"));

            List<String> mspOpt = Arrays.asList(msp1.toAbsolutePath().toString() +
                                                File.pathSeparatorChar +
                                                msp2.toAbsolutePath().toString());

            List<String> mpOpt = Arrays.asList(mp1.toAbsolutePath().toString() +
                                               File.pathSeparatorChar +
                                               mp2.toAbsolutePath().toString());

            fm.handleOption("--module-source-path", mspOpt.iterator());
            fm.handleOption("--module-path", mpOpt.iterator());
            fm.handleOption("-s", Arrays.asList(so.toString()).iterator());

            test.run(base, fm);
        }
    }

    private Set<String> toSet(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private void touch(Path... paths) throws IOException {
        for (Path p : paths) {
            Files.createDirectories(p.getParent());
            Files.newOutputStream(p).close();
        }
    }

    @SafeVarargs
    private void assertLocations(Iterable<Set<Location>> locations, Set<String>... expected) {
        List<Set<String>> actual =
                StreamSupport.stream(locations.spliterator(), true)
                             .map(locs -> locs.stream()
                                              .map(l -> toString(l))
                                              .collect(Collectors.toSet()))
                             .collect(Collectors.toList());

        if (!Objects.equals(actual, Arrays.asList(expected))) {
            throw new AssertionError("Unexpected output: " + actual);
        }
    }

    private void assertRefused(Callable r) throws Exception {
        try {
            r.call();
            throw new AssertionError("Expected exception did not occur");
        } catch (IllegalArgumentException ex) {
            //ok
        }
    }

    private static String toString(Location l) {
        return l.getName().replaceAll("\\[([0-9])\\.[0-9]:", "[$1.X,") + ":" +
               l.isModuleOrientedLocation() + ":" + l.isOutputLocation();
    }

    static interface TestExec {
        public void run(Path base, JavaFileManager fm) throws Exception;
    }

    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
}
