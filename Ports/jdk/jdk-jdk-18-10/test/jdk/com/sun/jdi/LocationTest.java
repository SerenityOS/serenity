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
 * @bug 4419453
 * @summary Test that Method.location() returns the right values
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g LocationTest.java
 * @run driver LocationTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

abstract class AbstractLocationTarg {
    abstract void foo();
}

class LocationTarg extends AbstractLocationTarg {
    public static void main(String[] args){
        System.out.println("Howdy!");  // don't change the following 3 lines
    }
    void foo() {
        System.out.println("Never here!");  // must be 3 lines after "Howdy!"
    }
}

    /********** test program **********/

public class LocationTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    LocationTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new LocationTest(args).startTests();
    }

    /********** test assist **********/

    Location getLocation(String refName, String methodName) {
        List refs = vm().classesByName(refName);
        if (refs.size() != 1) {
            failure("Test failure: " + refs.size() +
                    " ReferenceTypes named: " + refName);
            return null;
        }
        ReferenceType refType = (ReferenceType)refs.get(0);
        List meths = refType.methodsByName(methodName);
        if (meths.size() != 1) {
            failure("Test failure: " + meths.size() +
                    " methods named: " + methodName);
            return null;
        }
        Method meth = (Method)meths.get(0);
        return meth.location();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        Location loc;

        /*
         * Get to the top of main() to get everything loaded
         */
        startToMain("LocationTarg");

        /*
         * Test the values of location()
         */
        loc = getLocation("AbstractLocationTarg", "foo");
        if (loc != null) {
            failure("location of AbstractLocationTarg.foo() should have " +
                    "been null, but was: " + loc);
        }

        loc = getLocation("java.util.List", "clear");
        if (loc != null) {
            failure("location of java.util.List.clear() " +
                    "should have been null, but was: " + loc);
        }

        loc = getLocation("java.lang.Object", "getClass");
        if (loc == null) {
            failure("location of Object.getClass() " +
                    "should have been non-null, but was: " + loc);
        } else {
            if (!loc.declaringType().name().equals("java.lang.Object")) {
                failure("location.declaringType() of Object.getClass() " +
                        "should have been java.lang.Object, but was: " +
                        loc.declaringType());
            }
            if (!loc.method().name().equals("getClass")) {
                failure("location.method() of Object.getClass() " +
                        "should have been getClass, but was: " +
                        loc.method());
            }
            if (loc.codeIndex() != -1) {
                failure("location.codeIndex() of Object.getClass() " +
                        "should have been -1, but was: " +
                        loc.codeIndex());
            }
            if (loc.lineNumber() != -1) {
                failure("location.lineNumber() of Object.getClass() " +
                        "should have been -1, but was: " +
                        loc.lineNumber());
            }
        }
        Location mainLoc = getLocation("LocationTarg", "main");
        loc = getLocation("LocationTarg", "foo");
        if (loc == null) {
            failure("location of LocationTarg.foo() " +
                    "should have been non-null, but was: " + loc);
        } else {
            if (!loc.declaringType().name().equals("LocationTarg")) {
                failure("location.declaringType() of LocationTarg.foo() " +
                        "should have been LocationTarg, but was: " +
                        loc.declaringType());
            }
            if (!loc.method().name().equals("foo")) {
                failure("location.method() of LocationTarg.foo() " +
                        "should have been foo, but was: " +
                        loc.method());
            }
            if (loc.codeIndex() != 0) {  // implementation dependent!!!
                failure("location.codeIndex() of LocationTarg.foo() " +
                        "should have been 0, but was: " +
                        loc.codeIndex());
            }
            if (loc.lineNumber() != (mainLoc.lineNumber() + 3)) {
                failure("location.lineNumber() of LocationTarg.foo() " +
                        "should have been " + (mainLoc.lineNumber() + 3) +
                        ", but was: " + loc.lineNumber());
            }
        }


        /*
         * resume until the end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("LocationTest: passed");
        } else {
            throw new Exception("LocationTest: failed");
        }
    }
}
