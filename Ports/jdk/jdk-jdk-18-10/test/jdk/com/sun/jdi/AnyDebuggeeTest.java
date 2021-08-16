/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 *
 *  @bug 6224700
 *  @summary ReferenceType.nestedTypes() is too slow
 *  @author jjh
 *
 *  @run build TestScaffold VMConnection TargetListener TargetAdapter
 *  @run compile -g AnyDebuggeeTest.java
 *  @run driver AnyDebuggeeeTest
 *
 *  This test is intended to be run manually to investigate behaviors;
 *  it is not an actual test of any specific functionality, it just
 *  allows you to run the debugger part of this test on any debuggee.
 *  As set up, it prints the time to find all nested types and all
 *  subclasses in the debuggee, and so can be used to verify the
 *  fix for 6224700.
 *
 *  For other investigations, edit this test to do whatever you want.
 *  To run this test do this:
 *     runregress -no AnyDebuggeeTest <cmd line options>
 *  where <cmd line options> are the options to be used to
 *  launch the debuggee, with the classname prefixed with @@.
 *  For example, this would run java2d demo as the debuggee:
 *     runregress -no AnyDebuggeeTest -classpath $jdkDir/demo/jfc/Java2D/Java2Demo.jar \
 *                                    -client @@java2d.Java2Demo'
 * If <cmd line options> is not specified, then the AnyDebuggeeTarg class below
 * is run as the debuggee.
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import javax.swing.*;

import java.util.*;

class AnyDebuggeeTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");
        try {
            javax.swing.UIManager.setLookAndFeel( javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch( Throwable exc) {
        }
        JFrame f = new JFrame("JFrame");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ee) {
        }

        System.out.println("Goodbye from NestedClassesTarg!");
    }
}

    /********** test program **********/

public class AnyDebuggeeTest extends TestScaffold {
    static String targetName = "AnyDebuggeeTarg";
    ReferenceType targetClass;
    ThreadReference mainThread;

    AnyDebuggeeTest(String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        /*
         * If args contains @@xxxx, then that is the
         * name of the class we are to run.
         */
        for (int ii = 0; ii < args.length; ii ++) {
            if (args[ii].startsWith("@@")) {
                targetName = args[ii] = args[ii].substring(2);
            }
        }
        new AnyDebuggeeTest(args).startTests();
    }


    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe;
        bpe = startToMain(targetName);

        targetClass = bpe.location().declaringType();
        mainThread = bpe.thread();

        // Let debuggee run for awhile to get classes loaded
        resumeForMsecs(20000);

        List<ReferenceType> allClasses = vm().allClasses();
        System.out.println( allClasses.size() + " classes");


        int size = 0;
        long start = System.currentTimeMillis();
        for(ReferenceType rt: allClasses) {
            if (rt instanceof ClassType) {
            List<ReferenceType> nested = rt.nestedTypes();
            int sz = nested.size();
            size += sz;
        }
        }
        long end = System.currentTimeMillis();
        System.out.println(size + " nested types took " + (end - start) + " ms");

        size = 0;
        start = System.currentTimeMillis();
        for(ReferenceType rt: allClasses) {
            if (rt instanceof ClassType) {
                List<ClassType> subs = ((ClassType)rt).subclasses();
                int sz = subs.size();
                size += sz;
            }
        }
        end = System.currentTimeMillis();
        System.out.println(size + " subclasses took " + (end - start) + " ms");

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("AnyDebuggeeTest: passed");
        } else {
            throw new Exception("AnyDebuggeeTest: failed");
        }
    }
}
