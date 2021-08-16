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

package nsk.jdi.BScenarios.hotswap;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/**
 *  <code>tc10x002a</code> is deugee's part of the tc10x002.
 */
public class tc10x002a {

    public final static String brkpMethodName = "runIt";
    public final static int brkpLineNumber1 = 60;
    public final static int brkpLineNumber2 = 57;
    public final static int checkLastLine1 = 60;
    public final static int checkLastLine2 = 57;
    public final static int INITIAL_INT_VALUE = 42;
    public final static boolean INITIAL_BOOL_VALUE = true;

    private static Log log;

    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        log = new Log(System.err, argHandler);

        runIt();
        System.exit(Consts.TEST_PASSED + Consts.JCK_STATUS_BASE);
    }

    public static void runIt() {
        boolean i = INITIAL_BOOL_VALUE;
//      ^^^^^^^     ^^^^^^^^^^^^^^^^^^ redefined line // brkpLineNumber2 // checkLastLine2
        tc10x002a obj = new tc10x002a();
        obj.method_A();
        // brkpLineNumber1 // checkLastLine1
        System.err.println("i = " + i);
    }

    public void method_A() {
        method_B();
    }

    public void method_B() {
        method_C();
    }

    public void method_C() {
        System.err.println("method_C:: line 1");
        System.err.println("method_C:: line 2");
        System.err.println("method_C:: line 3");
        System.err.println("method_C:: line 4");
    }
}
