/*
 * Copyright (c) 2002, 2015, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4349534 4690242 4695338
 * @summary regression - bad LocalVariableTable attribute when no initialization needed
 * @author Tim Bell
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g GetLocalVariables2Test.java
 * @run driver GetLocalVariables2Test
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class GetLocalVariables2Targ {
    static boolean bar(int i) {
        if (i < 2) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        int i = 1;
        String command;
        if (i == 0) {
            command = "0";
        } else if (bar(i)) {
            command = "1";
        } else {
            command = "2";
        }
    }
}

    /********** test program **********/

public class GetLocalVariables2Test extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    GetLocalVariables2Test (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new GetLocalVariables2Test(args).startTests();
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("GetLocalVariables2Targ");
        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();
        EventRequestManager erm = vm().eventRequestManager();

        bpe = resumeTo("GetLocalVariables2Targ", "bar", "(I)Z");

        /*
         * Inspect the stack frame for main(), not bar()...
         */
        StackFrame frame = bpe.thread().frame(1);
        List localVars = frame.visibleVariables();
        System.out.println("    Visible variables at this point are: ");
        for (Iterator it = localVars.iterator(); it.hasNext();) {
            LocalVariable lv = (LocalVariable) it.next();
            System.out.print(lv.name());
            System.out.print(" typeName: ");
            System.out.print(lv.typeName());
            System.out.print(" signature: ");
            System.out.print(lv.type().signature());
            System.out.print(" primitive type: ");
            System.out.println(lv.type().name());

            if("command".equals(lv.name())) {
                failure("Failure: LocalVariable \"command\" should not be visible at this point.");
                if (lv.isVisible(frame)) {
                    System.out.println("Failure: \"command.isvisible(frame)\" returned true.");
                }
            }
        }

        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("GetLocalVariables2Test: passed");
        } else {
            throw new Exception("GetLocalVariables2Test: failed");
        }
    }
}
