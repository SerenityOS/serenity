/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package test.java.lang.invoke;

import org.junit.*;
import test.java.lang.invoke.remote.RemoteExample;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 *
 * @author jrose
 */
public abstract class MethodHandlesTest {

    static final Class<?> THIS_CLASS = MethodHandlesTest.class;
    // How much output?
    static int verbosity = 0;

    static {
        String vstr = System.getProperty(THIS_CLASS.getSimpleName()+".verbosity");
        if (vstr == null)
            vstr = System.getProperty(THIS_CLASS.getName()+".verbosity");
        if (vstr != null)  verbosity = Integer.parseInt(vstr);
    }

    // Set this true during development if you want to fast-forward to
    // a particular new, non-working test.  Tests which are known to
    // work (or have recently worked) test this flag and return on true.
    static final boolean CAN_SKIP_WORKING;

    static {
        String vstr = System.getProperty(THIS_CLASS.getSimpleName()+".CAN_SKIP_WORKING");
        if (vstr == null)
            vstr = System.getProperty(THIS_CLASS.getName()+".CAN_SKIP_WORKING");
        CAN_SKIP_WORKING = Boolean.parseBoolean(vstr);
    }

    // Set 'true' to do about 15x fewer tests, especially those redundant with RicochetTest.
    // This might be useful with -Xcomp stress tests that compile all method handles.
    static boolean CAN_TEST_LIGHTLY = Boolean.getBoolean(THIS_CLASS.getName()+".CAN_TEST_LIGHTLY");

    static final int MAX_ARG_INCREASE = 3;

    String testName;
    static int allPosTests, allNegTests;
    int posTests, negTests;

    @After
    public void printCounts() {
        if (verbosity >= 2 && (posTests | negTests) != 0) {
            System.out.println();
            if (posTests != 0)  System.out.println("=== "+testName+": "+posTests+" positive test cases run");
            if (negTests != 0)  System.out.println("=== "+testName+": "+negTests+" negative test cases run");
            allPosTests += posTests;
            allNegTests += negTests;
            posTests = negTests = 0;
        }
    }

    void countTest(boolean positive) {
        if (positive) ++posTests;
        else          ++negTests;
    }

    void countTest() { countTest(true); }

    void startTest(String name) {
        if (testName != null)  printCounts();
        if (verbosity >= 1)
            System.out.println(name);
        posTests = negTests = 0;
        testName = name;
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        calledLog.clear();
        calledLog.add(null);
        nextArgVal = INITIAL_ARG_VAL;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        int posTests = allPosTests, negTests = allNegTests;
        if (verbosity >= 0 && (posTests | negTests) != 0) {
            System.out.println();
            if (posTests != 0)  System.out.println("=== "+posTests+" total positive test cases");
            if (negTests != 0)  System.out.println("=== "+negTests+" total negative test cases");
        }
    }

    static List<Object> calledLog = new ArrayList<>();

    static Object logEntry(String name, Object... args) {
        return Arrays.asList(name, Arrays.asList(args));
    }

    public static Object called(String name, Object... args) {
        Object entry = logEntry(name, args);
        calledLog.add(entry);
        return entry;
    }

    static void assertCalled(String name, Object... args) {
        Object expected = logEntry(name, args);
        Object actual   = calledLog.get(calledLog.size() - 1);
        if (expected.equals(actual) && verbosity < 9)  return;
        System.out.println("assertCalled "+name+":");
        System.out.println("expected:   "+deepToString(expected));
        System.out.println("actual:     "+actual);
        System.out.println("ex. types:  "+getClasses(expected));
        System.out.println("act. types: "+getClasses(actual));
        assertEquals("previous method call", expected, actual);
    }

    static void printCalled(MethodHandle target, String name, Object... args) {
        if (verbosity >= 3)
            System.out.println("calling MH="+target+" to "+name+deepToString(args));
    }

    static String deepToString(Object x) {
        if (x == null)  return "null";
        if (x instanceof Collection)
            x = ((Collection)x).toArray();
        if (x instanceof Object[]) {
            Object[] ax = (Object[]) x;
            ax = Arrays.copyOf(ax, ax.length, Object[].class);
            for (int i = 0; i < ax.length; i++)
                ax[i] = deepToString(ax[i]);
            x = Arrays.deepToString(ax);
        }
        if (x.getClass().isArray())
            try {
                x = Arrays.class.getMethod("toString", x.getClass()).invoke(null, x);
            } catch (ReflectiveOperationException ex) { throw new Error(ex); }
        assert(!(x instanceof Object[]));
        return x.toString();
    }

    static Object castToWrapper(Object value, Class<?> dst) {
        Object wrap = null;
        if (value instanceof Number)
            wrap = castToWrapperOrNull(((Number)value).longValue(), dst);
        if (value instanceof Character)
            wrap = castToWrapperOrNull((char)(Character)value, dst);
        if (wrap != null)  return wrap;
        return dst.cast(value);
    }

    @SuppressWarnings("cast")  // primitive cast to (long) is part of the pattern
    static Object castToWrapperOrNull(long value, Class<?> dst) {
        if (dst == int.class || dst == Integer.class)
            return (int)(value);
        if (dst == long.class || dst == Long.class)
            return (long)(value);
        if (dst == char.class || dst == Character.class)
            return (char)(value);
        if (dst == short.class || dst == Short.class)
            return (short)(value);
        if (dst == float.class || dst == Float.class)
            return (float)(value);
        if (dst == double.class || dst == Double.class)
            return (double)(value);
        if (dst == byte.class || dst == Byte.class)
            return (byte)(value);
        if (dst == boolean.class || dst == boolean.class)
            return ((value % 29) & 1) == 0;
        return null;
    }

    static final int ONE_MILLION = (1000*1000),  // first int value
                     TEN_BILLION = (10*1000*1000*1000),  // scale factor to reach upper 32 bits
                     INITIAL_ARG_VAL = ONE_MILLION << 1;  // <<1 makes space for sign bit;
    static long nextArgVal;

    static long nextArg(boolean moreBits) {
        long val = nextArgVal++;
        long sign = -(val & 1); // alternate signs
        val >>= 1;
        if (moreBits)
            // Guarantee some bits in the high word.
            // In any case keep the decimal representation simple-looking,
            // with lots of zeroes, so as not to make the printed decimal
            // strings unnecessarily noisy.
            val += (val % ONE_MILLION) * TEN_BILLION;
        return val ^ sign;
    }

    static int nextArg() {
        // Produce a 32-bit result something like ONE_MILLION+(smallint).
        // Example: 1_000_042.
        return (int) nextArg(false);
    }

    static long nextArg(Class<?> kind) {
        if (kind == long.class   || kind == Long.class ||
            kind == double.class || kind == Double.class)
            // produce a 64-bit result something like
            // ((TEN_BILLION+1) * (ONE_MILLION+(smallint)))
            // Example: 10_000_420_001_000_042.
            return nextArg(true);
        return (long) nextArg();
    }

    static Object randomArg(Class<?> param) {
        Object wrap = castToWrapperOrNull(nextArg(param), param);
        if (wrap != null) {
            return wrap;
        }
        //import sun.invoke.util.Wrapper;
        //Wrapper wrap = Wrapper.forBasicType(dst);
        //if (wrap == Wrapper.OBJECT && Wrapper.isWrapperType(dst))
        //   wrap = Wrapper.forWrapperType(dst);
        //   if (wrap != Wrapper.OBJECT)
        //       return wrap.wrap(nextArg++);
        if (param.isInterface()) {
            for (Class<?> c : param.getClasses()) {
                if (param.isAssignableFrom(c) && !c.isInterface())
                    { param = c; break; }
            }
        }
        if (param.isArray()) {
            Class<?> ctype = param.getComponentType();
            Object arg = Array.newInstance(ctype, 2);
            Array.set(arg, 0, randomArg(ctype));
            return arg;
        }
        if (param.isInterface() && param.isAssignableFrom(List.class))
            return Arrays.asList("#"+nextArg());
        if (param.isInterface() || param.isAssignableFrom(String.class))
            return "#"+nextArg();
        else
            try {
                return param.newInstance();
            } catch (InstantiationException | IllegalAccessException ex) {
            }
        return null;  // random class not Object, String, Integer, etc.
    }

    static Object[] randomArgs(Class<?>... params) {
        Object[] args = new Object[params.length];
        for (int i = 0; i < args.length; i++)
            args[i] = randomArg(params[i]);
        return args;
    }

    static Object[] randomArgs(int nargs, Class<?> param) {
        Object[] args = new Object[nargs];
        for (int i = 0; i < args.length; i++)
            args[i] = randomArg(param);
        return args;
    }

    static Object[] randomArgs(List<Class<?>> params) {
        return randomArgs(params.toArray(new Class<?>[params.size()]));
    }

    @SafeVarargs @SuppressWarnings("varargs")
    static <T, E extends T> T[] array(Class<T[]> atype, E... a) {
        return Arrays.copyOf(a, a.length, atype);
    }

    @SafeVarargs @SuppressWarnings("varargs")
    static <T> T[] cat(T[] a, T... b) {
        int alen = a.length, blen = b.length;
        if (blen == 0)  return a;
        T[] c = Arrays.copyOf(a, alen + blen);
        System.arraycopy(b, 0, c, alen, blen);
        return c;
    }

    static Integer[] boxAll(int... vx) {
        Integer[] res = new Integer[vx.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = vx[i];
        }
        return res;
    }

    static Object getClasses(Object x) {
        if (x == null)  return x;
        if (x instanceof String)  return x;  // keep the name
        if (x instanceof List) {
            // recursively report classes of the list elements
            Object[] xa = ((List)x).toArray();
            for (int i = 0; i < xa.length; i++)
                xa[i] = getClasses(xa[i]);
            return Arrays.asList(xa);
        }
        return x.getClass().getSimpleName();
    }

    /** Return lambda(arg...[arity]) { new Object[]{ arg... } } */
    static MethodHandle varargsList(int arity) {
        return ValueConversions.varargsList(arity);
    }

    /** Return lambda(arg...[arity]) { Arrays.asList(arg...) } */
    static MethodHandle varargsArray(int arity) {
        return ValueConversions.varargsArray(arity);
    }

    static MethodHandle varargsArray(Class<?> arrayType, int arity) {
        return ValueConversions.varargsArray(arrayType, arity);
    }

    /** Variation of varargsList, but with the given rtype. */
    static MethodHandle varargsList(int arity, Class<?> rtype) {
        MethodHandle list = varargsList(arity);
        MethodType listType = list.type().changeReturnType(rtype);
        if (List.class.isAssignableFrom(rtype) || rtype == void.class || rtype == Object.class) {
            // OK
        } else if (rtype.isAssignableFrom(String.class)) {
            if (LIST_TO_STRING == null)
                try {
                    LIST_TO_STRING = PRIVATE.findStatic(PRIVATE.lookupClass(), "listToString",
                                                        MethodType.methodType(String.class, List.class));
                } catch (NoSuchMethodException | IllegalAccessException ex) { throw new RuntimeException(ex); }
            list = MethodHandles.filterReturnValue(list, LIST_TO_STRING);
        } else if (rtype.isPrimitive()) {
            if (LIST_TO_INT == null)
                try {
                    LIST_TO_INT = PRIVATE.findStatic(PRIVATE.lookupClass(), "listToInt",
                                                     MethodType.methodType(int.class, List.class));
                } catch (NoSuchMethodException | IllegalAccessException ex) { throw new RuntimeException(ex); }
            list = MethodHandles.filterReturnValue(list, LIST_TO_INT);
            list = MethodHandles.explicitCastArguments(list, listType);
        } else {
            throw new RuntimeException("varargsList: "+rtype);
        }
        return list.asType(listType);
    }

    /** Variation of varargsList, but with the given ptypes and rtype. */
    static MethodHandle varargsList(List<Class<?>> ptypes, Class<?> rtype) {
        MethodHandle list = varargsList(ptypes.size(), rtype);
        return list.asType(MethodType.methodType(rtype, ptypes));
    }

    private static MethodHandle LIST_TO_STRING, LIST_TO_INT;
    private static String listToString(List<?> x) { return x.toString(); }
    private static int listToInt(List<?> x) { return x.toString().hashCode(); }

    static MethodHandle changeArgTypes(MethodHandle target, Class<?> argType) {
        return changeArgTypes(target, 0, 999, argType);
    }

    static MethodHandle changeArgTypes(MethodHandle target,
            int beg, int end, Class<?> argType) {
        MethodType targetType = target.type();
        end = Math.min(end, targetType.parameterCount());
        ArrayList<Class<?>> argTypes = new ArrayList<>(targetType.parameterList());
        Collections.fill(argTypes.subList(beg, end), argType);
        MethodType ttype2 = MethodType.methodType(targetType.returnType(), argTypes);
        return target.asType(ttype2);
    }

    static MethodHandle addTrailingArgs(MethodHandle target, int nargs, Class<?> argClass) {
        int targetLen = target.type().parameterCount();
        int extra = (nargs - targetLen);
        if (extra <= 0)  return target;
        List<Class<?>> fakeArgs = Collections.<Class<?>>nCopies(extra, argClass);
        return MethodHandles.dropArguments(target, targetLen, fakeArgs);
    }

    // This lookup is good for all members in and under MethodHandlesTest.
    static final Lookup PRIVATE = MethodHandles.lookup();
    // This lookup is good for package-private members but not private ones.
    static final Lookup PACKAGE = PackageSibling.lookup();
    // This lookup is good for public members and protected members of PubExample
    static final Lookup SUBCLASS = RemoteExample.lookup();
    // This lookup is good only for public members in exported packages.
    static final Lookup PUBLIC  = MethodHandles.publicLookup();

    // Subject methods...
    static class Example implements IntExample {
        final String name;
        public Example() { name = "Example#"+nextArg(); }
        protected Example(String name) { this.name = name; }
        @SuppressWarnings("LeakingThisInConstructor")
        protected Example(int x) { this(); called("protected <init>", this, x); }
        //Example(Void x) { does not exist; lookup elicts NoSuchMethodException }
        @Override public String toString() { return name; }

        public void            v0()     { called("v0", this); }
        protected void         pro_v0() { called("pro_v0", this); }
        void                   pkg_v0() { called("pkg_v0", this); }
        private void           pri_v0() { called("pri_v0", this); }
        public static void     s0()     { called("s0"); }
        protected static void  pro_s0() { called("pro_s0"); }
        static void            pkg_s0() { called("pkg_s0"); }
        private static void    pri_s0() { called("pri_s0"); }

        public Object          v1(Object x) { return called("v1", this, x); }
        public Object          v2(Object x, Object y) { return called("v2", this, x, y); }
        public Object          v2(Object x, int    y) { return called("v2", this, x, y); }
        public Object          v2(int    x, Object y) { return called("v2", this, x, y); }
        public Object          v2(int    x, int    y) { return called("v2", this, x, y); }
        public static Object   s1(Object x) { return called("s1", x); }
        public static Object   s2(int x)    { return called("s2", x); }
        public static Object   s3(long x)   { return called("s3", x); }
        public static Object   s4(int x, int y) { return called("s4", x, y); }
        public static Object   s5(long x, int y) { return called("s5", x, y); }
        public static Object   s6(int x, long y) { return called("s6", x, y); }
        public static Object   s7(float x, double y) { return called("s7", x, y); }

        // for testing findConstructor:
        public Example(String x, int y) { this.name = x+y; called("Example.<init>", x, y); }
        public Example(int x, String y) { this.name = x+y; called("Example.<init>", x, y); }
        public Example(int x, int    y) { this.name = x+""+y; called("Example.<init>", x, y); }
        public Example(int x, long   y) { this.name = x+""+y; called("Example.<init>", x, y); }
        public Example(int x, float  y) { this.name = x+""+y; called("Example.<init>", x, y); }
        public Example(int x, double y) { this.name = x+""+y; called("Example.<init>", x, y); }
        public Example(int x, int    y, int z) { this.name = x+""+y+""+z; called("Example.<init>", x, y, z); }
        public Example(int x, int    y, int z, int a) { this.name = x+""+y+""+z+""+a; called("Example.<init>", x, y, z, a); }

        static final Lookup EXAMPLE = MethodHandles.lookup();  // for testing findSpecial
    }

    static final Lookup EXAMPLE = Example.EXAMPLE;
    public static class PubExample extends Example {
        public PubExample() { this("PubExample"); }
        protected PubExample(String prefix) { super(prefix+"#"+nextArg()); }
        protected void         pro_v0() { called("Pub/pro_v0", this); }
        protected static void  pro_s0() { called("Pub/pro_s0"); }
    }

    static class SubExample extends Example {
        @Override public void  v0()     { called("Sub/v0", this); }
        @Override void         pkg_v0() { called("Sub/pkg_v0", this); }
        @SuppressWarnings("LeakingThisInConstructor")
        private      SubExample(int x)  { called("<init>", this, x); }
        public SubExample() { super("SubExample#"+nextArg()); }
    }

    public static interface IntExample {
        public void            v0();
        public default void    vd() { called("vd", this); }
        public static class Impl implements IntExample {
            public void        v0()     { called("Int/v0", this); }
            final String name;
            public Impl() { name = "Impl#"+nextArg(); }
            @Override public String toString() { return name; }
        }
    }

    static interface SubIntExample extends IntExample { }

    static final Object[][][] ACCESS_CASES = {
        { { false, PUBLIC }, { false, SUBCLASS }, { false, PACKAGE }, { false, PRIVATE }, { false, EXAMPLE } }, //[0]: all false
        { { false, PUBLIC }, { false, SUBCLASS }, { false, PACKAGE }, { true, PRIVATE }, { true, EXAMPLE } }, //[1]: only PRIVATE
        { { false, PUBLIC }, { false, SUBCLASS }, { true, PACKAGE }, { true, PRIVATE }, { true, EXAMPLE } }, //[2]: PUBLIC false
        { { false, PUBLIC }, { true, SUBCLASS }, { true, PACKAGE }, { true, PRIVATE }, { true, EXAMPLE } }, //[3]: subclass OK
        { { true, PUBLIC }, { true, SUBCLASS }, { true, PACKAGE }, { true, PRIVATE }, { true, EXAMPLE } }, //[4]: all true
    };

    static Object[][] accessCases(Class<?> defc, String name, boolean isSpecial) {
        Object[][] cases;
        if (name.contains("pri_") || isSpecial) {
            cases = ACCESS_CASES[1]; // PRIVATE only
        } else if (name.contains("pkg_") || !Modifier.isPublic(defc.getModifiers())) {
            cases = ACCESS_CASES[2]; // not PUBLIC
        } else if (name.contains("pro_")) {
            cases = ACCESS_CASES[3]; // PUBLIC class, protected member
        } else {
            assertTrue(name.indexOf('_') < 0 || name.contains("fin_"));
            boolean pubc = Modifier.isPublic(defc.getModifiers());
            if (pubc)
                cases = ACCESS_CASES[4]; // all access levels
            else
                cases = ACCESS_CASES[2]; // PACKAGE but not PUBLIC
        }
        if (defc != Example.class && cases[cases.length-1][1] == EXAMPLE)
            cases = Arrays.copyOfRange(cases, 0, cases.length-1);
        return cases;
    }

    static Object[][] accessCases(Class<?> defc, String name) {
        return accessCases(defc, name, false);
    }

    static Lookup maybeMoveIn(Lookup lookup, Class<?> defc) {
        if (lookup == PUBLIC || lookup == SUBCLASS || lookup == PACKAGE)
            // external views stay external
            return lookup;
        return lookup.in(defc);
    }

    /** Is findVirtual (etc.) of "&lt;init&lt;" supposed to elicit a NoSuchMethodException? */
    static final boolean INIT_REF_CAUSES_NSME = true;

    static void assertExceptionClass(Class<? extends Throwable> expected,
                                     Throwable actual) {
        if (expected.isInstance(actual))  return;
        actual.printStackTrace();
        assertEquals(expected, actual.getClass());
    }

    static final boolean DEBUG_METHOD_HANDLE_NAMES = Boolean.getBoolean("java.lang.invoke.MethodHandle.DEBUG_NAMES");

    // rough check of name string
    static void assertNameStringContains(MethodHandle x, String s) {
        if (!DEBUG_METHOD_HANDLE_NAMES) {
            // ignore s
            assertEquals("MethodHandle"+x.type(), x.toString());
            return;
        }
        if (x.toString().contains(s))  return;
        assertEquals(s, x);
    }

    public static class HasFields {
        boolean iZ = false;
        byte iB = (byte)'B';
        short iS = (short)'S';
        char iC = 'C';
        int iI = 'I';
        long iJ = 'J';
        float iF = 'F';
        double iD = 'D';
        static boolean sZ = true;
        static byte sB = 1+(byte)'B';
        static short sS = 1+(short)'S';
        static char sC = 1+'C';
        static int sI = 1+'I';
        static long sJ = 1+'J';
        static float sF = 1+'F';
        static double sD = 1+'D';

        // final fields
        final boolean fiZ = false;
        final byte fiB = 2+(byte)'B';
        final short fiS = 2+(short)'S';
        final char fiC = 2+'C';
        final int fiI = 2+'I';
        final long fiJ = 2+'J';
        final float fiF = 2+'F';
        final double fiD = 2+'D';
        final static boolean fsZ = false;
        final static byte fsB = 3+(byte)'B';
        final static short fsS = 3+(short)'S';
        final static char fsC = 3+'C';
        final static int fsI = 3+'I';
        final static long fsJ = 3+'J';
        final static float fsF = 3+'F';
        final static double fsD = 3+'D';

        Object iL = 'L';
        String iR = "iR";
        static Object sL = 1+'L';
        static String sR = "sR";
        final Object fiL = 2+'L';
        final String fiR = "fiR";
        final static Object fsL = 3+'L';
        final static String fsR = "fsR";

        static final ArrayList<Object[]> STATIC_FIELD_CASES = new ArrayList<>();
        static final ArrayList<Object[]> INSTANCE_FIELD_CASES = new ArrayList<>();
        static {
            Object types[][] = {
                {'L',Object.class}, {'R',String.class},
                {'I',int.class}, {'J',long.class},
                {'F',float.class}, {'D',double.class},
                {'Z',boolean.class}, {'B',byte.class},
                {'S',short.class}, {'C',char.class},
            };
            HasFields fields = new HasFields();
            for (Object[] t : types) {
                for (int kind = 0; kind <= 1; kind++) {
                    boolean isStatic = (kind != 0);
                    ArrayList<Object[]> cases = isStatic ? STATIC_FIELD_CASES : INSTANCE_FIELD_CASES;
                    char btc = (Character)t[0];
                    String fname = (isStatic ? "s" : "i") + btc;
                    String finalFname = (isStatic ? "fs" : "fi") + btc;
                    Class<?> type = (Class<?>) t[1];
                    // non-final field
                    Field nonFinalField = getField(fname, type);
                    Object value = getValue(fields, nonFinalField);
                    if (type == float.class) {
                        float v = 'F';
                        if (isStatic)  v++;
                        assertTrue(value.equals(v));
                    }
                    assertTrue(isStatic == (Modifier.isStatic(nonFinalField.getModifiers())));
                    cases.add(new Object[]{ nonFinalField, value });

                    // setAccessible(true) on final field but static final field only has read access
                    Field finalField = getField(finalFname, type);
                    Object fvalue = getValue(fields, finalField);
                    finalField.setAccessible(true);
                    assertTrue(isStatic == (Modifier.isStatic(finalField.getModifiers())));
                    cases.add(new Object[]{ finalField, fvalue, Error.class});
                }
            }
            INSTANCE_FIELD_CASES.add(new Object[]{ new Object[]{ false, HasFields.class, "bogus_fD", double.class }, Error.class });
            STATIC_FIELD_CASES.add(new Object[]{ new Object[]{ true,  HasFields.class, "bogus_sL", Object.class }, Error.class });
        }

        private static Field getField(String name, Class<?> type) {
            try {
                Field field = HasFields.class.getDeclaredField(name);
                assertTrue(name.equals(field.getName()));
                assertTrue(type.equals(field.getType()));
                return field;
            } catch (NoSuchFieldException | SecurityException ex) {
                throw new InternalError("no field HasFields."+name);
            }
        }

        private static Object getValue(Object o, Field field) {
            try {
                return field.get(o);
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new InternalError("cannot fetch field HasFields."+field.getName());
            }
        }

        static Object[][] testCasesFor(int testMode) {
            Stream<Object[]> cases;
            if ((testMode & TEST_UNREFLECT) != 0) {
                cases = Stream.concat(STATIC_FIELD_CASES.stream(), INSTANCE_FIELD_CASES.stream());
            } else if ((testMode & TEST_FIND_STATIC) != 0) {
                cases = STATIC_FIELD_CASES.stream();
            } else if ((testMode & TEST_FIND_FIELD) != 0) {
                cases = INSTANCE_FIELD_CASES.stream();
            } else {
                throw new InternalError("unexpected test mode: " + testMode);
            }
            return cases.map(c -> mapTestCase(testMode, c)).toArray(Object[][]::new);

        }

        private static Object[] mapTestCase(int testMode, Object[] c) {
            // non-final fields (2-element) and final fields (3-element) if not TEST_SETTER
            if (c.length == 2 || (testMode & TEST_SETTER) == 0)
                return c;

            // final fields (3-element)
            assertTrue((testMode & TEST_SETTER) != 0 && c[0] instanceof Field && c[2] == Error.class);
            if ((testMode & TEST_UNREFLECT) == 0)
                return new Object[]{ c[0], c[2]};   // negative test case; can't set on final fields

            // unreflectSetter grants write access on instance final field if accessible flag is true
            // hence promote the negative test case to positive test case
            Field f = (Field) c[0];
            int mods = f.getModifiers();
            if (!Modifier.isFinal(mods) || (!Modifier.isStatic(mods) && f.isAccessible())) {
                // positive test case
                return new Object[]{ c[0], c[1] };
            } else {
                // otherwise, negative test case
                return new Object[]{ c[0], c[2]};
            }
        }
    }

    static final int TEST_UNREFLECT = 1, TEST_FIND_FIELD = 2, TEST_FIND_STATIC = 3, TEST_SETTER = 0x10, TEST_BOUND = 0x20, TEST_NPE = 0x40;

    static boolean testModeMatches(int testMode, boolean isStatic) {
        switch (testMode) {
        case TEST_FIND_STATIC:          return isStatic;
        case TEST_FIND_FIELD:           return !isStatic;
        case TEST_UNREFLECT:            return true;  // unreflect matches both
        }
        throw new InternalError("testMode="+testMode);
    }

    static class Callee {
        static Object id() { return called("id"); }
        static Object id(Object x) { return called("id", x); }
        static Object id(Object x, Object y) { return called("id", x, y); }
        static Object id(Object x, Object y, Object z) { return called("id", x, y, z); }
        static Object id(Object... vx) { return called("id", vx); }
        static MethodHandle ofType(int n) {
            return ofType(Object.class, n);
        }
        static MethodHandle ofType(Class<?> rtype, int n) {
            if (n == -1)
                return ofType(MethodType.methodType(rtype, Object[].class));
            return ofType(MethodType.genericMethodType(n).changeReturnType(rtype));
        }
        static MethodHandle ofType(Class<?> rtype, Class<?>... ptypes) {
            return ofType(MethodType.methodType(rtype, ptypes));
        }
        static MethodHandle ofType(MethodType type) {
            Class<?> rtype = type.returnType();
            String pfx = "";
            if (rtype != Object.class)
                pfx = rtype.getSimpleName().substring(0, 1).toLowerCase();
            String name = pfx+"id";
            try {
                return PRIVATE.findStatic(Callee.class, name, type);
            } catch (NoSuchMethodException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    static Object invokee(Object... args) {
        return called("invokee", args).hashCode();
    }

    protected static final String MISSING_ARG = "missingArg";
    protected static final String MISSING_ARG_2 = "missingArg#2";

    static Object targetIfEquals() {
        return called("targetIfEquals");
    }

    static Object fallbackIfNotEquals() {
        return called("fallbackIfNotEquals");
    }

    static Object targetIfEquals(Object x) {
        assertEquals(x, MISSING_ARG);
        return called("targetIfEquals", x);
    }

    static Object fallbackIfNotEquals(Object x) {
        assertFalse(x.toString(), x.equals(MISSING_ARG));
        return called("fallbackIfNotEquals", x);
    }

    static Object targetIfEquals(Object x, Object y) {
        assertEquals(x, y);
        return called("targetIfEquals", x, y);
    }

    static Object fallbackIfNotEquals(Object x, Object y) {
        assertFalse(x.toString(), x.equals(y));
        return called("fallbackIfNotEquals", x, y);
    }

    static Object targetIfEquals(Object x, Object y, Object z) {
        assertEquals(x, y);
        return called("targetIfEquals", x, y, z);
    }

    static Object fallbackIfNotEquals(Object x, Object y, Object z) {
        assertFalse(x.toString(), x.equals(y));
        return called("fallbackIfNotEquals", x, y, z);
    }

    static boolean loopIntPred(int a) {
        if (verbosity >= 5) {
            System.out.println("int pred " + a + " -> " + (a < 7));
        }
        return a < 7;
    }

    static boolean loopDoublePred(int a, double b) {
        if (verbosity >= 5) {
            System.out.println("double pred (a=" + a + ") " + b + " -> " + (b > 0.5));
        }
        return b > 0.5;
    }

    static boolean loopStringPred(int a, double b, String c) {
        if (verbosity >= 5) {
            System.out.println("String pred (a=" + a + ",b=" + b + ") " + c + " -> " + (c.length() <= 9));
        }
        return c.length() <= 9;
    }

    static int loopIntStep(int a) {
        if (verbosity >= 5) {
            System.out.println("int step " + a + " -> " + (a + 1));
        }
        return a + 1;
    }

    static double loopDoubleStep(int a, double b) {
        if (verbosity >= 5) {
            System.out.println("double step (a=" + a + ") " + b + " -> " + (b / 2.0));
        }
        return b / 2.0;
    }

    static String loopStringStep(int a, double b, String c) {
        if (verbosity >= 5) {
            System.out.println("String step (a=" + a + ",b=" + b + ") " + c + " -> " + (c + a));
        }
        return c + a;
    }

    static void vtarget(String[] a) {
        // naught, akin to identity
    }

    static void vtargetThrow(String[] a) throws Exception {
        throw new Exception("thrown");
    }

    static void vcleanupPassThrough(Throwable t, String[] a) {
        assertNull(t);
        // naught, akin to identity
    }

    static void vcleanupAugment(Throwable t, String[] a) {
        assertNull(t);
        a[0] = "augmented";
    }

    static void vcleanupCatch(Throwable t, String[] a) {
        assertNotNull(t);
        a[0] = "caught";
    }

    static void vcleanupThrow(Throwable t, String[] a) throws Exception {
        assertNotNull(t);
        throw new Exception("rethrown");
    }
}
// Local abbreviated copy of sun.invoke.util.ValueConversions
// This guy tests access from outside the same package member, but inside
// the package itself.
class ValueConversions {
    private static final Lookup IMPL_LOOKUP = MethodHandles.lookup();
    private static final Object[] NO_ARGS_ARRAY = {};
    private static Object[] makeArray(Object... args) { return args; }
    private static Object[] array() { return NO_ARGS_ARRAY; }
    private static Object[] array(Object a0)
                { return makeArray(a0); }
    private static Object[] array(Object a0, Object a1)
                { return makeArray(a0, a1); }
    private static Object[] array(Object a0, Object a1, Object a2)
                { return makeArray(a0, a1, a2); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3)
                { return makeArray(a0, a1, a2, a3); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4)
                { return makeArray(a0, a1, a2, a3, a4); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5)
                { return makeArray(a0, a1, a2, a3, a4, a5); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7, a8); }
    private static Object[] array(Object a0, Object a1, Object a2, Object a3,
                                  Object a4, Object a5, Object a6, Object a7,
                                  Object a8, Object a9)
                { return makeArray(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); }

    static MethodHandle[] makeArrays() {
        ArrayList<MethodHandle> arrays = new ArrayList<>();
        MethodHandles.Lookup lookup = IMPL_LOOKUP;
        for (;;) {
            int nargs = arrays.size();
            MethodType type = MethodType.genericMethodType(nargs).changeReturnType(Object[].class);
            String name = "array";
            MethodHandle array = null;
            try {
                array = lookup.findStatic(ValueConversions.class, name, type);
            } catch (ReflectiveOperationException ex) {
                // break from loop!
            }
            if (array == null)  break;
            arrays.add(array);
        }
        assertTrue(arrays.size() == 11);  // current number of methods
        return arrays.toArray(new MethodHandle[0]);
    }

    static final MethodHandle[] ARRAYS = makeArrays();

    /** Return a method handle that takes the indicated number of Object
     *  arguments and returns an Object array of them, as if for varargs.
     */
    public static MethodHandle varargsArray(int nargs) {
        if (nargs < ARRAYS.length)
            return ARRAYS[nargs];
        return MethodHandles.identity(Object[].class).asCollector(Object[].class, nargs);
    }

    public static MethodHandle varargsArray(Class<?> arrayType, int nargs) {
        Class<?> elemType = arrayType.getComponentType();
        MethodType vaType = MethodType.methodType(arrayType, Collections.<Class<?>>nCopies(nargs, elemType));
        MethodHandle mh = varargsArray(nargs);
        if (arrayType != Object[].class)
            mh = MethodHandles.filterReturnValue(mh, CHANGE_ARRAY_TYPE.bindTo(arrayType));
        return mh.asType(vaType);
    }

    static Object changeArrayType(Class<?> arrayType, Object[] a) {
        Class<?> elemType = arrayType.getComponentType();
        if (!elemType.isPrimitive())
            return Arrays.copyOf(a, a.length, arrayType.asSubclass(Object[].class));
        Object b = java.lang.reflect.Array.newInstance(elemType, a.length);
        for (int i = 0; i < a.length; i++)
            java.lang.reflect.Array.set(b, i, a[i]);
        return b;
    }

    private static final MethodHandle CHANGE_ARRAY_TYPE;
    static {
        try {
            CHANGE_ARRAY_TYPE = IMPL_LOOKUP.findStatic(ValueConversions.class, "changeArrayType",
                                                       MethodType.methodType(Object.class, Class.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException ex) {
            Error err = new InternalError("uncaught exception");
            err.initCause(ex);
            throw err;
        }
    }

    private static final List<Object> NO_ARGS_LIST = Arrays.asList(NO_ARGS_ARRAY);
    private static List<Object> makeList(Object... args) { return Arrays.asList(args); }
    private static List<Object> list() { return NO_ARGS_LIST; }
    private static List<Object> list(Object a0)
                { return makeList(a0); }
    private static List<Object> list(Object a0, Object a1)
                { return makeList(a0, a1); }
    private static List<Object> list(Object a0, Object a1, Object a2)
                { return makeList(a0, a1, a2); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3)
                { return makeList(a0, a1, a2, a3); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4)
                { return makeList(a0, a1, a2, a3, a4); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4, Object a5)
                { return makeList(a0, a1, a2, a3, a4, a5); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4, Object a5, Object a6)
                { return makeList(a0, a1, a2, a3, a4, a5, a6); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4, Object a5, Object a6, Object a7)
                { return makeList(a0, a1, a2, a3, a4, a5, a6, a7); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4, Object a5, Object a6, Object a7,
                                     Object a8)
                { return makeList(a0, a1, a2, a3, a4, a5, a6, a7, a8); }
    private static List<Object> list(Object a0, Object a1, Object a2, Object a3,
                                     Object a4, Object a5, Object a6, Object a7,
                                     Object a8, Object a9)
                { return makeList(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); }

    static MethodHandle[] makeLists() {
        ArrayList<MethodHandle> lists = new ArrayList<>();
        MethodHandles.Lookup lookup = IMPL_LOOKUP;
        for (;;) {
            int nargs = lists.size();
            MethodType type = MethodType.genericMethodType(nargs).changeReturnType(List.class);
            String name = "list";
            MethodHandle list = null;
            try {
                list = lookup.findStatic(ValueConversions.class, name, type);
            } catch (ReflectiveOperationException ex) {
                // break from loop!
            }
            if (list == null)  break;
            lists.add(list);
        }
        assertTrue(lists.size() == 11);  // current number of methods
        return lists.toArray(new MethodHandle[0]);
    }

    static final MethodHandle[] LISTS = makeLists();
    static final MethodHandle AS_LIST;

    static {
        try {
            AS_LIST = IMPL_LOOKUP.findStatic(Arrays.class, "asList", MethodType.methodType(List.class, Object[].class));
        } catch (NoSuchMethodException | IllegalAccessException ex) { throw new RuntimeException(ex); }
    }

    /** Return a method handle that takes the indicated number of Object
     *  arguments and returns List.
     */
    public static MethodHandle varargsList(int nargs) {
        if (nargs < LISTS.length)
            return LISTS[nargs];
        return AS_LIST.asCollector(Object[].class, nargs);
    }
}
// This guy tests access from outside the same package member, but inside
// the package itself.
class PackageSibling {
    static Lookup lookup() {
        return MethodHandles.lookup();
    }
}
