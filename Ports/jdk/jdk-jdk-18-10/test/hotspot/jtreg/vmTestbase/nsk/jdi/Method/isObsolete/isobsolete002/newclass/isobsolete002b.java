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

package nsk.jdi.Method.isObsolete;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE
/**
 * This is the redefining class for the isobsolete002 JDI test.
 */
public class isobsolete002b {

    static final String prefix = "**> debuggee: ";
    static Log log;

    isobsolete002b(Log log) {
        this.log = log;
        log.display(prefix + "   This is the class to redefine");
    }

    static int i1 = 0;
    static int i2 = 0;

    static void m2() {
        boolean b1 = true;
        log.display(prefix + "redefining method: b1 " + b1); // isobsolete001.brkpLineNumber
    }

    static void m1() {
        log.display(prefix + "method m1: before   m2()");
        m2();
        log.display(prefix + "method m1: after    m2()");
    }

}
