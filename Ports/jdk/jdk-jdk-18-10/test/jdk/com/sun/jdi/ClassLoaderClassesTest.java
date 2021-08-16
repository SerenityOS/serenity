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
 *  @test
 *  @bug 4450091
 *  @summary Test ClassLoaderReference.visibleClasses() which is
 *  a direct pass-through of the JVMDI function GetClassLoaderClasses
 *  for inclusion of primitive arrays.
 *  @author Robert Field
 *
 *  @run build TestScaffold VMConnection TargetListener TargetAdapter
 *  @run compile -g ClassLoaderClassesTest.java
 *  @run driver ClassLoaderClassesTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class ClassLoaderClassesTarg {
    static int[] intArray = new int[10];

    static {
        // make sure our class loader "creates" int[] before tested
        intArray[1] = 99;
    }

    public static void main(String[] args){
        System.out.println("Goodbye from ClassLoaderClassesTarg!");
    }
}

    /********** test program **********/

public class ClassLoaderClassesTest extends TestScaffold {
    ReferenceType targetClass;

    ClassLoaderClassesTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new ClassLoaderClassesTest(args).startTests();
    }

    /********** test assist **********/

    boolean findClass(String className) throws Exception {
        ClassLoaderReference cl = targetClass.classLoader();
        Iterator vci = cl.visibleClasses().iterator();
        while (vci.hasNext()) {
            ReferenceType rt = (ReferenceType)vci.next();
            println(rt.name() + " - " + rt.classLoader());
            if (rt.name().equals(className)) {
                return true;
            }
        }
        return false;
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main() to determine targetClass
         */
        BreakpointEvent bpe = startToMain("ClassLoaderClassesTarg");
        targetClass = bpe.location().declaringType();

        if (findClass("int[]")) {
            println("int[] found");
        } else {
            failure("failed - int[] not found");
        }

        // use it indirectly - throws ClassNotLoadedException on error
        Field arrayField = targetClass.fieldByName("intArray");
        ArrayType arrayType = (ArrayType)arrayField.type();
        println("Type for intArray is " + arrayType);

        /*
         * resume the target until end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("ClassLoaderClassesTest: passed");
        } else {
            throw new Exception("ClassLoaderClassesTest: failed");
        }
    }
}
