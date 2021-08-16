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

package nsk.jvmti.scenarios.events.EM01;

import java.lang.reflect.Field;
import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class em01t001 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em01t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    long timeout = 0;

    static public Object threadStarting = new Object();
    static public Object threadWaiting = new Object();

    /* =================================================================== */

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.events.EM01";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".em01t001a";
    static final String TESTED_FIELD_NAME = "toProvokePreparation";

    // run debuggee
    public int runIt(String argv[], PrintStream out) {

        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");
        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        logger.display("Timeout = " + timeout + " msc.");
        int classLoaderCount = argHandler.findOptionIntValue("classLoaderCount", 100);

        String args[] = argHandler.getArguments();
        if (args.length <= 0) {
            throw new Failure("Path for tested class file to load not specified");
        }

        String path = args[0];

        int status = em01t001.checkStatus(Consts.TEST_PASSED);

        logger.display("Loading " + TESTED_CLASS_NAME);

        CustomClassLoader classLoader;
        Class loadedClass[] = new Class[classLoaderCount];
        Field fld;
        Thread thrd[] = new Thread[classLoaderCount];

        for (int i = 0; i < classLoaderCount; i++) {

            classLoader = new CustomClassLoader();
            classLoader.setClassPath(path);

            try {
                loadedClass[i] = classLoader.loadClass(TESTED_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                logger.complain("class not found!!! " + TESTED_CLASS_NAME);
                return Consts.TEST_FAILED;
            }
        }

        for (int i = 0; i < classLoaderCount; i++) {

            // to provoke class preparation
            try {
                fld = loadedClass[i].getField(TESTED_FIELD_NAME);
            } catch (NoSuchFieldException e) {
                logger.complain("field not found!!! " + TESTED_FIELD_NAME);
                return Consts.TEST_FAILED;
            }
        }

        if (em01t001.checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED)
            status = Consts.TEST_FAILED;

        logger.display("starting threads");

        synchronized(threadWaiting) {
            for (int i = 0; i < classLoaderCount; i++) {
                // start thread and waiting for its finish
                synchronized(threadStarting) {
                    try {
                        thrd[i] = (Thread )loadedClass[i].newInstance();
                        thrd[i].start();
                        threadStarting.wait(timeout);
                    } catch (Exception e) {
                        logger.complain("Unexpected exception " + e);
                        e.printStackTrace();
                        return Consts.TEST_FAILED;
                    }
                }
            }
        }

        for (int i = 0; i < classLoaderCount; i++) {
            try {
                thrd[i].join();
            } catch(InterruptedException e) {
                logger.complain("Unexpected exception " + e);
                e.printStackTrace();
                return Consts.TEST_FAILED;
            }
        }

        if (em01t001.checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED)
            status = Consts.TEST_FAILED;

        return status;
    }

}
