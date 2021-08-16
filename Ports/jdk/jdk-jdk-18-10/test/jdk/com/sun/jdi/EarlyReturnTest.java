/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 6175634
 * @summary Allow early return from methods
 *
 * @bug 6431720
 * @summary Unexpected InvalidTypeException when call ThreadReference.forceEarlyReturn with VoidValue
 *
 * @bug 6432855
 * @summary Need a way to create JDI VoidValue for use in ThreadReference.forceEarlyReturn
 *
 * @author Tim Bell (based on MethodExitReturnValuesTest by Jim Holmlund)
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g EarlyReturnTest.java
 * @run driver EarlyReturnTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Array;

/*
 * This test has a debuggee which calls a static method
 * for each kind of JDI Value, and then an instance method
 * for each.
 *
 * The debugger sets breakpoints in all methods.  When a breakpoint
 * is hit the debugger requests an early return and supplies a new
 * return value.  It then checks that the correct return values are
 * included in the MethodExitEvents.
 *
 * Each value is stored in a static var in the debuggee.  The debugger
 * gets the values from these static vars to check for correct
 * return values in the MethodExitEvents.
 */

class EarlyReturnTarg {
    static boolean debuggerWatching = false;
    static int failureCount = 0;
    /*
     * These are the values that will be used by methods
     * returning normally.
     */
    static URL[] urls = new URL[1];
    public static byte      byteValue = 89;
    public static char      charValue = 'x';
    public static double    doubleValue = 2.2;
    public static float     floatValue = 3.3f;
    public static int       intValue = 1;
    public static long      longValue = Long.MAX_VALUE;
    public static short     shortValue = 8;
    public static boolean   booleanValue = false;

    public static Class       classValue = Object.class;
    public static ClassLoader classLoaderValue;
    {
        try {
            urls[0] = new URL("file:/foo");
        } catch (java.net.MalformedURLException ex) {
            throw new AssertionError(ex);
        }
        classLoaderValue = new URLClassLoader(urls);
    }

    public static Thread      threadValue = Thread.currentThread();
    public static ThreadGroup threadGroupValue = threadValue.getThreadGroup();
    public static String      stringValue = "abc";
    public static int[]       intArrayValue = new int[] {1, 2, 3};

    public static EarlyReturnTarg  objectValue =
        new EarlyReturnTarg();
    public String ivar = stringValue;

    /*
     * These are the values that will be used by methods
     * returning early.  These are != the normal values
     * defined above.
     */
    static URL[] eurls = new URL[1];
    public static byte      ebyteValue = 42;
    public static char      echarValue = 'a';
    public static double    edoubleValue = 6.6;
    public static float     efloatValue = 9.9f;
    public static int       eintValue = 7;
    public static long      elongValue = Long.MIN_VALUE;
    public static short     eshortValue = 3;
    public static boolean   ebooleanValue = true;

    public static Class eclassValue = String.class;
    public static ClassLoader eclassLoaderValue;
    {
        try {
            urls[0] = new URL("file:/bar");
        } catch (java.net.MalformedURLException ex) {
            throw new AssertionError(ex);
        }
        classLoaderValue = new URLClassLoader(urls);
    }
    public static Thread ethreadValue;
    public static ThreadGroup ethreadGroupValue;
    public static String estringValue = "wxyz";
    public static int[]       eintArrayValue = new int[] {10, 11, 12};

    public static java.util.Date eobjectValue = new java.util.Date();

    // Used to check the return values seen on the debugee side
    public static boolean chk(byte v) {
        return v == (debuggerWatching ? ebyteValue: byteValue);
    }
    public static boolean chk(char v) {
        return v == (debuggerWatching ? echarValue: charValue);
    }
    public static boolean chk(double v) {
        return v == (debuggerWatching ? edoubleValue: doubleValue);
    }
    public static boolean chk(float v) {
        return v == (debuggerWatching ? efloatValue: floatValue);
    }
    public static boolean chk(int v) {
        return v == (debuggerWatching ? eintValue: intValue);
    }
    public static boolean chk(long v) {
        return v == (debuggerWatching ? elongValue: longValue);
    }
    public static boolean chk(short v) {
        return v == (debuggerWatching ? eshortValue: shortValue);
    }
    public static boolean chk(boolean v) {
        return v == (debuggerWatching ? ebooleanValue: booleanValue);
    }
    public static boolean chk(String v) {
        return v.equals(debuggerWatching ? estringValue: stringValue);
    }
    public static boolean chk(Object v) {
        return v.equals(debuggerWatching ? eobjectValue: objectValue);
    }

    // Used to show which set of tests follows
    public static String s_show(String p1) { return p1;}

    // These are the static methods
    public static byte s_bytef(int p1){ return byteValue; }
    public static char s_charf()      { return charValue; }
    public static double s_doublef()  { return doubleValue; }
    public static float s_floatf()    { return floatValue; }
    public static int s_intf()        { return intValue; }
    public static long s_longf()      { return longValue; }
    public static short s_shortf()    { return shortValue; }
    public static boolean s_booleanf(){ return booleanValue; }
    public static String s_stringf()  { return stringValue; }
    public static Class s_classf()    { return classValue; }
    public static ClassLoader s_classLoaderf()
                                      { return classLoaderValue; }
    public static Thread s_threadf()  { return threadValue; }
    public static ThreadGroup s_threadGroupf()
                                      { return threadGroupValue; }
    public static int[] s_intArrayf() { return intArrayValue; }
    public static Object s_nullObjectf() { return null; }
    public static Object s_objectf()  { return objectValue; }
    public static void s_voidf()      { System.err.println("debugee in s_voidf");}

    // These are the instance methods
    public byte i_bytef(int p1)      { return byteValue; }
    public char i_charf()            { return charValue; }
    public double i_doublef()        { return doubleValue; }
    public float i_floatf()          { return floatValue; }
    public int i_intf()              { return intValue; }
    public long i_longf()            { return longValue; }
    public short i_shortf()          { return shortValue; }
    public boolean i_booleanf()      { return booleanValue; }
    public String i_stringf()        { return stringValue; }
    public Class i_classf()          { return classValue; }
    public ClassLoader i_classLoaderf()
                                     { return classLoaderValue; }
    public Thread i_threadf()        { return threadValue; }
    public ThreadGroup i_threadGroupf()
                                     { return threadGroupValue; }
    public int[] i_intArrayf()       { return intArrayValue; }
    public Object i_nullObjectf()    { return null; }
    public Object i_objectf()        { return objectValue; }
    public void i_voidf()            {}

    static void doit(EarlyReturnTarg xx) throws Exception {
        System.err.print("debugee in doit ");
        if (debuggerWatching) {
            System.err.println("with a debugger watching.  Early returns expected.");
        } else {
            System.err.println("with no debugger watching.  Normal returns.");
        }

        s_show("==========  Testing static methods ================");
        if (!chk( s_bytef(88))) failureCount++;
        if (!chk( s_charf())) failureCount++;
        if (!chk( s_doublef())) failureCount++;
        if (!chk( s_floatf())) failureCount++;
        if (!chk( s_intf())) failureCount++;
        if (!chk( s_longf())) failureCount++;
        if (!chk( s_shortf())) failureCount++;
        if (!chk( s_booleanf())) failureCount++;

        if (!chk( s_stringf())) failureCount++;
        s_classf();
        s_classLoaderf();
        s_threadf();
        s_threadGroupf();
        s_intArrayf();
        s_nullObjectf();
        if (!chk( s_objectf())) failureCount++;
        s_voidf();

        s_show("==========  Testing instance methods ================");
        if (!chk( xx.i_bytef(89))) failureCount++;
        if (!chk( xx.i_charf())) failureCount++;
        if (!chk( xx.i_doublef())) failureCount++;
        if (!chk( xx.i_floatf())) failureCount++;
        if (!chk( xx.i_intf())) failureCount++;
        if (!chk( xx.i_longf())) failureCount++;
        if (!chk( xx.i_shortf())) failureCount++;
        if (!chk( xx.i_booleanf())) failureCount++;
        if (!chk( xx.i_stringf())) failureCount++;
        xx.i_intArrayf();
        xx.i_classf();
        xx.i_classLoaderf();
        xx.i_threadf();
        xx.i_threadGroupf();
        xx.i_nullObjectf();
        if (!chk( xx.i_objectf())) failureCount++;
        xx.i_voidf();

    }

    /** Hang so that test fails */
    static void hang() {
        try {
            // ten minute nap
            Thread.currentThread().sleep(10 * 60 * 1000);
        } catch (InterruptedException exc) {
            // shouldn't happen
        }
    }

    public static void main(String[] args) throws Exception {
        // The debugger will stop at the start of main,
        // set breakpoints and then do a resume.
        System.err.println("debugee in main");
        EarlyReturnTarg xx =
            new EarlyReturnTarg();

        doit(xx);
        if (debuggerWatching && failureCount > 0) {
            hang();
            throw new Exception("EarlyReturnTarg: failed");
        }
    }
}



public class EarlyReturnTest extends TestScaffold {


    /*
     * Class patterns for which we don't want events (copied
     * from the "Trace.java" example):
     *     http://java.sun.com/javase/technologies/core/toolsapis/jpda/
     */
    private String[] excludes = {
        "javax.*",
        "sun.*",
        "com.sun.*",
        "com.oracle.*",
        "oracle.*"};

    static VirtualMachineManager vmm ;
    ClassType targetClass;
    Field theValueField;
    static int earlyReturns = 0;
    static final int expectedEarlyReturns = 34; // determined by inspection :-)

    EarlyReturnTest(String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        EarlyReturnTest meee = new EarlyReturnTest(args);
        vmm = Bootstrap.virtualMachineManager();
        meee.startTests();
    }

    // chkXXX methods lifted directly from MethodExitReturnValuesTest
    // These methods check for correct return values.  Thanks, Jim!
    void ckByteValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("ebyteValue");
        ByteValue theValue = (ByteValue)targetClass.getValue(theValueField);

        byte vv = theValue.value();
        byte rv = ((ByteValue)retValue).value();
        if (vv != rv) {
            failure("failure: byte: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: byte " + rv);
            earlyReturns++;
        }
    }

    void ckCharValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("echarValue");
        CharValue theValue = (CharValue)targetClass.getValue(theValueField);

        char vv = theValue.value();
        char rv = ((CharValue)retValue).value();
        if (vv != rv) {
            failure("failure: char: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: char " + rv);
            earlyReturns++;
        }
    }

    void ckDoubleValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("edoubleValue");
        DoubleValue theValue = (DoubleValue)targetClass.getValue(theValueField);

        double vv = theValue.value();
        double rv = ((DoubleValue)retValue).value();
        if (vv != rv) {
            failure("failure: double: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: double " + rv);
            earlyReturns++;
        }
    }

    void ckFloatValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("efloatValue");
        FloatValue theValue = (FloatValue)targetClass.getValue(theValueField);

        float vv = theValue.value();
        float rv = ((FloatValue)retValue).value();
        if (vv != rv) {
            failure("failure: float: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: float " + rv);
            earlyReturns++;
        }
    }

    void ckIntValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("eintValue");
        IntegerValue theValue = (IntegerValue)targetClass.getValue(theValueField);

        int vv = theValue.value();
        int rv = ((IntegerValue)retValue).value();
        if (vv != rv) {
            failure("failure: int: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: int " + rv);
            earlyReturns++;
        }
    }

    void ckLongValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("elongValue");
        LongValue theValue = (LongValue)targetClass.getValue(theValueField);

        long vv = theValue.value();
        long rv = ((LongValue)retValue).value();
        if (vv != rv) {
            failure("failure: long: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: long " + rv);
            earlyReturns++;
        }
    }

    void ckShortValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("eshortValue");
        ShortValue theValue = (ShortValue)targetClass.getValue(theValueField);

        short vv = theValue.value();
        short rv = ((ShortValue)retValue).value();
        if (vv != rv) {
            failure("failure: short: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: short " + rv);
            earlyReturns++;
        }
    }

    void ckBooleanValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("ebooleanValue");
        BooleanValue theValue = (BooleanValue)targetClass.getValue(theValueField);

        boolean vv = theValue.value();
        boolean rv = ((BooleanValue)retValue).value();
        if (vv != rv) {
            failure("failure: boolean: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: boolean " + rv);
            earlyReturns++;
        }
    }

    void ckStringValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("estringValue");
        StringReference theValue = (StringReference)targetClass.getValue(theValueField);

        String vv = theValue.value();
        String rv = ((StringReference)retValue).value();
        if (vv != rv) {
            failure("failure: String: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: String: " + rv);
            earlyReturns++;
        }
    }

    void ckClassValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("eclassValue");
        ClassObjectReference vv = (ClassObjectReference)targetClass.
            getValue(theValueField);

        ClassObjectReference rv = (ClassObjectReference)retValue;
        if (vv != rv) {
            failure("failure: Class: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: Class: " + rv);
            earlyReturns++;
        }
    }

    void ckClassLoaderValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("eclassLoaderValue");
        ClassLoaderReference vv = (ClassLoaderReference)targetClass.
            getValue(theValueField);

        ClassLoaderReference rv = (ClassLoaderReference)retValue;
        if (vv != rv) {
            failure("failure: ClassLoader: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: ClassLoader: " + rv);
            earlyReturns++;
        }
    }

    void ckThreadValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("ethreadValue");
        ThreadReference vv = (ThreadReference)targetClass.
            getValue(theValueField);

        ThreadReference rv = (ThreadReference)retValue;
        if (vv != rv) {
            failure("failure: Thread: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: Thread: " + rv);
            earlyReturns++;
        }
    }

    void ckThreadGroupValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("ethreadGroupValue");
        ThreadGroupReference vv = (ThreadGroupReference)targetClass.
            getValue(theValueField);

        ThreadGroupReference rv = (ThreadGroupReference)retValue;
        if (vv != rv) {
            failure("failure: ThreadgGroup: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: ThreadGroup: " + rv);
            earlyReturns++;
        }
    }

    void ckArrayValue(Value retValue) {
        Field theValueField = targetClass.fieldByName("eintArrayValue");
        ArrayReference theValue = (ArrayReference)targetClass.getValue(theValueField);
        IntegerValue theElem2 = (IntegerValue)theValue.getValue(2);

        ArrayReference theRetValue = (ArrayReference)retValue;
        IntegerValue retElem2 = (IntegerValue)theRetValue.getValue(2);
        int vv = theElem2.value();
        int rv = retElem2.value();
        if (vv != rv) {
            failure("failure: in[2]: expected " + vv + ", got " + rv);
        } else {
            System.out.println("Passed: int[2]: " + rv);
            earlyReturns++;
        }
    }

    void ckNullObjectValue(Value retValue) {
        if (retValue != null) {
            failure("failure: NullObject: expected " + null + ", got " + retValue);
        } else {
            System.out.println("Passed: NullObject: " + retValue);
            earlyReturns++;
        }
    }

    void ckObjectValue(Value retValue) {
        ObjectReference theRetValue = (ObjectReference)retValue;

        Field theIVarField = targetClass.fieldByName("eobjectValue");
        ObjectReference theRetValField = (ObjectReference)targetClass.getValue(theIVarField);

        if (! theRetValue.equals(theRetValField)) {
            failure("failure: Object: expected " + theIVarField + ", got " + theRetValField);
        } else {
            System.out.println("Passed: Object: " + theRetValField);
            earlyReturns++;
        }
    }

    void ckVoidValue(Value retValue) {
        System.out.println("Passed: Void");
        earlyReturns++;
    }

    public BreakpointRequest setBreakpoint(String clsName,
                                           String methodName,
                                           String methodSignature) {
        ReferenceType rt = findReferenceType(clsName);
        if (rt == null) {
            rt = resumeToPrepareOf(clsName).referenceType();
        }

        Method method = findMethod(rt, methodName, methodSignature);
        if (method == null) {
            throw new IllegalArgumentException("Bad method name/signature");
        }
        BreakpointRequest bpr = eventRequestManager().createBreakpointRequest(method.location());
        bpr.setSuspendPolicy(EventRequest.SUSPEND_ALL);
        bpr.enable();
        return bpr;
    }

    public void breakpointReached(BreakpointEvent event) {
        String origMethodName = event.location().method().name();
        String methodName = origMethodName.substring(2);
        ThreadReference tr = event.thread();

        if (vm().canForceEarlyReturn()) {

            try {

                if ("bytef".equals(methodName)){
                    Field theValueField = targetClass.fieldByName("ebyteValue");
                    ByteValue theValue = (ByteValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                    /*
                     * See what happens if we access the stack after the force
                     * and before the resume.  Disabling this since spec says
                     * the stack is undefined.  This type of code can be used to
                     * pursue just what that means.
                     *
                     * StackFrame sf = tr.frame(0);
                     * List<Value> ll = sf.getArgumentValues();
                     * for (Value vv: ll) {
                     *     System.out.println("vv = " + vv);
                     * }
                     */
                } else if ("charf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("echarValue");
                    CharValue theValue = (CharValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("doublef".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("edoubleValue");
                    DoubleValue theValue = (DoubleValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("floatf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("efloatValue");
                    FloatValue theValue = (FloatValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("intf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eintValue");
                    IntegerValue theValue = (IntegerValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("longf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("elongValue");
                    LongValue theValue = (LongValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("shortf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eshortValue");
                    ShortValue theValue = (ShortValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("booleanf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("ebooleanValue");
                    BooleanValue theValue = (BooleanValue)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("stringf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("estringValue");
                    StringReference theValue = (StringReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("classf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eclassValue");
                    ClassObjectReference theValue = (ClassObjectReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("classLoaderf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eclassLoaderValue");
                    ClassLoaderReference theValue = (ClassLoaderReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("threadf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("ethreadValue");
                    ThreadReference theValue = (ThreadReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("threadGroupf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("ethreadGroupValue");
                    ThreadGroupReference theValue = (ThreadGroupReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("intArrayf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eintArrayValue");
                    ArrayReference theValue = (ArrayReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("nullObjectf".equals(methodName)) {
                    tr.forceEarlyReturn(null);
                } else if ("objectf".equals(methodName)) {
                    Field theValueField = targetClass.fieldByName("eobjectValue");
                    ObjectReference theValue = (ObjectReference)targetClass.getValue(theValueField);
                    tr.forceEarlyReturn(theValue);
                } else if ("voidf".equals(methodName)) {
                    VoidValue theValue = vm().mirrorOfVoid();
                    tr.forceEarlyReturn(theValue);
                } else {
                    failure("failure: Unknown methodName: " + origMethodName);
                }

            } catch (Exception ex) {
                failure("failure: " + ex.toString());
                ex.printStackTrace();
            }
        } else {
            System.out.println("Cannot force early return for method: " + origMethodName);
        }
    }

    // This is the MethodExitEvent handler.
    public void methodExited(MethodExitEvent event) {
        String origMethodName = event.method().name();
        if (vm().canGetMethodReturnValues()) {
            Value retValue = event.returnValue();

            if (!origMethodName.startsWith("s_") &&
                !origMethodName.startsWith("i_")) {
                // Skip all uninteresting methods
                return;
            }

            String methodName = origMethodName.substring(2);
            if ("show".equals(methodName)) {
                System.out.println(retValue);
                return;
            }

            if ("bytef".equals(methodName))             ckByteValue(retValue);
            else if ("charf".equals(methodName))        ckCharValue(retValue);
            else if ("doublef".equals(methodName))      ckDoubleValue(retValue);
            else if ("floatf".equals(methodName))       ckFloatValue(retValue);
            else if ("intf".equals(methodName))         ckIntValue(retValue);
            else if ("longf".equals(methodName))        ckLongValue(retValue);
            else if ("shortf".equals(methodName))       ckShortValue(retValue);
            else if ("booleanf".equals(methodName))     ckBooleanValue(retValue);
            else if ("stringf".equals(methodName))      ckStringValue(retValue);
            else if ("classf".equals(methodName))       ckClassValue(retValue);
            else if ("classLoaderf".equals(methodName)) ckClassLoaderValue(retValue);
            else if ("threadf".equals(methodName))      ckThreadValue(retValue);
            else if ("threadGroupf".equals(methodName)) ckThreadGroupValue(retValue);
            else if ("intArrayf".equals(methodName))    ckArrayValue(retValue);
            else if ("nullObjectf".equals(methodName))  ckNullObjectValue(retValue);
            else if ("objectf".equals(methodName))      ckObjectValue(retValue);
            else if ("voidf".equals(methodName))        ckVoidValue(retValue);
            else {
                failure("failure: Unknown methodName: " + origMethodName);
            }
        } else {
            System.out.println("Return Value not available for method: " + origMethodName);
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */

        BreakpointEvent bpe = startToMain("EarlyReturnTarg");
        targetClass = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();

        /*
         * Ask for method exit events
         */
        MethodExitRequest exitRequest =
            eventRequestManager().createMethodExitRequest();

        for (int i=0; i<excludes.length; ++i) {
            exitRequest.addClassExclusionFilter(excludes[i]);
        }
        int sessionSuspendPolicy = EventRequest.SUSPEND_ALL;
        //sessionSuspendPolicy = EventRequest.SUSPEND_EVENT_THREAD;
        //sessionSuspendPolicy = EventRequest.SUSPEND_NONE;
        exitRequest.setSuspendPolicy(sessionSuspendPolicy);
        exitRequest.enable();

        /*
         * Turn on the flag so debugee knows to check for early
         * return values instead of regular return values.
         */
        Field flagField = targetClass.fieldByName("debuggerWatching");
        targetClass.setValue(flagField, vm().mirrorOf(true));


        /*
         * We set and enable breakpoints on all of the interesting
         * methods called by doit().  In the breakpointReached()
         * handler we force an early return with a different return
         * value.
         *
         * The MethodExitEvent handler will keep score.
         */

        setBreakpoint("EarlyReturnTarg", "s_bytef", "(I)B");
        setBreakpoint("EarlyReturnTarg", "s_charf", "()C");
        setBreakpoint("EarlyReturnTarg", "s_doublef", "()D");
        setBreakpoint("EarlyReturnTarg", "s_floatf", "()F");
        setBreakpoint("EarlyReturnTarg", "s_intf", "()I");
        setBreakpoint("EarlyReturnTarg", "s_longf", "()J");
        setBreakpoint("EarlyReturnTarg", "s_shortf", "()S");
        setBreakpoint("EarlyReturnTarg", "s_booleanf", "()Z");

        setBreakpoint("EarlyReturnTarg", "s_stringf", "()Ljava/lang/String;");
        setBreakpoint("EarlyReturnTarg", "s_classf", "()Ljava/lang/Class;");
        setBreakpoint("EarlyReturnTarg", "s_classLoaderf", "()Ljava/lang/ClassLoader;");
        setBreakpoint("EarlyReturnTarg", "s_threadf", "()Ljava/lang/Thread;");
        setBreakpoint("EarlyReturnTarg", "s_threadGroupf", "()Ljava/lang/ThreadGroup;");
        setBreakpoint("EarlyReturnTarg", "s_intArrayf", "()[I");
        setBreakpoint("EarlyReturnTarg", "s_nullObjectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnTarg", "s_objectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnTarg", "s_voidf", "()V");

        setBreakpoint("EarlyReturnTarg", "i_bytef", "(I)B");
        setBreakpoint("EarlyReturnTarg", "i_charf", "()C");
        setBreakpoint("EarlyReturnTarg", "i_doublef", "()D");
        setBreakpoint("EarlyReturnTarg", "i_floatf", "()F");
        setBreakpoint("EarlyReturnTarg", "i_intf", "()I");
        setBreakpoint("EarlyReturnTarg", "i_longf", "()J");
        setBreakpoint("EarlyReturnTarg", "i_shortf", "()S");
        setBreakpoint("EarlyReturnTarg", "i_booleanf", "()Z");
        setBreakpoint("EarlyReturnTarg", "i_stringf", "()Ljava/lang/String;");
        setBreakpoint("EarlyReturnTarg", "i_intArrayf", "()[I");
        setBreakpoint("EarlyReturnTarg", "i_classf", "()Ljava/lang/Class;");
        setBreakpoint("EarlyReturnTarg", "i_classLoaderf", "()Ljava/lang/ClassLoader;");
        setBreakpoint("EarlyReturnTarg", "i_threadf", "()Ljava/lang/Thread;");
        setBreakpoint("EarlyReturnTarg", "i_threadGroupf", "()Ljava/lang/ThreadGroup;");
        setBreakpoint("EarlyReturnTarg", "i_nullObjectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnTarg", "i_objectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnTarg", "i_voidf", "()V");

        /* Here we go.  This adds 'this' as a listener so
         * that our handlers above will be called.
         */
        listenUntilVMDisconnect();

        if (earlyReturns != expectedEarlyReturns) {
            failure("failure: Expected " + expectedEarlyReturns +
                    ", but got " + earlyReturns);
        }
        System.out.println("All done, " + earlyReturns + " passed");


        if (!testFailed) {
            System.out.println();
            System.out.println("EarlyReturnTest: passed");
        } else {
            System.out.println();
            System.out.println("EarlyReturnTest: failed");
            throw new Exception("EarlyReturnTest: failed");
        }
    }
}
