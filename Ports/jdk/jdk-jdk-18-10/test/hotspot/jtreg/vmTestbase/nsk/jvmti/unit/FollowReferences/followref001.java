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

public class followref001 extends DebugeeClass {

    /** Load native library if required.*/
    static {
        loadLibrary("followref001");
    }

    /** Run test from command line */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** Run test from JCK-compatible environment */
    public static int run(String argv[], PrintStream out) {
        return new followref001().runIt(argv, out);
    }

    /* =================================================================== */

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    int status = Consts.TEST_PASSED;

    /* constants */
    public static final int DEFAULT_CHAIN_LENGTH = 3;

    /** Tested object */
    public static followref001RootTestedClass rootObject = null;

    /** Run debugee code */
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        int chainLength = argHandler.findOptionIntValue("objects", DEFAULT_CHAIN_LENGTH);

        log.display("Creating chain of tested objects: " + chainLength + " length");
        rootObject = new followref001RootTestedClass(chainLength);

        log.display("Sync: two object chains created");
        status = checkStatus(status);

        log.display("Cleaning the unreachableChain field");
        /* This is to ensure that it is not GC-ed */
        followref001TestedClass savedChain = rootObject.cleanUnreachable();

        log.display("Sync: 2-nd object chain is unreachable from the root object");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

/** Class for root tested object */
class followref001RootTestedClass {
    int length;

    followref001TestedClass reachableChain   = null;
    followref001TestedClass unreachableChain = null;

    public followref001RootTestedClass(int length) {
        this.length = length;
        reachableChain   = new followref001TestedClass(length);
        unreachableChain = new followref001TestedClass(length);
        reachableChain.setChainTail(length, reachableChain);
        unreachableChain.setChainTail(length, unreachableChain);
    }

    public followref001TestedClass cleanUnreachable() {
        followref001TestedClass chain = unreachableChain;
        unreachableChain = null;
        return chain;
    }
}

/** Class for tested chain object */
class followref001TestedClass {

    followref001TestedClass next = null;

    boolean zz = true;
    byte    bb = 127;
    char    cc = 'C';
    short   ss = 1995;
    int     level;
    long    jj = 99999999;
    float   ff = 3.14f;
    double  dd = 3.14d;

    public followref001TestedClass(int length) {
        this.level = length;
        if (length > 1) {
            next = new followref001TestedClass(length - 1);
        }
    }

    public void setChainTail(int length, followref001TestedClass last) {
        if (length > 1) {
            next.setChainTail(length - 1, last);
        } else {
            next = last;
        }
    }
}
