/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

// This test is sensitive to -Xmx. It must be run with -xmx64m.
// Running with a different -Xmx requires changing the parameters and careful re-testing.
public class HumongousDuringDumpTransformer implements ClassFileTransformer {
    public byte[] transform(ClassLoader loader, String name, Class<?> classBeingRedefined,
                            ProtectionDomain pd, byte[] buffer) throws IllegalClassFormatException {
        if (name.equals("Hello")) {
            try {
                makeHumongousRegions();
            } catch (Throwable t) {
                array = null;
                humon = null;
                System.out.println("Unexpected error: " + t);
                t.printStackTrace();
            }
        }
        array = null;
        return null;
    }

    private static Instrumentation savedInstrumentation;

    public static void premain(String agentArguments, Instrumentation instrumentation) {
        long xmx = Runtime.getRuntime().maxMemory();
        if (xmx < 60 * 1024 * 1024 || xmx > 80 * 1024 * 1024) {
            System.out.println("Running with incorrect heap size: " + xmx);
            System.exit(1);
        }

        System.out.println("ClassFileTransformer.premain() is called");
        instrumentation.addTransformer(new HumongousDuringDumpTransformer(), /*canRetransform=*/true);
        savedInstrumentation = instrumentation;
    }

    public static Instrumentation getInstrumentation() {
        return savedInstrumentation;
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        premain(args, inst);
    }

    Object[] array;

    static final int DUMMY_SIZE = 4096 - 16 - 8;
    static final int HUMON_SIZE = 4 * 1024 * 1024 - 16 - 8;
    static final int SKIP = 13;

    byte humon[] = null;
    boolean first = true;

    public synchronized void makeHumongousRegions() {
        if (!first) {
            return;
        }
        System.out.println("===============================================================================");
        first = false;

        int total = 0;
        array = new Object[100000];
        System.out.println(array);

        // (1) Allocate about 8MB of old objects.
        for (int n=0, i=0; total < 8 * 1024 * 1024; n++) {
            // Make enough allocations to cause a GC (for 64MB heap) to create
            // old regions.
            //
            // But don't completely fill up the heap. That would cause OOM and
            // may not be handled gracefully inside class transformation!
            Object x = new byte[DUMMY_SIZE];
            if ((n  % SKIP) == 0) {
                array[i++] = x;
                total += DUMMY_SIZE;
            }
        }

        System.gc();

        // (2) Now allocate a humongous array. It will sit above the 8MB of old regions.
        humon = new byte[HUMON_SIZE];
        array = null;
        System.gc();
    }
}
