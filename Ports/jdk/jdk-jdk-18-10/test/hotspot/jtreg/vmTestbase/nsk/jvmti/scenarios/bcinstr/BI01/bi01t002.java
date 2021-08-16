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

package nsk.jvmti.scenarios.bcinstr.BI01;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import nsk.share.Consts;
import nsk.share.Log;
import nsk.share.Failure;
import nsk.share.CustomClassLoader;

import nsk.share.jvmti.ArgumentHandler;
import nsk.share.jvmti.DebugeeClass;

public class bi01t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new bi01t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    int status = Consts.TEST_PASSED;

    /* =================================================================== */

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.bcinstr.BI01";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".bi01t002a";
    static final String TESTED_CLASS_FILE_NAME
                        = TESTED_CLASS_NAME.replace('.', File.separatorChar) + ".class";
    static final String TESTED_METHOD_NAME = "methodA";
    static final int TOTAL_INSTRUMENTED_CLASSES = 2;
    static final int NEW_INTSRUMENTED_CODE = 0;
    static final int OLD_INTSRUMENTED_CODE = 1;

    Class<?> loadedClass[] = new Class<?>[TOTAL_INSTRUMENTED_CLASSES];

    native boolean setNewByteCode(int ind, byte[] byteCode);

    native void setClass(int ind, Class cls);

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");

        if (argv.length < 1) {
            logger.complain("This test expects path to tested class as 1-st argument");
            return Consts.TEST_FAILED;
        }

        //path to loaded class
        String path = argv[0];
        String pathToLoadedClass = path + File.separator + "loadclass";
        String pathToNewClass[] = new String[TOTAL_INSTRUMENTED_CLASSES];
        for (int i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
            int j = i + 1;
            pathToNewClass[i] = path + File.separator + "newclass"
                                    + (j<10? "0" : "") + j;
        }

        //reading byte codes for instrumented classes
        for (int i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
            logger.display("Reading bytes of instrumented class from: \n\t"
                                + pathToNewClass[i]);
            try {
                setNewByteCode(i, readBytes(pathToNewClass[i]));
            } catch (IOException e) {
                e.printStackTrace();
                throw new Failure("IOException in reading instrumented bytecode "
                                + "from:\n\t" + pathToNewClass[i] + e);
            }
        }

        /*debuggee finishes to read new byte codes nsk_jvmti_waitForSync#1*/
        if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
            return Consts.TEST_FAILED;
        }

        //loading class to generate JVMTI_EVENT_CLASS_FILE_LOAD_HOOK
        CustomClassLoader classLoader[] = new CustomClassLoader[TOTAL_INSTRUMENTED_CLASSES];
        for (int i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
            logger.display("===> Loading tested class\n\t" + i + ") " + TESTED_CLASS_NAME);
            classLoader[i] = new CustomClassLoader();
            classLoader[i].setClassPath(pathToLoadedClass);

            // loading tested classes
            try {
                loadedClass[i] = classLoader[i].loadClass(TESTED_CLASS_NAME);
            } catch (ClassNotFoundException e) {
                logger.complain("class not found!!! " + TESTED_CLASS_NAME);
                return Consts.TEST_FAILED;
            }

            /*debuggee finishes debuggee to load next class nsk_jvmti_waitForSync#2*/
            if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
                return Consts.TEST_FAILED;
            }

        }

        //checking that instrumenetation code works
        if (!checkInstrumentedMethods(NEW_INTSRUMENTED_CODE))
                status = Consts.TEST_FAILED;

        /*debuggee finishes to check instrumentation code works nsk_jvmti_waitForSync#3*/
        if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
            return Consts.TEST_FAILED;
        }

        //redefine class
        logger.display("debuggee sets classes to be redefined:");
        for (int i =0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {
            logger.display("\t" + i + ") class name: " + loadedClass[i].getName());
            setClass(i, loadedClass[i]);
        }

        /*debuggee sets classes to be redefined nsk_jvmti_waitForSync#4*/
        if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
            return Consts.TEST_FAILED;
        }

        //checking that instrumenetation code works
        if (!checkInstrumentedMethods(OLD_INTSRUMENTED_CODE))
                status = Consts.TEST_FAILED;

        /*debuggee finishes to check old byte code works nsk_jvmti_waitForSync#5*/
        if (checkStatus(Consts.TEST_PASSED) != Consts.TEST_PASSED) {
            status = Consts.TEST_FAILED;
        }

        if (status == Consts.TEST_FAILED) {
            logger.complain("Test FAILED");
        } else {
            logger.display("Test PASSED");
        }

        return status;
    }

    /** Checks instrumented methods. */
    boolean checkInstrumentedMethods(int caseParam) {
        //checking that instrumenetation code works
        Method meth = null;
        Integer res = null;
        boolean result = true;
        int expectedValue = 0;

        for (int i = 0; i < TOTAL_INSTRUMENTED_CLASSES; i++) {

            int j = i + 1;
            switch (caseParam) {
            case NEW_INTSRUMENTED_CODE:
                expectedValue = 100 + j;
                break;
            case OLD_INTSRUMENTED_CODE:
                expectedValue = 100;
                break;
            default:
                new Failure("Unknown testcase");
            }

            try {
                meth = loadedClass[i].getMethod(TESTED_METHOD_NAME);
                res = (Integer )meth.invoke(null);
            } catch (Exception e) {
                logger.complain("Exception in invoking method of "
                                        + "instrumented class:\n\t" + e);
                result = false;
                continue;
            }

            logger.display("Instrumented method returns value: " + res.intValue());

            if (res.intValue() != expectedValue) {
                logger.complain("Unexpected value for " + j
                                    + " case, expected: " + expectedValue);
                result = false;
            }
        }
        return result;
    }

    /** Read classfile for specified path. */
    byte[] readBytes(String path) throws IOException {
        String filename = path + File.separator + TESTED_CLASS_FILE_NAME;
        FileInputStream in = new FileInputStream(filename);
        byte[] bytes = new byte[in.available()];
        in.read(bytes);
        in.close();
        return bytes;
    }

}
