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

package nsk.jvmti.GarbageCollectionStart;

import java.io.*;
import java.math.*;

import nsk.share.*;

/**
 * This test exercises the JVMTI event <code>GarbageCollectionStart</code>.
 * <br>It verifies that the raw monitor functions and memory management
 * functions (<code>Allocate, Deallocate</code>) can be used during processing
 * this event. Usage of these functions is allowed by the JVMTI spec.
 */
public class gcstart002 {
    /* number of interations to provoke garbage collecting */
    final static int ITERATIONS = 2;

    static {
        try {
            System.loadLibrary("gcstart002");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"gcstart002\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new gcstart002().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        try {
            for (int i=0; i<ITERATIONS; i++)
                ClassUnloader.eatMemory(); // provoke garbage collecting
        } catch (OutOfMemoryError e) {
            // ignoring
        }

        return Consts.TEST_PASSED;
    }
}
