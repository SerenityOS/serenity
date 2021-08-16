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
 * @bug 4451941 4527072
 * @summary Test argument types for invoke
 * @author Robert Field
 *
 * @library ..
 *
 * @run build  TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g InvokeTest.java
 * @run driver InvokeTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;

    /********** target program **********/

class InvokeTarg {
    static InvokeTarg myself = null;

    boolean[] aBooleanArray = new boolean[] {true, true};
    byte[] aByteArray = new byte[] {4, 2};
    char[] aCharArray = new char[] {'k', 'p'};
    short[] aShortArray = new short[] {55,12, 12};
    int[] aIntArray = new int[] {6, 3, 1};
    long[] aLongArray = new long[] {3423423};
    float[] aFloatArray = new float[] {(float)2.1};
    double[] aDoubleArray = new double[] {3.141595358979};

    boolean[][] aBoolean2DArray = new boolean[][]
                                   {{true, false}, {false, true}};
    byte[][] aByte2DArray = new byte[][] {{22,66}, {8,9}};
    char[][] aChar2DArray = new char[][] {{22,66}, {8,9}};
    short[][] aShort2DArray = new short[][] {{22,66}, {8,9}};
    int[][] aInt2DArray = new int[][] {{22,66}, {8,9}};
    long[][] aLong2DArray = new long[][] {{22,66}, {8,9}};
    float[][] aFloat2DArray = new float[][] {{22,66}, {8,9}};
    double[][] aDouble2DArray = new double[][] {{22,66}, {8,9}};

    String[] aStringArray = new String[] {"testing"};
    String[][] aString2DArray = new String[][]
                                     {{"hi", "there"}, {"oh"}};
    Date aDate = new Date();
    Date[] aDateArray = new Date[] {};
    Date[][] aDate2DArray = new Date[][] {{}};

    String aString = "jjxx";
    long longCheck = 0;
    boolean booleanCheck = false;
    boolean voidCheck = false;
    Object objectCheck = null;

    public static void main(String[] args){
        System.out.println("Howdy!");
        (new InvokeTarg()).sayHi();
    }

    void sayHi() {
    }

    void checkIn() {
    }

    boolean invokeVoid() {
        voidCheck = true;
        checkIn();
        return true;
    }

    boolean invokeBoolean(boolean val) {
        booleanCheck = val;
        checkIn();
        return val;
    }

    byte invokeByte(byte val) {
        longCheck = val;
        checkIn();
        return val;
    }

    char invokeChar(char val) {
        longCheck = val;
        checkIn();
        return val;
    }

    short invokeShort(short val) {
        longCheck = val;
        checkIn();
        return val;
    }

    int invokeInt(int val) {
        longCheck = val;
        checkIn();
        return val;
    }

    long invokeLong(long val) {
        longCheck = val;
        checkIn();
        return val;
    }

    float invokeFloat(float val) {
        longCheck = (long)val;
        checkIn();
        return val;
    }

    double invokeDouble(double val) {
        longCheck = (long)val;
        checkIn();
        return val;
    }

    boolean[] invokeBooleanArray(boolean[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    byte[] invokeByteArray(byte[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    char[] invokeCharArray(char[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    short[] invokeShortArray(short[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    int[] invokeIntArray(int[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    long[] invokeLongArray(long[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    float[] invokeFloatArray(float[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    double[] invokeDoubleArray(double[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    boolean[][] invokeBoolean2DArray(boolean[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    byte[][] invokeByte2DArray(byte[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    char[][] invokeChar2DArray(char[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    short[][] invokeShort2DArray(short[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    int[][] invokeInt2DArray(int[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    long[][] invokeLong2DArray(long[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    float[][] invokeFloat2DArray(float[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    double[][] invokeDouble2DArray(double[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    String invokeString(String val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    String[] invokeStringArray(String[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    String[][] invokeString2DArray(String[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    Date invokeDate(Date val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    Date[] invokeDateArray(Date[] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    Date[][] invokeDate2DArray(Date[][] val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    String invokeCombo(int[][] arr, String val) {
        objectCheck = val;
        checkIn();
        return val;
    }

    int[][] invokeCombo2(int[][] val, String str) {
        objectCheck = val;
        checkIn();
        return val;
    }
}

    /********** test program **********/

public class InvokeTest extends TestScaffold {
    ReferenceType targetClass;
    ThreadReference mainThread;
    ObjectReference thisObject;
    Field longCheckField;
    Field booleanCheckField;
    Field voidCheckField;
    Field objectCheckField;
    Value longValue;
    Value booleanValue;
    Value objectValue;
    Value voidValue;

    InvokeTest (String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        new InvokeTest(args).startTests();
    }

    /********** event handlers **********/

    // not use now
    public void breakpointReached(BreakpointEvent event) {
        println("Got BreakpointEvent");
        longValue = thisObject.getValue(longCheckField);
        booleanValue = thisObject.getValue(booleanCheckField);
        objectValue = thisObject.getValue(objectCheckField);
        voidValue = thisObject.getValue(voidCheckField);
    }

    /********** test assist **********/

    void invoke(Method method, List args, Value value) {
        Value returnValue = null;

        try {
            returnValue = thisObject.invokeMethod(mainThread,
                                                    method, args, 0);
        } catch ( Exception ee) {
            println("Got Exception: " + ee);
            ee.printStackTrace();
        }
        println("        return val = " + returnValue);
        // It has to be the same value as what we passed in!
        if (returnValue.equals(value)) {
            println("         " + method.name() + " return value matches: "
                    + value);
        } else {
            if (value != null) {
                failure("FAIL: " + method.name() + " returned: " + returnValue +
                        " expected: " + value );
            } else {
                println("         " + method.name() + " return value : " + returnValue);
            }

        }
        Value checkValue = (value instanceof PrimitiveValue)?
                              ((value instanceof BooleanValue)?
                                        booleanValue : longValue) :
                              objectValue;
    }


    void invoke(String methodName, String methodSig,
                List args, Value value)
        throws Exception {
        Method method = findMethod(targetClass, methodName, methodSig);
        if ( method == null) {
            failure("FAILED: Can't find method: " + methodName  + " for class = " + targetClass);
            return;
        }
        invoke(method, args, value);
    }

    void invoke(String methodName, String methodSig, Value value)
                                           throws Exception {
        List args = new ArrayList(1);
        args.add(value);
        invoke(methodName, methodSig, args, value);
    }


    void invoke(String methodName, String methodSig, String fieldName)
                                           throws Exception {
        invoke(methodName, methodSig, fieldValue(fieldName));
    }

    private Method toStringMethod;
    Method gettoStringMethod() {
        if ( toStringMethod != null) {
            return toStringMethod;
        }

        // We have to find it.  First find java.lang.Object
        List myClasses = vm().allClasses();
        Iterator iter = myClasses.iterator();
        ReferenceType objectMirror = null;
        while (iter.hasNext()) {
            ReferenceType xx = (ReferenceType)iter.next();
            if (xx.name().equals("java.lang.Object")) {
                objectMirror = xx;
                break;
            }
        }

        if (objectMirror == null) {
            return null;
        }

        // Then find toSting
        List meths = objectMirror.methods();
        iter = meths.iterator();
        while (iter.hasNext()) {
            toStringMethod = (Method)iter.next();
            if (toStringMethod.name().equals("toString")) {
                return toStringMethod;
           }
       }
       toStringMethod = null;
       return null;
    }

    // This calls toString on a field
    protected void callToString(String fieldName) throws Exception {
        // Sorry for this kludgy use of global vars.
        ObjectReference saveObject = thisObject;
        Method toStringMethod = gettoStringMethod();

        Field theField = targetClass.fieldByName(fieldName);
        thisObject = (ObjectReference)thisObject.getValue( theField);
        invoke(toStringMethod, new ArrayList(0), null);
        thisObject = saveObject;
    }

    Value fieldValue(String fieldName) {
        Field field = targetClass.fieldByName(fieldName);
        return thisObject.getValue(field);
    }


    /********** test core **********/

    protected void runTests() throws Exception {
        /*
         * Get to the top of sayHi()
         * to determine targetClass and mainThread
         */
        BreakpointEvent bpe = startTo("InvokeTarg", "sayHi", "()V");
        targetClass = bpe.location().declaringType();

        mainThread = bpe.thread();

        StackFrame frame = mainThread.frame(0);
        thisObject = frame.thisObject();
        longCheckField = targetClass.fieldByName("longCheck");
        booleanCheckField = targetClass.fieldByName("booleanCheck");
        objectCheckField = targetClass.fieldByName("objectCheck");
        voidCheckField = targetClass.fieldByName("voidCheck");
        callToString("aBooleanArray");

        invoke("invokeVoid",    "()Z",  new ArrayList(0), vm().mirrorOf(true));

        invoke("invokeBoolean", "(Z)Z", vm().mirrorOf(true));
        invoke("invokeByte",    "(B)B", vm().mirrorOf((byte)14));
        invoke("invokeChar",    "(C)C", vm().mirrorOf('h'));
        invoke("invokeShort",   "(S)S", vm().mirrorOf((short)54));
        invoke("invokeInt",     "(I)I", vm().mirrorOf((int)414));
        invoke("invokeLong",    "(J)J", vm().mirrorOf((long)140000));
        invoke("invokeFloat",   "(F)F", vm().mirrorOf((float)315));
        invoke("invokeDouble",  "(D)D", vm().mirrorOf((double)181818));

        invoke("invokeBooleanArray",    "([Z)[Z", "aBooleanArray");
        invoke("invokeByteArray",    "([B)[B", "aByteArray");
        invoke("invokeCharArray",    "([C)[C", "aCharArray");
        invoke("invokeShortArray",   "([S)[S", "aShortArray");
        invoke("invokeIntArray",     "([I)[I", "aIntArray");
        invoke("invokeLongArray",    "([J)[J", "aLongArray");
        invoke("invokeFloatArray",   "([F)[F", "aFloatArray");
        invoke("invokeDoubleArray",  "([D)[D", "aDoubleArray");

        invoke("invokeBoolean2DArray",    "([[Z)[[Z", "aBoolean2DArray");
        invoke("invokeByte2DArray",    "([[B)[[B", "aByte2DArray");
        invoke("invokeChar2DArray",    "([[C)[[C", "aChar2DArray");
        invoke("invokeShort2DArray",   "([[S)[[S", "aShort2DArray");
        invoke("invokeInt2DArray",     "([[I)[[I", "aInt2DArray");
        invoke("invokeLong2DArray",    "([[J)[[J", "aLong2DArray");
        invoke("invokeFloat2DArray",   "([[F)[[F", "aFloat2DArray");
        invoke("invokeDouble2DArray",  "([[D)[[D", "aDouble2DArray");

        invoke("invokeString",    "(Ljava/lang/String;)Ljava/lang/String;",
                                  vm().mirrorOf("Howdy"));
        invoke("invokeStringArray",    "([Ljava/lang/String;)[Ljava/lang/String;",
                                  "aStringArray");
        invoke("invokeString2DArray",    "([[Ljava/lang/String;)[[Ljava/lang/String;",
                                  "aString2DArray");

        invoke("invokeDate",    "(Ljava/util/Date;)Ljava/util/Date;",
                                  "aDate");
        invoke("invokeDateArray",  "([Ljava/util/Date;)[Ljava/util/Date;",
                                  "aDateArray");
        invoke("invokeDate2DArray", "([[Ljava/util/Date;)[[Ljava/util/Date;",
                                  "aDate2DArray");

        Value i2 = fieldValue("aInt2DArray");
        Value str = vm().mirrorOf("Later");
        List args = new ArrayList(2);
        args.add(i2);
        args.add(str);
        invoke("invokeCombo",
               "([[ILjava/lang/String;)Ljava/lang/String;",
               args, str);
        invoke("invokeCombo2",
               "([[ILjava/lang/String;)[[I",
               args, i2);
        /*
         * resume the target listening for events
         */
        listenUntilVMDisconnect();

        /*
         * deal with results of test
         * if anything has called failure("foo") testFailed will be true
         */
        if (!testFailed) {
            println("InvokeTest: passed");
        } else {
            throw new Exception("InvokeTest: failed");
        }
    }
}
