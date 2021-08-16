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

package nsk.jvmti.scenarios.events.EM06;

import java.lang.reflect.Field;
import java.io.PrintStream;
import java.io.File;

import nsk.share.*;
import nsk.share.jvmti.*;

public class em06t001 extends DebugeeClass {

    // load native library if required
    static {
        System.loadLibrary("em06t001");
    }

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em06t001().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    int status = Consts.TEST_PASSED;

    /* =================================================================== */

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.events.EM06";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".em06t001a";
    static final String TESTED_FIELD_NAME = "toProvokePreparation";

    int classLoaderCount;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");
        classLoaderCount = argHandler.findOptionIntValue("classLoaderCount", 100);

        String args[] = argHandler.getArguments();
        if (args.length <= 0) {
            throw new Failure("Path for tested class file to load not specified");
        }

        String path = args[0];

        int currStatus;
        status = em06t001.checkStatus(Consts.TEST_PASSED);

        logger.display("Loading " + TESTED_CLASS_NAME);

        CustomClassLoader classLoader;
        Class loadedClass = null;
        Field fld;

        for (int i = 0; i < classLoaderCount; i++) {

            classLoader = new CustomClassLoader();
            classLoader.setClassPath(path);

            try {
                loadedClass = classLoader.loadClass(TESTED_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                logger.complain("class not found!!! " + TESTED_CLASS_NAME);
                return Consts.TEST_FAILED;
            }

            // to provoke class preparation
            try {
                fld = loadedClass.getField(TESTED_FIELD_NAME);
            } catch (NoSuchFieldException e) {
                logger.complain("field not found!!! " + TESTED_FIELD_NAME);
                return Consts.TEST_FAILED;
            }

        }

        currStatus = em06t001.checkStatus(Consts.TEST_PASSED);
        if (currStatus != Consts.TEST_PASSED)
            status = currStatus;

        return status;
    }

}
