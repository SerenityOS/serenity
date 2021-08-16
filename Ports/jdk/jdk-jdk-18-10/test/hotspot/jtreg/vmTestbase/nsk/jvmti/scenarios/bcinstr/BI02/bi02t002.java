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

package nsk.jvmti.scenarios.bcinstr.BI02;

import java.io.*;

import nsk.share.*;
import nsk.share.jvmti.*;

public class bi02t002 extends DebugeeClass {

    static final int MAGIC_NUMBER = 101;
    static final String bi02t002aClassName =
        "nsk.jvmti.scenarios.bcinstr.BI02.bi02t002a";

    // run test from command line
    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        // JCK-compatible exit
        System.exit(run(argv, System.out) + Consts.JCK_STATUS_BASE);
    }

    // run test from JCK-compatible environment
    public static int run(String argv[], PrintStream out) {
        return new bi02t002().runIt(argv, out);
    }

    /* =================================================================== */

    // scaffold objects
    ArgumentHandler argHandler = null;
    int status = Consts.TEST_PASSED;
    Log log = null;
    long timeout = 0;

    /* new bytecodes of tested class */
    public static byte newClassBytes[] = null;

    // run debuggee
    public int runIt(String argv[], PrintStream out) {
        argHandler = new ArgumentHandler(argv);
        log = new Log(out, argHandler);
        timeout = argHandler.getWaitTime() * 60 * 1000;

        String fileDir = ".";
        String args[] = argHandler.getArguments();
        if (args.length > 0) {
            fileDir = args[0];
        }

        String fileNameA = fileDir + File.separator + "newclass" + File.separator +
            bi02t002aClassName.replace('.', File.separatorChar) + ".class";
        log.display("Reading bytes of new class: \n\t" + fileNameA);
        try {
            FileInputStream in = new FileInputStream(fileNameA);
            newClassBytes = new byte[in.available()];
            in.read(newClassBytes);
            in.close();
        } catch (Exception e) {
            throw new Failure("Unexpected exception while reading class file:\n\t" + e);
        }

        bi02t002a testedClass = new bi02t002a();
        int value = testedClass.check();
        log.display("Before redefinition: " + value);
        if (value != 1) {
            log.complain("Wrong value: " + value +
                ", expected: " + 1);
            status = Consts.TEST_FAILED;
        }

        log.display("redefining bi02t002a class via RedefineClasses function");
        status = checkStatus(status);

        value = testedClass.check();
        log.display("After redefinition: " + value);
        if (value != MAGIC_NUMBER) {
            log.complain("Wrong value: " + value +
                ", expected: " + MAGIC_NUMBER);
            status = Consts.TEST_FAILED;
        }

        log.display("Debugee finished");
        return checkStatus(status);
    }
}

/* =================================================================== */

class bi02t002a {

    public int check() {
        return 1;
    }
}
