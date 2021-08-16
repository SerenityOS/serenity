/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8174749 8213307
 * @summary MemberNameTable should reuse entries
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -Xbootclasspath/a:. MemberNameLeak
 */

import java.io.*;
import java.nio.file.*;
import java.lang.invoke.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;
import sun.hotspot.code.Compiler;
import sun.hotspot.gc.GC;
import jdk.test.lib.classloader.ClassWithManyMethodsClassLoader;

public class MemberNameLeak {
    private static String className  = "MemberNameLeakTestClass";
    private static String methodPrefix = "method";
    // The size of the ResolvedMethodTable is 1024. 2000 entries
    // is enough to trigger a grow/cleaning of the table after a GC.
    private static int methodCount = 2000;
    public static ArrayList<MethodHandle> keepAlive;

    static class Leak {
      public void callMe() {
      }

      public static void main(String[] args) throws Throwable {
        Leak leak = new Leak();
        WhiteBox wb = WhiteBox.getWhiteBox();

        keepAlive = new ArrayList<>(methodCount);

        ClassWithManyMethodsClassLoader classLoader = new ClassWithManyMethodsClassLoader();
        Class<?> clazz = classLoader.create(className, methodPrefix, methodCount);

        long before = wb.resolvedMethodItemsCount();

        Object o = clazz.newInstance();
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());

        for (int i = 0; i < methodCount; i++) {
          MethodType mt = MethodType.fromMethodDescriptorString("()V", classLoader);
          String methodName = methodPrefix + i;
          // findSpecial leaks some native mem
          // Add entry to ResolvedMethodTable.
          MethodHandle mh0 = lookup.findSpecial(clazz, methodName, mt, clazz);
          // Find entry in ResolvedMethodTable.
          MethodHandle mh1 = lookup.findSpecial(clazz, methodName, mt, clazz);

          mh1.invoke(o);

          keepAlive.add(mh1);
        }

        long after = wb.resolvedMethodItemsCount();

        System.out.println("wb.resolvedMethodItemsCount() after setup: " + after);

        if (after == before) {
          throw new RuntimeException("Too few resolved methods");
        }

        keepAlive = null;

        // Wait until ServiceThread cleans ResolvedMethod table
        int cnt = 0;
        while (true) {
          if (cnt++ % 30 == 0) {
            System.gc();  // make mh unused
          }

          if (after > wb.resolvedMethodItemsCount() + 50) {
            // Entries have been removed.
            break;
          }

          Thread.sleep(100);
        }
      }
    }

    private static Path createGcLogPath(String prefix) throws IOException {
        Path gcLog = Utils.createTempFile(prefix, "log");
        Files.delete(gcLog);
        return gcLog;
    }

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void test(GC gc, boolean doConcurrent) throws Throwable {
        Path gcLogPath = createGcLogPath("gc." + gc + "." + doConcurrent);
        System.err.println("test(" + gc + ", " + doConcurrent + ")" + " " + dateFormat.format(new Date()));
        // Run this Leak class with logging
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                                      "-Xlog:membername+table=trace,gc+verify=debug,gc:" + gcLogPath + ":time,utctime,uptime,pid,level,tags",
                                      "-XX:+UnlockExperimentalVMOptions",
                                      "-XX:+UnlockDiagnosticVMOptions",
                                      "-XX:+WhiteBoxAPI",
                                      "-Xbootclasspath/a:.",
                                      "-XX:+VerifyBeforeGC",
                                      "-XX:+VerifyAfterGC",
                                      doConcurrent ? "-XX:+ExplicitGCInvokesConcurrent" : "-XX:-ExplicitGCInvokesConcurrent",
                                      "-XX:+ClassUnloading",
                                      "-XX:+ClassUnloadingWithConcurrentMark",
                                      "-XX:+Use" + gc + "GC",
                                      Leak.class.getName());

        // Check process
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.outputTo(System.out);
        output.errorTo(System.err);
        output.shouldHaveExitValue(0);

        // Check gc log file
        OutputAnalyzer gcLogOutput = new OutputAnalyzer(gcLogPath);

        // Hardcoded names for classes generated by GeneratedClassLoader
        String descriptor = className + "." + methodPrefix + "0()V";
        gcLogOutput.shouldContain("ResolvedMethod entry added for " + descriptor);
        gcLogOutput.shouldContain("ResolvedMethod entry found for " + descriptor);
        gcLogOutput.shouldContain("ResolvedMethod entry removed");

        System.err.println("test(" + gc + ", " + doConcurrent + ")" + " done " + dateFormat.format(new Date()));
    }

    private static boolean supportsSTW(GC gc) {
        return !(gc == GC.Epsilon);
    }

    private static boolean supportsConcurrent(GC gc) {
        return !(gc == GC.Epsilon || gc == GC.Serial || gc == GC.Parallel);
    }

    private static void test(GC gc) throws Throwable {
        if (supportsSTW(gc)) {
            test(gc, false);
        }
        if (supportsConcurrent(gc)) {
            test(gc, true);
        }
    }

    public static void main(java.lang.String[] unused) throws Throwable {
      test(GC.selected());
    }
}
