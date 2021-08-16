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
 * @bug 4916263
 * @summary Test
 * @author Serguei Spitsyn
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g LocalVariableEqual.java
 * @run driver LocalVariableEqual
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;

    /********** target program **********/

class LocalVariableEqualTarg {
    public static void main(String[] args){
        int intVar = 10;
        System.out.println("LocalVariableEqualTarg: Started");
        intVar = staticMeth(intVar);
        System.out.println("LocalVariableEqualTarg: Finished");
    }

    public static int staticMeth(int intArg) {
        System.out.println("staticMeth: Started");
        int result;
        {
             { boolean bool_1 = false;
               intArg++;
               {
                 { byte byte_2 = 2;
                   intArg++;
                 }
                 byte byte_1 = 1;
                 intArg++;
               }
             }
             boolean bool_2 = true;
             intArg++;
        }
        {
             {
               {
                 { char   char_1 = '1';
                   intArg++;
                 }
                 short  short_1 = 1;
                 intArg++;
               }
             }
             { short  short_2 = 2;
               intArg++;
             }
             char   char_2 = '2';
             intArg++;
        }
        {
             { int int_1 = 1;
               intArg++;
             }
             long long_1 = 1;
             intArg++;
        }
        {
             { float  float_1 = 1;
               intArg++;
             }
             double double_2 = 2;
             intArg++;
        }
        {
             { String string_1 = "1";
               intArg++;
             }
             Object obj_2 = new Object();
             intArg++;
        }
        result = 10;
        System.out.println("staticMeth: Finished");
        return result;
    }
}


    /********** test program **********/

public class LocalVariableEqual extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;

    LocalVariableEqual (String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        new LocalVariableEqual(args).startTests();
    }

    /********** test assist **********/

    Method getMethod(String className, String methodName) {
        List refs = vm().classesByName(className);
        if (refs.size() != 1) {
            failure("Test failure: " + refs.size() +
                    " ReferenceTypes named: " + className);
            return null;
        }
        ReferenceType refType = (ReferenceType)refs.get(0);
        List meths = refType.methodsByName(methodName);
        if (meths.size() != 1) {
            failure("Test failure: " + meths.size() +
                    " methods named: " + methodName);
            return null;
        }
        return (Method)meths.get(0);
    }

    void printVariable(LocalVariable lv, int index) throws Exception {
        if (lv == null) {
            println(" Var  name: null");
            return;
        }
        String tyname = lv.typeName();
        println(" Var: " + lv.name() + ", index: " + index + ", type: " + tyname +
                ", Signature: " + lv.type().signature());
        // Sorry, there is no way to take local variable slot numbers using JDI!
        // It is because method LocalVariableImpl.slot() is private.
    }

    void compareTwoEqualVars(LocalVariable lv1, LocalVariable lv2) {
        if (lv1.equals(lv2)) {
            println(" Success: equality of local vars detected");
        } else {
            failure(" Failure: equality of local vars is NOT detected");
        }
        if (lv1.hashCode() == lv2.hashCode()) {
            println(" Success: hashCode's of equal local vars are equal");
        } else {
            failure(" Failure: hashCode's of equal local vars differ");
        }
        if (lv1.compareTo(lv2) == 0) {
            println(" Success: compareTo() is correct for equal local vars");
        } else {
            failure(" Failure: compareTo() is NOT correct for equal local vars");
        }
    }

    void compareTwoDifferentVars(LocalVariable lv1, LocalVariable lv2) {
        if (!lv1.equals(lv2)) {
            println(" Success: difference of local vars detected");
        } else {
            failure(" Failure: difference of local vars is NOT detected");
        }
        if (lv1.hashCode() != lv2.hashCode()) {
            println(" Success: hashCode's of different local vars differ");
        } else {
            failure(" Failure: hashCode's of different local vars are equal");
        }
        if (lv1.compareTo(lv2) != 0) {
            println(" Success: compareTo() is correct for different local vars");
        } else {
            failure(" Failure: compareTo() is NOT correct for different local vars");
        }
    }

    void compareAllVariables(String className, String methodName) throws Exception {
        println("compareAllVariables for method: " + className + "." + methodName);
        Method method = getMethod(className, methodName);
        List localVars;
        try {
            localVars = method.variables();
            println("\n Success: got a list of all method variables: " + methodName);
        }
        catch (com.sun.jdi.AbsentInformationException ex) {
            failure("\n AbsentInformationException has been thrown");
            return;
        }

        // We consider N*N combinations for set of N variables
        int index1 = 0;
        for (Iterator it1 = localVars.iterator(); it1.hasNext(); index1++) {
            LocalVariable lv1 = (LocalVariable) it1.next();

            int index2 = 0;
            for (Iterator it2 = localVars.iterator(); it2.hasNext(); index2++) {
                LocalVariable lv2 = (LocalVariable) it2.next();

                println("\n Two variables:");
                printVariable(lv1, index1);
                printVariable(lv2, index2);
                println("");
                if (index1 == index2) {
                    compareTwoEqualVars(lv1, lv2);
                } else {
                    compareTwoDifferentVars(lv1, lv2);
                }
            }
        }
        println("");
        return;
    }

    /********** test core **********/

    protected void runTests() throws Exception {

        /*
         * Get to the top of main() to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("LocalVariableEqualTarg");
        println("startToMain(LocalVariableEqualTarg)");

        compareAllVariables("LocalVariableEqualTarg", "staticMeth");

        /*
         * resume until the end
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("\nLocalVariableEqual: passed");
        } else {
            throw new Exception("\nLocalVariableEqual: FAILED");
        }
    }
}
