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

package nsk.jdwp.VirtualMachine.RedefineClasses;

import nsk.share.*;

/**
 * This class is to be redefined.
 */
public class redefinecls001b {

    // values for tested fields
    public static final int INITIAL_FIELD_VALUE = 111;
    public static final int FINAL_FIELD_VALUE = 222;

    // values for method redefinition
    public static final int METHOD_NOT_INVOKED = 0;
    public static final int REDEFINED_METHOD_INVOKED = 10;
    public static final int NOT_REDEFINED_METHOD_INVOKED = 20;

    // static field
    public static int staticField = INITIAL_FIELD_VALUE;
    // object field
    public int objectField = INITIAL_FIELD_VALUE;

    public static Log log;
    // fields for methods redefinition results
    public static int constructorInvoked = METHOD_NOT_INVOKED;
    public static int staticMethodInvoked = METHOD_NOT_INVOKED;
    public static int objectMethodInvoked = METHOD_NOT_INVOKED;

    // constructor
    public redefinecls001b(int value) {
        log.display("Constructor invoked: NOT REDEFINED");
        constructorInvoked = NOT_REDEFINED_METHOD_INVOKED;
        objectField = value;
    }

    // static method to be redefined
    public static void testedStaticMethod() {
        log.display("Static method invoked: NOT REDEFINED");
        staticMethodInvoked = NOT_REDEFINED_METHOD_INVOKED;

        log.display("Static fields values:");
        log.display("    staticField: " + staticField
                                + " (expected: " + FINAL_FIELD_VALUE + ")");
    }

    // object method to be redefined
    public void testedObjectMethod() {
        log.display("Object method invoked: NOT REDEFINED");
        objectMethodInvoked = NOT_REDEFINED_METHOD_INVOKED;

        log.display("Object fields values:");
        log.display("    objectField: " + objectField
                                + " (expected: " + FINAL_FIELD_VALUE + ")"); // BREAKPOINT_LINE_BEFORE
    }
}
