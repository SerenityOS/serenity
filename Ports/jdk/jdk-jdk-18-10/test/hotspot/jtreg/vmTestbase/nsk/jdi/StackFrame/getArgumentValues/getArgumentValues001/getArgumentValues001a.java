/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
package nsk.jdi.StackFrame.getArgumentValues.getArgumentValues001;

import java.util.*;
import nsk.share.*;
import nsk.share.jdi.*;

//    THIS TEST IS LINE NUMBER SENSITIVE

/*
 * Debugee class, starts thread which executes methods with various arguments.
 *
 * !!! Edit carefully, array 'getArgumentValues001a.TestClass.testLocations' contains hardcored line numbers !!!
 */
public class getArgumentValues001a extends AbstractJDIDebuggee {

    // class contains methods with different arguments
    static class TestClass {

        Log log;

        TestClass(Log log) {
            this.log = log;
        }

        static List<LocationData> testLocations = new ArrayList<LocationData>();

        static Object expectedArgumentValues[];

        // methods receiving as arguments all possible primitive types

        void testMethod1(boolean arg1) {
            log.display("testMethod1"); // testLocations.at(0)
        }

        void testMethod2(byte arg1) {
            log.display("testMethod2"); // testLocations.at(1)
        }

        void testMethod3(short arg1) {
            log.display("testMethod3"); // testLocations.at(2)
        }

        void testMethod4(char arg1) {
            log.display("testMethod4"); // testLocations.at(3)
        }

        void testMethod5(int arg1) {
            log.display("testMethod5"); // testLocations.at(4) //  // testLocations.at(13)
        }

        void testMethod6(long arg1) {
            log.display("testMethod6"); // testLocations.at(5)
        }

        void testMethod7(float arg1) {
            log.display("testMethod7"); // testLocations.at(6)
        }

        void testMethod8(double arg1) {
            log.display("testMethod8"); // testLocations.at(7)
        }

        // method receiving Object as argument
        void testMethod9(Object object) {
            log.display("testMethod9"); // testLocations.at(8)
        }

        // method receiving String as argument
        void testMethod10(String object) {
            log.display("testMethod10"); // testLocations.at(9)
        }

        // method receiving TestClass as argument
        void testMethod11(TestClass object) {
            log.display("testMethod11"); // testLocations.at(10)
        }

        // method without arguments
        void testMethod12() {
            log.display("testMethod12"); // testLocations.at(11)
        }

        // method receiving all primitive type and Object as arguments
        void testMethod13(boolean arg1, byte arg2, short arg3, char arg4, int arg5, long arg6, float arg7, double arg8, Object arg9) {
            log.display("testMethod13"); // testLocations.at(12)
        }

        // method with single arument changes argument's value several times
        void testMethod14(int arg1) {
            log.display("testMethod14"); // testLocations.at(14)
            arg1++;
            TestClass.expectedArgumentValues = new Object[] { new Value(arg1) };
            log.display("testMethod14"); // testLocations.at(15)
            arg1--;
            TestClass.expectedArgumentValues = new Object[] { new Value(arg1) };
            log.display("testMethod14"); // testLocations.at(16)
            arg1 = 0;
            TestClass.expectedArgumentValues = new Object[] { new Value(arg1) };
            log.display("testMethod14"); // testLocations.at(17)
        }

        // method with several arguments changes its values in loop
        void testMethod15(int arg1, float arg2, Object arg3) {
            long v1 = (long)(arg1 + arg2);
            for (int i = 0; i < 10; i++) {
                arg1 = (int)(i * arg2 - v1);
                arg2 += 1;
                arg2 += v1;
                arg3 = (i % 2 == 0) ? null : new Object();
                TestClass.expectedArgumentValues = new Object[] { new Value(arg1), new Value(arg2), arg3};
                log.display("testMethod15");  // testLocations.at(18)
            }
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    arg1 = (int)(i * arg2 + j + v1);
                    arg2 += i;
                    arg3 = arg1 + " " + arg2;
                    TestClass.expectedArgumentValues = new Object[] { new Value(arg1), new Value(arg2), arg3};
                    log.display("testMethod15"); // testLocations.at(19)
                }
            }
        }

        // static method with different arguments
        static void testMethod16(Log log, boolean arg1, short arg2, char arg3, int arg4, long arg5, float arg6, double arg7, Object arg8) {
            log.display("testMethod16"); // testLocations.at(20)
        }

        // static method changing argument value
        static void testMethod17(Log log, int arg1) {
            log.display("testMethod17"); // testLocations.at(21)
            arg1++;
            TestClass.expectedArgumentValues = new Object[] { log, new Value(arg1) };
            log.display("testMethod17"); // testLocations.at(22)
            arg1--;
            TestClass.expectedArgumentValues = new Object[] { log, new Value(arg1) };
            log.display("testMethod17"); // testLocations.at(23)
            arg1 = 0;
            TestClass.expectedArgumentValues = new Object[] { log, new Value(arg1) };
            log.display("testMethod17"); // testLocations.at(24)
        }

        // static method changing arguments in loop
        static void testMethod18(Log log, int arg1, float arg2, Object arg3) {
            long v1 = (long)(arg1 + arg2);
            for (int i = 0; i < 10; i++) {
                arg1 = (int)(i * arg2 - v1);
                arg2 += 1;
                arg2 += v1;
                arg3 = (i % 2 == 0) ? null : new Object();
                TestClass.expectedArgumentValues = new Object[] { log, new Value(arg1), new Value(arg2), arg3};
                log.display("testMethod18"); // testLocations.at(25)
            }
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    arg1 = (int)(i * arg2 + j + v1);
                    arg2 += i;
                    arg3 = arg1 + " " + arg2;
                    TestClass.expectedArgumentValues = new Object[] { log, new Value(arg1), new Value(arg2), arg3};
                    log.display("testMethod18"); // testLocations.at(26)
                }
            }
        }

        // method receiving arrays as arguments
        void testMethod19(boolean[] arg1, byte[] arg2, short[] arg3, char[] arg4, int[] arg5, long[] arg6, float[] arg7, double[] arg8, Object[] arg9) {
            log.display("testMethod19"); // testLocations.at(27)
        }

        // method receiving multidimensional arrays as arguments
        void testMethod20(boolean[][][] arg1, byte[][][] arg2, short[][][] arg3, char[][][] arg4, int[][][] arg5, long[][][] arg6, float[][][] arg7, double[][][] arg8, Object[][][] arg9) {
            log.display("testMethod20"); // testLocations.at(28)
        }

        static {
            // primitive values
            testLocations.add(new LocationData(54, 1));
            testLocations.add(new LocationData(58, 1));
            testLocations.add(new LocationData(62, 1));
            testLocations.add(new LocationData(66, 1));
            testLocations.add(new LocationData(70, 1));
            testLocations.add(new LocationData(74, 1));
            testLocations.add(new LocationData(78, 1));
            testLocations.add(new LocationData(82, 1));

            // objects
            testLocations.add(new LocationData(87, 1));
            testLocations.add(new LocationData(92, 1));
            testLocations.add(new LocationData(97, 1));

            // method without args
            testLocations.add(new LocationData(102, 1));

            // method with many arguments
            testLocations.add(new LocationData(107, 1));

            // method 'testMethod5' is called 50 times
            testLocations.add(new LocationData(70, 50));

            // method 3 times changes argument value
            testLocations.add(new LocationData(112, 1));
            testLocations.add(new LocationData(115, 1));
            testLocations.add(new LocationData(118, 1));
            testLocations.add(new LocationData(121, 1));

            // method changes arguments in loop
            testLocations.add(new LocationData(133, 10));
            testLocations.add(new LocationData(141, 25));

            // static method with many arguments
            testLocations.add(new LocationData(148, 1));

            // static method 3 times changes argument value
            testLocations.add(new LocationData(153, 1));
            testLocations.add(new LocationData(156, 1));
            testLocations.add(new LocationData(159, 1));
            testLocations.add(new LocationData(162, 1));

            // static method changes arguments in loop
            testLocations.add(new LocationData(174, 10));
            testLocations.add(new LocationData(182, 25));

            // arrays
            testLocations.add(new LocationData(189, 1));

            // multidimensional arrays
            testLocations.add(new LocationData(194, 1));
        }
    }

    public static void main(String args[]) {
        new getArgumentValues001a().doTest(args);
    }

    static class LocationData {

        public LocationData(int lineNumber, int breakpointsNumber) {
            this.lineNumber = lineNumber;
            this.breakpointsNumber = breakpointsNumber;
        }

        public int breakpointsNumber;

        public int lineNumber;
    }

    /*
     * This class used to store primitive type values, storing type name is stored in
     * field type and value is stored in field '<TypeName>Value'
     */
    static class Value {
        boolean booleanValue;

        byte byteValue;

        short shortValue;

        char charValue;

        int intValue;

        long longValue;

        float floatValue;

        double doubleValue;

        String name;

        Value(boolean value) {
            name = "boolean";
            booleanValue = value;
        }

        Value(byte value) {
            name = "byte";
            byteValue = value;
        }

        Value(short value) {
            name = "short";
            shortValue = value;
        }

        Value(char value) {
            name = "char";
            charValue = value;
        }

        Value(int value) {
            name = "int";
            intValue = value;
        }

        Value(long value) {
            name = "long";
            longValue = value;
        }

        Value(float value) {
            name = "float";
            floatValue = value;
        }

        Value(double value) {
            name = "double";
            doubleValue = value;
        }
    }

    public static final String COMMAND_START_TEST_THREAD = "COMMAND_START_TEST_THREAD";

    public static final String COMMAND_STOP_TEST_THREAD = "COMMAND_STOP_TEST_THREAD";

    public String[] doInit(String args[]) {
        args = super.doInit(args);

        try {
            Class.forName(TestClass.class.getName());
        } catch (Throwable t) {
            setSuccess(false);
            System.out.println("Unexpected exception during initialization: " + t);
            t.printStackTrace();
            throw new TestBug("Unexpected exception during initialization: " + t);
        }

        return args;
    }

    class TestThread extends Thread {
        public void run() {
            TestClass testClass = new TestClass(log);

            TestClass.expectedArgumentValues = new Object[] { new Value(true) };
            testClass.testMethod1(true);

            TestClass.expectedArgumentValues = new Object[] { new Value((byte)10) };
            testClass.testMethod2((byte) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((short) 10) };
            testClass.testMethod3((short) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((char) 10) };
            testClass.testMethod4((char) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((int) 10) };
            testClass.testMethod5((int) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((long) 10) };
            testClass.testMethod6((long) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((float) 10) };
            testClass.testMethod7((float) 10);

            TestClass.expectedArgumentValues = new Object[] { new Value((double) 10) };
            testClass.testMethod8((double) 10);

            Object arg = new Object();
            TestClass.expectedArgumentValues = new Object[] { arg };
            testClass.testMethod9(arg);

            arg = "String";
            TestClass.expectedArgumentValues = new Object[] { arg };
            testClass.testMethod10((String) arg);

            arg = testClass;
            TestClass.expectedArgumentValues = new Object[] { arg };
            testClass.testMethod11((TestClass) arg);

            TestClass.expectedArgumentValues = new Object[] {};
            testClass.testMethod12();

            arg = new Object();
            TestClass.expectedArgumentValues = new Object[] { new Value(false), new Value((byte) 0), new Value((short) 1), new Value((char) 2), new Value(3),
                    new Value((long) 4), new Value((float) 5), new Value((double) 6), arg };
            testClass.testMethod13(false, (byte) 0, (short) 1, (char) 2, 3, 4, 5, 6, arg);

            for (int i = 0; i < 50; i++) {
                int intArg = 50 + i;
                TestClass.expectedArgumentValues = new Object[] { new Value(intArg) };
                testClass.testMethod5(intArg);
            }

            TestClass.expectedArgumentValues = new Object[] { new Value((int) 10) };
            testClass.testMethod14(10);

            testClass.testMethod15(11, 0.1f, new Object());

            arg = new Object();
            TestClass.expectedArgumentValues = new Object[] { log, new Value(false), new Value((short) 1), new Value((char) 2), new Value(3),
                    new Value((long) 4), new Value((float) 5), new Value((double) 6), arg };
            TestClass.testMethod16(log, false, (short) 1, (char) 2, 3, 4, 5, 6, arg);

            TestClass.expectedArgumentValues = new Object[] {log, new Value((int) 10) };
            TestClass.testMethod17(log, 10);

            TestClass.testMethod18(log, 11, 0.1f, new Object());

            {
                boolean[] arg1 = {};
                byte[] arg2 = {};
                short[] arg3 = {};
                char[] arg4 = {};
                int[] arg5 = {};
                long[] arg6 = {};
                float[] arg7 = {};
                double[] arg8 = {};
                Object[] arg9 = {};
                TestClass.expectedArgumentValues = new Object[] {arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9};
                testClass.testMethod19(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
            }
            {
                boolean[][][] arg1 = {};
                byte[][][] arg2 = {};
                short[][][] arg3 = {};
                char[][][] arg4 = {};
                int[][][] arg5 = {};
                long[][][] arg6 = {};
                float[][][] arg7 = {};
                double[][][] arg8 = {};
                Object[][][] arg9 = {};
                TestClass.expectedArgumentValues = new Object[] {arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9};
                testClass.testMethod20(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9);
            }
        }
    }

    private TestThread testThread;

    public boolean parseCommand(String command) {
        if (super.parseCommand(command))
            return true;

        if (command.equals(COMMAND_START_TEST_THREAD)) {

            if (testThread != null)
                throw new TestBug("Thread is already created");

            testThread = new TestThread();
            testThread.start();

            return true;
        } else if (command.equals(COMMAND_STOP_TEST_THREAD)) {

            if (testThread == null)
                throw new TestBug("Thread isn't created");

            try {
                testThread.join();
            } catch (InterruptedException e) {
                setSuccess(false);
                log.complain("Unexpected exception: " + e);
                e.printStackTrace(log.getOutStream());
            }

            return true;
        }

        return false;
    }
}
