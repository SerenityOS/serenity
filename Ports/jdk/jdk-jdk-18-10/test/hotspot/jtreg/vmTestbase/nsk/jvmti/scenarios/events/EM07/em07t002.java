/*
 * Copyright (c) 2004, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.scenarios.events.EM07;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.PrintStream;
import java.io.File;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * Test executes the following scenario to check events <code>COMPILED_METHOD_LOAD</code>,
 * <code>COMPILED_METHOD_UNLOAD</code>:
 *
 * <ol>
 *    <li>adds the <can_generate_compiled_method_load_events> capability in the OnLoad phase;</li>
 *    <li>sets callbacks for <code>COMPILED_METHOD_LOAD</code>,
 *        <code>COMPILED_METHOD_UNLOAD</code> events during the OnLoad phase;</li>
 *    <li>enables these events during the <code>OnLoad</code> phase;</li>
 *    <li>provides the state to provoke generation of chosen events (see details below);</li>
 *    <li>checks number of <code>COMPILED_METHOD_UNLOAD</code> events is less than
 *        <code>COMPILED_METHOD_LOAD</code> or equal.</li>
 * </ol>
 *
 * To provide state provoking <code>COMPILED_METHOD_UNLOAD</code> event to be sent test
 * does the following steps:
 *
 * <ul>
 *    <li>loads tested class which is subclass of <code>java.lang.Thread</code> and
 *        placed in <code>loadclass</code> directory to avoid loading at startup.
 *        This tested class overrides method <code>Thread.run()</code> so as
 *        <code>javaMethod</code> is called 1000 times - it is enough to provoke
 *        <code>COMPILED_METHOD_LOAD</code>;
 *    <li>creates this thread and starts it by using reflective information about
 *        loaded class;
 *    <li>to provoke <code>COMPILED_METHOD_UNLOAD</code> the tested class is unload.
 *
 * Sometimes <code>COMPILED_METHOD_UNLOAD</code> may not be recieved because VM exits.
 * So to provide assured state for <code>COMPILED_METHOD_UNLOAD</code> this scenario
 * can be repeated. Repeat count can be defined in command line by <code>attempts</code>
 * optional parameter for agentlib. Default value of <code>attempts</code> is 1.
 * Also path to tested class must be set via command line as first argument,
 * eg.
 *
 *<pre>
 *  ${JAVA_HOME}/bin/java -XX:+PrintCompilation \
 *                          -cp ${TESTBASE}/bin/classes \
 *                          -agentlib:em07t002="attempts=2 -verbose" \
 *                          nsk.jvmti.scenarios.events.EM07.em07t002 \
 *                          ${PATH_TO_TESTED_CLASS}
 *</pre>
 */
public class em07t002 extends DebugeeClass {

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new em07t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    static Log log = null;
    Log.Logger logger;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    /* =================================================================== */

    static final String PACKAGE_NAME = "nsk.jvmti.scenarios.events.EM07";
    static final String TESTED_CLASS_NAME = PACKAGE_NAME + ".em07t002a";

    // run debuggee
    public int runIt(String args[], PrintStream out) {
        argHandler = new ArgumentHandler(args);
        log = new Log(out, argHandler);
        logger = new Log.Logger(log,"debuggee> ");
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds
        int attempts = argHandler.findOptionIntValue("attempts", 1);

        if (args.length < 1) {
            logger.complain("This test expects path to tested class as 1-st argument");
            return Consts.TEST_FAILED;
        }

        String path = args[0];

        Class<?> loadedClass;
        Thread thrd;
        ClassUnloader unloader = new ClassUnloader();
        for (int i = 0; i < attempts; i++) {
            logger.display("======================================");
            logger.display(" " + (i+1) + "-attempt");
            logger.display("======================================");
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
                                + "trying to unload class...");
            thrd = null;
            loadedClass = null;
            if (!unloader.unloadClass()) {
                logger.complain("WARNING::Class couldn't be unloaded");
            } else {
                logger.display("ClassLoading:: Tested class was successfully unloaded.");
            }

            if (checkStatus(status) == Consts.TEST_FAILED) {
                return Consts.TEST_FAILED;
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
