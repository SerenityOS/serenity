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

package nsk.jdwp.Method.IsObsolete;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdwp.*;

import java.io.*;

/**
 * This class represents debuggee part in the test.
 */
public class isobsolete002a {

    // scaffold objects
    static volatile ArgumentHandler argumentHandler = null;
    static volatile Log log = null;

    // breakpoint line in isobsolete002b
    static final int BREAKPOINT_LINE = 44;

    public static void main(String args[]) {
        System.exit(isobsolete002.JCK_STATUS_BASE + isobsolete002a.runIt(args, System.err));
    }

    public static int runIt(String args[], PrintStream out) {
        //make log for debugee messages
        argumentHandler = new ArgumentHandler(args);
        log = new Log(out, argumentHandler);

        // create tested thread
        log.display("Creating object of tested class");
        isobsolete002b.log = log;
        isobsolete002b object = new isobsolete002b();
        log.display("  ... object created");

        log.display("Invoking tested method before class redefinition");
        object.testedMethod(100);
        log.display("  ... tested method invoked");

        log.display("Invoking tested method after class redefinition");
        object.testedMethod(100);
        log.display("  ... tested method invoked");

        // exit
        log.display("Debugee PASSED");
        return isobsolete002.PASSED;
    }
}
