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
package nsk.jvmti.RetransformClasses;

import nsk.share.Consts;
import java.io.*;
import java.util.*;

public class retransform003 {
    static final String LOADED_CLASS = "nsk.share.jvmti.RetransformClasses.LinearHierarchy.Class1";

    public int runIt(String[] args, PrintStream out) {
        try {
            rightAgentOrder = new int[] { 2, 1, 3 };
            Class klass = Class.forName(LOADED_CLASS);

            rightAgentOrder = new int[] { 1, 3 };
            forceLoadedClassesRetransformation(klass);
            return status;
        }
        catch (ClassNotFoundException e) {
            return Consts.TEST_FAILED;
        }
    }

    static int[] rightAgentOrder = null;
    static int curPosition = 0;
    static int status = Consts.TEST_PASSED;

    public static void callback(String className, int agentID) {
        System.out.printf("Class: %70s; Agent ID: %d\n", className, agentID);

        if (agentID != rightAgentOrder[curPosition]) {
            System.out.printf("ERROR: %d'th agent was invoked instead of %d'th.\n", agentID, rightAgentOrder[curPosition]);
            status = Consts.TEST_FAILED;
        }

        curPosition = ++curPosition % rightAgentOrder.length;
    }

    static native boolean forceLoadedClassesRetransformation(Class klass);

    /** run test from command line */
    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + Consts.JCK_STATUS_BASE);
    }

    /** run test from JCK-compatible environment */
    public static int run(String args[], PrintStream out) {
        return (new retransform003()).runIt(args, out);
    }
}
