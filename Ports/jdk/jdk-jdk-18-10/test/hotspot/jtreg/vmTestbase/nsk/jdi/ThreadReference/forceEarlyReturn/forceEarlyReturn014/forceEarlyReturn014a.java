/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.ThreadReference.forceEarlyReturn.forceEarlyReturn014;

import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE
public class forceEarlyReturn014a extends AbstractJDIDebuggee {

    public static void main(String args[]) {
        new forceEarlyReturn014a().doTest(args);
    }

    public int publicField1 = 1;

    public int publicField2 = 2;

    public int hotMethod() {
        return publicField1++ + publicField2 * 2; // breakpointLine
    }

    public static final String breakpointMethodName = "hotMethod";

    public static final int breakpointLine = 39;

    public static final int expectedHotMethodReturnValue = Integer.MAX_VALUE;

    public static final String COMMAND_EXECUTE_HOT_METHOD = "executeHotMethod";

    public static final String COMMAND_FIRE_HOT_METHOD = "fireHotMethod";

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_EXECUTE_HOT_METHOD)) {
            int result = hotMethod();

            if (result != expectedHotMethodReturnValue) {
                setSuccess(false);
                log.complain("Unexpected value is returned from hotMethod(): " + result);
            }

            return true;
        } else if (command.equals(COMMAND_FIRE_HOT_METHOD)) {
            // after this loop hotMethod() should be compiled
            for (int i = 0; i < 20000; i++)
                hotMethod();

            return true;
        }

        return false;
    }
}
