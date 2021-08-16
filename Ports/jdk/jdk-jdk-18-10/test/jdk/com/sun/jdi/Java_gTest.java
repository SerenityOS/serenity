/*
 * Copyright (c) 2001, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4500906 4433599 4740097
 * @summary vmexec= debug java fails for SunCommandLineLauncher
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g Java_gTest.java
 * @run driver Java_gTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.File;

    /********** target program **********/

class Java_gTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from Java_gTarg!");
    }
}

    /********** test program **********/

public class Java_gTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    Java_gTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        /*
         * On Windows, this test needs msvcrtd.dll which is installed
         * as part of Vis C++.  We don't want this test to fail
         * if msvcrtd.dll is not present
         */
        String mslibName = System.mapLibraryName("msvcrtd");
        if (mslibName.equals("msvcrtd.dll")) {
            try {
                System.loadLibrary("msvcrtd");
            } catch (Throwable ee) {
                // If it isn't there, just pass
                System.out.println("Exception looking for msvcrtd.dll: " + ee);
                System.out.println("msvcrtd.dll does not exist.  Let the test pass");
                return;
            }
        }

        /*
         * This test would like to run the debug (java) executable.
         * If java is not found, we don't want a spurious test failure,
         * so check before attempting to run.
         *
         * We can't catch the IOException because it is thrown
         * on a separate thread (com.sun.tools.jdi.AbstractLauncher$Helper),
         * so check for the expected executable before attempting to launch.
         */

        String specialExec = "java";
        String sep = System.getProperty("file.separator");
        String jhome =  System.getProperty("java.home");
        String jbin = jhome + sep + "bin";
        File binDir = new File(jbin);
        if ((new File(binDir, specialExec).exists()) ||
            (new File(binDir, specialExec + ".exe").exists())) {
            /*
             * A java executable does in fact exist in the
             * expected location.  Run the real test.
             */
            args = new String[2];
            args[0] = "-connect";
            args[1] = "com.sun.jdi.CommandLineLaunch:vmexec=" + specialExec;
            new Java_gTest(args).startTests();
        } else {
            System.out.println("No java executable exists.  Let the test pass.");
        }
    }

    /********** event handlers **********/

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("Java_gTarg");
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("Java_gTest: passed");
        } else {
            throw new Exception("Java_gTest: failed");
        }
    }
}
