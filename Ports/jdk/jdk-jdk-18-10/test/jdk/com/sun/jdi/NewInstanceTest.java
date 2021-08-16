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
 * @bug 4419450
 * @summary Test newInstance() for arrays - currently covers
 * only reference type arrays (see bug #4450091).
 * @author Robert Field
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g NewInstanceTest.java
 * @run driver NewInstanceTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

interface IfcFoo {
}
class ClsFoo implements IfcFoo {
}

class NewInstanceTarg {
    static ClsFoo aFoo = new ClsFoo();

    static Object[] objArray = new Object[10];
    static ClsFoo[] clsArray = new ClsFoo[10];
    static IfcFoo[] ifc0Array = new IfcFoo[10];
    static IfcFoo[] ifcArray = {aFoo, aFoo};

    public static void main(String[] args){
        System.out.println("Howdy!");
        System.out.println("Goodbye from NewInstanceTarg!");
    }
}

    /********** test program **********/

public class NewInstanceTest extends TestScaffold {
    ReferenceType targetClass;

    NewInstanceTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new NewInstanceTest(args).startTests();
    }

    /********** test assist **********/

    void makeArray(String fieldName) throws Exception {
        println("Making array for field: " + fieldName);
        Field arrayField = targetClass.fieldByName(fieldName);
        ArrayType arrayType = (ArrayType)arrayField.type();
        println("Type for " + fieldName + " is " + arrayType);
        ArrayReference arrayReference = arrayType.newInstance(20);
        println("Passed subtest: " + fieldName);
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass
         */
        BreakpointEvent bpe = startToMain("NewInstanceTarg");
        targetClass = bpe.location().declaringType();

        makeArray("objArray");
        makeArray("clsArray");
        makeArray("ifc0Array");
        makeArray("ifcArray");

        /*
         * resume the target until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("NewInstanceTest: passed");
        } else {
            throw new Exception("NewInstanceTest: failed");
        }
    }
}
