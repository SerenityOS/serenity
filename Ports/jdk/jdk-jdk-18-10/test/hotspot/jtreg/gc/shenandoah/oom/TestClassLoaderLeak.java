/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

/**
 * @test
 * @summary Test OOME in due to classloader leak
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @run driver TestClassLoaderLeak
 */

import java.util.*;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

public class TestClassLoaderLeak {

    static final int SIZE = 1 * 1024 * 1024;
    static final int COUNT = 128;

    static volatile Object sink;

    static class Dummy {
        static final int[] PAYLOAD = new int[SIZE];
    }

    static class MyClassLoader extends ClassLoader {
        final String path;

        MyClassLoader(String path) {
            this.path = path;
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try {
                File f = new File(path, name + ".class");
                if (!f.exists()) {
                    return super.loadClass(name);
                }

                Path path = Paths.get(f.getAbsolutePath());
                byte[] cls = Files.readAllBytes(path);
                return defineClass(name, cls, 0, cls.length, null);
            } catch (IOException e) {
                throw new ClassNotFoundException(name);
            }
        }
    }

    static void load(String path) throws Exception {
        ClassLoader cl = new MyClassLoader(path);
        Class<Dummy> c = (Class<Dummy>) Class.forName("TestClassLoaderLeak$Dummy", true, cl);
        if (c.getClassLoader() != cl) {
            throw new IllegalStateException("Should have loaded by target loader");
        }
        sink = c;
    }

    public static void passWith(String... args) throws Exception {
        testWith(true, args);
    }

    public static void failWith(String... args) throws Exception {
        testWith(false, args);
    }

    public static void testWith(boolean shouldPass, String... args) throws Exception {
        List<String> pbArgs = new ArrayList<>();
        pbArgs.add("-Xmx128m");
        pbArgs.add("-XX:+UnlockExperimentalVMOptions");
        pbArgs.add("-XX:+UnlockDiagnosticVMOptions");
        pbArgs.add("-XX:+UseShenandoahGC");
        pbArgs.addAll(Arrays.asList(args));
        pbArgs.add(TestClassLoaderLeak.class.getName());
        pbArgs.add("test");

        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(pbArgs.toArray(new String[0]));

        OutputAnalyzer analyzer = new OutputAnalyzer(pb.start());

        if (shouldPass) {
            analyzer.shouldHaveExitValue(0);
            analyzer.shouldNotContain("java.lang.OutOfMemoryError");
            analyzer.shouldContain("All good");
        } else {
            analyzer.shouldHaveExitValue(1);
            analyzer.shouldContain("java.lang.OutOfMemoryError");
            analyzer.shouldNotContain("All good");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String classDir = TestClassLoaderLeak.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            for (int c = 0; c < COUNT; c++) {
                load(classDir);
            }
            System.out.println("All good");
            return;
        }

        String[][][] modeHeuristics = new String[][][] {
             {{"satb"},    {"adaptive", "compact", "static", "aggressive"}},
             {{"iu"},      {"adaptive", "aggressive"}},
             {{"passive"}, {"passive"}}
        };

        for (String[][] mh : modeHeuristics) {
            String mode = mh[0][0];
            String[] heuristics = mh[1];
            for (String h : heuristics) {
                // Forceful enabling should work
                passWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:+ClassUnloading");
                passWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:+ClassUnloadingWithConcurrentMark");

                // Even when concurrent unloading is disabled, Full GC has to recover
                passWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:+ClassUnloading", "-XX:-ClassUnloadingWithConcurrentMark");
                passWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:+ClassUnloading", "-XX:-ClassUnloadingWithConcurrentMark", "-XX:ShenandoahUnloadClassesFrequency=0");
                passWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:+ClassUnloading", "-XX:+ClassUnloadingWithConcurrentMark", "-XX:ShenandoahUnloadClassesFrequency=0");

                // Should OOME when unloading forcefully disabled, even if local flags try to enable it back
                failWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:-ClassUnloading");
                failWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:-ClassUnloading", "-XX:+ClassUnloadingWithConcurrentMark");
                failWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:-ClassUnloading", "-XX:+ClassUnloadingWithConcurrentMark", "-XX:ShenandoahUnloadClassesFrequency=1");
                failWith("-XX:ShenandoahGCMode=" + mode, "-XX:ShenandoahGCHeuristics=" + h, "-XX:-ClassUnloading", "-XX:-ClassUnloadingWithConcurrentMark", "-XX:ShenandoahUnloadClassesFrequency=1");
            }
        }
    }
}
