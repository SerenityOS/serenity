/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jdwp.VirtualMachine.RedefineClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class redefinecls001a {

    // line nunber for breakpoint
    public static final int BREAKPOINT_LINE_BEFORE = 76;
    public static final int BREAKPOINT_LINE_AFTER = 86;

    // fields for methods redefinition results
//    public static Integer constructorInvoked = Integer(redefinecls001.METHOD_NOT_INVOKED);
//    public static Integer staticMethodInvoked = Integer(redefinecls001.METHOD_NOT_INVOKED);
//    public static Integer objectMethodInvoked = Integer(redefinecls001.METHOD_NOT_INVOKED);

    // scaffold objects
    static volatile ArgumentHandler argumentHandler = null;
    static volatile Log log = null;

    public static void main(String args[]) {
        System.exit(redefinecls001.JCK_STATUS_BASE + redefinecls001a.runIt(args, System.err));
    }

    public static int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // create tested thread
        log.display("Creating object of tested class");
        redefinecls001b.staticField = redefinecls001b.FINAL_FIELD_VALUE;
        redefinecls001b.log = log;
        redefinecls001b object = new redefinecls001b(redefinecls001b.FINAL_FIELD_VALUE);
        log.display("  ... object created");

        // invoke tested method before redefinition
        log.display("Invoking tested methods before redefinition");
        redefinecls001b.testedStaticMethod();
        object.testedObjectMethod();
        printInvocationResult();

        log.display("\nBreakpoint line before redefinition reached");
        // next line is for breakpoint before redefinition
        int foo = 0; // BREAKPOINT_LINE_BEFORE

        // invoke tested method after redefinition
        log.display("Invoking tested methods after redefinition");
        redefinecls001b.testedStaticMethod();
        object.testedObjectMethod();
        printInvocationResult();

        log.display("\nBreakpoint line after redefinition reached");
        // next line is for breakpoint after redefinition
        foo = 1; // BREAKPOINT_LINE_AFTER

        log.display("Debugee PASSED");
        return redefinecls001.PASSED;
    }

    // print information about kind of invoked methods
    private static void printInvocationResult() {
        log.display("Result of methods invokation:");
        log.display("    constructorInvoked:  " + methodKind(redefinecls001b.constructorInvoked));
        log.display("    staticMethodInvoked: " + methodKind(redefinecls001b.staticMethodInvoked));
        log.display("    objectMethodInvoked: " + methodKind(redefinecls001b.objectMethodInvoked));
    }

    // return string representation of kind of invoked method
    private static String methodKind(int kind) {
        switch (kind) {
            case redefinecls001b.METHOD_NOT_INVOKED:
                return "METHOD_NOT_INVOKED";
            case redefinecls001b.REDEFINED_METHOD_INVOKED:
                return "REDEFINED_METHOD_INVOKED";
            case redefinecls001b.NOT_REDEFINED_METHOD_INVOKED:
                return "NOT_REDEFINED_METHOD_INVOKED";
            default:
                return "UNKNOWN_METHOD_KIND";
        }
    }
}
