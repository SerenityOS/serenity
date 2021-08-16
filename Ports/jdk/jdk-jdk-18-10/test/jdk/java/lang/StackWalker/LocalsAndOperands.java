/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8020968 8147039 8156073
 * @summary Tests for locals and operands
 * @modules java.base/java.lang:open
 * @run testng/othervm -Xint -DtestUnused=true LocalsAndOperands
 * @run testng/othervm -Xcomp LocalsAndOperands
 */

/*
 * @test
 * @bug 8020968 8147039 8156073
 * @modules java.base/java.lang:open
 * @requires !vm.graal.enabled
 * @run testng/othervm -Xcomp -XX:-TieredCompilation LocalsAndOperands
 */

import org.testng.annotations.*;
import static org.testng.Assert.*;
import java.lang.StackWalker.StackFrame;
import static java.lang.StackWalker.Option.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

public class LocalsAndOperands {
    static final boolean debug = false;
    static final boolean is32bit;
    static final boolean testUnused;

    static Class<?> liveStackFrameClass;
    static Class<?> primitiveSlotClass;
    static Class<?> primitiveSlot32Class;
    static Class<?> primitiveSlot64Class;

    static StackWalker extendedWalker;
    static Method getLocals;
    static Method getOperands;
    static Method getMonitors;
    static Method primitiveSize;
    static Method primitiveLongValue;
    static Method primitiveIntValue;
    static Method getExtendedWalker;

    private static final long LOWER_LONG_VAL = 4L; // Lower bits
    private static final long UPPER_LONG_VAL = 0x123400000000L; // Upper bits
    private static final long NEG_LONG_VAL = Long.MIN_VALUE;

    private static final double LOWER_DOUBLE_VAL = Double.longBitsToDouble(0xABCDL);
    private static final double UPPER_DOUBLE_VAL = Double.longBitsToDouble(0x432100000000L);

    static {
        try {
            liveStackFrameClass = Class.forName("java.lang.LiveStackFrame");
            primitiveSlotClass = Class.forName("java.lang.LiveStackFrame$PrimitiveSlot");
            primitiveSlot32Class = Class.forName("java.lang.LiveStackFrameInfo$PrimitiveSlot32");
            primitiveSlot64Class = Class.forName("java.lang.LiveStackFrameInfo$PrimitiveSlot64");

            getLocals = liveStackFrameClass.getDeclaredMethod("getLocals");
            getLocals.setAccessible(true);

            getOperands = liveStackFrameClass.getDeclaredMethod("getStack");
            getOperands.setAccessible(true);

            getMonitors = liveStackFrameClass.getDeclaredMethod("getMonitors");
            getMonitors.setAccessible(true);

            primitiveSize = primitiveSlotClass.getDeclaredMethod("size");
            primitiveSize.setAccessible(true);

            primitiveLongValue = primitiveSlotClass.getDeclaredMethod("longValue");
            primitiveLongValue.setAccessible(true);

            primitiveIntValue = primitiveSlotClass.getDeclaredMethod("intValue");
            primitiveIntValue.setAccessible(true);

            getExtendedWalker = liveStackFrameClass.getMethod("getStackWalker", Set.class);
            getExtendedWalker.setAccessible(true);
            extendedWalker = (StackWalker) getExtendedWalker.invoke(null,
                    EnumSet.noneOf(StackWalker.Option.class));

            String dataModel = System.getProperty("sun.arch.data.model");
            if ("32".equals(dataModel)) {
                is32bit = true;
            } else if ("64".equals(dataModel)) {
                is32bit= false;
            } else {
                throw new RuntimeException("Weird data model:" + dataModel);
            }
            System.out.println("VM bits: " + dataModel);

            testUnused = System.getProperty("testUnused") != null;
        } catch (Throwable t) { throw new RuntimeException(t); }
    }

    /** Helper method to return a StackFrame's locals */
    static Object[] invokeGetLocals(StackFrame arg) {
        try {
            return (Object[]) getLocals.invoke(arg);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /*****************
     * DataProviders *
     *****************/

    /** Calls KnownLocalsTester.testLocals* and provides LiveStackFrames */
    @DataProvider
    public static StackFrame[][] knownLocalsProvider() {
        List<StackFrame[]> list = new ArrayList<>(3);
        list.add(new KnownLocalsTester().testLocalsKeepAlive());
        list.add(new KnownLocalsTester().testLocalsKeepAliveArgs(0xA, 'z',
                "himom", 0x3FF00000000L + 0xFFFF, Math.PI));
        if (testUnused) {
            list.add(new KnownLocalsTester().testLocalsUnused());
        }
        return list.toArray(new StackFrame[1][1]);
    }

    /****************
     * Test methods *
     ****************/

    /**
     * Check for expected local values in the LiveStackFrame
     */
    @Test(dataProvider = "knownLocalsProvider")
    public static void checkLocalValues(StackFrame... frames) {
        dumpFramesIfDebug(frames);
        try {
            Stream.of(frames)
                  .filter(f -> KnownLocalsTester.TEST_METHODS.contains(f.getMethodName()))
                  .forEach(LocalsAndOperands::checkFrameLocals);
        } catch (Exception e) { dumpFramesIfNotDebug(frames); throw e; }
    }

    /**
     * Check the locals in the given StackFrame against the expected values.
     */
    private static void checkFrameLocals(StackFrame f) {
        Object[] expectedArray = KnownLocalsTester.LOCAL_VALUES;
        Object[] locals = invokeGetLocals(f);

        for (int i = 0; i < locals.length; i++) {
            Object expected = expectedArray[i];
            Object observed = locals[i];

            if (expected == null) { /* skip nulls in golden values */
                continue;
            } else if (expected instanceof KnownLocalsTester.TwoSlotValue) {
                // confirm integrity of expected values
                assertEquals(expectedArray[i+1], null,
                        "Malformed array of expected values - slot after TwoSlotValue should be null");
                assertLongIsInSlots(locals[i], locals[i+1],
                        ((KnownLocalsTester.TwoSlotValue)expected).value);
                i++; // skip following slot
            } else if (primitiveSlotClass.isInstance(observed)) { // single slot primitive
                assertTrue(primitiveValueEquals(observed, expected),
                        "Local value mismatch: local " + i + " value is " +
                          observed + ", expected " + expected);
            } else if (expected instanceof Class) {
                assertTrue(((Class)expected).isInstance(observed),
                        "Local value mismatch: local " + i + " expected instancof " +
                          expected + " but got " + observed);
            } else if (expected instanceof String) {
                assertEquals(expected, observed, "Local value mismatch: local " +
                        i + " value is " + observed + ", expected " + expected);
            } else {
                throw new RuntimeException("Unrecognized expected local value " +
                        i + ": " + expected);
            }
        }
    }

    /**
     * Sanity check for locals and operands, including testng/jtreg frames
     * using all StackWalker options.
     */
    @Test
    public synchronized void fullStackSanityCheck() throws Throwable {
        if (debug) {
            System.out.println("Running fullStackSanityCheck");
        }
        StackWalker sw = (StackWalker) getExtendedWalker.invoke(null,
                EnumSet.of(SHOW_HIDDEN_FRAMES, SHOW_REFLECT_FRAMES,
                           RETAIN_CLASS_REFERENCE));
        sw.forEach(f -> {
            if (debug) {
                printLocals(f);
            } else {
                try {
                    System.out.println("    " + f + ": " +
                      ((Object[]) getLocals.invoke(f)).length + " locals, " +
                      ((Object[]) getOperands.invoke(f)).length + " operands, " +
                      ((Object[]) getMonitors.invoke(f)).length + " monitors");
                } catch (IllegalAccessException|InvocationTargetException t) {
                    throw new RuntimeException(t);
                }
            }
        });
    }

    /**
     * Test that LiveStackFrames are not provided with the default StackWalker
     * options.
     */
    @Test
    public static void noLocalsSanityCheck() {
        StackWalker sw = StackWalker.getInstance();
        sw.forEach(f -> {
            assertFalse(liveStackFrameClass.isInstance(f),
                        "should not be LiveStackFrame");
        });
    }

    /**
     * Class stack-walking methods with a known set of methods and local variables.
     */
    static class KnownLocalsTester {
        private StackWalker walker;

        KnownLocalsTester() {
            this.walker = extendedWalker;
        }

        /**
         * Perform stackwalk without keeping local variables alive and return an
         * array of the collected StackFrames
         */
        private synchronized StackFrame[] testLocalsUnused() {
            // Unused local variables will become dead
            int x = 0xA;
            char c = 'z'; // 0x7A
            String hi = "himom";
            long l = 0x3FF00000000L + 0xFFFFL;
            double d =  Math.PI;

            return walker.walk(s ->
                s.filter(f -> TEST_METHODS.contains(f.getMethodName()))
                 .toArray(StackFrame[]::new)
            );
        }

        /**
         * Perform stackwalk, keeping local variables alive, and return a list of
         * the collected StackFrames
         */
        private synchronized StackFrame[] testLocalsKeepAlive() {
            int x = 0xA;
            char c = 'z'; // 0x7A
            String hi = "himom";
            long l = 0x3FF00000000L + 0xFFFFL;
            double d =  Math.PI;

            StackFrame[] frames = walker.walk(s ->
                s.filter(f -> TEST_METHODS.contains(f.getMethodName()))
                 .toArray(StackFrame[]::new)
            );

            // Use local variables so they stay alive
            System.out.println("Stayin' alive: "+this+" "+x+" "+c+" "+hi+" "+l+" "+d);
            return frames;
        }

        /**
         * Perform stackwalk, keeping method arguments alive, and return a list of
         * the collected StackFrames
         */
        private synchronized StackFrame[] testLocalsKeepAliveArgs(int x, char c,
                                                                  String hi, long l,
                                                                  double d) {
            StackFrame[] frames = walker.walk(s ->
                s.filter(f -> TEST_METHODS.contains(f.getMethodName()))
                 .toArray(StackFrame[]::new)
            );

            // Use local variables so they stay alive
            System.out.println("Stayin' alive: "+this+" "+x+" "+c+" "+hi+" "+l+" "+d);
            return frames;
        }

        // An expected two-slot local (i.e. long or double)
        static class TwoSlotValue {
            public long value;
            public TwoSlotValue(long value) { this.value = value; }
        }

        // Expected values for locals in KnownLocalsTester.testLocals* methods
        private final static Object[] LOCAL_VALUES = new Object[] {
            LocalsAndOperands.KnownLocalsTester.class,
            Integer.valueOf(0xA),
            Integer.valueOf(0x7A),
            "himom",
            new TwoSlotValue(0x3FF00000000L + 0xFFFFL),
            null, // 2nd slot
            new TwoSlotValue(Double.doubleToRawLongBits(Math.PI)),
            null, // 2nd slot
            Integer.valueOf(0)
        };

        private final static List<String> TEST_METHODS =
                List.of("testLocalsUnused",
                        "testLocalsKeepAlive",
                        "testLocalsKeepAliveArgs");
    }

    /* Simpler tests of long & double arguments */

    @Test
    public static void testUsedLongArg() throws Exception {
        usedLong(LOWER_LONG_VAL);
        usedLong(UPPER_LONG_VAL);
        usedLong(NEG_LONG_VAL);
    }

    private static void usedLong(long longArg) throws Exception {
        StackFrame[] frames = extendedWalker.walk(s ->
            s.filter(f -> "usedLong".equals(f.getMethodName()))
             .toArray(StackFrame[]::new)
        );
        try {
            dumpFramesIfDebug(frames);

            Object[] locals = (Object[]) getLocals.invoke(frames[0]);
            assertLongIsInSlots(locals[0], locals[1], longArg);
            System.out.println("Stayin' alive: " + longArg);
        } catch (Exception t) {
            dumpFramesIfNotDebug(frames);
            throw t;
        }
    }

    @Test
    public static void testUnusedLongArg() throws Exception {
        if (testUnused) {
            unusedLong(NEG_LONG_VAL);
        }
    }

    private static void unusedLong(long longArg) throws Exception {
        StackFrame[] frames = extendedWalker.walk(s ->
            s.filter(f -> "unusedLong".equals(f.getMethodName()))
             .toArray(StackFrame[]::new)
        );
        try {
            dumpFramesIfDebug(frames);

            final Object[] locals = (Object[]) getLocals.invoke(frames[0]);
            assertLongIsInSlots(locals[0], locals[1], NEG_LONG_VAL);
        } catch (Exception t) {
            dumpFramesIfNotDebug(frames);
            throw t;
        }
    }

    @Test
    public static void testUsedDoubleArg() throws Exception {
        usedDouble(LOWER_DOUBLE_VAL);
        usedDouble(UPPER_DOUBLE_VAL);
    }

    private static void usedDouble(double doubleArg) throws Exception {
        StackFrame[] frames = extendedWalker.walk(s ->
            s.filter(f -> "usedDouble".equals(f.getMethodName()))
             .toArray(StackFrame[]::new)
        );
        try {
            dumpFramesIfDebug(frames);

            Object[] locals = (Object[]) getLocals.invoke(frames[0]);
            assertDoubleIsInSlots(locals[0], locals[1], doubleArg);
            System.out.println("Stayin' alive: " + doubleArg);
        } catch (Exception t) {
            dumpFramesIfNotDebug(frames);
            throw t;
        }
    }

    /*******************
     * Utility Methods *
     *******************/

    /**
     * Print stack trace with locals
     */
    public static void dumpStackWithLocals(StackFrame...frames) {
        Stream.of(frames).forEach(LocalsAndOperands::printLocals);
    }

    public static void dumpFramesIfDebug(StackFrame...frames) {
        if (debug) { dumpStackWithLocals(frames); }
    }

    public static void dumpFramesIfNotDebug(StackFrame...frames) {
        if (!debug) { dumpStackWithLocals(frames); }
    }

    /**
     * Print the StackFrame and an indexed list of its locals
     */
    public static void printLocals(StackWalker.StackFrame frame) {
        try {
            System.out.println("Locals for: " + frame);
            Object[] locals = (Object[]) getLocals.invoke(frame);
            for (int i = 0; i < locals.length; i++) {
                String localStr = null;

                if (primitiveSlot64Class.isInstance(locals[i])) {
                    localStr = String.format("0x%X",
                            (Long)primitiveLongValue.invoke(locals[i]));
                } else if (primitiveSlot32Class.isInstance(locals[i])) {
                    localStr = String.format("0x%X",
                            (Integer)primitiveIntValue.invoke(locals[i]));
                } else if (locals[i] != null) {
                    localStr = locals[i].toString();
                }
                System.out.format("  local %d: %s type %s\n", i, localStr, type(locals[i]));
            }

            Object[] operands = (Object[]) getOperands.invoke(frame);
            for (int i = 0; i < operands.length; i++) {
                System.out.format("  operand %d: %s type %s%n", i, operands[i],
                                  type(operands[i]));
            }

            Object[] monitors = (Object[]) getMonitors.invoke(frame);
            for (int i = 0; i < monitors.length; i++) {
                System.out.format("  monitor %d: %s%n", i, monitors[i]);
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String type(Object o) {
        try {
            if (o == null) {
                return "null";
            } else if (primitiveSlotClass.isInstance(o)) {
                int s = (int)primitiveSize.invoke(o);
                return s + "-byte primitive";
            } else {
                return o.getClass().getName();
            }
        } catch(Exception e) { throw new RuntimeException(e); }
    }

    /*
     * Check if the PrimitiveValue "primVal" contains the specified value,
     * either a Long or an Integer.
     */
    static boolean primitiveValueEquals(Object primVal, Object expectedVal) {
        try {
            if (expectedVal instanceof Long) {
                assertFalse(is32bit);
                assertTrue(primitiveSlot64Class.isInstance(primVal));
                assertTrue(8 == (int)primitiveSize.invoke(primVal));
                return Objects.equals(primitiveLongValue.invoke(primVal), expectedVal);
            } else if (expectedVal instanceof Integer) {
                int expectedInt = (Integer)expectedVal;
                if (is32bit) {
                    assertTrue(primitiveSlot32Class.isInstance(primVal),
                            "expected a PrimitiveSlot32 on 32-bit VM");
                    assertTrue(4 == (int)primitiveSize.invoke(primVal));
                    return expectedInt == (int)primitiveIntValue.invoke(primVal);
                } else {
                    assertTrue(primitiveSlot64Class.isInstance(primVal),
                            "expected a PrimitiveSlot64 on 64-bit VM");
                    assertTrue(8 == (int)primitiveSize.invoke(primVal));
                    // Look for int expectedVal in high- or low-order 32 bits
                    long primValLong = (long)primitiveLongValue.invoke(primVal);
                    return (int)(primValLong & 0x00000000FFFFFFFFL) == expectedInt ||
                           (int)(primValLong >>> 32) == expectedInt;
                }
            } else {
                throw new RuntimeException("Called with non-Integer/Long: " + expectedVal);
            }
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * Assert that the expected 2-slot long value is stored somewhere in the
     * pair of slots.
     * Throw exception if long value isn't in the two slots given.
     * Accounts for 32 vs 64 bit, but is lax on endianness (accepts either)
     */
    static void assertLongIsInSlots(Object primVal0, Object primVal1, long expected) {
        try {
            if (is32bit) {
                int upper = (int)(expected & 0xFFFFFFFFL);
                int lower = (int)(expected >> 32);

                if (!((primitiveValueEquals(primVal0, upper) &&
                       primitiveValueEquals(primVal1, lower)) ||
                      (primitiveValueEquals(primVal0, lower) &&
                       primitiveValueEquals(primVal1, upper)))) {
                    throw new RuntimeException(String.format("0x%X and 0x%X of 0x%016X not found in 0x%X and 0x%X",
                            upper, lower, expected,
                            (int)primitiveIntValue.invoke(primVal0),
                            (int)primitiveIntValue.invoke(primVal1)));
                }
            } else {
                if (!(primitiveValueEquals(primVal0, expected) ||
                      primitiveValueEquals(primVal1, expected))) {
                    throw new RuntimeException(String.format("0x%016X not found in 0x%016X or 0x%016X",
                            expected,
                            (long)primitiveLongValue.invoke(primVal0),
                            (long)primitiveLongValue.invoke(primVal1)));
                }
            }
        } catch (IllegalAccessException|InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    static void assertDoubleIsInSlots(Object primVal0, Object primVal1, double expected) {
        assertLongIsInSlots(primVal0, primVal1, Double.doubleToRawLongBits(expected));
    }
}
