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

import java.io.PrintStream;

import nsk.share.*;
import nsk.share.jvmti.*;

public class em01t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em01t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    static public Object threadStarting = new Object();
    static public Object threadWaiting = new Object();

    /* =================================================================== */

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.events.EM01";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".em01t002a";
    static final String TESTED_FIELD_NAME = "toProvokePreparation";

    int classLoaderCount;

    native Class loadClass(CustomClassLoader loader, String className);
    native boolean prepareClass(Class klass);
    native boolean startThread(Thread thread);

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");
        timeout = argHandler.getWaitTime() * 60000; // milliseconds
        logger.display("Timeout = " + timeout + " msc.");
        classLoaderCount = argHandler.findOptionIntValue("classLoaderCount", 100);

        String args[] = argHandler.getArguments();
        if (args.length <= 0) {
            throw new Failure("Path for tested class file to load not specified");
        }

        String path = args[0];

        int currStatus;
        status = em01t002.checkStatus(Consts.TEST_PASSED);

        CustomClassLoader classLoader;
        Class loadedClass[] = new Class[classLoaderCount];
        Thread thrd[] = new Thread[classLoaderCount];

        logger.display("Loading " + TESTED_CLASS_NAME);

        for (int i = 0; i < classLoaderCount; i++) {

            classLoader = new CustomClassLoader();
            classLoader.setClassPath(path);

            loadedClass[i] = loadClass(classLoader, TESTED_CLASS_NAME);
            if (loadedClass[i] == null) {
                logger.complain("class not loaded!!! " + TESTED_CLASS_NAME);
                return Consts.TEST_FAILED;
            }
        }

        for (int i = 0; i < classLoaderCount; i++) {

            // to provoke class preparation
            if (!prepareClass(loadedClass[i])) {
                logger.complain("class not prepared!!! " + TESTED_CLASS_NAME);
                return Consts.TEST_FAILED;
            }

        }

        currStatus = em01t002.checkStatus(Consts.TEST_PASSED);
        if (currStatus != Consts.TEST_PASSED)
            status = currStatus;


        logger.display("starting threads");

        synchronized(threadWaiting) {
            for (int i = 0; i < classLoaderCount; i++) {

                synchronized(threadStarting) {
                    // start thread and waiting for its finish
                    try {
                        thrd[i] = (Thread )loadedClass[i].newInstance();
                        startThread(thrd[i]);
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

        currStatus = em01t002.checkStatus(Consts.TEST_PASSED);
        if (currStatus != Consts.TEST_PASSED)
            status = currStatus;

        return status;
    }

}
