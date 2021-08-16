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
 * <br>It verifies that the events are only sent for tagged objects.
 * The test also checks that the JVMTI raw monitor, memory management,
 * and environment local storage functions can be used during processing
 * this event.<p>
 * The test works as follows. An array of instances of the special
 * class <code>objfree002t</code> is used for testing as follows.
 * The objects with odd array indexes are skipped from tagging, all
 * other ones are tagged. Then, after nulling the objects and GC() call,
 * all received <code>ObjectFree</code> events, if so, should have only
 * the even tag numbers.<br>
 * If there are no ObjectFree events, the test passes with appropriate
 * message.
 */
public class objfree002 {
    /**
     * number of objects to be created
     */
    private final static int OBJ_NUM = 10;

    /**
     * Object to be tagged
     */
    objfree002t[] objToTag = new objfree002t[OBJ_NUM];

    private Log log;

    static {
        try {
            System.loadLibrary("objfree002");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load \"objfree002\" library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native int check();
    native void setTag(objfree002t obj, int num);

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // produce JCK-like exit status
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new objfree002().runThis(argv, out);
    }

    private int runThis(String argv[], PrintStream out) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);

        String args[] = argHandler.getArguments();
        if (args.length <= 0)
            throw new Failure("TEST BUG: number of GC() calls is not specified");

        try {
            int callNum = Integer.parseInt(args[0]);

            log.display("Creating " + OBJ_NUM
                + " instance(s) of the tested class ...\n");
            for (int i=0; i<OBJ_NUM; i++) {
                objToTag[i] =  new objfree002t();
                setTag(objToTag[i], i); // tagging some of the objects
            }
            log.display("\nCreating " + OBJ_NUM
                + " instance(s) done\n");

            log.display("Freeing up the instance(s) ...\n");
            for (int i=0; i<OBJ_NUM; i++)
                objToTag[i] =  null;

            log.display("Calling GC() " + callNum
                + " times ...\n");
            for (int i=0; i<callNum; i++)
                System.gc();
            log.display("Calling GC() done\n");

            return check();
        } catch(Exception e) {
            e.printStackTrace();
            throw new Failure("TEST FAILURE: caught unexpected "
                + e);
        }
    }
}
