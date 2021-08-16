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

package nsk.jvmti.GetObjectHashCode;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class objhashcode001 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("objhashcode001");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new objhashcode001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    // tested thread
    public static objhashcode001TestedClass testedObject = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        log.display("Creating tested object");
        testedObject = new objhashcode001TestedClass();

        log.display("Sync: object created");
        status = checkStatus(status);

        log.display("Changing object data");
        testedObject.change();

        log.display("Sync: object data changed");
        status = checkStatus(status);

        return status;
    }
}

/* =================================================================== */

// class for tested object
class objhashcode001TestedClass {

    // static data
    public static int staticIntValue = 100;
    public static long staticLongValue = 100;
    public static double staticDoubleValue = 100.001;
    public static String staticStringValue = "hundred";

    // public object data
    public int publicIntValue = 100;
    public long publicLongValue = 100;
    public double publicDoubleValue = 100.002;
    public String publicStringValue = "hundred";

    // private object data
    private int privateIntValue = 100;
    private long privateLongValue = 100;
    private double privateDoubleValue = 100.001;
    private String privateStringValue = "hundred";

    // change all data
    public void change() {

        // change static data
        staticIntValue = -200;
        staticLongValue = 200;
        staticDoubleValue = -200.002e-2;
        staticStringValue = "changed";

        // change public object data
        publicIntValue = 300;
        publicLongValue = -300;
        publicDoubleValue = 300.003e+3;
        publicStringValue = "changed too";

        // change private object data
        privateIntValue = -400;
        privateLongValue = 400;
        privateDoubleValue = -400.004e-4;
        privateStringValue = null;

    }
}
