/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ObjectReference.invokeMethod;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 * This is a main debuggee class.
 */
public class invokemethod014t {
    public static Log log;

    public static void main(String args[]) {
        System.exit(run(args) + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[]) {
        return new invokemethod014t().runIt(args);
    }

    private int runIt(String args[]) {
        ArgumentHandler argHandler = new ArgumentHandler(args);
        IOPipe pipe = argHandler.createDebugeeIOPipe();

        // force debuggee VM to load dummy classes
        invokemethod014tDummyClass invokemethod014tdummyCls =
            new invokemethod014tDummyClass();
        invokemethod014tDummySuperClass invokemethod014tdummySCls =
            new invokemethod014tDummySuperClass();
        nsk.jdi.ObjectReference.dummyPackage.invokemethod014a
            invokemethod014adummyCls =
                new nsk.jdi.ObjectReference.dummyPackage.invokemethod014a();

        log = argHandler.createDebugeeLog();

        Thread.currentThread().setName(invokemethod014.DEBUGGEE_THRNAME);

        // Now the debuggee is ready for testing
        pipe.println(invokemethod014.COMMAND_READY);
        String cmd = pipe.readln();
        if (cmd.equals(invokemethod014.COMMAND_QUIT)) {
            log.complain("Debuggee: exiting due to the command "
                + cmd);
            return Consts.TEST_PASSED;
        }

        int stopMeHere = 0;// invokemethod014.DEBUGGEE_STOPATLINE

        cmd = pipe.readln();
        if (!cmd.equals(invokemethod014.COMMAND_QUIT)) {
            log.complain("TEST BUG: unknown debugger command: "
                + cmd);
            System.exit(Consts.JCK_STATUS_BASE +
                Consts.TEST_FAILED);
        }
        return Consts.TEST_PASSED;
    }

}

/**
 * Dummy superclass used to provoke IllegalArgumentException
 * in the debugger: private methods cannot be accessed from
 * other classes.
 */
class invokemethod014tDummySuperClass {
    private byte prByteMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prByteMeth\" was invoked!");
        return 127;
    }

    private short prShortMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prShortMeth\" was invoked!");
        return -32768;
    }

    private int prIntMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prIntMeth\" was invoked!");
        return 2147483647;
    }

    private long prLongMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prLongMeth\" was invoked!");
        return 9223372036854775807L;
    }

    private float prFloatMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prFloatMeth\" was invoked!");
        return 5.1F;
    }

    private double prDoubleMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prDoubleMeth\" was invoked!");
        return 6.2D;
    }

    private char prCharMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prCharMeth\" was invoked!");
        return 'a';
    }

    private boolean prBooleanMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prBooleanMeth\" was invoked!");
        return false;
    }

    private String prStrMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prStrMeth\" was invoked!");
        return "string method";
    }

    private void prVoidMeth() {
        invokemethod014t.log.complain("invokemethod014tDummySuperClass: private method \"prVoidMeth\" was invoked!");
    }
}

/**
 * Dummy subclass used to provoke IllegalArgumentException
 * in the debugger.
 */
class invokemethod014tDummyClass extends invokemethod014tDummySuperClass {}
