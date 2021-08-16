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

/*
 * Unit tests for JVMTI OBJECT_FREE event
 *
 * WARNING: This test makes the assumption that System.gc() will free some
 * objects used in test. This is not a good assumption. Please mail if you
 * can think of a better way to test this event.
 *
 */

package nsk.jvmti.unit.heap;

import nsk.share.jvmti.unit.*;
import java.io.PrintStream;

public class ObjectFreeTests {

    final static int JCK_STATUS_BASE = 95;
    final static int PASSED = 0;
    final static int FAILED = 2;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {

        /*
         * Tag 50,000 objects, do a free GCs, and hope that we get 50,000
         * OBJECT_FREE events
         */

        Heap.setObjectFreeCallback();

        int count = 50*1000;

        for (int i=0; i<count; i++) {
            Heap.setTag(new Object(), 100);
        }

        // do a few GCs - hopefully objects will be GC'ed
        for (int i=0; i<5; i++) {
            System.gc();
        }

        int freed = Heap.getObjectFreeCount();

        System.out.println("Number of object free events: " + freed +
            ", expected: " + count);
        if (freed > count) {
            throw new RuntimeException("More events than objects");
        }
        if (freed < count) {
            System.err.println("WARNING: less than expected!!!");
        }

        return PASSED;
    }

}
