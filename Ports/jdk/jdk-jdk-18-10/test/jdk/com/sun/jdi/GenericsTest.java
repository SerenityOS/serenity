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
 * @bug 4421040
 * @summary  JPDA: Add support for JSR-014 Generics
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g GenericsTest.java
 * @run driver GenericsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class GenericsTarg {
    static Gen1<String> genField = new Gen1<String>();;
    static Sub1 sub1Field = new Sub1();

    String[] strArray = null;
    int intField = 0;
    Object objField;
    public static void main(String[] args){
        //genField.print();
        System.out.println("Goodbye from GenericsTarg!");
    }
}
class Gen1<tt> {
    tt field1;
    Gen1() {
        System.out.println("Gen1<tt> ctor called");
    }
    tt method1(tt p1) {
        Gen1<String> xxx = null;
        System.out.println("method1: param is " + p1);
        return p1;
    }
    String method2() {
        String str = "This local variable is not generic";
        return str;
    }
}

class Sub1 extends Gen1<String> {
    String method1(String p1) {
        System.out.println("method1 has been overridden: param is " + p1);
        return "hi";
    }
}

    /********** test program **********/

public class GenericsTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    static boolean useOld;

    GenericsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args) throws Exception {
        /*
         * The 1.5 FE must be able to talk to a 1.4 BE, ie, JDWP version <= 1.4.
         * This is hard to test since this test file must be compiled with
         * -source 1.5 which will cause its class file to be version 49 which
         * won't run on a pre 1.5 JDK.   We can simulate this though
         * by passing
         *      -xjdk <pathname>
         * to this test which causes the debuggee to be run on that JDK.
         * This should be a version of 1.5 that accepts classfile version 49,
         * but which still contains the 1.4 version of JDWP.
         * This trick verifies that the calls to genericSignature() methods
         * in the test do not cause the generic JDWP commands to be issued.
         * The value to use for this is currently:
         * /java/re/jdk/1.5/promoted/all/b17/binaries/solaris-sparc
         */
        if (args.length > 1 && args[0].equals("-xjdk")) {
            System.setProperty("java.home", args[1]);
            useOld = true;

            // Delete this arg
            String[] args1 = new String[args.length - 2];
            for (int ii = 0; ii < args.length -2; ii++) {
                args1[ii] = args[ii + 2];
            }
            args = args1;
        }

        new GenericsTest(args).startTests();
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("GenericsTarg");
        targetClass = bpe.location().declaringType();
        {
            /*
             * Prove that arrays aren't broken and that
             * null is returned if there is no generic signature
             */
            Field strArray = targetClass.fieldByName("strArray");
            ReferenceType fieldType = (ReferenceType)(strArray.type());
            String genSig = fieldType.genericSignature();
            System.out.println("strArray name = " + strArray);
            System.out.println("         type = " + fieldType);
            System.out.println("          sig = " + fieldType.signature());
            System.out.println("       genSig = " + genSig);
            if (!useOld && genSig != null) {
                failure("FAILED: Expected generic signature = null for "
                        + fieldType.name() + ", received: " + genSig);
            }
        }
        {
            // prove that primitives aren't broken.
            Field intField = targetClass.fieldByName("intField");
            Type fieldType = (Type)(intField.type());
            System.out.println("intField name = " + intField);
            System.out.println("         type = " + fieldType);
            System.out.println("          sig = " + fieldType.signature());
        }

        Field genField = targetClass.fieldByName("genField");
        ReferenceType gen1Class = (ReferenceType)(genField.type());
        String genSig;
        String expected;
        {
            // Verify genericSignature for a class
            expected = "<tt:Ljava/lang/Object;>Ljava/lang/Object;";
            genSig = gen1Class.genericSignature();
            System.out.println("genField name = " + genField);
            System.out.println("         type = " + gen1Class);
            System.out.println("          sig = " + gen1Class.signature());
            System.out.println("       genSig = " + genSig);
            if (!useOld && !expected.equals(genSig)) {
                failure("FAILED: Expected generic signature for gen1: " +
                        expected + ", received: " + genSig);
            }
        }
        {
            // Verify genericSignature() for a field
            List genFields = gen1Class.fields();
            Field field1 = (Field)genFields.get(0);
            // there is only one field
            expected = "Ttt;";
            genSig = field1.genericSignature();
            System.out.println("field1 name = " + field1);
            System.out.println("       type = " + gen1Class.signature());
            System.out.println("        sig = " + field1.signature());
            System.out.println("    gen sig = " + genSig);
            if (!useOld && !expected.equals(genSig)) {
                failure("FAILED: Expected generic signature for field1: " +
                        expected + ", received: " + genSig);
            }
        }
        {
            // Verify genericSignature() for a method
            List genMethods = gen1Class.methodsByName("method1");
            // There is only uno
            Method method1 = (Method)genMethods.get(0);
            expected = "(Ttt;)Ttt;";
            genSig = method1.genericSignature();
            System.out.println("method1 name = " + method1);
            System.out.println("        type = " + gen1Class.signature());
            System.out.println("         sig = " + method1.signature());
            System.out.println("     gen sig = " + genSig);
            System.out.println("     bridge  = " + method1.isBridge());
            if (!useOld && !expected.equals(genSig)) {
                failure("FAILED: Expected generic signature for method1: " +
                        expected + ", received: " + genSig);
            }

            // Verify this is not a bridge method
            if (method1.isBridge()) {
                failure("FAILED: Expected gen1.method1 to not be a bridge"
                         + " method but it is");
            }

            // Verify genericSignature for a local var
            List localVars = method1.variables();
            String[] expectedGenSigs = { "Ttt", "Gen1<String>" };
            for ( int ii = 0 ; ii < localVars.size(); ii++) {
                expected = expectedGenSigs[ii];
                LocalVariable pp = (LocalVariable)localVars.get(ii);
                genSig = pp.genericSignature();
                System.out.println("   local var " + ii + " = " + pp.name());
                System.out.println("      sig      = " + pp.signature());
                System.out.println("      gen sig  = " + genSig);
                //jjh Uncomment when generics for local vars are available from
                //jjh javac and hotspot.  See:
                //jjh   4914602 LVT entries for classfile version > 49 must be converted
                //jjh if (!useOld && !expected.equals(genSig)) {
                //jjh    failure("FAILED: Expected generic signature for local var: " +
                //jjh            expected + ", received: " + genSig);
                //jjh }
            }
        }
        {
            // Verify genericSignature() for a method2
            List genMethods = gen1Class.methodsByName("method2");
            // There is only uno
            Method method2 = (Method)genMethods.get(0);
            expected = "null";
            genSig = method2.genericSignature();
            genSig = (genSig == null) ? "null" : genSig;
            System.out.println("method2 name = " + method2);
            System.out.println("        type = " + gen1Class.signature());
            System.out.println("         sig = " + method2.signature());
            System.out.println("     gen sig = " + genSig);
            System.out.println("     bridge  = " + method2.isBridge());
            if (!useOld && !expected.equals(genSig)) {
                failure("FAILED: Expected generic signature for method2: " +
                        expected + ", received: " + genSig);
            }

            // Verify this is not a bridge method
            if (method2.isBridge()) {
                failure("FAILED: Expected gen1.method2 to not be a bridge"
                         + " method but it is");
            }

            // Verify genericSignature for a local var
            List localVars = method2.variables();
            expected = "null";
            for ( int ii = 0 ; ii < localVars.size(); ii++) {
                LocalVariable pp = (LocalVariable)localVars.get(ii);
                genSig = pp.genericSignature();
                genSig = (genSig == null) ? "null" : genSig;

                System.out.println("   local var " + ii + " = " + pp.name());
                System.out.println("      sig      = " + pp.signature());
                System.out.println("      gen sig  = " + genSig);
                if (!useOld && !expected.equals(genSig)) {
                   failure("FAILED: Expected generic signature for local var: " +
                           expected + ", received: " + genSig);
                }
            }
        }
        {
            Field sub1Field = targetClass.fieldByName("sub1Field");
            ReferenceType sub1Class = (ReferenceType)(sub1Field.type());
            List<Method> sub1Methods = sub1Class.methodsByName("method1");
            for (Method mm: sub1Methods) {
                System.out.println("method is: " + mm);
            }
            /*
             * There should be two methods - the first is the
             * method1 defined in Sub1, and the 2nd is a javac generated
             * bridge method.
             */
            Method method1 = (Method)sub1Methods.get(1);
            System.out.println("\nmethod1 name = " + method1);
            System.out.println("         sig = " + method1.signature());
            System.out.println("      bridge = " + method1.isBridge());
            if (!useOld && !method1.isBridge()) {
                failure("FAILED: Expected Sub1.method1 to be a bridge method"
                         + " but it isn't");
            }

        }
        {
            // Verify genericSignature for a non generic class
            genSig = targetClass.genericSignature();
            if (genSig != null) {
                failure("FAILED: Expected generic signature = null for "
                        + targetClass.name() + ", received: " + genSig);
            }
        }
        {
            // Verify genericSignature for a non generic field
            Field objField = targetClass.fieldByName("objField");
            genSig = objField.genericSignature();
            if (genSig != null) {
                failure("FAILED: Expected generic signature = null for "
                        + objField.name() + ", received: " + genSig);
            }
        }
        {
            // Verify genericSignature for a non generic method
            List methods = targetClass.methodsByName("main");
            Method main = (Method)methods.get(0);
            genSig = main.genericSignature();
            if (genSig != null) {
                failure("FAILED: Expected generic signature = null for "
                        + main.name() + ", received: " + genSig);
            }
        }
        if (0 == 1) {
            mainThread = bpe.thread();
            EventRequestManager erm = vm().eventRequestManager();
            StepRequest request = erm.createStepRequest(mainThread,
                                                    StepRequest.STEP_LINE,
                                                    StepRequest.STEP_INTO);
            request.enable();
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
            println("GenericsTest: passed");
        } else {
            throw new Exception("GenericsTest: failed");
        }
    }
}
