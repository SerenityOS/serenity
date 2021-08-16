/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4642611
 * @summary Test that method.allLineLocations() should
 *          throw AbsentInformationException exception
 * @author Serguei Spitsyn
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g:none NoLocInfoTest.java
 * @run driver NoLocInfoTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

interface InterfaceNoLocInfoTarg {
    int instanceMeth();
    int instanceMeth1();
}

abstract class AbstractNoLocInfoTarg implements InterfaceNoLocInfoTarg {
    protected int fld;

    // Constructor
    AbstractNoLocInfoTarg() {
        fld = 1000;
    }

    public abstract int instanceMeth();

    public int instanceMeth1() {
        fld = 999;
        fld = instanceMeth();
        return 0;
    }
}

class NoLocInfoTarg extends AbstractNoLocInfoTarg {

    // Class has a default constructor

    public static void main(String[] args){
        System.out.println("A number is: " + new NoLocInfoTarg().instanceMeth());
    }

    public static int staticMeth() {
        int i = 2;
        return i;
    }

    public int instanceMeth() {
        int i = 0;
        i++;
        return i + staticMeth();
    }

    private void voidInstanceMeth() {}
    static native int staticNativeMeth();
    native boolean instanceNativeMeth();
}

    /********** test program **********/

public class NoLocInfoTest extends TestScaffold {
    final String[] args;

    NoLocInfoTest (String args[]) {
        super(args);
        this.args = args;
    }

    public static void main(String[] args)      throws Exception {
        new NoLocInfoTest(args).startTests();
    }

    /********** test assist **********/

    Method getMethod(String className, String methodName) {
        List refs = vm().classesByName(className);
        if (refs.size() != 1) {
            failure("Failure: " + refs.size() +
                    " ReferenceTypes named: " + className);
            return null;
        }
        ReferenceType refType = (ReferenceType)refs.get(0);
        List meths = refType.methodsByName(methodName);
        if (meths.size() != 1) {
            failure("Failure: " + meths.size() +
                    " methods named: " + methodName);
            return null;
        }
        return (Method)meths.get(0);
    }

    void checkLineNumberTable(String className, String methodName) {
        println("GetLineNumberTable for method: " + className + "." + methodName);
        Method method = getMethod(className, methodName);

        try {
            List locations = method.allLineLocations();
            failure("Failure: com.sun.jdi.AbsentInformationException was expected; " +
                    "LineNumberTable.size() = " + locations.size());
        }
        catch (com.sun.jdi.AbsentInformationException ex) {
            println("Success: com.sun.jdi.AbsentInformationException thrown as expected");
        }
        println("");
    }

    void checkEmptyLineNumberTable(String className, String methodName) {
        println("GetLineNumberTable for abstract/native method: " +
                 className + "." + methodName);
        Method method = getMethod(className, methodName);

        try {
            int size = method.allLineLocations().size();
            if (size == 0) {
               println("Succes: LineNumberTable.size() == " + size + " as expected");
            } else {
               failure("Failure: LineNumberTable.size()==" + size + ", but ZERO was expected");
            }
        }
        catch (com.sun.jdi.AbsentInformationException ex) {
            failure("Failure: com.sun.jdi.AbsentInformationException was not expected; ");
        }
        println("");
    }

    /********** test core **********/

    protected void runTests() throws Exception {

        /*
         * Get to the top of main() to get everything loaded
         */
        startToMain("NoLocInfoTarg");

        println("\n Abstract Methods:");
        // For abtsract methods allLineLocations() always returns empty List
        checkEmptyLineNumberTable("InterfaceNoLocInfoTarg", "instanceMeth");
        checkEmptyLineNumberTable("InterfaceNoLocInfoTarg", "instanceMeth1");
        checkEmptyLineNumberTable("AbstractNoLocInfoTarg",  "instanceMeth");

        println("\n Native Methods:");
        // For native methods allLineLocations() always returns empty List
        checkEmptyLineNumberTable("NoLocInfoTarg", "staticNativeMeth");
        checkEmptyLineNumberTable("NoLocInfoTarg", "instanceNativeMeth");

        println("\n Non-Abstract Methods of Abstract class:");
        checkLineNumberTable("AbstractNoLocInfoTarg", "<init>");
        checkLineNumberTable("AbstractNoLocInfoTarg", "instanceMeth1");

        println("\n Methods of Non-Abstract class:");
        checkLineNumberTable("NoLocInfoTarg", "<init>"); // default constructor
        checkLineNumberTable("NoLocInfoTarg", "main");
        checkLineNumberTable("NoLocInfoTarg", "instanceMeth");
        checkLineNumberTable("NoLocInfoTarg", "instanceMeth1"); // inherited
        checkLineNumberTable("NoLocInfoTarg", "voidInstanceMeth");

        /*
         * resume until the end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("NoLocInfoTest: passed");
        } else {
            throw new Exception("NoLocInfoTest: failed");
        }
    }
}
