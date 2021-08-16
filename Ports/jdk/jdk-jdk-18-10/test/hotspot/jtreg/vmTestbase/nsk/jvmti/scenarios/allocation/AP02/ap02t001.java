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

package nsk.jvmti.scenarios.allocation.AP02;

import java.io.*;
import java.lang.reflect.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class ap02t001 extends DebugeeClass {

    public static void main(String[] argv) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        return new ap02t001().runThis(argv, out);
    }

    private static native void throwException (Class cls);

    /* scaffold objects */
    ArgumentHandler argHandler = null;
    Log log = null;
    long timeout = 0;
    int status = Consts.TEST_PASSED;

    private int runThis(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000; // milliseconds

        Exception e1 = null;
        Exception e2 = null;

        try {
            throw new ap02t001Exception("Got expected exception thrown from Java code");
        } catch (Exception e) {
            e.printStackTrace(out);
            log.display("CASE 1: Exception thrown from Java code");
            status = checkStatus(status);
            e1 = e;
        }

        try {
            throwException(e1.getClass());
        } catch (Exception e) {
            e.printStackTrace(out);
            if (e.equals(e1)) {
                status = Consts.TEST_FAILED;
                log.complain("Native method throwed the same ap02t001Exception instance");
            }
            if (!(e instanceof ap02t001Exception)) {
                status = Consts.TEST_FAILED;
                log.complain("Native method throwed unexpected exception: " + e.getClass().toString());
            }
            log.display("CASE 2: Exception thrown by call to native method");
            status = checkStatus(status);
            e2 = e;
        }
        // Ensure that e1 and e2 are not optimized by the compiler (see 6951137).
        if (e1.toString().length() + e2.toString().length() < 0) {
            log.display("Should never happen");
            return Consts.TEST_FAILED;
        }

        return status;
    }
}

class ap02t001Exception extends Exception {

    ap02t001Exception () {
        super();
    }

    ap02t001Exception (String s) {
        super(s);
    }
}
