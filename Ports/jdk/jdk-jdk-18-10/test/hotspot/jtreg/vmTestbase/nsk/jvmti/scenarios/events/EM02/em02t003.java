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

package nsk.jvmti.scenarios.events.EM02;

import java.lang.reflect.Method;
import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;


import java.util.*;
import java.math.*;

public class em02t003 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em02t003().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    int status = Consts.TEST_PASSED;

    static final int STEP_AMOUNT = 3;
    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.events.EM02";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".em02t003a";

    // run debuggee
    public int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");

        String path;
        if (args.length == 0) {
            path = "loadclass";
        } else {
            path = args[0];
        }

        Class<?> loadedClass;
        Thread thrd;

        ClassUnloader unloader = new ClassUnloader();
        for (int i = 0; i < 3; i++) {
            try {
                unloader.loadClass(TESTED_CLASS_NAME, path);
            } catch(ClassNotFoundException e) {
                e.printStackTrace();
                return Consts.TEST_FAILED;
            }
            logger.display("ClassLoading:: Tested class was successfully loaded.");

            logger.display("MethodCompiling:: Provoke compiling.");
            loadedClass = unloader.getLoadedClass();

            try {
                thrd = (Thread )loadedClass.newInstance();
            } catch (Exception e) {
                logger.complain("Unexpected exception " + e);
                e.printStackTrace();
                return Consts.TEST_FAILED;
            }

            if (!invokeMethod(loadedClass, thrd, "start")) {
                return Consts.TEST_FAILED;
            }

            if (!invokeMethod(loadedClass, thrd, "join")) {
                return Consts.TEST_FAILED;
            }

            logger.display("MethodCompiling:: Provoke unloading compiled method - "
                                + "\n\ttrying to unload class...");
            thrd = null;
            loadedClass = null;
            if (!unloader.unloadClass()) {
                logger.complain("WARNING::Class couldn't be unloaded");
            } else {
                logger.display("ClassLoading:: Tested class was successfully unloaded.");
            }

            if (checkStatus(Consts.TEST_PASSED) == Consts.TEST_FAILED) {
                status = Consts.TEST_FAILED;
            }

            logger.display("\n");
        }
        return status;
    }

    boolean invokeMethod(Class<?> cls, Thread thrd, String methodName) {

        Method method;

        try {
            method = cls.getMethod(methodName);
            method.invoke(thrd);
        } catch (Exception e) {
            logger.complain("Unexpected exception " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
