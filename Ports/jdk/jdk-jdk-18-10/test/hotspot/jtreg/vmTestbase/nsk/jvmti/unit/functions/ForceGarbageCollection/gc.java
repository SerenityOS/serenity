/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.functions.ForceGarbageCollection;

import java.io.PrintStream;

public class gc {

    int k;
    int y;
    private final static int _SIZE = 3000000;

    static {
        try {
            System.loadLibrary("gc");
        } catch(UnsatisfiedLinkError ule) {
            System.err.println("Could not load gc library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int GetResult();
    native static void jvmtiForceGC();
    native static void checkGCStart();
    native static void checkGCFinish();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95 /* STATUS_TEMP */ );
    }

    public static int run(String argv[], PrintStream out) {
         jvmtiForceGC();
         run();
         jvmtiForceGC();
         run1();
         jvmtiForceGC();
         run();
         jvmtiForceGC();
         checkGCStart();
         checkGCFinish();
         return GetResult();
    }

    public static void run() {

        long start, end;
        float difference;

        start = Runtime.getRuntime().totalMemory();

        try {
            gc[] array = new gc[_SIZE];
            for (int i = 0; i < _SIZE; i++) {
                array[i] = new gc();
            }
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }

        end = Runtime.getRuntime().totalMemory();

        difference = ( end -  start ) / _SIZE;

        System.out.println("start = " + start);
        System.out.println("end   = " + end);
    }

    public static void run1() {

        long start, end;
        float difference;

        start = Runtime.getRuntime().totalMemory();

        try {
            gc[] array = new gc[_SIZE];
            for (int i = 0; i < _SIZE; i++) {
                array[i] = new gc();
            }
        } catch (OutOfMemoryError e) {
            System.out.println(e);
        }

        end = Runtime.getRuntime().totalMemory();

        difference = ( end -  start ) / _SIZE;

        System.out.println("start = " + start);
        System.out.println("end   = " + end);
    }

}
