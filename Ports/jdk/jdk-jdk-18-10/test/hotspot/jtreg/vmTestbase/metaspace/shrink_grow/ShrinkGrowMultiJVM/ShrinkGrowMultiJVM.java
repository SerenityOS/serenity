/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase metaspace/shrink_grow/ShrinkGrowMultiJVM.
 * VM Testbase keywords: [nonconcurrent]
 *
 * @requires vm.opt.final.ClassUnloading
 * @library /vmTestbase /test/lib
 * @build metaspace.shrink_grow.ShrinkGrowMultiJVM.ShrinkGrowMultiJVM
 * @run driver metaspace.shrink_grow.ShrinkGrowMultiJVM.ShrinkGrowMultiJVM
 */

package metaspace.shrink_grow.ShrinkGrowMultiJVM;

import jdk.test.lib.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * This test starts several JVMs and run ShrinkGrow metaspace test.
 *
 * It expected that all the parameters on start new processes are given
 * in the command line.
 */
public class ShrinkGrowMultiJVM {
    private static final String[] TEST_ARGS = {
            Paths.get(Utils.TEST_JDK)
                 .resolve("bin")
                 .resolve("java")
                 .toAbsolutePath()
                 .toString(),
            "-Xlog:gc:gc_$i.log", // LOG_GC_ARG_INDEX
            "-XX:MetaspaceSize=10m",
            "-XX:MaxMetaspaceSize=20m",
            "-cp",
            Utils.TEST_CLASS_PATH
    };
    private static final int LOG_GC_ARG_INDEX = 1;
    public static void main(String argv[]) {
        String[] testJavaOpts = Utils.getTestJavaOpts();
        String[] args = new String[TEST_ARGS.length + testJavaOpts.length + 2];
        System.arraycopy(TEST_ARGS, 0, args, 0, TEST_ARGS.length);
        System.arraycopy(testJavaOpts, 0, args, TEST_ARGS.length, testJavaOpts.length);
        args[args.length - 2] = metaspace.shrink_grow.ShrinkGrowTest.ShrinkGrowTest.class.getCanonicalName();
        args[args.length - 1] = "jvm#$i";

        for (int i = 0; i < args.length; ++i) {
            System.out.println("%arg #" + i + ": " + args[i]);
        }

        List<Process> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // will be used as jvm id
            args[args.length - 1] = "jvm#" + i;
            args[LOG_GC_ARG_INDEX] = "-Xlog:gc:gc_" + i + ".log";
            ProcessBuilder pb = new ProcessBuilder(args);
            try {
                Process p = pb.start();
                // Redirect.INHERIT doesn't work w/ @run driver
                new Thread(() -> copy(p.getInputStream(), System.out)).start();
                new Thread(() -> copy(p.getErrorStream(), System.out)).start();
                list.add(p);
                System.out.println("=== process #" + i + " started");
            } catch (IOException e) {
                throw new Error("Failed to start process " + i, e);
            }
        }
        int failedCount = 0;
        for (int i = 0; i < list.size(); i++) {
            Process p = list.get(i);
            try {
                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    failedCount++;
                    System.out.println("=== process #" + i + " exitCode=" + exitCode);
                }
            } catch (InterruptedException e) {
                failedCount++;
                System.out.println("=== process #" + i + " waitFor failed");
                e.printStackTrace(System.out);
            }
        }
        if (failedCount != 0) {
            throw new AssertionError(failedCount + " out of " + list.size() + " tests failed");
        }
    }

    private static void copy(InputStream is, OutputStream os) {
        byte[] buffer = new byte[1024];
        int n;
        try (InputStream close = is) {
            while ((n = is.read(buffer)) != -1) {
                os.write(buffer, 0, n);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
