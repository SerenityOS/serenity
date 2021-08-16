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
 * @bug 4870984
 * @summary  JPDA: Add support for RFE 4856541 - varargs
 * @author jjh
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g VarargsTest.java
 * @run driver VarargsTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class VarargsTarg {

    // These are args that will get passed
    static String[] strArray = new String[] {"a", "b"};
    static int[] intArray = new int[] {1, 2};

    // We will pass these to a varargs instance method
    static VarargsTarg vt1 = new VarargsTarg("vt1", "");
    static VarargsTarg vt2 = new VarargsTarg("vt2", "");

    String iname;

    VarargsTarg(String ... name) {
        iname = "";
        for (int ii = 0; ii < name.length; ii++) {
            iname += name[ii];
        }
    }

    public static void main(String[] args){
        System.out.println("Howdy!");
        /*
         * This isn't really part of the test, it just shows
         * the kinds of calls the debugger test will do and lets
         * you verify how javac handles these calls.
         */
        System.out.println("debuggee: " + varString());
        System.out.println("debuggee: " + varString(null));
        System.out.println("debuggee: " + varString("a"));
        System.out.println("debuggee: " + varString("b", "c"));
        System.out.println("debuggee: " + fixedString(null));
        System.out.println("debuggee: " + vt1.varStringInstance(vt1, vt2));
        System.out.println("debuggge: " + varInt(1, 2, 3));
        System.out.println("debuggee: " + varInteger( new Integer(89)));

        // Should be autoboxed: javac converts the ints to Integers
        // Needs a new method in java.lang.Integer which is only
        // in the generics workspace.
        System.out.println("debugggee: " + varInteger(3, 5, 6));

        System.out.println("Goodbye from VarargsTarg!");
        bkpt();
    }
    static void bkpt() {
    }

    /*
     * Define the methods to be called from the debugger
     */
    static String fixedInt(int p1) {
        return "" + p1;
    }

    static String fixedInteger(Integer p1) {
        return "" + p1;
    }

     static String varInt(int... ss) {
         String retVal = "";
         for (int ii = 0; ii < ss.length; ii++) {
             retVal += ss[ii];
         }
         return retVal;
     }

    static String varInteger(Integer... ss) {
        String retVal = "";
        for (int ii = 0; ii < ss.length; ii++) {
            retVal += ss[ii];
        }
        return retVal;
    }

    static String varString(String... ss) {
        if (ss == null) {
            return "-null-";
        }

        String retVal = "";
        for (int ii = 0; ii < ss.length; ii++) {
            retVal += ss[ii];
        }
        return retVal;
    }

    static String varString2(int p1, String... ss) {
        return p1 + varString(ss);
    }

    static String fixedString(String ss) {
        return "-fixed-";
    }

    String varStringInstance(VarargsTarg... args) {
        if (args == null) {
            return "-null-";
        }
        //System.out.println("debugee: ss length = " + ss.length);
        String retVal = iname + ": ";
        for (int ii = 0; ii < args.length; ii++) {
            retVal += args[ii].iname;
        }
        return retVal;
    }

}

    /********** test program **********/

public class VarargsTest extends TestScaffold {
    ClassType targetClass;
    ThreadReference mainThread;

    VarargsTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new VarargsTest(args).startTests();
    }

    void fail(String reason) {
        failure(reason);
    }

    /*
     * Call a method in the debuggee and verify the return value.
     */
    void doInvoke(Object ct, Method mm, List args, Object expected) {
        StringReference returnValue = null;
        try {
            returnValue = doInvokeNoVerify(ct, mm, args);
        } catch (Exception ee) {
            fail("failure: invokeMethod got exception : " + ee);
            ee.printStackTrace();
            return;
        }
        if (!returnValue.value().equals(expected)) {
            fail("failure: expected \"" + expected + "\", got \"" +
                 returnValue.value() + "\"");
        }
    }

    /*
     * Call a method in the debuggee.
     */
    StringReference doInvokeNoVerify(Object ct, Method mm, List args)
        throws Exception {
        StringReference returnValue = null;
        if (ct instanceof ClassType) {
            returnValue = (StringReference)((ClassType)ct).
                invokeMethod(mainThread, mm, args, 0);
        } else {
            returnValue = (StringReference)((ObjectReference)ct).
                invokeMethod(mainThread, mm, args, 0);
        }
        return returnValue;
    }

    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startToMain("VarargsTarg");
        targetClass = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();

        /*
         * Run past the calls the debuggee makes
         * just to see what they do.
         */
        bpe = resumeTo("VarargsTarg", "bkpt", "()V");

        /*
         * Find Method objects for varString and varString2
         * Both are tested just to show that the code works
         * if there is just one param or if there is more than one.
         */
        ReferenceType rt = findReferenceType("VarargsTarg");

        List mList;

        /*
         * The test consists of calling the varargs static and instance methods
         * (and constructor) passing primitives, Strings, and Objects, and also
         * passing arrays of the above instead of individual args.
         * The same code is used in the underlying JDI implementations
         * for calling instance methods, static methods, and constructors
         * so this test doesn't have to try all possible argument configurations
         * with each type of method.
         */

        mList = rt.methodsByName("varString");
        Method varString = (Method)mList.get(0);

        mList = rt.methodsByName("varString2");
        Method varString2 = (Method)mList.get(0);

        if (!varString.isVarArgs()) {
            fail("failure: varString is not flagged as being var args");
        }
        if (!varString2.isVarArgs()) {
            fail("failure: varString2 is not flagged as being var args");
        }

        /*
         * Setup arg lists for both varString and varString2 that
         * have null in the varargs position.
         */

        {
            // call varString()
            ArrayList nullArg1 = new ArrayList(0);
            doInvoke(targetClass, varString, nullArg1,  "");
        }
        {
            // call varString(null)
            ArrayList nullArg1 = new ArrayList(1);
            nullArg1.add(null);
            doInvoke(targetClass, varString, nullArg1,  "-null-");
        }
        {
            // call varString(9)
            ArrayList nullArg2 = new ArrayList(1);
            nullArg2.add(vm().mirrorOf(9));
            doInvoke(targetClass, varString2, nullArg2,  "9");
        }
        {
            // call varString(9, null)
            ArrayList nullArg2 = new ArrayList(2);
            nullArg2.add(vm().mirrorOf(9));
            nullArg2.add(null);
            doInvoke(targetClass, varString2, nullArg2,  "9-null-");
        }
        {
            ArrayList args1 = new ArrayList(4);
            args1.add(vm().mirrorOf("1"));

            // call varString("1")
            doInvoke(targetClass, varString, args1, "1");

            // call varString("1", "2")
            args1.add(vm().mirrorOf("2"));
            args1.add(vm().mirrorOf("3"));
            args1.add(vm().mirrorOf("4"));
            doInvoke(targetClass, varString, args1, "1234");
        }
        {
            ArrayList args2 = new ArrayList(2);
            args2.add(vm().mirrorOf(9));
            args2.add(vm().mirrorOf("1"));

            // call varString2(9, "1");
            doInvoke(targetClass, varString2, args2, "91");

            // call varString2(9, "1", "2");
            args2.add(vm().mirrorOf("2"));
            doInvoke(targetClass, varString2, args2, "912");
        }

        {
            /*
             * Passing an array of Strings should work too.
             */
            Field ff = targetClass.fieldByName("strArray");
            Value vv1 = targetClass.getValue(ff);

            // call varString(new String[] {"a", "b"})
            ArrayList argsArray = new ArrayList(1);
            argsArray.add(vv1);
            doInvoke(targetClass, varString, argsArray, "ab");

            /*
             * But passing an array of Strings and another String
             * should fail
             */
            argsArray.add(vm().mirrorOf("x"));
            boolean isOk = false;
            try {
                // call varString(new String[] {"a", "b"}, "x")
                doInvokeNoVerify(targetClass, varString, argsArray);
            } catch (Exception ee) {
                /*
                 * Since the number of args passed is > than
                 * the number of params, JDI assumes they are var args
                 * and tries to put the array containing the "a" and
                 * "be" elements into a the first element of an array
                 * of Strings.  This fails because you can't store
                 * an array into a String
                 */
                isOk = true;
                //ee.printStackTrace();
            }
            if (!isOk) {
                fail("failure: an array and a String didn't cause an exception");
            }
        }

        {
            /*
             * Test calling instance method instead of static method,
             * and passing non-String objects
             */
            Field vtField = targetClass.fieldByName("vt1");
            Value vv1 = targetClass.getValue(vtField);

            vtField = targetClass.fieldByName("vt2");
            Value vv2 = targetClass.getValue(vtField);

            /* Create a new instance by calling the varargs
             * ctor.
             * call new VarargsTarg("vt3", "xx");
             */
            Value vv3;
            {
                mList = rt.methodsByName("<init>");
                Method ctor = (Method)mList.get(0);
                if (!ctor.isVarArgs()) {
                    fail("failure: Constructor is not varargs");
                }
                ArrayList argsArray = new ArrayList(2);
                argsArray.add(vm().mirrorOf("vt3"));
                argsArray.add(vm().mirrorOf("xx"));
                vv3 = targetClass.newInstance(mainThread, ctor, argsArray, 0);
            }
            // call vt1.varStringInstance(vv1, vv2, vv3)
            mList = rt.methodsByName("varStringInstance");
            Method varStringInstance = (Method)mList.get(0);

            ArrayList argsArray = new ArrayList(3);
            argsArray.add(vv1);
            argsArray.add(vv2);
            argsArray.add(vv3);
            doInvoke(vv1, varStringInstance, argsArray, "vt1: vt1vt2vt3xx");
        }
        {
            /*
             * tests with primitive types
             */
            List mlist;
            Method mm;
            ArrayList ll = new ArrayList(2);

            // call fixedInt(21)
            mlist = rt.methodsByName("fixedInt");
            mm = (Method)mlist.get(0);
            ll.add(vm().mirrorOf(21));
            doInvoke(targetClass, mm, ll, "21");

            // autoboxing is not implemented in JDI.
            // call fixedInteger(21)
            //mlist = rt.methodsByName("fixedInteger");
            //mm = (Method)mlist.get(0);
            //doInvoke(targetClass, mm, ll, "21");

            mlist = rt.methodsByName("varInt");
            mm = (Method)mlist.get(0);

            // call varInt( new int[] {1, 2});
            Field ff = targetClass.fieldByName("intArray");
            Value vv1 = targetClass.getValue(ff);
            ll.set(0, vv1);
            doInvoke(targetClass, mm, ll, "12");

            // call varInt(21, 22)
            ll.set(0, vm().mirrorOf(21));
            ll.add(vm().mirrorOf(22));
            doInvoke(targetClass, mm, ll, "2122");

            mlist = rt.methodsByName("varInteger");
            mm = (Method)mlist.get(0);

            // call varInteger(1, 2)
            // autoboxing is not implemented.
            //doInvoke(targetClass, mm, ll, "2122");
        }

        /*
         * We don't really need this for the test, but
         * but without it, we sometimes hit 4728096.
         */
        listenUntilVMDisconnect();
        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("VarargsTest: passed");
        } else {
            throw new Exception("VarargsTest: failed");
        }
    }
}
