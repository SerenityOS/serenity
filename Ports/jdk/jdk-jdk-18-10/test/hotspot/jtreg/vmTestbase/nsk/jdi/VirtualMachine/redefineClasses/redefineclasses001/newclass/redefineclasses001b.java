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

package nsk.jdi.VirtualMachine.redefineClasses;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

/**
 * This is the redefining class for the redefineclasses001 JDI test.
 */

public class redefineclasses001b {

    static final String prefix = "**> debuggee: ";
    static Log log;

    redefineclasses001b(Log log) {
        this.log = log;
        log.display(prefix + "   This is the class to be redefine");
    }

    static int i1 = 0;

    static int bpline = 2;

    static void m2() {
        i1 = 2;
        i1 = 2;
    }

    static void m1() {
        log.display(prefix + "redefining method: before:   m2()");
        m2();
        log.display(prefix + "redefining method: after:    m2()");
        log.display(prefix + "redefining method: value of i1 == " + i1);
    }

}
