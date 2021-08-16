/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4245011
 * @summary Test launcher command line construction
 * @author Gordon Hirsch
 *
 * @run build TestScaffold VMConnection
 * @run compile -g HelloWorld.java
 * @run build LaunchCommandLine
 *
 * @run driver LaunchCommandLine
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;

public class LaunchCommandLine extends TestScaffold {
    public static void main(String args[]) throws Exception {
        new LaunchCommandLine(args).startTests();
    }

    LaunchCommandLine(String args[]) {
        // args are set in code below
        super(args);
    }

    protected void runTests() throws Exception {
        String[] args = new String[2];
        args[0] = "-connect";
        args[1] = "com.sun.jdi.CommandLineLaunch:main=HelloWorld a b c \"a b c\"";
        testArgs(args);
        System.out.println("com.sun.jdi.CommandLineLaunch: passed");

        // Add test for RawCommandLineLauncher?
    }

    void testArgs(String[] args) throws Exception {
        connect(args);
        waitForVMStart();

        /*
         * Get to a point where the command line args are accessible.
         */
        BreakpointEvent bp = resumeTo("HelloWorld", "main", "([Ljava/lang/String;)V");

        StackFrame frame = bp.thread().frame(0);
        LocalVariable argsVariable = frame.visibleVariableByName("args");
        ArrayReference argsArray = (ArrayReference)frame.getValue(argsVariable);

        List argValues = argsArray.getValues();

        if (argValues.size() != 4) {
            throw new Exception("Wrong number of command line arguments: " + argValues.size());
        }

        String string = ((StringReference)argValues.get(0)).value();
        if (!string.equals("a")) {
            throw new Exception("Bad command line argument value: " + string);
        }
        string = ((StringReference)argValues.get(1)).value();
        if (!string.equals("b")) {
            throw new Exception("Bad command line argument value: " + string);
        }
        string = ((StringReference)argValues.get(2)).value();
        if (!string.equals("c")) {
            throw new Exception("Bad command line argument value: " + string);
        }
        string = ((StringReference)argValues.get(3)).value();
        if (!string.equals("a b c")) {
            throw new Exception("Bad command line argument value: " + string);
        }

        // Allow application to complete
        resumeToVMDisconnect();
    }

}
