/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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
 *  @test
 *  @bug 4238644 4238643 4238641 4944198 8181500
 *  @summary Test javac regressions in the generation of line number info
 *  @author Gordon Hirsch
 *
 *  @run build TestScaffold VMConnection TargetListener TargetAdapter
 *  @run compile -XDstringConcat=inline -g LineNumberInfo.java ControlFlow.java
 *
 *  @run driver LineNumberInfo
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.List;
import java.util.Iterator;

public class LineNumberInfo extends TestScaffold {
    /*
     * These two arrays are used to validate the line number
     * information returned by JDI. There are limitations to
     * this approach:
     * - there are no strict rules about
     *   what constitutes the "right" line number mapping, so
     *   this kind of test may have false negatives with other
     *   compilers.
     * - this test is also sensitive to the compiler's code generation;
     *   if that changes, this test will likely need updating.
     * - this test assumes that JDI code index == class file
     *   byte code index which may not be true in all VMs.
     * - To find the values for these tables, compile ControlFlow.java and then
     *   do:
     *   javap -classpath _jj1.solaris-sparc/JTwork/classes/com/sun/jdi \
     *                    -l ControlFlow
     */
    final int[] lineNumbers = {
        15,
        16,
        19,
        20,
        22,
        25,
        26,
        28,
        32,
        33,
        34,
        36,
        37,
        36,
        37,
        40,
        41,
        42,
        45,
        46,
        45,
        49,
        51,
        53,
        55,
        57,
        59,
        60,
        62,
        65,
        67,
        69,
        71,
        73,
        75,
        78
    };

    final int[] codeIndices = {
        0  ,
        7  ,
        15 ,
        22 ,
        33 ,
        43 ,
        50 ,
        60 ,
        68 ,
        76 ,
        77 ,
        85 ,
        93 ,
        96 ,
        105,
        107,
        111,
        119,
        129,
        139,
        178,
        184,
        240,
        250,
        260,
        270,
        280,
        288,
        291,
        301,
        336,
        346,
        356,
        366,
        376,
        384
    };

    LineNumberInfo(String args[]) {
        super(args);
    }

    public static void main(String args[]) throws Exception {
        new LineNumberInfo(args).startTests();
    }

    protected void runTests() throws Exception {
        startUp("ControlFlow");

        // Get the ControlFlow class loaded.
        ClassPrepareEvent event = resumeToPrepareOf("ControlFlow");

        ClassType clazz = (ClassType)event.referenceType();
        Method method = clazz.concreteMethodByName("go", "()V");
        List locations = method.allLineLocations();

        if (lineNumbers.length != codeIndices.length) {
            failure("FAILED: Bad test. Line number and code index arrays " +
                 "must be equal in size");
        }

        if (locations.size() != codeIndices.length) {
            // Help the tester see why it failed.
            Iterator iter = locations.iterator();
            while (iter.hasNext()) {
                Location location = (Location)iter.next();
                System.err.println("location=" + location);
            }

            failure("FAILED: Bad line number table size: jdi=" +
                 locations.size() +
                 ", test=" + codeIndices.length);
        }

        int i = 0;
        Iterator iter = locations.iterator();
        while (iter.hasNext()) {
            Location location = (Location)iter.next();
            if (location.codeIndex() != codeIndices[i]) {
                failure("FAILED: Code index mismatch: jdi=" +
                                    location.codeIndex() +
                                    ", test=" + codeIndices[i]);
            }
            if (location.lineNumber() != lineNumbers[i]) {
                failure("FAILED: Line number mismatch: jdi=" +
                                    location.lineNumber() +
                                    ", test=" + lineNumbers[i]);
            }
            i++;
        }


        // Allow application to complete
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("LineNumberInfo: passed");
        } else {
            throw new Exception("LineNumberInfo: failed");
        }

    }
}
