/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.extension.EX03;

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ex03t001 extends DebugeeClass {

    /** Run test from command line. */
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** Run test from JCK-compatible environment. */
    public static int run(String argv[], PrintStream out) {
        return new ex03t001().runIt(argv, out);
    }

    /* =================================================================== */

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.extension.EX03";
    static final String TESTED_CLASS_NAME1 = PACKAGE_NAME + ".ex03t001a";
    static final String TESTED_CLASS_NAME2 = PACKAGE_NAME + ".ex03t001b";

    /** Run debuggee code. */
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        if (argv.length < 1) {
            log.complain("This test expects path to checked class as 1-st argument");
            return Consts.TEST_FAILED;
        }
        String path = argv[0];

        log.display("CASE #1:");
        ClassUnloader unloader = new ClassUnloader();
        Class loadedClass = loadClass(unloader, TESTED_CLASS_NAME1, path);

        status = checkStatus(status);
        if (status == Consts.TEST_FAILED) {
            return status;
        }

        log.display("Perform unloading of class ex03t001a. ");
        loadedClass = null;
        if (!unloader.unloadClass()) {
            log.complain("WARNING: failed to unload class ex03t001b.");
            log.complain("Terminating the test with status: PASSED");
            return Consts.TEST_PASSED;
        } else {
            log.display("Class ex03t001a was successfully unloaded.");
        }

        log.display("CASE #2:");
        unloader = new ClassUnloader();
        loadedClass = loadClass(unloader, TESTED_CLASS_NAME2, path);

        status = checkStatus(status);

        log.display("Perform unloading of class ex03t001b. ");
        loadedClass = null;
        if (!unloader.unloadClass()) {
            log.complain("WARNING: failed to unload class ex03t001b.");
            log.complain("Terminating the test with status: PASSED");
            return Consts.TEST_PASSED;
        } else {
            log.display("Class ex03t001b was successfully unloaded.");
        }

        status = checkStatus(status);
        return status;
    }


    Class loadClass (ClassUnloader unloader, String className, String path) {
        try {
            unloader.loadClass(className, path);
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            throw new Failure(e);
        }
        Class cls = unloader.getLoadedClass();
        log.display("Class " + cls.getName() + " was successfully loaded.");
        return cls;
    }
}
