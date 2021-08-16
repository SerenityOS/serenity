/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.FollowReferences;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class followref003 extends DebugeeClass {

    /** Load native library if required.*/
    static {
        loadLibrary("followref003");
    }

    /** Run test from command line. */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }
    /** Run test from JCK-compatible environment. */
    public static int run(String argv[], PrintStream out) {
        return new followref003().runIt(argv, out);
    }

    /* =================================================================== */

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    /* constants */
    public static final int DEFAULT_CHAIN_LENGTH = 3;

    /** Tested object. */
    public static followref003RootTestedClass object = null;

    /** Run debugee code. */
    public int runIt(String argv[], PrintStream out) {
        // Dummy arrays to check array lenth in ObjectReferenceCallback
        // Caution: Don't move arrays as the callback depends on slots numbers
        boolean[] boo_arr = { false, true };
        byte   [] byt_arr = { 0, 1 };
        char   [] chr_arr = { 0, 1 };
        short  [] sht_arr = { 0, 1 };
        int    [] int_arr = { 0, 1 };
        long   [] lng_arr = { 0, 1 };
        float  [] flt_arr = { 3.141592f, 0.999999f };
        double [] dbl_arr = { 3.1415926, 0.9999999 };
        String [] str_arr = { "string1", "string2", "string3" };

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        int chainLength = argHandler.findOptionIntValue("objects", DEFAULT_CHAIN_LENGTH);

        log.display("Creating chain of tested objects: " + chainLength + " length");
        object = new followref003RootTestedClass(chainLength);

        log.display("Sync: objects created");
        status = checkStatus(status);

        log.display("Cleaning links to unreachable objects");
        followref003TestedClass savedChain = object.cleanUnreachable();

        log.display("Sync: objects are unreachable");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

/** Class for root tested object. */
class followref003RootTestedClass {
    int length;

    followref003TestedClass reachableChain   = null;
    followref003TestedClass unreachableChain = null;

    public followref003RootTestedClass(int length) {
        this.length = length;
        reachableChain   = new followref003TestedClass(length);
        unreachableChain = new followref003TestedClass(length);

        reachableChain.tail.tail.tail   = reachableChain;
        unreachableChain.tail.tail.tail = unreachableChain;
    }

    public followref003TestedClass cleanUnreachable() {
        followref003TestedClass chain = unreachableChain;
        unreachableChain = null;
        return chain;
    }
}

/** Class for tested chain object. */
class followref003TestedClass {

    followref003TestedClass tail = null;

    boolean zz = true;
    byte    bb = 127;
    char    cc = 'C';
    short   ss = 1995;
    int     level;
    long    jj = 99999999;
    float   ff = 3.14f;
    double  dd = 3.14d;
    // String  str = "Fake String";
    // short[] ssArr = { 128, 256 };

    public followref003TestedClass(int length) {
        this.level = length;
        if (length > 1) {
            tail = new followref003TestedClass(length - 1);
        }
    }
}
