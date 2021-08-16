/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6431735
 * @summary Unexpected ClassCastException in ThreadReference.forceEarlyReturn
 * @author Jim Holmlund
 *
 * @run build TestScaffold VMConnection TargetListener TargetAdapter
 * @run compile -g EarlyReturnNegativeTest.java
 * @run driver EarlyReturnNegativeTest
 */
import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Array;

/*
 * This test has a debuggee which calls an instance method
 * for each kind of JDI value return type.
 *
 * The debugger sets breakpoints in all methods.  When a breakpoint
 * is hit the debugger requests an early return and supplies a new
 * return value. The new value is not compatible with the method's
 * return type so an InvalidTypeException should be thrown.
 *
 * Each value is stored in a static var in the debuggee.  The debugger
 * gets the values from these static vars to pass back to the
 * debuggee in forceEarlyReturn.
 *
 * This test was created out of EarlyReturnTest.java.  Not all of the
 * debuggee methods are actually used, just the ones needed to test
 * for correct operation.  I left the others in just in case they come
 * in handy in the future.
 */

class EarlyReturnNegativeTarg {
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
    public static Object[]    objectArrayValue = new Object[] {"a", "b", "c"};

    public static EarlyReturnNegativeTarg  objectValue =
        new EarlyReturnNegativeTarg();
    public String ivar = stringValue;


    // Used to show which set of tests follows
    public static String s_show(String p1) { return p1;}

    // These are the instance methods
    public byte i_bytef()            { return byteValue; }
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
    public Object[] i_objectArrayf() { return objectArrayValue; }
    public Object i_nullObjectf()    { return null; }
    public Object i_objectf()        { return objectValue; }
    public void i_voidf()            {}

    static void doit(EarlyReturnNegativeTarg xx) throws Exception {
        System.err.print("debugee in doit ");

        s_show("==========  Testing instance methods ================");
        xx.i_bytef();
        xx.i_charf();
        xx.i_doublef();
        xx.i_floatf();
        xx.i_intf();
        xx.i_longf();
        xx.i_shortf();
        xx.i_booleanf();
        xx.i_stringf();
        xx.i_intArrayf();
        xx.i_objectArrayf();
        xx.i_classf();
        xx.i_classLoaderf();
        xx.i_threadf();
        xx.i_threadGroupf();
        xx.i_nullObjectf();
        xx.i_objectf();
        xx.i_voidf();

    }

    public static void main(String[] args) throws Exception {
        /*
         * The debugger will stop at the start of main,
         * set breakpoints and then do a resume.
         */
        System.err.println("debugee in main");

        EarlyReturnNegativeTarg xx =
            new EarlyReturnNegativeTarg();

        doit(xx);
    }
}



public class EarlyReturnNegativeTest extends TestScaffold {

    static VirtualMachineManager vmm ;
    ClassType targetClass;
    Field theValueField;

    ByteValue byteVV;
    CharValue charVV;
    DoubleValue doubleVV;
    FloatValue floatVV;
    IntegerValue integerVV;
    LongValue longVV;
    ShortValue shortVV;
    BooleanValue booleanVV;
    ObjectReference objectVV;
    ArrayReference intArrayVV;
    ArrayReference objectArrayVV;
    VoidValue voidVV;

    EarlyReturnNegativeTest(String args[]) {
        super(args);
    }

    public static void main(String[] args)      throws Exception {
        EarlyReturnNegativeTest meee = new EarlyReturnNegativeTest(args);
        vmm = Bootstrap.virtualMachineManager();
        meee.startTests();
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

    void doEarly(ThreadReference tr, String methodName, Value val) {
        try {
            tr.forceEarlyReturn(val);
        } catch (InvalidTypeException ex) {
            System.out.println("Ok: " + methodName);
            return;
        } catch (Exception ex) {
            failure("failure: " + ex.toString());
            ex.printStackTrace();
            return;
        }
        failure("Expected InvalidTypeException for " + methodName + ", " + val + " but didn't get it.");
    }

    public void breakpointReached(BreakpointEvent event) {
        String origMethodName = event.location().method().name();
        String methodName = origMethodName.substring(2);
        ThreadReference tr = event.thread();

        if (vm().canForceEarlyReturn()) {

            /* There are some incompatible classes of values.  In the following,
             * we test each combination.
             */
            if ("shortf".equals(methodName)){
                doEarly(tr, origMethodName, booleanVV);
                doEarly(tr, origMethodName, objectVV);
                doEarly(tr, origMethodName, voidVV);
                doEarly(tr, origMethodName, intArrayVV);
                doEarly(tr, origMethodName, objectArrayVV);

            } else if ("booleanf".equals(methodName)) {
                doEarly(tr, origMethodName, shortVV);
                doEarly(tr, origMethodName, objectVV);
                doEarly(tr, origMethodName, voidVV);
                doEarly(tr, origMethodName, intArrayVV);
                doEarly(tr, origMethodName, objectArrayVV);

            } else if ("intArrayf".equals(methodName)) {
                doEarly(tr, origMethodName, booleanVV);
                doEarly(tr, origMethodName, shortVV);
                doEarly(tr, origMethodName, voidVV);
                doEarly(tr, origMethodName, objectVV);
                doEarly(tr, origMethodName, objectArrayVV);

            } else if ("objectArrayf".equals(methodName)) {
                doEarly(tr, origMethodName, booleanVV);
                doEarly(tr, origMethodName, shortVV);
                doEarly(tr, origMethodName, voidVV);
                doEarly(tr, origMethodName, objectVV);
                doEarly(tr, origMethodName, intArrayVV);

            } else if ("objectf".equals(methodName)) {
                doEarly(tr, origMethodName, booleanVV);
                doEarly(tr, origMethodName, shortVV);
                doEarly(tr, origMethodName, voidVV);

             } else if ("voidf".equals(methodName)) {
                doEarly(tr, origMethodName, booleanVV);
                doEarly(tr, origMethodName, shortVV);
                doEarly(tr, origMethodName, objectVV);
                doEarly(tr, origMethodName, intArrayVV);
                doEarly(tr, origMethodName, objectArrayVV);

            } else {
                // just ignore others
                System.out.println("Ignoring: " + methodName);
                return;
            }
        } else {
            System.out.println("Cannot force early return for method: " + origMethodName);
        }
    }

    protected void runTests() throws Exception {
        /*
         * Get to the top of main()
         * to determine targetClass and mainThread
         */

        BreakpointEvent bpe = startToMain("EarlyReturnNegativeTarg");
        targetClass = (ClassType)bpe.location().declaringType();
        mainThread = bpe.thread();

        /*
         * We set and enable breakpoints on all of the interesting
         * methods called by doit().  In the breakpointReached()
         * handler we force an early return with a different return
         * value.
         *
         */

        setBreakpoint("EarlyReturnNegativeTarg", "i_bytef", "()B");
        setBreakpoint("EarlyReturnNegativeTarg", "i_charf", "()C");
        setBreakpoint("EarlyReturnNegativeTarg", "i_doublef", "()D");
        setBreakpoint("EarlyReturnNegativeTarg", "i_floatf", "()F");
        setBreakpoint("EarlyReturnNegativeTarg", "i_intf", "()I");
        setBreakpoint("EarlyReturnNegativeTarg", "i_longf", "()J");
        setBreakpoint("EarlyReturnNegativeTarg", "i_shortf", "()S");
        setBreakpoint("EarlyReturnNegativeTarg", "i_booleanf", "()Z");
        setBreakpoint("EarlyReturnNegativeTarg", "i_stringf", "()Ljava/lang/String;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_intArrayf", "()[I");
        setBreakpoint("EarlyReturnNegativeTarg", "i_objectArrayf", "()[Ljava/lang/Object;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_classf", "()Ljava/lang/Class;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_classLoaderf", "()Ljava/lang/ClassLoader;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_threadf", "()Ljava/lang/Thread;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_threadGroupf", "()Ljava/lang/ThreadGroup;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_nullObjectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_objectf", "()Ljava/lang/Object;");
        setBreakpoint("EarlyReturnNegativeTarg", "i_voidf", "()V");

        /* Create Value objects to be passed in forceEarlyReturn calls */
        Field theValueField = targetClass.fieldByName("byteValue");
        byteVV = (ByteValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("charValue");
        charVV = (CharValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("doubleValue");
        doubleVV = (DoubleValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("floatValue");
        floatVV = (FloatValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("intValue");
        integerVV = (IntegerValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("longValue");
        longVV = (LongValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("shortValue");
        shortVV = (ShortValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("booleanValue");
        booleanVV = (BooleanValue)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("objectValue");
        objectVV = (ObjectReference)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("intArrayValue");
        intArrayVV = (ArrayReference)targetClass.getValue(theValueField);

        theValueField = targetClass.fieldByName("objectArrayValue");
        objectArrayVV = (ArrayReference)targetClass.getValue(theValueField);

        voidVV = vm().mirrorOfVoid();

        /* Here we go.  This adds 'this' as a listener so
         * that our handlers above will be called.
         */
        listenUntilVMDisconnect();

        if (!testFailed) {
            System.out.println();
            System.out.println("EarlyReturnNegativeTest: passed");
        } else {
            System.out.println();
            System.out.println("EarlyReturnNegativeTest: failed");
            throw new Exception("EarlyReturnNegativeTest: failed");
        }
    }
}
