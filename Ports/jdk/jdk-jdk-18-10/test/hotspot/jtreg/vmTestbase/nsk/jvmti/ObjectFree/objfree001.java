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

package nsk.jvmti.ObjectFree;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

/**
 * This test exercises the JVMTI event <code>ObjectFree</code>.
 * <br>It verifies that the the JVMTI raw monitor, memory management,
 * and environment local storage functions can be used during processing
 * this event.<p>
 * The test works as follows. The special class <code>objfree001u</code>
 * is loaded from directory <code>loadclass</code> by a custom class
 * loader. Instance of <code>objfree001u</code> stored in field
 * <code>unClsObj</code> is tagged by the agent via SetTag(). Then
 * the class is tried to be unloaded. Received <code>ObjectFree</code>
 * event is checked to have previously set tag. The JVMTI functions
 * mentioned above should be invoked sucessfully during the event callback
 * as well. If there are no ObjectFree events, the test passes with
 * appropriate message.
 */
public class objfree001 {
    /**
     * number of dummy object created to provoke a tested instance
     * to be GC'ed
     */
    private final int OBJ_NUM = 10000;

    /**
     * class signature to be loaded and then unloaded
     */
    private final static String CLS_TO_BE_UNLOADED =
        "nsk.jvmti.ObjectFree.objfree001u";

    /**
     * Object to be tagged in the agent part of the test
     */
    Object unClsObj = null;

    private Log log;

    static {
        try {
            System.loadLibrary("objfree001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"objfree001\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native void setTag(Object objToTag);
    native void inform(boolean clsUnloaded);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new objfree001().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        String args[] = argHandler.getArguments();
        if (args.length <= 1)
            throw new Failure("TEST BUG: path for class to be loaded and/or number of GC() calls\n"
                + "\tis not specified");

        String location = args[0];
        String clsDir = location + File.separator + "loadclass";

        ClassUnloader clsUnLoader = new ClassUnloader();
        try {
            int callNum = Integer.parseInt(args[1]);

            // load the class
            log.display("\nTrying to load class from "
                + clsDir + " ...");
            clsUnLoader.loadClass(CLS_TO_BE_UNLOADED, clsDir);

            Class unCls = clsUnLoader.getLoadedClass();
            log.display("Creating instance of \""
                + unCls + "\" ...");
            unClsObj = unCls.newInstance();
            log.display("Instance created: " + unClsObj.toString()
                + "\n\tgetClass=" + unClsObj.getClass() + "\n");

            // set tag for an instance of the class
            setTag(unClsObj);

            // unload the class
            log.display("Trying to unload \""
                + unCls + "\" ...");

            Object obj[] = new Object[OBJ_NUM];
            for (int i=0; i<OBJ_NUM; i++) {
                obj[i] =  new Object();
            }

            unClsObj = null;
            unCls = null;
            for (int i=0; i<OBJ_NUM; i++) {
                obj[i] =  null;
            }
            boolean clsUnloaded = clsUnLoader.unloadClass();
            clsUnLoader = null;

            for (int i=0; i<callNum; i++)
                System.gc();

            log.display("Unloading of the class has"
                + new String((clsUnloaded)? " ":" not ")
                + "been detected"
                + new String((clsUnloaded)? "!\n":"\n"));

            inform(clsUnloaded);

            return Consts.TEST_PASSED;
        } catch(Exception e) {
            e.printStackTrace(log.getOutStream());
            throw new Failure("TEST FAILURE: caught unexpected "
                + e);
        }
    }
}
