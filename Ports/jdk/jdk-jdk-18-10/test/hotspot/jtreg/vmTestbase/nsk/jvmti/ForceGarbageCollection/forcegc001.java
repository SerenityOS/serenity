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

package nsk.jvmti.ForceGarbageCollection;

import java.io.PrintStream;
import java.lang.ref.SoftReference;

import nsk.share.*;
import nsk.share.jvmti.*;

public class forcegc001 extends DebugeeClass {

    /* Load native library if required. */
    static {
        loadLibrary("forcegc001");
    }

    /* Run test from command line. */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    /* Run test from JCK-compatible environment. */
    public static int run(String argv[], PrintStream out) {
        return new forcegc001().runIt(argv, out);
    }

    /* =================================================================== */

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    /* constants */
    public static final int DEFAULT_OBJECTS_COUNT = 100;

    /* Run debuggee code. */
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        int count = argHandler.findOptionIntValue("objects", DEFAULT_OBJECTS_COUNT);

        log.display("Creating tested objects: " + count + " objects");
        Object objects[] = new Object[count];
        for (int i = 0; i < count; i++) {
            objects[i] = new Object();
        }

        log.display("Creating soft references for: " + count + " objects");
        SoftReference<?> refs[] = new SoftReference[count];
        for (int i = 0; i < count; i++) {
            refs[i] = new SoftReference<Object>(objects[i]);
        }

        log.display("Clearing stroong references to the tested objects");
        objects = null;

        log.display("Sync: objects ready for GC");
        status = checkStatus(status);

        String kind = "after ForceGarbageCollection()";

        log.display("Checking soft references " + kind + ": " + count + " references");
        int found = checkObjects(count, refs, kind);
        if (found > 0) {

            kind = "after System.gc()";
            System.gc();

            log.display("Checking soft references " + kind + ": " + count + " references");
            int found1 = checkObjects(count, refs, kind);
            if (found1 < found) {
                log.println("# WARNING: " + found1 + " of " + found
                            + " softly reachable objects were GCed\n"
                            + "#   by System.gc() but not by ForceGarbageCollection()");
            }
        }

        return status;
    }

    /* Check if soft references to objects were cleared by GC, */
    public int checkObjects(int count, SoftReference<?> refs[], String kind) {
        int found = 0;

        for (int i = 0; i < count; i++) {
            if (refs[i].get() != null)
                found++;
        }

        if (found > 0) {
            log.println("# WARNING: " + found + " of " + count
                            + " softly reachable objects not GCed " + kind);
        } else {
            log.display("All " + found + " of " + count
                            + " softly reachable objects GCed " + kind);
        }

        return found;
    }
}
